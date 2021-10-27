package org.recipe;

import java.util.UUID;
import java.util.function.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ThreadLocalRandom.current;

/**
 * TODO Documentation...
 */
@FunctionalInterface
interface Generator<T> extends Supplier<T> {

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
    default Generator<T>
        filter
            (Predicate<? super T> predicate)
    {
        requireNonNull(predicate);
        return () -> {
            int tries = 0;
            while (tries < 100) {
                T t = get();
                if (predicate.test(t))
                    return t;
                tries++;
            }
            throw new AssertionError();
        };
    }

    default Generator<T> doto(Consumer<? super T> action) {
        requireNonNull(action);
        return () -> {
            T t = get();
            action.accept(t);
            return t;
        };
    }

    default Generator<T> nullable() {
        return () -> current().nextBoolean()
                        ? get()
                        : null;
    }

    default Stream<T> stream() {
        return Stream.generate(this);
    }

    // ---------------- FACTORIES ----------------

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
            var gen = (Generator<T>) gens[i];
            return gen.get();
        };
    }

    static <T> Generator<T> of(T val) {
        requireNonNull(val);
        return () -> val;
    }

    static <T> Generator<T> of(Generator<T> gen) {
        return requireNonNull(gen);
    }

    static Generator<UUID> ofUUID() {
        return UUID::randomUUID;
    }

    static Generator<Boolean> ofBoolean() {
        return current()::nextBoolean;
    }

}
