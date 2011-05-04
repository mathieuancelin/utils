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
                .apply(new Transformation<String, String>() {
            @Override
            public String apply(String t) {
                return t.toUpperCase();
            }
        }).get();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testJoin() {
        Collection<String> values = Arrays.asList(new String[] {"Hello", "dude", "!"});
        String expected1 = "Hello | dude | !";
        String expected2 = "HELLO | DUDE | !";
        String expected3 = "before => HELLO | DUDE | ! <= after";
        String join1 = join(values).with(" | ");
        Assert.assertEquals(expected1, join1);
        String join2 = join(values).labelized(new Transformation<String, String>() {
            @Override
            public String apply(String t) {
                return t.toUpperCase();
            }
        }).with(" | ");
        Assert.assertEquals(expected2, join2);
        String join3 = join(values).labelized(new Transformation<String, String>() {
            @Override
            public String apply(String t) {
                return t.toUpperCase();
            }
        }).before("before => ").after(" <= after").with(" | ");        
        Assert.assertEquals(expected3, join3);
    }

    @Test
    public void testTransformation() {
        Collection<Person> values = Arrays.asList(new Person[] {new Person("John", "Doe"), new Person("John", "Adams")});
        Collection<String> expected = Arrays.asList(new String[] {"John", "John"}); 
        String expected1 = "John | John";
        Collection<String> transformed = forEach(values).apply(new Transformation<Person, String>() {
            @Override
            public String apply(Person t) {
                return t.getName();
            }
        }).get();
        Assert.assertEquals(expected, transformed);
        String join1 = join(values).labelized(new Transformation<Person, String>() {
            @Override
            public String apply(Person t) {
                return t.getName();
            }
        }).with(" | ");
        Assert.assertEquals(expected1, join1);
    }

    @Test
    public void testCollections2() {
        Collection<Integer> values = Arrays.asList(new Integer[] {1, 2, 3, 4, 5, 6, -8, -2, -1, -28});
        int expectedTotal = 21;
        int expectedCount = 6;
        Sum sum = new Sum();
        forEach(values)
            .filteredBy(greaterThan(0))
                .execute(sum);
        Assert.assertEquals(expectedTotal, sum.sum());
        Assert.assertEquals(expectedCount, sum.count());
        sum = new Sum();
        forEach(values)
            .filteredBy(greaterThan(0))
            .filteredBy(lesserThan(6))
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

    private class Person {
        
        private String name;

        private String surname;

        public Person(String name, String surname) {
            this.name = name;
            this.surname = surname;
        }

        public String getName() {
            return name;
        }

        public String getSurname() {
            return surname;
        }
    }
}
