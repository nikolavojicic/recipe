// Copyright (c) Nikola Vojičić

package org.recipe;

import org.recipe.exception.RecipeFilterException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

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
     * Applies {@code mapper} that returns Recipe, to the produced values.
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    default <R> Recipe<R>
        flatMap
            (Function<? super   T,
                      ? extends Recipe<? extends R>> mapper)
    {
        requireNonNull(mapper);
        return () -> mapper.apply(get()).get();
    }

    /**
     * Applies {@code mapper} that causes side effects, to the produced values.
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    default Recipe<T>
        mapEffect
            (Consumer<? super T> mapper)
    {
        requireNonNull(mapper);
        return () -> {
            T value = get();
            mapper.accept(value);
            return value;
        };
    }

    /**
     * Applies {@code binder} to the values produced by {@code this} and {@code recipe}.
     * @throws NullPointerException if {@code recipe} or {@code binder} is {@code null}
     */
    default <U, R> Recipe<R>
        bind
            (Recipe    <? extends U> recipe,
             BiFunction<? super   T,
                        ? super   U,
                        ? extends R> binder)
    {
        requireNonNull(recipe);
        requireNonNull(binder);
        return () -> binder.apply(get(), recipe.get());
    }

    /**
     * Applies {@code binder} that returns Recipe, to the values produced by {@code this} and {@code recipe}.
     * @throws NullPointerException if {@code recipe} or {@code binder} is {@code null}
     */
    default <U, R> Recipe<R>
        flatBind
            (Recipe    <? extends U> recipe,
             BiFunction<? super   T,
                        ? super   U,
                        ? extends Recipe<? extends R>> binder)
    {
        requireNonNull(recipe);
        requireNonNull(binder);
        return () -> binder.apply(get(), recipe.get()).get();
    }

    /**
     * Applies {@code binder} that causes side effects, to the values produced by {@code this} and {@code recipe}.
     * @throws NullPointerException if {@code recipe} or {@code binder} is {@code null}
     */
    default <U> Recipe<T>
        bindEffect
            (Recipe    <? extends U> recipe,
             BiConsumer<? super   T,
                        ? super   U> binder)
    {
        requireNonNull(recipe);
        requireNonNull(binder);
        return () -> {
            T value = get();
            binder.accept(value, recipe.get());
            return value;
        };
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
            (Recipe<? extends T> recipe)
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
            (Function<? super Recipe<? extends T>,
                      ? extends R>                 lifter)
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
            (Recipe<T> recipe)
    {
        return requireNonNull(recipe);
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
            (Recipe<? extends T>... recipes)
    {
        if (recipes.length == 0)
            throw new IllegalArgumentException("Empty recipes");
        List<Recipe<? extends T>> recipeList = new ArrayList<>(recipes.length);
        for (Recipe<? extends T> recipe : recipes)
            recipeList.add(requireNonNull(recipe));
        return () -> recipeList
                .get(current().nextInt(0, recipeList.size()))
                .get();
    }

}
