package org.nioreactor;

import org.nioreactor.util.Preconditions;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Acceptor thread.
 * <p/>
 * Created by ribeirux on 8/10/14.
 */
public class Acceptor extends Thread {

    private static final Logger LOGGER = Logger.getLogger(Acceptor.class.getName());

    private static final AtomicLong COUNTER = new AtomicLong(0);

    private final ReentrantLock mainLock = new ReentrantLock();
    private final ReactorConfig config;
    private final Dispatcher dispatcher;
    private final Selector selector;
    private volatile ReactorStatus status = ReactorStatus.INACTIVE;

    public Acceptor(final ReactorConfig config, final Dispatcher dispatcher) throws IOException {
        super("I/O acceptor " + COUNTER.getAndIncrement());
        this.config = Preconditions.checkNotNull(config, "config is null");
        this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher is null");
        this.selector = Selector.open();
    }

    public Selector selector() {
        return selector;
    }

    @Override
    public void run() {
        this.status = ReactorStatus.ACTIVE;

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
            closeSelector();
        }
    }

    /**
     * Attempts graceful shutdown of this I/O reactor.
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (this.status.compareTo(ReactorStatus.ACTIVE) > 0) {
                // already (being) shutdown
                return;
            }

            // if inactive, just close the opened selector
            if (this.status == ReactorStatus.INACTIVE) {
                this.status = ReactorStatus.SHUT_DOWN;
                closeSelector();
                return;
            }
            this.status = ReactorStatus.SHUTTING_DOWN;
        } finally {
            mainLock.unlock();
        }

        this.selector.wakeup();
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
            try {
                this.selector.close();
            } catch (final IOException ex) {
                LOGGER.log(Level.WARNING, "Could not close selector", ex);
            }
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
}
