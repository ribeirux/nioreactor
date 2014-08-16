package org.nioreactor.example;

import org.nioreactor.EventKey;
import org.nioreactor.EventListener;
import org.nioreactor.EventListenerFactory;
import org.nioreactor.ListeningReactor;
import org.nioreactor.ReactorConfig;
import org.nioreactor.SessionContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Time Server example.
 * <p/>
 * Created by ribeirux on 02/08/14.
 */
public final class PongServer {

    private final static Logger LOGGER = Logger.getLogger(PongServer.class.getName());

    public static void main(final String[] args) {
        final ReactorConfig config = ReactorConfig.builder(new EventListenerFactory() {
            @Override
            public EventListener create() {
                return new PongEventListener();
            }
        }).workers(5).build();

        try {
            final ListeningReactor ioReactor = new ListeningReactor(config);
            try {
                ioReactor.bind();
                LOGGER.info("Server started. Press any key to shutdown the server");
                System.in.read();
            } finally {
                LOGGER.info("Shutting down");
                try {
                    ioReactor.shutdown();
                    ioReactor.awaitTermination(2, TimeUnit.MINUTES);
                } catch (final InterruptedException e) {
                    LOGGER.log(Level.WARNING, "shutdown interrupted");
                }
            }
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error: ", e);
        }
    }

    private static class PongEventListener implements EventListener {

        private final static Logger LOGGER = Logger.getLogger(PongEventListener.class.getName());

        private final ByteBuffer buffer = ByteBuffer.wrap("Pong...".getBytes(StandardCharsets.UTF_8));

        public void connected(final SessionContext session) {
            LOGGER.info("User connected:" + session.remoteAddress());
            session.interestEvent(EventKey.WRITE);
            session.setSocketTimeout(2000);
        }

        public void inputReady(final SessionContext session) {
        }

        public void outputReady(final SessionContext session) {
            buffer.rewind();
            try {
                session.channel().write(buffer);
                if (!this.buffer.hasRemaining()) {
                    session.close();
                }
            } catch (final IOException ex) {
                LOGGER.log(Level.SEVERE, "I/O error: ", ex);
                session.close();
            }
        }

        public void disconnected(final SessionContext session) {
            LOGGER.info("User disconnected:" + session.remoteAddress());
        }
    }
}
