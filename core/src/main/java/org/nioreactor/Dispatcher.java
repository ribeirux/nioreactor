package org.nioreactor;

import java.nio.channels.SocketChannel;

/**
 * Socket chanel dispatcher. Mainly used to forward accepted requests.
 * <p/>
 * Created by ribeirux on 8/10/14.
 */
public interface Dispatcher {

    void start();

    void dispatch(SocketChannel socketChannel);

    void shutdown();

    void await() throws InterruptedException;
}
