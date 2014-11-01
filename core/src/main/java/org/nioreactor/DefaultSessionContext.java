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
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of {@link org.nioreactor.SessionContext}.
 * <p>
 * Created by ribeirux on 02/08/14.
 */
final class DefaultSessionContext implements SessionContext {

    private static final Logger LOG = Logger.getLogger(DefaultSessionContext.class.getName());

    private final ReentrantLock mainLock = new ReentrantLock();
    private final Map<AttributeKey<?>, Object> attributes = new ConcurrentHashMap<>();
    private final SelectionKey key;
    private final SocketChannel channel;
    private final DefaultWorker dispatcher;
    private volatile boolean closed = false;

    DefaultSessionContext(final SelectionKey key, final DefaultWorker dispatcher) {
        this.key = Preconditions.checkNotNull(key, "key is null");
        this.channel = (SocketChannel) this.key.channel();
        this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher is null");
    }

    @Override
    public ByteChannel channel() {
        return channel;
    }

    @Override
    public SocketAddress remoteAddress() {
        return this.channel.socket().getRemoteSocketAddress();
    }

    @Override
    public SocketAddress localAddress() {
        return this.channel.socket().getLocalSocketAddress();
    }

    @Override
    public void interestEvent(final EventKey op) {
        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            if (!this.closed) {
                this.key.interestOps(op.interestOps());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            if (!this.closed) {
                this.closed = true;
                this.key.cancel();

                try {
                    this.key.channel().close();
                } catch (final IOException ex) {
                    LOG.log(Level.WARNING, "Could not close channel", ex);
                }

                this.dispatcher.queueClosedSession(this);
                if (this.key.selector().isOpen()) {
                    this.key.selector().wakeup();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public <T> T putAttribute(final AttributeKey<T> key, final T value) {
        Preconditions.checkNotNull(key, "key is null");
        Preconditions.checkNotNull(value, "value is null");
        return key.cast(this.attributes.put(key, value));
    }

    @Override
    public <T> T getAttribute(final AttributeKey<T> key) {
        Preconditions.checkNotNull(key, "key is null");
        return key.cast(this.attributes.get(key));
    }

    @Override
    public <T> T removeAttribute(final AttributeKey<T> key) {
        Preconditions.checkNotNull(key, "key is null");
        return key.cast(this.attributes.remove(key));
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        final SocketAddress remoteAddress = remoteAddress();
        final SocketAddress localAddress = localAddress();

        if ((remoteAddress != null) && (localAddress != null)) {
            formatAddress(buffer, localAddress);
            buffer.append("<->");
            formatAddress(buffer, remoteAddress);
        }
        buffer.append('[');

        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            buffer.append(closed ? "CLOSED" : "ACTIVE");
            buffer.append("][");
            if (this.key.isValid()) {
                buffer.append(EventKey.formatOps(this.key.interestOps()));
                buffer.append(':');
                buffer.append(EventKey.formatOps(this.key.readyOps()));
            }
        } finally {
            lock.unlock();
        }

        buffer.append(']');

        return buffer.toString();
    }

    private void formatAddress(final StringBuilder buffer, final SocketAddress socketAddress) {
        if (socketAddress instanceof InetSocketAddress) {
            final InetSocketAddress address = ((InetSocketAddress) socketAddress);
            buffer.append((address.getAddress() != null) ? address.getAddress().getHostAddress() : address.getAddress()).append(':').append(address.getPort());
        } else {
            buffer.append(socketAddress);
        }
    }
}
