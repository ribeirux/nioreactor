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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * I/O reactor that is capable of listening incoming connections and forward to workers.
 * <p/>
 * Created by ribeirux on 26/07/14.
 */
// TODO test shutdown without start
public class ListeningReactor extends Thread {

    private static final Logger LOGGER = Logger.getLogger(ListeningReactor.class.getName());

    private static final AtomicLong COUNTER = new AtomicLong(0);

    private final ReentrantLock mainLock = new ReentrantLock();
    private final Condition termination = mainLock.newCondition();
    private final ReactorConfig config;
    private final Dispatcher dispatcher;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private volatile ReactorStatus status = ReactorStatus.INACTIVE;

    public ListeningReactor(final ReactorConfig config) throws IOException {
        super("I/O acceptor " + COUNTER.getAndIncrement());
        this.config = Preconditions.checkNotNull(config);
        this.dispatcher = new MultiworkerDispatcher(config.workers(), config.eventListenerFactory());
        this.selector = Selector.open();
        this.serverChannel = buildServerChannel();
    }

    private static void closeServerSocketChannel(final ServerSocketChannel serverSocketChannel) {
        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "Could not server socket channel", e);
            }
        }
    }

    private static void closeSelector(final Selector selector) {
        try {
            selector.close();
        } catch (final IOException ex) {
            LOGGER.log(Level.WARNING, "Could not close selector", ex);
        }
    }

    private ServerSocketChannel buildServerChannel() throws IOException {
        final ServerSocketChannel serverChannel = ServerSocketChannel.open();
        try {
            final ServerSocket socket = serverChannel.socket();
            socket.setReuseAddress(this.config.soReuseAddress());
            if (this.config.soTimeout() > 0) {
                socket.setSoTimeout(this.config.soTimeout());
            }
            if (this.config.rcvBufSize() > 0) {
                socket.setReceiveBufferSize(this.config.rcvBufSize());
            }
            serverChannel.configureBlocking(false);

            return serverChannel;
        } catch (final IOException ex) {
            closeServerSocketChannel(serverChannel);

            throw ex;
        }
    }

    public void bind() throws IOException {
        // call start only once
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (this.status != ReactorStatus.INACTIVE) {
                throw new IllegalStateException("Start not allowed");
            }

            status = ReactorStatus.ACTIVE;
        } finally {
            mainLock.unlock();
        }

        try {
            this.serverChannel.socket().bind(this.config.bindAddress(), this.config.backlogSize());
            this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (final IOException e) {
            // set the status to inactive and let the client decide what to do
            status = ReactorStatus.INACTIVE;
            throw e;
        }

        this.dispatcher.start();
        this.start();
    }

    @Override
    public void run() {
        try {
            while (this.status == ReactorStatus.ACTIVE) {
                final int readyCount = this.selector.select();
                if (this.status == ReactorStatus.ACTIVE) {
                    processEvents(readyCount);
                }
            }
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "Unrecoverable exception. Shutting down acceptor", e);
        } finally {
            doShutdown();
        }
    }

    private void processEvents(final int readyCount) throws IOException {
        if (readyCount > 0) {
            final Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
            for (final SelectionKey key : selectedKeys) {
                if (key.isValid()) {
                    processEvent(key);
                }
            }

            selectedKeys.clear();
        }
    }

    private void processEvent(final SelectionKey key) throws IOException {
        try {
            if (key.isAcceptable()) {
                final SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                if (socketChannel != null) {
                    prepareSocket(socketChannel.socket());
                    dispatcher.dispatch(socketChannel);
                }
            }
        } catch (final CancelledKeyException ex) {
            LOGGER.log(Level.WARNING, "attempt is made to use a selection key that is no longer valid", ex);
        }
    }

    private void prepareSocket(final Socket socket) throws IOException {
        socket.setTcpNoDelay(this.config.tcpNoDelay());
        socket.setKeepAlive(this.config.soKeepAlive());
        if (this.config.soTimeout() > 0) {
            socket.setSoTimeout(this.config.soTimeout());
        }
        if (this.config.sndBufSize() > 0) {
            socket.setSendBufferSize(this.config.sndBufSize());
        }
        if (this.config.rcvBufSize() > 0) {
            socket.setReceiveBufferSize(this.config.rcvBufSize());
        }
        final int linger = this.config.soLinger();
        if (linger >= 0) {
            socket.setSoLinger(true, linger);
        }
    }

    public ReactorStatus getStatus() {
        return this.status;
    }

    /**
     * Attempts graceful shutdown of this I/O reactor.
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (this.status.compareTo(ReactorStatus.ACTIVE) > 0) {
                return;
            }

            // if inactive, just close the opened selector
            if (this.status == ReactorStatus.INACTIVE) {
                doShutdown();
                this.status = ReactorStatus.SHUT_DOWN;
                return;
            }

            this.status = ReactorStatus.SHUTTING_DOWN;
        } finally {
            mainLock.unlock();
        }

        this.selector.wakeup();
    }

    public void await() throws InterruptedException {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            while (this.status != ReactorStatus.SHUT_DOWN) {
                termination.await();
            }
        } finally {
            mainLock.unlock();
        }
    }

    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (; ; ) {
                if (this.status == ReactorStatus.SHUT_DOWN) {
                    return true;
                }

                if (nanos <= 0) {
                    return false;
                }

                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    private void doShutdown() {
        LOGGER.info("Shutting down I/O reactor");

        closeServerSocketChannel(this.serverChannel);

        closeSelector();

        this.dispatcher.shutdown();

        try {
            this.dispatcher.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // finally, change the state to SHUT_DOWN
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            this.status = ReactorStatus.SHUT_DOWN;
            this.termination.signalAll();
        } finally {
            mainLock.unlock();
        }
    }

    private void closeSelector() {
        if (this.selector.isOpen()) {
            for (final SelectionKey key : this.selector.keys()) {
                try {
                    final Channel channel = key.channel();
                    if (channel != null) {
                        channel.close();
                    }
                } catch (final IOException ex) {
                    LOGGER.log(Level.WARNING, "Could not close channel", ex);
                }
            }

            // Stop dispatching I/O events
            closeSelector(this.selector);
        }
    }
}
