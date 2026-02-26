package net.renars.orbital.services;

import net.renars.orbital.Orbital;
import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.Entity;
import net.renars.orbital.utils.RepoScheme;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class Repository<V extends Entity> {
    // in-memory storage --Renars
    private final Map<Long, V> storage = new HashMap<>();
    private final RepoScheme<V> scheme;
    final DynamoDbClient client;

    public Repository(RepoScheme<V> scheme, DynamoDbClient dbClient) {
        this.scheme = scheme;
        this.client = dbClient;
        loadFromDB();
    }

    private void createTable() {
        var tables = client.listTables().tableNames();
        if (tables.contains(scheme.tableName())) return;
        client.createTable(builder -> builder.keySchema(ks -> ks.attributeName("id").keyType(KeyType.HASH))
                .tableName(scheme.tableName())
                .provisionedThroughput(pt -> pt.readCapacityUnits(5L).writeCapacityUnits(5L))
        );
    }

    private void loadFromDB() {
        createTable();
        var req = ScanRequest.builder()
                .tableName(scheme.tableName())
                .build();
        var scan = client.scan(req);
        if (!scan.hasItems()) return;
        for (var item : scan.items()) {
            var compound = DataHolder.from(item);
            var entity = scheme.extract(compound);
            if (entity.isError()) {
                Orbital.LOGGER.error("{} | Failed to load entity from DB: {}\n{}", scheme.tableName(), entity.errorMsg(), item);
                continue;
            }
            var value = entity.value();
            loadToStorage(value.id(), value);
        }
    }

    protected void loadedEntity(V entity) {
    }

    public void saveToDB(V entity) {
        var result = scheme.loader().serialize(entity);
        if (result.isError()) {
            Orbital.LOGGER.error("{} | Failed to serialize entity for DB save: {}\n{}", scheme.tableName(), result.errorMsg(), entity);
            return;
        }
        var compound = result.value();
        var item = compound.toMap();
        client.putItem(builder -> builder.tableName(scheme.tableName()).item(item));
    }

    public void loadToStorage(long key, V value) {
        storage.put(key, value);
        loadedEntity(value);
    }

    public Optional<V> get(long key) {
        return Optional.of(storage.get(key));
    }

    public void delete(long key) {
        storage.remove(key);
    }

    public boolean containsKey(long key) {
        return storage.containsKey(key);
    }

    public Map<Long, V> getAll() {
        return new HashMap<>(storage);
    }

    public Stream<V> values() {
        return storage.values().stream();
    }

    protected Optional<V> filterBy(Predicate<V> predicate) {
        return values()
                .filter(predicate)
                .findFirst();
    }

    // ja es vēlāk izveidošu multi-server, šo būs jāizstrādā no jauna
    // --Renars
    public long nextID() {
        return storage.size();
    }
}
