// SPDX-FileCopyrightText: © 2022 Nikola Vojičić <nikolavojicic@outlook.com>
// SPDX-License-Identifier: MIT

package io.sourceforge.recipe;

import io.sourceforge.recipe.exception.RecipeFilterException;
import io.sourceforge.recipe.util.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.sourceforge.recipe.util.Fn.doto;
import static java.util.Collections.singleton;
import static java.util.concurrent.ThreadLocalRandom.current;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({
        "ConstantConditions",
        "ConfusingArgumentToVarargsMethod",
        "ResultOfMethodCallIgnored"})
class RecipeTest {

    @Test
    void of_null() {
        assertThrows(NullPointerException.class, () -> Recipe.of(null));
    }

    @Test
    void of_random() {
        Recipe<Integer> rec = Recipe.of(() -> current().nextInt());
        assertNotEquals(
                1,
                Stream.generate(rec).limit(100).collect(toSet()).size());
    }

    @Test
    void of_mutable() {
        Recipe<Integer> rec = Recipe
                .of(AtomicInteger::new)
                .map(AtomicInteger::incrementAndGet);
        assertEquals(1, rec.get());
        assertEquals(1, rec.get());
    }

    @Test
    void ofValue_null() {
        assertNull(Recipe.ofValue(null).get());
    }

    @Test
    void ofValue_random() {
        Recipe<Integer> rec = Recipe.ofValue(current().nextInt());
        assertEquals(
                singleton(rec.get()),
                Stream.generate(rec).limit(100).collect(toSet()));
    }

    @Test
    void ofValue_mutable() {
        Recipe<Integer> rec = Recipe
                .ofValue(new AtomicInteger())
                .map(AtomicInteger::incrementAndGet);
        assertEquals(1, rec.get());
        assertEquals(2, rec.get());
    }

    @Test
    void ofNull_test() {
        assertThrows(RecipeFilterException.class, Recipe.ofNull().filter(Objects::nonNull)::get);
    }

    @Test
    void ofEnum_null() {
        assertThrows(NullPointerException.class, () -> Recipe.ofEnum(null));
    }

    enum E0 {}

    @Test
    void ofEnum_withoutConstants() {
        assertThrows(IllegalArgumentException.class, () -> Recipe.ofEnum(E0.class));
    }

    @SuppressWarnings("unused")
    enum E1 {X}

    @Test
    void ofEnum_withOneConstant() {
        assertEquals(
                EnumSet.allOf(E1.class),
                Stream.generate(Recipe.ofEnum(E1.class)).limit(100).collect(toSet()));
    }

    @SuppressWarnings("unused")
    enum E3 {X, Y, Z}

    @Test
    void ofEnum_withMultipleConstants() {
        assertEquals(
                EnumSet.allOf(E3.class),
                Stream.generate(Recipe.ofEnum(E3.class)).limit(100).collect(toSet()));
    }

    @Test
    void oneOf_null() {
        assertThrows(NullPointerException.class, () -> Recipe.oneOf(null));
        assertThrows(NullPointerException.class, () -> Recipe.oneOf(null, () -> 1));
        assertThrows(NullPointerException.class, () -> Recipe.oneOf(() -> 1, null));
    }

    @Test
    void oneOf_empty() {
        assertThrows(IllegalArgumentException.class, Recipe::oneOf);
    }

    @Test
    void oneOf_one() {
        Recipe<Integer> rec = Recipe.oneOf(
                Recipe
                        .ofValue(new AtomicInteger())
                        .map(AtomicInteger::incrementAndGet));
        assertEquals(1, rec.get());
        assertEquals(2, rec.get());
    }

    @Test
    void oneOf_multiple() {
        Recipe<?> rec = Recipe.oneOf(
                Recipe.oneOf(
                        () -> "foo",
                        () -> "bar"),
                () -> current().nextBoolean());
        assertEquals(
                Stream.of("foo", "bar", true, false).collect(toSet()),
                Stream.generate(rec).limit(100).collect(toSet()));
    }

