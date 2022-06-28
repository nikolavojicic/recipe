// SPDX-FileCopyrightText: © 2022 Nikola Vojičić <nikolavojicic@outlook.com>
// SPDX-License-Identifier: MIT

package org.recipe;

import org.junit.jupiter.api.Test;
import org.recipe.exception.RecipeFilterException;
import org.recipe.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static java.util.concurrent.ThreadLocalRandom.current;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.recipe.util.Fn.doto;

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
    void lift_null() {
        Recipe<Integer> rec = Recipe.ofValue(5);
        assertThrows(NullPointerException.class, () -> rec.lift(null));
    }

    @Test
    void lift_nonNull() {
        Recipe<Long> rec = Recipe
                .ofValue(5)
                .lift(Stream::generate)
                .map(s -> s.limit(10))
                .map(Stream::count);
        assertEquals(10, rec.get());
    }

}