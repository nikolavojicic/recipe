// SPDX-FileCopyrightText: © 2022 Nikola Vojičić <nikolavojicic@outlook.com>
// SPDX-License-Identifier: MIT

package io.sourceforge.recipe.util;

import io.sourceforge.recipe.Recipe;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Stream;

import static io.sourceforge.recipe.util.Fn.*;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ResultOfMethodCallIgnored")
class PairTest {

    private static <T, U> boolean assertAndGetEquals(
            Pair<Pair<T, U>, Pair<T, U>> pair)
    {
        assertEquals(pair.first(), pair.second());
        return true;
    }

    @Test
    void test_nullable() {
        Recipe<Integer> recFirst = Recipe.ofValue(5).or(() -> null);
        Recipe<String> recSecond = Recipe.ofValue("five").or(() -> null);
        Recipe<Set<Boolean>> rec = recFirst
                .bind(recfn(recSecond), Pair::new)
                .map(doto(Pair::hashCode))
                .map(doto(Pair::toString))
                .bind(fnrec(pair -> new Pair<>(null, null)
                                .withFirst(pair.first())
                                .withSecond(pair.second())),
                        Pair::new)
                .map(PairTest::assertAndGetEquals)
                .wrap(Stream::generate)
                .map(s -> s.limit(100).collect(toSet()));
        assertEquals(singleton(true), rec.get());
    }

    @Test
    void test_unmodifiable() {
        Pair<Integer, String> oldPair = new Pair<>(5, "five");
        Pair<Integer, String> newPair = oldPair.withFirst(6).withSecond("six");
        Pair<Integer, Integer> mapPair = oldPair.mapFirst(x -> x + 1).mapSecond(String::length);
        assertEquals(5, oldPair.first());
        assertEquals("five", oldPair.second());
        assertEquals(6, newPair.first());
        assertEquals("six", newPair.second());
        assertEquals(6, mapPair.first());
        assertEquals(4, mapPair.second());
    }

}
