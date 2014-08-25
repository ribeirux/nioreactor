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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link ServerPromise}.
 * <p/>
 * Created by ribeirux on 8/18/14.
 */
public class DefaultServerPromise implements ServerPromise {

    private final ListeningReactor reactor;
    private final Thread thread;

    public DefaultServerPromise(final ListeningReactor reactor, final ThreadFactory threadFactory) {
        this.reactor = Preconditions.checkNotNull(reactor, "reactor");
        this.thread = threadFactory.newThread(reactor);
    }

    public ServerPromise bind() {
        thread.start();
        return this;
    }

    @Override
    public ReactorStatus getStatus() {
        return reactor.getStatus();
    }

    @Override
    public void shutdown() {
        reactor.shutdown();
    }

    @Override
    public void await() throws InterruptedException {
        thread.join();
    }

    @Override
    public void await(final long timeout, final TimeUnit unit) throws InterruptedException {
        thread.join(unit.toMillis(timeout));
    }
}
