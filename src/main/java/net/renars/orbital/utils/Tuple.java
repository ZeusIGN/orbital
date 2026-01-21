package net.renars.orbital.utils;

import java.util.Objects;

// immutable versija no @Pair --Renars
public record Tuple<F, S>(F first, S second) {
    public static <F, S> Tuple<F, S> from(F first, S second) {
        return new Tuple<>(first, second);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Tuple<?, ?>(Object otherFirst, Object otherSecond) &&
               (Objects.equals(first, otherFirst)) &&
               (Objects.equals(second, otherSecond));
    }
}
