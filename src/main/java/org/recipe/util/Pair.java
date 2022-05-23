package org.recipe.util;

import java.util.Objects;

public final class Pair<T, U> {

    private final T first;
    private final U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T first() {
        return first;
    }

    public U second() {
        return second;
    }

    public <X> Pair<X, U> withFirst(X first) {
        return new Pair<>(first, second);
    }

    public <X> Pair<T, X> withSecond(X second) {
        return new Pair<>(first, second);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair))
            return false;
        Pair<?, ?> that = (Pair<?, ?>) obj;
        return Objects.equals(this.first, that.first)
            && Objects.equals(this.second, that.second);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(first)
                  + Objects.hashCode(second);
    }

    @Override
    public String toString() {
        return "Pair[first=" + first + ", second=" + second + ']';
    }

}
