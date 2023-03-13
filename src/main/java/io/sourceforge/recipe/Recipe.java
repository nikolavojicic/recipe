// SPDX-FileCopyrightText: © 2022 Nikola Vojičić <nikolavojicic@outlook.com>
// SPDX-License-Identifier: MIT

package io.sourceforge.recipe;

import io.sourceforge.recipe.exception.RecipeFilterException;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.sourceforge.recipe.util.Fn.fnrec;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ThreadLocalRandom.current;
import static java.util.stream.Collectors.toList;

/**
 * Extends {@link Supplier} with higher-order methods that enable composition
 * of suppliers and transformation and filtering of the results produced by
 * the functional method {@link Supplier#get()}.
 *
 * @see <a href="https://github.com/nikolavojicic/recipe"/>https://github.com/nikolavojicic/recipe</a>
 */
@FunctionalInterface
public interface Recipe<T> extends Supplier<T> {

    /**
     * @return recipe that applies {@code mapper} to the values produced by {@code this} recipe
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    default <R> Recipe<R>
        map
            (Function<? super   T,
                      ? extends R> mapper)
    {
        requireNonNull(mapper);
        return () -> mapper.apply(get());
    }

    /**
     * @return recipe that applies {@code binder} to the values:<br/>
     *         1. produced by {@code this} recipe<br/>
     *         2. produced by the recipe returned by {@code recipeFn}, given the value from 1.
     * @throws NullPointerException if {@code recipeFn} or {@code binder} is {@code null}
     */
    default <U, R> Recipe<R>
        bind
            (Function  <? super   T,
                        ? extends Supplier<? extends U>> recipeFn,
             BiFunction<? super   T,
                        ? super   U,
                        ? extends R> binder)
    {
        requireNonNull(recipeFn);
        requireNonNull(binder);
        return () -> {
            T value = get();
            return binder.apply(value, recipeFn.apply(value).get());
        };
    }

    /**
     * @return recipe for values produced by {@code this} recipe that match the {@code predicate}
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @throws RecipeFilterException if {@code predicate} returns {@code false} for 100 values in a row
     */
    default Recipe<T>
        filter
            (Predicate<? super T> predicate)
    {
        requireNonNull(predicate);
        return () -> {
            int tries = 0;
            while (tries < 100) {
                T value = get();
                if (predicate.test(value))
                    return value;
                tries++;
            }
            throw new RecipeFilterException(
                    "Couldn't satisfy predicate after 100 tries.");
        };
    }

    /**
     * @return recipe that randomly chooses between {@code this} and {@code recipe}
     * @throws NullPointerException if {@code recipe} is {@code null}
     */
    default Recipe<T>
        or
            (Supplier<? extends T> recipe)
    {
        requireNonNull(recipe);
        return () -> current().nextBoolean()
                ? get()
                : recipe.get();
    }

    /**
     * @return recipe that applies {@code wrapper} to {@code this}
     * @throws NullPointerException if {@code wrapper} is {@code null}
     */
    default <R> Recipe<R>
        wrap
            (Function<? super   Recipe<? extends T>,
                      ? extends R>                   wrapper)
    {
        requireNonNull(wrapper);
        return () -> wrapper.apply(this);
    }

    // ---------------- FACTORIES ----------------

    /**
     * @return given {@code recipe}
     * @throws NullPointerException if {@code recipe} is {@code null}
     */
    static <T> Recipe<T>
        of
            (Supplier<? extends T> recipe)
    {
        return recipe::get;
    }

    /**
     * @return recipe that constantly returns {@code value}
     */
    static <T> Recipe<T>
        ofValue
            (T value)
    {
        return () -> value;
    }

    /**
     * @return recipe that constantly returns {@code null}
     */
    static <T> Recipe<T>
        ofNull
            ()
    {
        return ofValue(null);
    }

    /**
     * @return recipe that randomly chooses between enum constants of the {@code type}
     * @throws NullPointerException if {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code type} has no enum constants
     **/
    static <T extends Enum<T>> Recipe<T>
        ofEnum
            (Class<T> type)
    {
        T[] constants = type.getEnumConstants();
        if (constants.length == 0)
            throw new IllegalArgumentException("Empty enum.");
        return Recipe
                .ofValue(Arrays.asList(constants))
                .bind(fnrec(__ -> current().nextInt(0, constants.length)), List::get);
    }

    /**
     * @return recipe that randomly chooses between {@code recipes}
     * @throws IllegalArgumentException if {@code recipes} is empty
     * @throws NullPointerException if any of {@code recipes} is {@code null}
     */
    @SafeVarargs
    static <T> Recipe<T>
        oneOf
            (Supplier<? extends T>... recipes)
    {
        if (recipes.length == 0)
            throw new IllegalArgumentException("Empty recipes.");
        return Recipe
                .ofValue(Arrays.stream(recipes).map(Recipe::of).collect(toList()))
                .bind(fnrec(__ -> current().nextInt(0, recipes.length)), List::get)
                .map(Recipe::get);
    }

}
