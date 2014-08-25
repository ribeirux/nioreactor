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
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sequentially forwards the socket channel to one of the workers.
 * <p/>
 * Created by ribeirux on 8/10/14.
 */
public class MultiworkerDispatcher implements Dispatcher {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final List<DefaultWorker> dispatchers;
    private final List<Thread> threads;

    public MultiworkerDispatcher(final int workers, final EventListenerFactory factory, final ThreadFactory threadFactory) throws IOException {
        Preconditions.checkArgument(workers > 0, "number of workers should be higher than 0");
        Preconditions.checkNotNull(factory, "factory is null");

        final List<DefaultWorker> dispatchersInit = new ArrayList<>(workers);
        final List<Thread> threadsInit = new ArrayList<>(workers);
        try {
            for (int i = 0; i < workers; i++) {
                final DefaultWorker newDispatcher = new DefaultWorker(factory.create());
                dispatchersInit.add(newDispatcher);
                threadsInit.add(threadFactory.newThread(newDispatcher));
            }
        } catch (final IOException e) {
            shutdownDispatchers(dispatchersInit);
            throw e;
        }

        this.dispatchers = Collections.unmodifiableList(dispatchersInit);
        this.threads = Collections.unmodifiableList(threadsInit);
    }

    private static void shutdownDispatchers(final List<DefaultWorker> dispatchers) {
        for (final DefaultWorker dispatcher : dispatchers) {
            dispatcher.shutdown();
        }
    }

    @Override
    public void start() {
        // start all workers
        for (final Thread t : threads) {
            t.start();
        }
    }

    @Override
    public void dispatch(final SocketChannel socketChannel) {
        this.dispatchers.get((this.counter.getAndIncrement() & 0x7fffffff) % dispatchers.size()).dispatch(socketChannel);
    }

    @Override
    public void shutdown() {
        shutdownDispatchers(this.dispatchers);
    }

    @Override
    public void await() throws InterruptedException {
        for (final Thread t : threads) {
            t.join();
        }
    }
}
