package net.renars.orbital.data;

import net.renars.orbital.utils.Result;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Datu serilizācija un deserializācija.
 * Arī nodrošina validāciju un default vērtības.
 * --Renars
 */
public class DataLoader<V extends Entity> {
    final Validator validator = new Validator();
    private Deserializer<V> deserializer;
    private Function<V, DataHolder> serializer = Entity::serialize;

    public DataLoader() {

    }

    public DataLoader<V> deserializer(Deserializer<V> deserializer) {
        this.deserializer = deserializer;
        return this;
    }

    public DataLoader<V> serializer(Function<V, DataHolder> serializer) {
        this.serializer = serializer;
        return this;
    }

    public Validator validator() {
        return validator;
    }

    // validē, vai dataHolder satur visus nepieciešamos laukus un default vērtības --Renars
    public Result<Void> checkValidity(DataHolder dataHolder) {
        for (var entry : validator.fields.entrySet()) {
            var id = entry.getKey();
            var extractor = entry.getValue();
            var value = extractor.apply(dataHolder, id);
            var defaultExtractor = validator.defaulters.get(id);
            if (value == null && defaultExtractor != null) {
                defaultExtractor.accept(dataHolder, id);
                continue;
            }
            if (value == null) return Result.error("Missing field: " + id);
        }
        return Result.ok();
    }

    // deserializē dataHolder arī validējot to --Renars
    public Result<V> deserialize(DataHolder dataHolder) {
        var validity = checkValidity(dataHolder);
        if (validity.isError()) return Result.error(validity.errorMsg());
        if (deserializer == null) return Result.error("Deserializer not defined");
        V entity = deserializer.create(dataHolder);
        if (entity == null) return Result.error("Failed to create entity from data!");
        return Result.ok(entity);
    }

    // serializē entity arī validējot to --Renars
    public Result<DataHolder> serialize(V entity) {
        if (serializer == null) return Result.error("Serializer not defined");
        DataHolder dataHolder = serializer.apply(entity);
        var validity = checkValidity(dataHolder);
        if (validity.isError()) return Result.error(validity.errorMsg());
        return Result.ok(dataHolder);
    }

    @FunctionalInterface
    public interface Deserializer<V> {
        V create(DataHolder dataHolder);
    }

    public class Validator {
        final LinkedHashMap<String, BiFunction<DataHolder, String, Object>> fields = new LinkedHashMap<>();
        final HashMap<String, BiConsumer<DataHolder, String>> defaulters = new HashMap<>();

        // definē lauku, kas ir nepieciešams validācijai --Renars
        public Validator add(String id, BiFunction<DataHolder, String, Object> extractor) {
            put(id, extractor, null);
            return this;
        }

        // definē lauku, kas ir nepieciešams validācijai, un arī default vērtību, ja lauks nav klāt --Renars
        public Validator withDefault(String id, BiFunction<DataHolder, String, Object> extractor, BiConsumer<DataHolder, String> defaulter) {
            put(id, extractor, defaulter);
            return this;
        }

        // helperis --Renars
        private void put(String id, BiFunction<DataHolder, String, Object> extractor, BiConsumer<DataHolder, String> defaulter) {
            if (fields.containsKey(id)) throw new IllegalStateException("Field already defined: " + id);
            fields.put(id, extractor);
            if (defaulter != null) defaulters.put(id, defaulter);
        }

        public DataLoader<V> build() {
            return DataLoader.this;
        }
    }
}
