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

/**
 * Vienkārša abstrakta repository klase, kas nodrošina pamata CRUD operācijas un in-memory cache.
 * --Renars
 */
public abstract class Repository<V extends Entity> {
    // in-memory storage --Renars
    private final Map<Long, V> storage = new HashMap<>();
    // scheme, kas nosaka, kā entītijas tiek saglabātas un ielādētas no DB --Renars
    private final RepoScheme<V> scheme;
    long highestID = 0;
    final DynamoDbClient client;

    public Repository(RepoScheme<V> scheme, DynamoDbClient dbClient) {
        this.scheme = scheme;
        this.client = dbClient;
        loadFromDB();
    }

    // ja tabula neeksistē, izveido to --Renars
    private void createTable() {
        var tables = client.listTables().tableNames();
        if (tables.contains(scheme.tableName())) return;
        client.createTable(builder -> builder.keySchema(ks -> ks.attributeName("id").keyType(KeyType.HASH))
                .tableName(scheme.tableName())
                .provisionedThroughput(pt -> pt.readCapacityUnits(5L).writeCapacityUnits(5L))
        );
    }

    // ielādē visus entity no DB un ieliek tos in-memory storage --Renars
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

    // saglabā entity state DB --Renars
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

    // ielādē entity in-memory storage --Renars
    public void loadToStorage(long key, V value) {
        storage.put(key, value);
        loadedEntity(value);
    }

    // atgriež Optional, lai izvairītos no null pointer exceptioniem --Renars
    public Optional<V> get(long key) {
        return Optional.ofNullable(storage.get(key));
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
