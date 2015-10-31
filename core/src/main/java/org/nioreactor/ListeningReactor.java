/*
 * Copyright 2014 Pedro Ribeiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nioreactor;

import org.nioreactor.util.Preconditions;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.nioreactor.SocketOption.SO_KEEPALIVE;
import static org.nioreactor.SocketOption.SO_LINGER;
import static org.nioreactor.SocketOption.SO_RCVBUF;
import static org.nioreactor.SocketOption.SO_REUSEADDR;
import static org.nioreactor.SocketOption.SO_SNDBUF;
import static org.nioreactor.SocketOption.SO_TIMEOUT;
import static org.nioreactor.SocketOption.TCP_NODELAY;

/**
 * I/O reactor that is capable of listening incoming connections and forward to workers.
 * <p>
 * Created by ribeirux on 26/07/14.
 */
public class ListeningReactor implements Runnable {

    private static final Logger LOG = Logger.getLogger(ListeningReactor.class.getName());

    private final ReentrantLock mainLock = new ReentrantLock();
    private final SocketConfig config;
    private final Dispatcher dispatcher;
    private final SocketAddress socketAddress;
    private final int backlog;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private volatile ReactorStatus status = ReactorStatus.INACTIVE;

    public ListeningReactor(final SocketConfig config, final Dispatcher dispatcher,
                            final SocketAddress socketAddress, final int backlog) throws IOException {
        this.config = Preconditions.checkNotNull(config);
        this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher is null");
        this.socketAddress = socketAddress;
        this.backlog = backlog;
        this.selector = Selector.open();
        this.serverChannel = buildServerChannel();
    }

    private ServerSocketChannel buildServerChannel() throws IOException {
        final ServerSocketChannel newChannel = ServerSocketChannel.open();
        try {
            final ServerSocket socket = newChannel.socket();
            socket.setReuseAddress(this.config.option(SO_REUSEADDR));

            final int timeout = this.config.option(SO_TIMEOUT);
            if (timeout > 0) {
                socket.setSoTimeout(timeout);
            }

            final int rcvBuf = this.config.option(SO_RCVBUF);
            if (rcvBuf > 0) {
                socket.setReceiveBufferSize(rcvBuf);
            }

            newChannel.configureBlocking(false);

            return newChannel;
        } catch (final IOException ex) {
            closeChannel(newChannel);

            throw ex;
        }
    }

    private void closeChannel(final Closeable channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (final IOException e) {
                LOG.log(Level.WARNING, "Could not server socket channel", e);
            }
        }
    }

    @Override
    public void run() {
        this.status = ReactorStatus.ACTIVE;

        try {
            this.serverChannel.socket().bind(socketAddress, backlog);
            this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            this.dispatcher.start();

            while (this.status == ReactorStatus.ACTIVE) {
                final int readyCount = this.selector.select();
                if (this.status == ReactorStatus.ACTIVE) {
                    processEvents(readyCount);
                }
            }
        } catch (final IOException e) {
            LOG.log(Level.SEVERE, "Unrecoverable exception. Shutting down acceptor", e);
        } finally {
            doShutdown();
        }
    }

    private void processEvents(final int readyCount) throws IOException {
        if (readyCount > 0) {
            final Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
            for (final SelectionKey key : selectedKeys) {
                processEvent(key);
            }

            selectedKeys.clear();
        }
    }

    private void doShutdown() {
        LOG.info("Shutting down I/O reactor");

        closeChannel(this.serverChannel);

        closeSelector();

        this.dispatcher.shutdown();

        try {
            this.dispatcher.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // finally, change the state to SHUT_DOWN
        this.status = ReactorStatus.SHUT_DOWN;
    }

    private void processEvent(final SelectionKey key) throws IOException {
        if (key.isValid()) {
            try {
                if (key.isAcceptable()) {
                    final SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                    if (socketChannel != null) {
                        prepareSocket(socketChannel.socket());
                        dispatcher.dispatch(socketChannel);
                    }
                }
            } catch (final CancelledKeyException ex) {
                LOG.log(Level.WARNING, "selection key that is no longer valid", ex);
            }
        }
    }

    private void closeSelector() {
        if (this.selector.isOpen()) {
            this.selector.keys().forEach(k -> closeChannel(k.channel()));

            // Stop dispatching I/O events
            closeSelector(this.selector);
        }
    }

    private void prepareSocket(final Socket socket) throws IOException {
        socket.setTcpNoDelay(this.config.option(TCP_NODELAY));
        socket.setKeepAlive(this.config.option(SO_KEEPALIVE));

        final int timeout = this.config.option(SO_TIMEOUT);
        if (timeout > 0) {
            socket.setSoTimeout(timeout);
        }

        final int sndBuf = this.config.option(SO_SNDBUF);
        if (sndBuf > 0) {
            socket.setSendBufferSize(sndBuf);
        }

        final int rcvBuf = this.config.option(SO_RCVBUF);
        if (rcvBuf > 0) {
            socket.setReceiveBufferSize(rcvBuf);
        }

        final int linger = this.config.option(SO_LINGER);
        if (linger >= 0) {
            socket.setSoLinger(true, linger);
        }
    }

    private void closeSelector(final Closeable selector) {
        try {
            selector.close();
        } catch (final IOException ex) {
            LOG.log(Level.WARNING, "Could not close selector", ex);
        }
    }

    public ReactorStatus getStatus() {
        return this.status;
    }

    public void shutdown() {
        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            if (this.status.compareTo(ReactorStatus.ACTIVE) > 0) {
                return;
            }

            // if inactive, just close the opened selector
            if (this.status == ReactorStatus.INACTIVE) {
                doShutdown();
                return;
            }

            this.status = ReactorStatus.SHUTTING_DOWN;
        } finally {
            lock.unlock();
        }

        this.selector.wakeup();
    }
}
