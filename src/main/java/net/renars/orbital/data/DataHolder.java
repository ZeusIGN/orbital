package net.renars.orbital.data;

import jakarta.annotation.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Vienkāršs wrapper ap DynamoDB AttributeValue.
 * --Renars
 */
public class DataHolder {
    private final Map<String, AttributeValue> data;

    public DataHolder() {
        this.data = new HashMap<>();
    }

    public DataHolder(Map<String, AttributeValue> data) {
        this.data = data;
    }

    public DataHolder putString(String key, String value) {
        data.put(key, builder().s(value).build());
        return this;
    }

    public DataHolder putNumber(String key, Number value) {
        data.put(key, builder().n(value.toString()).build());
        return this;
    }

    public DataHolder putLong(String key, long value) {
        data.put(key, builder().n(value + "").build());
        return this;
    }

    public DataHolder putBoolean(String key, Boolean value) {
        data.put(key, builder().bool(value).build());
        return this;
    }

    public DataHolder putList(String key, List<AttributeValue> value) {
        data.put(key, builder().l(value).build());
        return this;
    }

    public <T> DataHolder putList(String key, List<T> value, Function<T, AttributeValue> mapper) {
        var attrList = value.stream()
                .map(mapper)
                .toList();
        data.put(key, builder().l(attrList).build());
        return this;
    }

    public DataHolder putMap(String key, Map<String, AttributeValue> value) {
        data.put(key, builder().m(value).build());
        return this;
    }

    public DataHolder putCompound(String key, DataHolder dataHolder) {
        data.put(key, builder().m(dataHolder.toMap()).build());
        return this;
    }

    public static Number toNumber(AttributeValue value) {
        if (value.n() == null) return null;
        return Double.parseDouble(value.n());
    }

    public static Long toLong(AttributeValue value) {
        if (value.n() == null) return null;
        return Long.parseLong(value.n());
    }

    public static Boolean toBool(AttributeValue value) {
        return value.bool();
    }

    public static String toString(AttributeValue value) {
        return value.s();
    }

    public static List<AttributeValue> toList(AttributeValue value) {
        return value.l();
    }

    public static Map<String, AttributeValue> toMap(AttributeValue value) {
        return value.m();
    }

    public @Nullable String getString(String key) {
        var attr = data.get(key);
        if (attr == null || attr.s() == null) return null;
        return toString(attr);
    }

    public String getString(String key, String defaultValue) {
        var str = getString(key);
        return str != null ? str : defaultValue;
    }

    public Number getNumber(String key) {
        var attr = data.get(key);
        if (attr == null || attr.n() == null) return null;
        return toNumber(attr);
    }

    public Number getNumber(String key, Number defaultValue) {
        var num = getNumber(key);
        return num != null ? num : defaultValue;
    }

    public Long getLong(String key) {
        var attr = data.get(key);
        if (attr == null || attr.n() == null) return null;
        return toLong(attr);
    }

    public Long getLong(String key, Long defaultValue) {
        var num = getLong(key);
        return num != null ? num : defaultValue;
    }

    public Integer getInteger(String key) {
        var attr = data.get(key);
        if (attr == null || attr.n() == null) return null;
        // zinam, ka skaitlis nav null --Renars
        return toNumber(attr).intValue();
    }

    public Integer getInteger(String key, Integer defaultValue) {
        var num = getInteger(key);
        return num != null ? num : defaultValue;
    }

    public Boolean getBoolean(String key) {
        var attr = data.get(key);
        if (attr == null || attr.bool() == null) return null;
        return toBool(attr);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        var bool = getBoolean(key);
        return bool != null ? bool : defaultValue;
    }

    public List<AttributeValue> getList(String key) {
        var attr = data.get(key);
        if (attr == null || attr.l() == null) return null;
        return toList(attr);
    }

    public List<AttributeValue> getList(String key, List<AttributeValue> defaultValue) {
        var list = getList(key);
        return list != null ? list : defaultValue;
    }

    public <V> List<V> getList(String key, Function<AttributeValue, V> mapper, List<V> defaultValue) {
        var list = getList(key);
        if (list == null) return defaultValue;
        return list.stream()
                .map(mapper)
                .toList();
    }

    public <V> List<V> getList(String key, Function<AttributeValue, V> mapper) {
        var list = getList(key);
        if (list == null) return null;
        return list.stream()
                .map(mapper)
                .toList();
    }

    public Map<String, AttributeValue> getMap(String key) {
        var attr = data.get(key);
        if (attr == null || attr.m() == null) return null;
        return toMap(attr);
    }

    public Map<String, AttributeValue> getMap(String key, Map<String, AttributeValue> defaultValue) {
        var map = getMap(key);
        return map != null ? map : defaultValue;
    }

    public DataHolder getCompound(String key) {
        var map = getMap(key);
        if (map == null) return null;
        return DataHolder.from(map);
    }

    public DataHolder getCompound(String key, DataHolder defaultValue) {
        var compound = getCompound(key);
        return compound != null ? compound : defaultValue;
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public Map<String, AttributeValue> toMap() {
        return new HashMap<>(data);
    }

    private AttributeValue.Builder builder() {
        return AttributeValue.builder();
    }

    public static DataHolder from(Map<String, AttributeValue> map) {
        return new DataHolder(new HashMap<>(map));
    }
}
