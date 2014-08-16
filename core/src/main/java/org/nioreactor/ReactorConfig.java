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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Fluent reactor config.
 * <p/>
 * Created by ribeirux on 20/07/14.
 */
public final class ReactorConfig {

    private final EventListenerFactory eventListenerFactory;
    private final SocketAddress bindAddress;
    private final int workers;
    private final int soTimeout;
    private final boolean soReuseAddress;
    private final int soLinger;
    private final boolean soKeepAlive;
    private final boolean tcpNoDelay;
    private final int sndBufSize;
    private final int rcvBufSize;
    private final int backlogSize;

    private ReactorConfig(final Builder builder) {
        this.eventListenerFactory = builder.eventListenerFactory;
        this.bindAddress = builder.bindAddress;
        this.workers = builder.workers;
        this.soTimeout = builder.soTimeout;
        this.soReuseAddress = builder.soReuseAddress;
        this.soLinger = builder.soLinger;
        this.soKeepAlive = builder.soKeepAlive;
        this.tcpNoDelay = builder.tcpNoDelay;
        this.sndBufSize = builder.sndBufSize;
        this.rcvBufSize = builder.rcvBufSize;
        this.backlogSize = builder.backlogSize;
    }

    public static Builder builder(final EventListenerFactory eventListenerFactory) {
        return new Builder(Preconditions.checkNotNull(eventListenerFactory));
    }

    public EventListenerFactory eventListenerFactory() {
        return eventListenerFactory;
    }

    public SocketAddress bindAddress() {
        return bindAddress;
    }

    public int workers() {
        return workers;
    }

    public int soTimeout() {
        return soTimeout;
    }

    public boolean soReuseAddress() {
        return soReuseAddress;
    }

    public int soLinger() {
        return soLinger;
    }

    public boolean soKeepAlive() {
        return soKeepAlive;
    }

    public boolean tcpNoDelay() {
        return tcpNoDelay;
    }

    public int sndBufSize() {
        return sndBufSize;
    }

    public int rcvBufSize() {
        return rcvBufSize;
    }

    public int backlogSize() {
        return backlogSize;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ReactorConfig{");
        sb.append("eventListenerFactory=").append(eventListenerFactory);
        sb.append(", bindAddress=").append(bindAddress);
        sb.append(", workers=").append(workers);
        sb.append(", soTimeout=").append(soTimeout);
        sb.append(", soReuseAddress=").append(soReuseAddress);
        sb.append(", soLinger=").append(soLinger);
        sb.append(", soKeepAlive=").append(soKeepAlive);
        sb.append(", tcpNoDelay=").append(tcpNoDelay);
        sb.append(", sndBufSize=").append(sndBufSize);
        sb.append(", rcvBufSize=").append(rcvBufSize);
        sb.append(", backlogSize=").append(backlogSize);
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {
        // mandatory
        private final EventListenerFactory eventListenerFactory;

        // optional
        private SocketAddress bindAddress = new InetSocketAddress(8080);
        private int workers = Runtime.getRuntime().availableProcessors();
        private int soTimeout = 0;
        private boolean soReuseAddress = false;
        private int soLinger = -1;
        private boolean soKeepAlive = false;
        private boolean tcpNoDelay = true;
        private int sndBufSize = 0;
        private int rcvBufSize = 0;
        private int backlogSize = 1000000;

        private Builder(final EventListenerFactory eventListenerFactory) {
            this.eventListenerFactory = Preconditions.checkNotNull(eventListenerFactory, "eventListenerFactory is null");
        }

        private Builder bindAddress(final SocketAddress socketAddress) {
            this.bindAddress = Preconditions.checkNotNull(socketAddress);
            return this;
        }

        public Builder workers(final int workers) {
            this.workers = workers;
            return this;
        }

        public Builder withSoTimeout(final int soTimeout) {
            this.soTimeout = soTimeout;
            return this;
        }

        public Builder soReuseAddress(final boolean soReuseAddress) {
            this.soReuseAddress = soReuseAddress;
            return this;
        }

        public Builder withSoLinger(final int soLinger) {
            this.soLinger = soLinger;
            return this;
        }

        public Builder soKeepAlive(final boolean soKeepAlive) {
            this.soKeepAlive = soKeepAlive;
            return this;
        }

        public Builder tcpNoDelay(final boolean tcpNoDelay) {
            this.tcpNoDelay = tcpNoDelay;
            return this;
        }

        public Builder withSndBufSize(final int sndBufSize) {
            this.sndBufSize = sndBufSize;
            return this;
        }

        public Builder withRcvBufSize(final int rcvBufSize) {
            this.rcvBufSize = rcvBufSize;
            return this;
        }

        public Builder withBacklogSize(final int backlogSize) {
            this.backlogSize = backlogSize;
            return this;
        }

        public ReactorConfig build() {
            return new ReactorConfig(this);
        }
    }
}
