package net.renars.orbital.data;

import net.renars.orbital.utils.Result;

import java.util.LinkedHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    public Result<Void> checkValidity(DataHolder dataHolder) {
        for (var entry : validator.fields.entrySet()) {
            var id = entry.getKey();
            var extractor = entry.getValue();
            var value = extractor.apply(dataHolder, id);
            if (value == null) return Result.error("Missing field: " + id);
        }
        return Result.ok();
    }

    public Result<V> deserialize(DataHolder dataHolder) {
        var validity = checkValidity(dataHolder);
        if (validity.isError()) return Result.error(validity.errorMsg());
        if (deserializer == null) return Result.error("Deserializer not defined");
        V entity = deserializer.create(dataHolder);
        return Result.ok(entity);
    }

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

        public Validator add(String id, BiFunction<DataHolder, String, Object> extractor) {
            fields.put(id, extractor);
            return this;
        }

        public DataLoader<V> build() {
            return DataLoader.this;
        }
    }
}
