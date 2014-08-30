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

package org.nioreactor.example;

import org.nioreactor.AttributeKey;
import org.nioreactor.EventKey;
import org.nioreactor.EventListener;
import org.nioreactor.EventListenerFactory;
import org.nioreactor.ServerBuilder;
import org.nioreactor.ServerPromise;
import org.nioreactor.SessionContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Echo Server example.
 * <p/>
 * Created by ribeirux on 02/08/14.
 */
public final class EchoServer {

    private final static Logger LOGGER = Logger.getLogger(EchoServer.class.getName());

    public static void main(final String[] args) {
        try {
            final ServerPromise server = ServerBuilder.newBuilder(new EventListenerFactory() {
                @Override
                public EventListener create() {
                    return new EchoEventListener();
                }
            }).workers(5).bind(8080);

            // add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    server.shutdown();
                    try {
                        server.await(10, TimeUnit.SECONDS);
                    } catch (final InterruptedException e) {
                        // We're shutting down, so just ignore.
                    }
                }
            });

            LOGGER.info("Server started");
            server.await();
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error: ", e);
        } catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, "Shutdown interrupted: ", e);
        }
    }

    private static class EchoEventListener implements EventListener {

        public static final int BUFFER_SIZE = 1024;
        private final static Logger LOGGER = Logger.getLogger(EchoEventListener.class.getName());
        private static final AttributeKey<ByteBuffer> BUFFER = new AttributeKey<>("BUFFER", ByteBuffer.class);

        public void connected(final SessionContext session) {
            LOGGER.fine("connected:" + session.remoteAddress());

            session.setSocketTimeout(2000);
            session.putAttribute(BUFFER, ByteBuffer.allocateDirect(BUFFER_SIZE));
            session.interestEvent(EventKey.READ);
        }

        public void inputReady(final SessionContext session) {
            LOGGER.fine("readable:" + session.remoteAddress());

            final ByteBuffer buffer = session.getAttribute(BUFFER);
            try {
                buffer.compact();
                final int count = session.channel().read(buffer);
                if (count < 0) {
                    session.close();
                } else {
                    if (buffer.position() > 0) {
                        session.interestEvent(buffer.limit() == buffer.capacity() ?
                                // buffer is full. Set it to write mode
                                EventKey.WRITE :
                                // we have info in buffer and it's not full
                                EventKey.READ_WRITE);
                    }
                }
            } catch (final IOException e) {
                LOGGER.log(Level.SEVERE, "I/O error: ", e);
                session.close();
            }
        }

        public void outputReady(final SessionContext session) {
            LOGGER.fine("writable:" + session.remoteAddress());

            final ByteBuffer buffer = session.getAttribute(BUFFER);
            try {
                buffer.flip();
                session.channel().write(buffer);
                if (!buffer.hasRemaining()) {
                    // nothing to write, set to read mode
                    session.interestEvent(EventKey.READ);
                }
            } catch (final IOException ex) {
                LOGGER.log(Level.SEVERE, "I/O error: ", ex);
                session.close();
            }
        }

        public void disconnected(final SessionContext session) {
            LOGGER.fine("disconnected:" + session.remoteAddress());
        }
    }
}
