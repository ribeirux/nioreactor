package org.nioreactor;

import org.nioreactor.util.Preconditions;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sequentially forwards the socket channel to one of the workers.
 * <p/>
 * Created by ribeirux on 8/10/14.
 */
public class MultiworkerDispatcher implements Dispatcher {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final List<Dispatcher> dispatchers;

    public MultiworkerDispatcher(final int workers, final EventListenerFactory factory) throws IOException {
        Preconditions.checkArgument(workers > 0, "number of workers should be higher than 0");
        Preconditions.checkNotNull(factory, "factory is null");
        this.dispatchers = buildDefaultDispatchers(workers, factory);
    }

    private static void shutdownDispatchers(final List<Dispatcher> dispatchers) {
        for (final Dispatcher dispatcher : dispatchers) {
            dispatcher.shutdown();
        }
    }

    private static List<Dispatcher> buildDefaultDispatchers(final int workers, final EventListenerFactory factory) throws IOException {
        final List<Dispatcher> dispatchersInit = new ArrayList<>(workers);
        try {
            for (int i = 0; i < workers; i++) {
                dispatchersInit.add(new DefaultDispatcher(factory.create()));
            }
        } catch (final IOException e) {
            shutdownDispatchers(dispatchersInit);

            throw e;
        }

        return Collections.unmodifiableList(dispatchersInit);
    }

    @Override
    public void start() {
        // start all workers
        for (final Dispatcher dispatcher : dispatchers) {
            dispatcher.start();
        }
    }

    @Override
    public void dispatch(final SocketChannel socketChannel) {
        this.dispatchers.get((this.counter.getAndIncrement() & 0x7fffffff)
                % dispatchers.size()).dispatch(socketChannel);
    }

    @Override
    public void shutdown() {
        shutdownDispatchers(this.dispatchers);
    }

    @Override
    public void await() throws InterruptedException {
        for (final Dispatcher dispatcher : dispatchers) {
            dispatcher.await();
        }
    }
}
