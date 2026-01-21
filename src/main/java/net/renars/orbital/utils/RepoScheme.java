package net.renars.orbital.utils;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.DataLoader;
import net.renars.orbital.data.Entity;

public interface RepoScheme<V extends Entity> {
    default boolean valid(DataHolder dataHolder) {
        return extract(dataHolder).isOk();
    }

    default Result<V> extract(DataHolder dataHolder) {
        return loader().deserialize(dataHolder);
    }

    default Result<DataHolder> serialize(V entity) {
        return loader().serialize(entity);
    }

    String tableName();

    DataLoader<V> loader();
}