    @Test
    void oneOf_argumentMutation() {
        Recipe<?>[] recs = new Recipe<?>[]{
                () -> "foo",
                () -> "bar",
                () -> current().nextBoolean()
        };
        Recipe<?> rec = Recipe.oneOf(recs);
        assertEquals(
                Stream.of("foo", "bar", true, false).collect(toSet()),
                Stream.generate(rec).limit(100).collect(toSet()));
        recs[0] = null;
        recs[1] = null;
        recs[2] = null;
        assertThrows(NullPointerException.class, () -> Recipe.oneOf(recs));
        assertEquals(
                Stream.of("foo", "bar", true, false).collect(toSet()),
                Stream.generate(rec).limit(100).collect(toSet()));
    }

    @Test
    void map_null() {
        Recipe<Integer> rec = Recipe.ofValue(5);
        assertThrows(NullPointerException.class, () -> rec.map(null));
    }

    @Test
    void map_nonNull() {
        Recipe<Integer> rec = Recipe.ofValue(5).map(x -> x + 1);
        assertEquals(6, rec.get());
    }

    @Test
    void bind_null() {
        Recipe<Integer> rec = Recipe.ofValue(5);
        assertThrows(NullPointerException.class, () -> rec.bind(null, null));
        assertThrows(NullPointerException.class, () -> rec.bind(null, (x, y) -> x));
        assertThrows(NullPointerException.class, () -> rec.bind(x -> () -> x, null));
    }

    @Test
    void bind_nonNull() {
        Recipe<Pair<Boolean, Integer>> rec = Recipe
                .of(() -> current().nextBoolean())
                .bind(b -> () -> b ? 1 : 0, Pair::new);
        assertEquals(
                Stream.of(
                        new Pair<>(true, 1),
                        new Pair<>(false, 0)).collect(toSet()),
                Stream.generate(rec).limit(100).collect(toSet()));
    }

    @Test
    void filter_null() {
        Recipe<Integer> rec = Recipe.ofValue(5);
        assertThrows(NullPointerException.class, () -> rec.filter(null));
    }

    @Test
    void filter_unsatisfiable() {
        List<Integer> counter = new ArrayList<>();
        Recipe<Integer> rec = Recipe
                .of(() -> current().nextInt(1, 100))
                .map(doto(counter::add))
                .filter(x -> x == 0);
        assertThrows(RecipeFilterException.class, rec::get);
        assertEquals(100, counter.size());
    }

    @Test
    void filter_barelySatisfiable() {
        AtomicInteger counter = new AtomicInteger();
        Recipe<Integer> rec = Recipe
                .of(() -> 99)
                .filter(x -> x < counter.incrementAndGet())
                .map(__ -> counter.get());
        assertEquals(100, rec.get());
        assertEquals(101, rec.get());
        assertEquals(102, rec.get());
        counter.set(0);
        assertEquals(100, rec.get());
        assertEquals(101, rec.get());
        assertEquals(102, rec.get());
    }

    @Test
    void filter_satisfiable() {
        Recipe<Integer> rec = Recipe
                .oneOf(() -> 1, () -> null, () -> 2)
                .filter(Objects::nonNull)
                .filter(x -> x > 1);
        assertEquals(2, rec.get());
    }

    @Test
    void or_null() {
        Recipe<Integer> rec = Recipe.ofValue(5);
        assertThrows(NullPointerException.class, () -> rec.or(null));
    }

    @Test
    void or_nonNull() {
        Recipe<Integer> rec = Recipe
                .ofValue(5)
                .or(() -> 6)
                .or(() -> null);
        assertEquals(
                Stream.of(5, 6, null).collect(toSet()),
                Stream.generate(rec).limit(100).collect(toSet()));
    }

    @Test
    void wrap_null() {
        Recipe<Integer> rec = Recipe.ofValue(5);
        assertThrows(NullPointerException.class, () -> rec.wrap(null));
    }

    @Test
    void wrap_nonNull() {
        Recipe<Long> rec = Recipe
                .ofValue(5)
                .wrap(Stream::generate)
                .map(s -> s.limit(10))
                .map(Stream::count);
        assertEquals(10, rec.get());
    }

}