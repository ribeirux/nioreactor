package org.nioreactor;

/**
 * Creates a new {@link org.nioreactor.EventListener}.
 * <p/>
 * Created by ribeirux on 26/07/14.
 */
public interface EventListenerFactory {

    /**
     * Creates a new event listener for each worker.
     *
     * @return and event listener
     */
    EventListener create();
}
