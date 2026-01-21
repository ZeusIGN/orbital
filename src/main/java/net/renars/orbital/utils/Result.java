package net.renars.orbital.utils;

public class Result<V> {
    private final V value;
    private final String error;

    private Result(V value, String error) {
        this.value = value;
        this.error = error;
    }

    public static <V> Result<V> ok(V value) {
        return new Result<>(value, null);
    }

    public static <V> Result<V> ok() {
        return new Result<>(null, null);
    }

    public static <V> Result<V> error(String error) {
        return new Result<>(null, error);
    }

    public boolean isOk() {
        return error == null;
    }

    public boolean isError() {
        return !isOk();
    }

    public V value() {
        if (isError()) throw new IllegalStateException("Cannot get value from an error result: " + error);
        if (value == null) throw new IllegalStateException("Result has no value");
        return value;
    }

    public String errorMsg() {
        return error;
    }
}
