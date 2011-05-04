package cx.ath.mancel01.utils;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

import static cx.ath.mancel01.utils.C.*;

public class AppTest {

    @Test
    public void testCollections() {
        Collection<String> values = Arrays.asList(new String[] {"Hello", "dude", ",", "how", "are", "you", "today", "!"});
        Collection<String> expected = Arrays.asList(new String[] {"HELLO", "DUDE", ",", "HOW", "ARE", "YOU", "TODAY", "!"});
        Collection<String> result = 
            forEach(values)
                .apply(new Transformation<String>() {
            @Override
            public String apply(String t) {
                return t.toUpperCase();
            }
        }).get();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testCollections2() {
        Collection<Integer> values = Arrays.asList(new Integer[] {1, 2, 3, 4, 5, 6, -8, -2, -1, -28});
        int expectedTotal = 21;
        int expectedCount = 6;
        Sum sum = new Sum();
        forEach(values)
            .filter(greaterThan(0))
                .execute(sum);
        Assert.assertEquals(expectedTotal, sum.sum());
        Assert.assertEquals(expectedCount, sum.count());
        sum = new Sum();
        forEach(values)
            .filter(greaterThan(0))
            .filter(lesserThan(6))
                .execute(sum);
        Assert.assertEquals(15, sum.sum());
        Assert.assertEquals(5, sum.count());
    }

    private class Sum implements Function<Integer> {
        
        private int value = 0;

        private int items = 0;

        public void add(int i) {
            value += i;
            items++;
        }

        public int sum() {
            return value;
        }

        public int count() {
            return items;
        }

        @Override
        public void apply(Integer t) {
            add(t);
        }
    }
}
