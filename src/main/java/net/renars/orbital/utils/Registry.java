package net.renars.orbital.utils;

import java.util.*;
import java.util.function.Function;

public class Registry<K, V> {
    final Map<K, V> registry = new HashMap<>();
    final Set<K> lockedKeys = new HashSet<>();
    final Map<Class<?>, Function<Object, K>> keyMappers = new HashMap<>();
    final Map<Class<?>, Function<Collection<V>, Collection<Object>>> valueMappers = new HashMap<>();
    V defaultReturn = null;

    public Registry() {
    }

    public Registry<K, V> setDefaultReturn(V defaultReturn) {
        this.defaultReturn = defaultReturn;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <M> Registry<K, V> keyMapper(Class<M> type, Function<M, K> mapper) {
        keyMappers.put(type, (Function<Object, K>) mapper);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <M> Registry<K, V> valueMapper(Class<M> type, Function<Collection<V>, Collection<M>> mapper) {
        valueMappers.put(type, (Function<Collection<V>, Collection<Object>>) (Function<?, ?>) mapper);
        return this;
    }

    public Collection<K> keys() {
        return registry.keySet();
    }

    public Collection<V> values() {
        return registry.values();
    }

    public boolean containsKey(K key) {
        return registry.containsKey(key);
    }

    public boolean containsMapped(Object o) {
        return registry.containsKey(keyMappers.get(o.getClass()).apply(o));
    }

    public void register(K key, V value) {
        if (registry.containsKey(key)) throw new IllegalArgumentException("Key already exists in registry: " + key);
        registry.put(key, value);
    }

    public void reset() {
        registry.clear();
        lockedKeys.clear();
    }

    public void lock(K key) {
        lockedKeys.add(key);
    }

    public boolean isLocked(K key) {
        return lockedKeys.contains(key);
    }

    public void unregister(K key) {
        if (lockedKeys.contains(key)) throw new IllegalStateException("Key is locked and cannot be unregistered: " + key);
        registry.remove(key);
    }

    public void force(K key, V value) {
        if (lockedKeys.contains(key)) throw new IllegalStateException("Key is locked and cannot be modified: " + key);
        registry.put(key, value);
    }

    public V get(K key) {
        return registry.getOrDefault(key, defaultReturn);
    }

    public V getMapped(Object key) {
        return get(keyMappers.get(key.getClass()).apply(key));
    }

    @SuppressWarnings("unchecked")
    public <M> Collection<M> getMappedValues(Class<M> type) {
        return (Collection<M>) valueMappers.get(type).apply(values());
    }
}
