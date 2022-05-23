package org.recipe.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

public class Fn {

    private Fn() {
        throw new AssertionError();
    }

    /**
     * @return function that applies action to the given value and returns that value.
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static <T> UnaryOperator<T>
        doto
            (Consumer<? super T> action)
    {
        requireNonNull(action);
        return x -> {
            action.accept(x);
            return x;
        };
    }

    /**
     * @return function that applies action to the given values and returns the first value.
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static <T, U> BiFunction<T, U, T>
        doto
            (BiConsumer<? super T,
                        ? super U> action)
    {
        requireNonNull(action);
        return (x, y) -> {
            action.accept(x, y);
            return x;
        };
    }

}
