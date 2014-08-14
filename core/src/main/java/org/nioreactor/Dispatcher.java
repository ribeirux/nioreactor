package org.nioreactor;

import java.nio.channels.SocketChannel;

/**
 * Socket chanel dispatcher. Mainly used to forward accepted requests.
 *
 * Created by ribeirux on 8/10/14.
 */
public interface Dispatcher {

    void dispatch(SocketChannel socketChannel);
}
