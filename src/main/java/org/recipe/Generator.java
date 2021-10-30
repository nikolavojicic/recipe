// Copyright (c) Nikola Vojičić

package org.recipe;

import java.util.Optional;
import java.util.function.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ThreadLocalRandom.current;

/**
 * TODO Documentation...
 */
@FunctionalInterface
public interface Generator<T> extends Supplier<T> {

    /**
     * TODO Documentation...
     */
    default <R> Generator<R>
        map
            (Function <? super   T,
                       ? extends R> mapper)
    {
        requireNonNull(mapper);
        return () -> mapper.apply(get());
    }

    /**
     * TODO Documentation...
     */
    default <R> Generator<R>
        flatMap
            (Function <? super   T,
                       ? extends Generator<? extends R>> mapper)
    {
        requireNonNull(mapper);
        return () -> mapper.apply(get()).get();
    }

    /**
     * TODO Documentation...
     */
    default <U, R> Generator<R>
        bind
            (Generator <? extends U> gen,
             BiFunction<? super   T,
                        ? super   U,
                        ? extends R> binder)
    {
        requireNonNull(gen);
        requireNonNull(binder);
        return () -> binder.apply(get(), gen.get());
    }

    /**
     * TODO Documentation...
     */
    default <U, R> Generator<R>
        flatBind
            (Generator <? extends U> gen,
             BiFunction<? super   T,
                        ? super   U,
                        ? extends Generator<? extends R>> binder)
    {
        requireNonNull(gen);
        requireNonNull(binder);
        return () -> binder.apply(get(), gen.get()).get();
    }

    /**
     * TODO Documentation...
     */
    default Generator<Optional<T>>
        filter
            (Predicate<? super T> predicate)
    {
        requireNonNull(predicate);
        return () -> {
            int tries = 0;
            while (tries < 100) {
                T t = get();
                if (predicate.test(t))
                    return Optional.ofNullable(t);
                tries++;
            }
            return Optional.empty();
        };
    }

    /**
     * TODO Documentation...
     */
    default Generator<T> doto(Consumer<? super T> action) {
        requireNonNull(action);
        return () -> {
            T t = get();
            action.accept(t);
            return t;
        };
    }
    default Generator<T>
        or
            (Generator<? extends T> gen)
    {
        requireNonNull(gen);
        return () -> current().nextBoolean()
                ? get()
                : gen.get();
    }

    /**
     * TODO Documentation...
     */
    default <R> Generator<R>
        lift
            (Function<? super   Generator<? extends T>,
                      ? extends R>                      lifter)
    {
        requireNonNull(lifter);
        return () -> lifter.apply(this);
    }

    // ---------------- FACTORIES ----------------

    /**
     * TODO Documentation...
     */
    static <T> Generator<T>
        of
            (Generator<T> gen)
    {
        return requireNonNull(gen);
    }

    /**
     * TODO Documentation...
     */
    @SafeVarargs
    static <T> Generator<T>
        oneOf
            (Generator<? extends T>... gens)
    {
        if (gens.length == 0)
            throw new IllegalArgumentException("Empty gens");
        return () -> {
            int i = current().nextInt(0, gens.length);
            @SuppressWarnings("unchecked")
            Generator<T> gen = (Generator<T>) gens[i];
            return gen.get();
        };
    }

}
