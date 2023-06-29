package securecompute.combinations;

import com.github.seregamorph.hamcrest.MoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static securecompute.combinations.Permutation.identity;
import static securecompute.combinations.Permutation.swap;
import static securecompute.combinations.Structure.SWAP_0_1;

class PermutationTest {
    @Test
    void testToString() {
        Permutation p = Permutation.of(0, 5, 3, 4, 2, 1);

        assertThat(p, hasOrder(6));
        assertThat(SWAP_0_1, hasOrder(2));
        assertThat(identity(), hasOrder(1));

        assertEquals("(1, 5)(2, 3, 4)", p.toString());
        assertEquals("(1, 5)(2, 4, 3)", p.inverse().toString());
        assertEquals("id", identity().toString());
        assertEquals("id", p.compose(p.inverse()).toString());
        assertEquals("id", p.andThen(p.inverse()).toString());
        assertEquals("(0, 1)", SWAP_0_1.toString());
        assertEquals("(1, 5)", swap(1, 5).toString());
        assertEquals("(0, 5, 1)(2, 3, 4)", p.compose(SWAP_0_1).toString());
        assertEquals("(0, 1, 5)(2, 3, 4)", p.andThen(SWAP_0_1).toString());
        assertEquals("(2, 3, 4)", p.compose(swap(1, 5)).toString());
        assertEquals("(2, 4, 3)", p.compose(swap(1, 5)).inverse().toString());
    }

    @Test
    void testSmallGroups() {
        Set<Permutation> v4, a4, s4, d5, a5, s5;

        v4 = generateGroup(swap(1, 2).compose(swap(3, 4)), swap(2, 3).compose(swap(1, 4)));
        assertThat(v4, hasSize(4));

        a4 = generateGroup(swap(1, 2).compose(swap(2, 3)), swap(2, 3).compose(swap(3, 4)));
        assertThat(a4, hasSize(12));

        s4 = generateGroup(swap(1, 2).compose(swap(2, 3)), swap(3, 4));
        assertThat(s4, hasSize(24));

        d5 = generateGroup(swap(1, 2).compose(swap(3, 4)), swap(2, 3).compose(swap(4, 5)));
        assertThat(d5, hasSize(10));

        a5 = generateGroup(swap(1, 2).compose(swap(2, 3)), swap(3, 4).compose(swap(4, 5)));
        assertThat(a5, hasSize(60));

        s5 = generateGroup(swap(1, 2).compose(swap(2, 3)).compose(swap(3, 4)), swap(4, 5));
        assertThat(s5, hasSize(120));

        assertThat(v4, everyItem(is(in(a4))));
        assertThat(v4, not(everyItem(is(in(d5)))));
        assertThat(a4, everyItem(is(in(s4))));
        assertThat(a4, everyItem(is(in(a5))));
        assertThat(s4, everyItem(is(in(s5))));
        assertThat(s4, not(everyItem(is(in(a5)))));
        assertThat(d5, everyItem(is(in(a5))));
        assertThat(a5, everyItem(is(in(s5))));
    }

    private static Set<Permutation> generateGroup(Permutation... generators) {
        Set<Permutation> set = new LinkedHashSet<>();
        Queue<Permutation> queue = new ArrayDeque<>();
        queue.add(identity());
        set.add(queue.peek());
        for (Permutation p; (p = queue.poll()) != null; ) {
            for (Permutation g : generators) {
                Permutation q = p.compose(g);
                if (set.add(q)) {
                    queue.add(q);
                }
            }
        }
        return set;
    }

    private static int order(Permutation p) {
        return generateGroup(p).size();
    }

    private static Matcher<Permutation> hasOrder(int order) {
        return MoreMatchers.where(PermutationTest::order, is(order));
    }
}
