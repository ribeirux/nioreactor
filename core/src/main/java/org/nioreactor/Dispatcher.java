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

import java.nio.channels.SocketChannel;

/**
 * Socket channel dispatcher. Mainly used to forward accepted requests.
 * <p>
 * Created by ribeirux on 8/10/14.
 */
public interface Dispatcher {

    void start();

    void dispatch(SocketChannel socketChannel);

    void shutdown();

    void await() throws InterruptedException;
}
