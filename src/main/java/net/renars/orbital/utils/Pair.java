package net.renars.orbital.utils;

import lombok.Getter;
import lombok.Setter;

public class Pair<F, S> {
    @Setter
    @Getter
    public F first;
    @Setter
    @Getter
    public S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public Pair<F, S> copy() {
        return new Pair<>(this.first, this.second);
    }

    public static <G, H> Pair<G, H> of(G first, H second) {
        return new Pair<>(first, second);
    }

    public boolean equals(Object obj) {
        return obj instanceof Pair<?, ?> pair && pair.first.equals(first) && pair.second.equals(second);
    }
}
