package org.nioreactor.util;

/**
 * Utility class to validation preconditions.
 * <p/>
 * Created by ribeirux on 26/07/14.
 */
public final class Preconditions {

    private Preconditions() {
    }

    public static void checkArgument(final boolean argument) {
        if (!argument) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(final boolean argument, final Object cause) {
        if (!argument) {
            throw new IllegalArgumentException(String.valueOf(cause));
        }
    }

    public static <T> T checkNotNull(final T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }

        return reference;
    }

    public static <T> T checkNotNull(final T reference, final Object cause) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(cause));
        }

        return reference;
    }
}
