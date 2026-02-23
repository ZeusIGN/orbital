package net.renars.orbital.data;

import net.renars.orbital.utils.Serializable;

import java.util.Set;

public interface Entity extends Serializable {
    long id();

    default Set<Serializable> children() {
        return Set.of();
    }

    default void loadChildren(DataHolder dataHolder) {
    }
}
