package org.nioreactor;

import org.nioreactor.util.Preconditions;

import java.nio.channels.SocketChannel;
import java.util.List;

/**
 * Sequentially forwards the socket channel to one of the workers.
 *
 * Created by ribeirux on 8/10/14.
 */
public class ChannelDispatcherPool implements Dispatcher {

    private final List<? extends Dispatcher> dispatchers;
    private int counter = 0;

    public ChannelDispatcherPool(final List<? extends Dispatcher> dispatchers) {
        Preconditions.checkArgument(dispatchers != null && !dispatchers.isEmpty(), "dispatchers is null or empty");
        this.dispatchers = dispatchers;
    }

    @Override
    public void dispatch(final SocketChannel socketChannel) {
        final int i = (this.counter++ & 0x7fffffff) % dispatchers.size();
        this.dispatchers.get(i).dispatch(socketChannel);
    }
}
