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
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker used to handle accepted connections.
 * <p/>
 * Created by ribeirux on 26/07/14.
 */
public class DefaultWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(DefaultWorker.class.getName());

    private final ReentrantLock mainLock = new ReentrantLock();
    private final EventListener listener;
    private final Selector selector;
    private final Set<SessionContext> sessions;
    private final Queue<SocketChannel> newChannels;
    private final Queue<SessionContext> closedSessions;
    private volatile ReactorStatus status = ReactorStatus.INACTIVE;

    public DefaultWorker(final EventListener listener) throws IOException {
        this.listener = Preconditions.checkNotNull(listener, "listener is null");
        this.selector = Selector.open();
        this.sessions = new HashSet<>();
        this.newChannels = new ConcurrentLinkedQueue<>();
        this.closedSessions = new ConcurrentLinkedQueue<>();
    }

    private static SessionContext getSession(final SelectionKey key) {
        return (SessionContext) key.attachment();
    }

    public void dispatch(final SocketChannel socketChannel) {
        this.newChannels.add(Preconditions.checkNotNull(socketChannel));
        this.selector.wakeup();
    }

    public void closeSession(final SessionContext session) {
        queueClosedSession(session);
    }

    @Override
    public void run() {
        this.status = ReactorStatus.ACTIVE;

        try {
            // Exit select loop if graceful shutdown has been completed
            while (this.status == ReactorStatus.ACTIVE || !this.sessions.isEmpty()) {
                final int readyCount = this.selector.select();
                if (this.status == ReactorStatus.SHUTTING_DOWN) {
                    closeSessions();
                    closeNewChannels();
                }

                // Process selected I/O events
                if (readyCount > 0) {
                    processEvents(this.selector.selectedKeys());
                }

                // Process closed sessions
                processClosedSessions();

                // If active process new channels
                if (this.status == ReactorStatus.ACTIVE) {
                    processNewChannels();
                }
            }
        } catch (final ClosedSelectorException ignore) {
            // ignored
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "Unrecoverable exception. Shutting down dispatcher", e);
        } finally {
            doShutdown();
        }
    }

    /**
     * Attempts graceful shutdown of this I/O reactor.
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (this.status.compareTo(ReactorStatus.ACTIVE) > 0) {
                return;
            }

            // if inactive, just close the opened selector
            if (this.status == ReactorStatus.INACTIVE) {
                doShutdown();
                this.status = ReactorStatus.SHUT_DOWN;
                return;
            }
            this.status = ReactorStatus.SHUTTING_DOWN;
        } finally {
            mainLock.unlock();
        }

        this.selector.wakeup();
    }

    private void doShutdown() {
        closeNewChannels();
        closeSelector();
        processClosedSessions();

        this.status = ReactorStatus.SHUT_DOWN;
    }

    private void closeSelector() {
        if (this.selector.isOpen()) {
            for (final SelectionKey key : this.selector.keys()) {
                final SessionContext session = getSession(key);
                if (session != null) {
                    session.close();
                }
            }

            try {
                this.selector.close();
            } catch (final IOException ex) {
                LOGGER.log(Level.WARNING, "Could not close channel", ex);
            }
        }
    }

    private void processNewChannels() throws IOException {
        SocketChannel channel;
        while ((channel = this.newChannels.poll()) != null) {
            final SelectionKey key;
            try {
                channel.configureBlocking(false);
                key = channel.register(this.selector, 0);
            } catch (final ClosedChannelException ex) {
                // channel is closed. just process the other channels
                continue;
            }

            final SessionContext session;
            try {
                session = new DefaultSessionContext(key, this);
                session.setSocketTimeout(channel.socket().getSoTimeout());
            } catch (final CancelledKeyException ex) {
                continue;
            }

            try {
                this.sessions.add(session);
                key.attach(session);
                sessionCreated(session);
            } catch (final CancelledKeyException ex) {
                queueClosedSession(session);
                key.attach(null);
            }
        }
    }

    private void sessionCreated(final SessionContext session) {
        try {
            this.listener.connected(session);
        } catch (final CancelledKeyException ex) {
            queueClosedSession(session);
        }
    }

    private void closeSessions() {
        for (final SessionContext session : this.sessions) {
            session.close();
        }
    }

    private void closeNewChannels() {
        SocketChannel entry;
        while ((entry = this.newChannels.poll()) != null) {
            try {
                entry.close();
            } catch (final IOException ex) {
                LOGGER.log(Level.WARNING, "Could not close channel", ex);
            }
        }
    }

    private void processEvents(final Set<SelectionKey> selectedKeys) {
        for (final SelectionKey key : selectedKeys) {
            if (key.isValid()) {
                processEvent(key);
            }
        }

        selectedKeys.clear();
    }

    private void processEvent(final SelectionKey key) {
        final DefaultSessionContext session = (DefaultSessionContext) key.attachment();
        try {
            if (key.isReadable()) {
                final SessionContext session1 = getSession(key);
                try {
                    this.listener.inputReady(session1);
                } catch (final CancelledKeyException ex) {
                    queueClosedSession(session1);
                    key.attach(null);
                }
            }

            if (key.isWritable()) {
                final SessionContext session1 = getSession(key);
                try {
                    this.listener.outputReady(session1);
                } catch (final CancelledKeyException ex) {
                    queueClosedSession(session1);
                    key.attach(null);
                }
            }
        } catch (final CancelledKeyException ex) {
            queueClosedSession(session);
            key.attach(null);
        }
    }

    private void processClosedSessions() {
        SessionContext session;
        while ((session = this.closedSessions.poll()) != null) {
            if (this.sessions.remove(session)) {
                try {
                    this.listener.disconnected(session);
                } catch (final CancelledKeyException ex) {
                    // ignore
                }
            }
        }
    }

    private void queueClosedSession(final SessionContext session) {
        if (session != null) {
            this.closedSessions.add(session);
        }
    }
}
