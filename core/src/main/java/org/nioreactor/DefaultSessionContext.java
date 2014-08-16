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
 * <p/>
 * Created by ribeirux on 02/08/14.
 */
public class DefaultSessionContext implements SessionContext {

    private static final Logger LOGGER = Logger.getLogger(DefaultSessionContext.class.getName());

    private final ReentrantLock mainLock = new ReentrantLock();
    private final SelectionKey key;
    private final ByteChannel channel;
    private final Map<AttributeKey<?>, Object> attributes;
    private final DefaultDispatcher dispatcher;
    private volatile SessionStatus status = SessionStatus.ACTIVE;
    private volatile int socketTimeout = 0;

    public DefaultSessionContext(final SelectionKey key, final DefaultDispatcher dispatcher) {
        this.key = Preconditions.checkNotNull(key, "key is null");
        this.channel = (ByteChannel) this.key.channel();
        this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher is null");
        this.attributes = new ConcurrentHashMap<>();
    }

    private static void formatAddress(final StringBuilder buffer, final SocketAddress socketAddress) {
        if (socketAddress instanceof InetSocketAddress) {
            final InetSocketAddress address = ((InetSocketAddress) socketAddress);
            buffer.append((address.getAddress() != null) ? address.getAddress().getHostAddress() : address.getAddress()).append(':').append(address.getPort());
        } else {
            buffer.append(socketAddress);
        }
    }

    private static void formatOps(final StringBuilder buffer, final int ops) {
        if ((ops & SelectionKey.OP_READ) > 0) {
            buffer.append('r');
        }
        if ((ops & SelectionKey.OP_WRITE) > 0) {
            buffer.append('w');
        }
        if ((ops & SelectionKey.OP_ACCEPT) > 0) {
            buffer.append('a');
        }
        if ((ops & SelectionKey.OP_CONNECT) > 0) {
            buffer.append('c');
        }
    }

    @Override
    public ByteChannel channel() {
        return channel;
    }

    @Override
    public SocketAddress remoteAddress() {
        return this.channel instanceof SocketChannel ? ((SocketChannel) this.channel).socket().getRemoteSocketAddress() : null;
    }

    @Override
    public SocketAddress localAddress() {
        return this.channel instanceof SocketChannel ? ((SocketChannel) this.channel).socket().getLocalSocketAddress() : null;
    }

    @Override
    public void interestEvent(final EventKey op) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (this.status != SessionStatus.CLOSED) {
                this.key.interestOps(op.interestOps());
                this.key.selector().wakeup();
            }
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void close() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (this.status != SessionStatus.CLOSED) {
                this.status = SessionStatus.CLOSED;
                this.key.cancel();

                try {
                    this.key.channel().close();
                } catch (final IOException ex) {
                    LOGGER.log(Level.WARNING, "Could not close channel", ex);
                }

                this.dispatcher.closeSession(this);
                if (this.key.selector().isOpen()) {
                    this.key.selector().wakeup();
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public SessionStatus getStatus() {
        return this.status;
    }

    @Override
    public boolean isClosed() {
        return this.status == SessionStatus.CLOSED;
    }

    @Override
    public int getSocketTimeout() {
        return this.socketTimeout;
    }

    @Override
    public void setSocketTimeout(final int timeout) {
        this.socketTimeout = timeout;
    }

    @Override
    @SuppressWarnings("unckecked")
    public <T> T putAttribute(final AttributeKey<T> key, final T value) {
        return (T) this.attributes.put(key, value);
    }

    @Override
    @SuppressWarnings("unckecked")
    public <T> T getAttribute(final AttributeKey<T> key) {
        return (T) this.attributes.get(key);
    }

    @Override
    @SuppressWarnings("unckecked")
    public <T> T removeAttribute(final AttributeKey<T> key) {
        return (T) this.attributes.remove(key);
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
        buffer.append("[");

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            buffer.append(status);
            buffer.append("][");
            if (this.key.isValid()) {
                formatOps(buffer, this.key.interestOps());
                buffer.append(":");
                formatOps(buffer, this.key.readyOps());
            }
        } finally {
            mainLock.unlock();
        }

        buffer.append("]");

        return buffer.toString();
    }
}
