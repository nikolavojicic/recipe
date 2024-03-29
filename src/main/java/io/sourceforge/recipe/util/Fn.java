// SPDX-FileCopyrightText: © 2022 Nikola Vojičić <nikolavojicic@outlook.com>
// SPDX-License-Identifier: MIT

package io.sourceforge.recipe.util;

import io.sourceforge.recipe.Recipe;

import java.util.function.*;

import static java.util.Objects.requireNonNull;

/**
 * Non-instantiable class with function-related static utility methods.
 */
public class Fn {

    private Fn() {
        throw new AssertionError();
    }

    /**
     * @return function that applies {@code action} to the given value and returns that value
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
     * @return function that applies {@code action} to the given values and returns the Pair of those values
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static <T, U> BiFunction<T, U, Pair<T, U>>
        dotwo
            (BiConsumer<? super T,
                        ? super U> action)
    {
        requireNonNull(action);
        return (x, y) -> {
            action.accept(x, y);
            return new Pair<>(x, y);
        };
    }

    /**
     * @return function that applies {@code action} to the given values and returns the first of those values
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static <T, U> BiFunction<T, U, T>
        biFirst
            (BiConsumer<? super T,
                        ? super U> action)
    {
        requireNonNull(action);
        return (x, y) -> {
            action.accept(x, y);
            return x;
        };
    }

    /**
     * @return function that applies {@code action} to the given values and returns the second of those values
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static <T, U> BiFunction<T, U, U>
        biSecond
            (BiConsumer<? super T,
                        ? super U> action)
    {
        requireNonNull(action);
        return (x, y) -> {
            action.accept(x, y);
            return y;
        };
    }

    /**
     * @return function that constantly returns the given {@code recipe}
     * @throws NullPointerException if {@code recipe} is {@code null}
     */
    public static <T, R> Function<T, Recipe<R>>
        recfn
            (Supplier<? extends R> recipe)
    {
        Recipe<R> rec = Recipe.of(recipe);
        return __ -> rec;
    }

    /**
     * @return function that returns recipe that produces the return value of the provided {@code function}
     * @throws NullPointerException if {@code function} is {@code null}
     */
    public static <T, R> Function<T, Recipe<R>>
        fnrec
            (Function<? super   T,
                      ? extends R> function)
    {
        requireNonNull(function);
        return x -> Recipe.ofValue(function.apply(x));
    }

}
