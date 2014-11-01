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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

/**
 * Server builder.
 * <p>
 * Created by ribeirux on 8/17/14.
 */
public class ServerBuilder {

    private static final int DEFAULT_BACKLOG = 1000000;

    // mandatory
    private final EventListenerFactory eventListenerFactory;

    // optional
    private final DefaultSocketConfig.Builder socketConfigBuilder = DefaultSocketConfig.builder();
    private ThreadFactory acceptorThreadFactory = new AcceptorThreadFactory();
    private ThreadFactory dispatcherThreadFactory = new DispatcherThreadFactory();
    private int workers = Runtime.getRuntime().availableProcessors();

    protected ServerBuilder(final EventListenerFactory factory) {
        this.eventListenerFactory = Preconditions.checkNotNull(factory, "eventListenerFactory is null");
    }

    public static ServerBuilder builder(final EventListenerFactory factory) {
        return new ServerBuilder(factory);
    }

    public <T> ServerBuilder socketOption(final SocketOption<T> key, final T value) {
        socketConfigBuilder.option(key, value);
        return this;
    }

    public ServerBuilder acceptorThreadFactory(final ThreadFactory factory) {
        this.acceptorThreadFactory = Preconditions.checkNotNull(factory, "acceptor thread factory is null");
        return this;
    }

    public ServerBuilder dispatcherThreadFactory(final ThreadFactory factory) {
        this.dispatcherThreadFactory = Preconditions.checkNotNull(factory, "dispatcher thread factory is null");
        return this;
    }

    public ServerBuilder workers(final int workers) {
        Preconditions.checkArgument(workers > 0, "workers <= 0");
        this.workers = workers;
        return this;
    }

    public ServerPromise bind(final int port) throws IOException {
        return bind(new InetSocketAddress(port));
    }

    public ServerPromise bind(final SocketAddress address) throws IOException {
        return bind(address, DEFAULT_BACKLOG);
    }

    public ServerPromise bind(final SocketAddress address, final int backlog) throws IOException {
        final SocketConfig socketConfig = this.socketConfigBuilder.build();
        final Dispatcher dispatcher = new MultiworkerDispatcher(workers, eventListenerFactory, dispatcherThreadFactory);
        try {
            final ListeningReactor listeningReactor = new ListeningReactor(socketConfig, dispatcher, address, backlog);
            return new DefaultServerPromise(listeningReactor, acceptorThreadFactory).bind();
        } catch (final IOException e) {
            // cleanup dispatcher
            dispatcher.shutdown();
            throw e;
        }
    }

    public ServerPromise bind(final String host, final int port) throws IOException {
        return bind(new InetSocketAddress(host, port));
    }

}
