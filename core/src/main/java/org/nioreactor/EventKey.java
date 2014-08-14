package org.nioreactor;

import java.nio.channels.SelectionKey;

/**
 * * Type of I/O event notifications.
 * <p/>
 * Created by ribeirux on 8/11/14.
 */
public enum EventKey {

    /**
     * Interest in data input.
     */
    READ(SelectionKey.OP_READ),
    /**
     * Interest in data output.
     */
    WRITE(SelectionKey.OP_WRITE),
    /**
     * Interest in data input/output.
     */
    READ_WRITE(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

    private final int interestOps;

    private EventKey(final int interestOps) {
        this.interestOps = interestOps;
    }

    public int interestOps() {
        return interestOps;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EventKey{");
        sb.append("interestOps=").append(interestOps);
        sb.append('}');
        return sb.toString();
    }
}
