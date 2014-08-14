package org.nioreactor;

import org.nioreactor.util.Preconditions;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * I/O reactor that is capable of listening incoming connections and forward to workers.
 *
 * Created by ribeirux on 26/07/14.
 */
public class ListeningReactor {

    private static final Logger LOGGER = Logger.getLogger(ListeningReactor.class.getName());

    private final ReentrantLock mainLock = new ReentrantLock();
    private final Condition termination = mainLock.newCondition();
    private final ReactorConfig config;
    private final List<DefaultDispatcher> dispatchers;
    private final Acceptor acceptor;
    private final ServerSocketChannel serverChannel;
    private volatile ReactorStatus status = ReactorStatus.INACTIVE;

    public ListeningReactor(final ReactorConfig config) throws IOException {
        this.config = Preconditions.checkNotNull(config);
        dispatchers = buildDefaultDispatchers(config);
        acceptor = new Acceptor(config, new ChannelDispatcherPool(this.dispatchers));
        serverChannel = buildServerChannel();
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

    private static void shutdownDispatchers(final List<DefaultDispatcher> dispatchers) {
        for (final DefaultDispatcher dispatcher : dispatchers) {
            dispatcher.shutdown();
        }
    }

    private List<DefaultDispatcher> buildDefaultDispatchers(final ReactorConfig config) throws IOException {
        final List<DefaultDispatcher> dispatchersInit = new ArrayList<>(config.workers());
        try {
            for (int i = 0; i < config.workers(); i++) {
                dispatchersInit.add(new DefaultDispatcher(config.eventListenerFactory().create()));
            }
        } catch (final IOException e) {
            shutdownDispatchers(dispatchersInit);

            throw e;
        }

        return Collections.unmodifiableList(dispatchersInit);
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


    public ReactorStatus getStatus() {
        return this.status;
    }

    public void start() throws IOException {
        LOGGER.info("Staring I/O reactor");

        // call start only once
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (status != ReactorStatus.INACTIVE) {
                throw new IllegalStateException("Start not allowed");
            }

            status = ReactorStatus.ACTIVE;
        } finally {
            mainLock.unlock();
        }

        // start all workers
        for (final DefaultDispatcher dispatcher : dispatchers) {
            dispatcher.start();
        }

        // bind
        serverChannel.socket().bind(this.config.bindAddress(), this.config.backlogSize());
        serverChannel.register(acceptor.selector(), SelectionKey.OP_ACCEPT);

        // start acceptor
        acceptor.start();
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

    public void shutdown() {
        LOGGER.info("Shutting down I/O reactor");

        closeServerSocketChannel(this.serverChannel);

        // Close out all channels
        acceptor.shutdown();

        // Attempt to shut down I/O dispatchers gracefully
        shutdownDispatchers(this.dispatchers);

        // Join worker threads
        joinThreads();

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

    private void joinThreads() {
        try {
            acceptor.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (final DefaultDispatcher dispatcher : dispatchers) {
            try {
                dispatcher.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
