/*
 *  Copyright 2011-2012 Mathieu ANCELIN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package cx.ath.mancel01.utils;

import static cx.ath.mancel01.utils.C.eList;
import cx.ath.mancel01.utils.F.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class CollectionTest {

    @Test
    public void map() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", ",", "how", "are", "you", "today", "!"});
        List<String> expected = Arrays.asList(new String[]{"HELLO", "DUDE", ",", "HOW", "ARE", "YOU", "TODAY", "!"});
        List<String> result = eList(values).map(new F.Function<String, String>() {
            @Override
            public String apply(String t) {
                return t.toUpperCase();
            }
        });
        Assert.assertEquals(expected, result);
    }

    //@Test
    public void parMap() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", ",", "how", "are", "you", "today", "!"});
        List<String> expected = Arrays.asList(new String[]{"HELLO", "DUDE", ",", "HOW", "ARE", "YOU", "TODAY", "!"});
        List<String> result = eList(values).parMap(new F.Function<String, String>() {
            @Override
            public String apply(String t) {
                return t.toUpperCase();
            }
        });
        Assert.assertEquals(expected, result);
    }

    @Test
    public void filter() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", ",", "how", "are", "you", "today", "!"});
        List<String> expected = Arrays.asList(new String[]{"Hello", "today"});
        List<String> result = eList(values).filter(new F.Function<String, Boolean>() {
            @Override
            public Boolean apply(String t) {
                return t.length() > 4;
            }
        });
        Assert.assertEquals(expected, result);
    }

    @Test
    public void filterNot() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", ",", "how", "are", "you", "today", "!"});
        List<String> expected = Arrays.asList(new String[]{"Hello", "today"});
        List<String> result = eList(values).filterNot(new F.Function<String, Boolean>() {
            @Override
            public Boolean apply(String t) {
                return t.length() <= 4;
            }
        });
        Assert.assertEquals(expected, result);
    }

    //@Test
    public void parFilter() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", ",", "how", "are", "you", "today", "!"});
        List<String> expected = Arrays.asList(new String[]{"Hello", "today"});
        List<String> result = eList(values).parFilter(new F.Function<String, Boolean>() {
            @Override
            public Boolean apply(String t) {
                return t.length() > 4;
            }
        });
        Assert.assertEquals(expected, result);
    }

    //@Test
    public void parFilterNot() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", ",", "how", "are", "you", "today", "!"});
        List<String> expected = Arrays.asList(new String[]{"Hello", "today"});
        List<String> result = eList(values).parFilterNot(new F.Function<String, Boolean>() {
            @Override
            public Boolean apply(String t) {
                return t.length() <= 4;
            }
        });
        Assert.assertEquals(expected, result);
    }

    @Test
    public void rem() {
        List<String> values = Arrays.asList(new String[]{"1", "2", "3", "4", "5"});
        List<String> expected = Arrays.asList(new String[]{"2", "3", "4", "5"});
        List<String> result = eList(values).rem("1");
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void rem2() {
        List<String> values = Arrays.asList(new String[]{"1", "2", "3", "4", "5"});
        List<String> expected = Arrays.asList(new String[]{"2", "3", "4", "5"});
        List<String> result = eList(values).rem(0);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void _() {
        List<String> expected = Arrays.asList(new String[]{"1", "2", "3", "4", "5"});
        List<String> result = eList("1")._("2")._("3")._("4")._("5");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void reduce() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        String expected = "Hello | dude | !";
        String result = eList(values).reduce(new F.F2<String, String, String>() {

            @Override
            public String apply(String a, String b) {
                return a + " | " + b;
            }
        });
        Assert.assertEquals(expected, result);
    }


    @Test
    public void reduceRight() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        String expected = "! | dude | Hello";
        String result = eList(values).reduceRight(new F.F2<String, String, String>() {

            @Override
            public String apply(String a, String b) {
                return a + " | " + b;
            }
        });
        Assert.assertEquals(expected, result);
    }

    @Test
    public void head() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        List<String> values2 = new ArrayList<String>();
        Assert.assertEquals(eList(values).head(), "Hello");
        Assert.assertNull(eList(values2).head());
    }

    @Test
    public void headOption() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        List<String> values2 = new ArrayList<String>();
        Option<String> result1 = eList(values).headOption();
        Option<String> result2 = eList(values2).headOption();
        Assert.assertTrue(result1.isDefined());
        Assert.assertTrue(result2.isEmpty());
    }

    @Test
    public void tail() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        List<String> expected = Arrays.asList(new String[]{"dude", "!"});
        List<String> result = eList(values).tail();
        Assert.assertEquals(result, expected);
    }

    @Test
    public void count() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        int result = eList(values).count(new F.Function<String, Boolean>() {

            @Override
            public Boolean apply(String t) {
                return "!".equals(t);
            }
        });
        Assert.assertEquals(result, 1);
    }

    @Test
    public void find() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        Option<String> result = eList(values).find(new F.Function<String, Boolean>() {

            @Override
            public Boolean apply(String t) {
                return "!".equals(t);
            }
        });
        Assert.assertTrue(result.isDefined());
        Option<String> result2 = eList(values).find(new F.Function<String, Boolean>() {

            @Override
            public Boolean apply(String t) {
                return "bla".equals(t);
            }
        });
        Assert.assertTrue(result2.isEmpty());
    }

    @Test
    public void join() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        String expected = "Hello | dude | !";
        String result = eList(values).mkString(" | ");
        Assert.assertEquals(expected, result);
    }

    //@Test
    public void parJoin() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        String expected = "Hello | dude | !";
        String result = eList(values).parMkString(" | ");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void takeLeft() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        List<String> expected = Arrays.asList(new String[]{"Hello"});
        List<String> result = eList(values).takeLeft(1);
        Assert.assertEquals(result, expected);
    }

    @Test
    public void takeRight() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        List<String> expected = Arrays.asList(new String[]{"!"});
        List<String> result = eList(values).takeRight(1);
        Assert.assertEquals(result, expected);
    }

    @Test
    public void takeWhile() {
        List<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        List<String> expected = Arrays.asList(new String[]{"Hello", "dude"});
        List<String> result = eList(values).takeWhile(new F.Function<String, Boolean>() {

            @Override
            public Boolean apply(String t) {
                return !"!".equals(t);
            }
        });
        Assert.assertEquals(result, expected);
    }
}
