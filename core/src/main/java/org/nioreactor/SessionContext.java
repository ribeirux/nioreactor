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

import java.net.SocketAddress;
import java.nio.channels.ByteChannel;

/**
 * Session context that shared across the request lifecycle.
 * <p/>
 * Created by ribeirux on 26/07/14.
 */
public interface SessionContext {

    /**
     * Returns the underlying I/O channel.
     *
     * @return the I/O channel.
     */
    ByteChannel channel();

    /**
     * Returns remote address.
     *
     * @return socket address.
     */
    SocketAddress remoteAddress();

    /**
     * Returns local address.
     *
     * @return socket address.
     */
    SocketAddress localAddress();

    /**
     * Sets the interest in an I/O event.
     *
     * @param ops the I/O event.
     */
    void interestEvent(EventKey ops);

    /**
     * Terminates the session and closes the underlying I/O channel.
     */
    void close();

    /**
     * Gets the status of the session:
     *
     * @return session status.
     */
    SessionStatus getStatus();

    /**
     * Checks if the session has been terminated.
     *
     * @return <code>true</code> if the session has been terminated,
     * <code>false</code> otherwise.
     */
    boolean isClosed();

    /**
     * Gets the socket timeout in milliseconds. The value of
     * <code>0</code> signifies the session cannot time out.
     *
     * @return socket timeout.
     */
    int getSocketTimeout();

    /**
     * Sets value of the socket timeout in milliseconds. The value of
     * <code>0</code> signifies the session cannot time out.
     *
     * @param timeout socket timeout.
     */
    void setSocketTimeout(int timeout);

    /**
     * This method can be used to associate a particular object with the
     * session by the given attribute key.
     *
     * @param key   key of the attribute.
     * @param value value of the attribute.
     * @return the previous value associated with key, or null if there was no mapping.
     */
    <T> T putAttribute(AttributeKey<T> key, T value);

    /**
     * Returns the value of the attribute with the given key. The value can be
     * <code>null</code> if not set.
     * <p/>
     *
     * @param key key of the attribute.
     * @return value of the attribute.
     */
    <T> T getAttribute(AttributeKey<T> key);

    /**
     * Removes the attribute with the given key.
     *
     * @param key key of the attribute to be removed.
     * @return value of the removed attribute.
     */
    <T> T removeAttribute(AttributeKey<T> key);
}
