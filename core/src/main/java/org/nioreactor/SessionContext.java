package org.nioreactor;

import java.net.SocketAddress;
import java.nio.channels.ByteChannel;

/**
 * Session context that shared across the request lifecycle.
 *
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
     * session by the given attribute name.
     *
     * @param name name of the attribute.
     * @param obj  value of the attribute.
     */
    void setAttribute(String name, Object obj);

    /**
     * Returns the value of the attribute with the given name. The value can be
     * <code>null</code> if not set.
     * <p/>
     *
     * @param name name of the attribute.
     * @return value of the attribute.
     * @see #setAttribute(String, Object)
     */
    Object getAttribute(String name);

    /**
     * Removes attribute with the given name.
     *
     * @param name name of the attribute to be removed.
     * @return value of the removed attribute.
     * @see #setAttribute(String, Object)
     */
    Object removeAttribute(String name);
}
