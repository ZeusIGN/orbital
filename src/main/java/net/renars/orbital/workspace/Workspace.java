package net.renars.orbital.workspace;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.Entity;

public class Workspace implements Entity {
    private final int id;
    private String name;

    public Workspace(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public DataHolder serialize() {
        return null;
    }
}
