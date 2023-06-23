package securecompute;

import java.util.function.Supplier;

public abstract class Lazy<T> implements Supplier<T> {
    private T value;

    protected abstract T compute();

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<T>() {
            @Override
            protected T compute() {
                return supplier.get();
            }
        };
    }

    @Override
    public T get() {
        T value = this.value;
        if (value == null) {
            this.value = value = compute();
        }
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Lazy && get().equals(((Lazy<?>) obj).get());
    }

    @Override
    public int hashCode() {
        return get().hashCode();
    }

    @Override
    public String toString() {
        return get().toString();
    }
}
