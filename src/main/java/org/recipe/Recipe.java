// Copyright (c) Nikola Vojičić

package org.recipe;

import org.recipe.exception.RecipeFilterException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ThreadLocalRandom.current;

@FunctionalInterface
public interface Recipe<T> extends Supplier<T> {

    /**
     * Applies {@code mapper} to the produced values.
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
     * Applies {@code binder} to the values produced by {@code this} and {@code recipe}.
     * @throws NullPointerException if {@code recipe} or {@code binder} is {@code null}
     */
    default <U, R> Recipe<R>
        bind
            (Supplier  <? extends U> recipe,
             BiFunction<? super   T,
                        ? super   U,
                        ? extends R> binder)
    {
        requireNonNull(recipe);
        requireNonNull(binder);
        return () -> binder.apply(get(), recipe.get());
    }

    /**
     * @return recipe for values that match the {@code predicate}
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
     * @return recipe that applies {@code lifter} to {@code this}
     * @throws NullPointerException if {@code lifter} is {@code null}
     */
    default <R> Recipe<R>
        lift
            (Function<? super   Recipe<? extends T>,
                      ? extends R>                   lifter)
    {
        requireNonNull(lifter);
        return () -> lifter.apply(this);
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
            throw new IllegalArgumentException("Empty recipes");
        List<Supplier<? extends T>> recipeList = new ArrayList<>(recipes.length);
        for (Supplier<? extends T> recipe : recipes)
            recipeList.add(requireNonNull(recipe));
        return () -> recipeList
                .get(current().nextInt(0, recipeList.size()))
                .get();
    }

}
