// SPDX-FileCopyrightText: © 2022 Nikola Vojičić <nikolavojicic@outlook.com>
// SPDX-License-Identifier: MIT

package io.sourceforge.recipe.util;

import org.junit.jupiter.api.Test;
import io.sourceforge.recipe.Recipe;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static io.sourceforge.recipe.util.Fn.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
class FnTest {

    @Test
    void doto_null() {
        assertThrows(NullPointerException.class, () -> doto(null));
    }

    @Test
    void doto_nonNull() {
        Recipe<Set<Integer>> set = Recipe
                .of(HashSet<Integer>::new)
                .map(doto(hs -> hs.add(5)));
        assertEquals(singleton(5), set.get());
    }

    @Test
    void dotwo_null() {
        assertThrows(NullPointerException.class, () -> dotwo(null));
    }

    @Test
    void dotwo_nonNull() {
        Recipe<Set<Integer>> set = Recipe
                .of(HashSet<Integer>::new)
                .bind(__ -> () -> 5, dotwo(Set::add))
                .map(Pair::first);
        assertEquals(singleton(5), set.get());
    }

    @Test
    void recfn_null() {
        assertThrows(NullPointerException.class, () -> recfn(null));
    }

    @Test
    void recfn_nonNull() {
        Recipe<Set<Integer>> set = Recipe
                .of(HashSet<Integer>::new)
                .bind(recfn(() -> 5), dotwo(Set::add))
                .map(Pair::first);
        assertEquals(singleton(5), set.get());
    }

    @Test
    void fnrec_null() {
        assertThrows(NullPointerException.class, () -> fnrec(null));
    }

    @Test
    void fnrec_nonNull() {
        Recipe<Set<Integer>> set = Recipe
                .of(HashSet<Integer>::new)
                .bind(fnrec(HashSet::size), dotwo(Set::add))
                .map(Pair::first);
        assertEquals(singleton(0), set.get());
    }

}
