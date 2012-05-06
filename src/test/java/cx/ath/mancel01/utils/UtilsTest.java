package cx.ath.mancel01.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

import static cx.ath.mancel01.utils.F.*;
import static cx.ath.mancel01.utils.F.Unit.*;
import static cx.ath.mancel01.utils.F.Either.*;
import static cx.ath.mancel01.utils.C.*;
import static cx.ath.mancel01.utils.M.*;
import static cx.ath.mancel01.utils.Y.*;

public class UtilsTest implements Utils {

    @Test(expected = java.lang.AssertionError.class)
    public void testMapAndBind() {
        Function<String, Unit> func = new Function<String, Unit>() {
            @Override
            public Unit apply(String s) {
                Assert.fail("failure");
                return unit();
            }
        };
        Option.some("mathieu").map(func);
    }

    @Test
    public void testEither() {
        Function<String, Unit> success = new Function<String, Unit>() {
            @Override
            public Unit apply(String s) {
                System.out.println("success for " + s);
                return unit();
            }
        };

        Function<Throwable, Unit> failure = new Function<Throwable, Unit>() {
            @Override
            public Unit apply(Throwable t) {
                System.out.println("failure with " + t);
                return unit();
            }
        };

        Function<String, Either<Throwable, String>> uppercase = new Function<String, Either<Throwable, String>>() {
            @Override
            public Either<Throwable, String> apply(String value) {
                try {
                    return eitherRight(value.toUpperCase());
                } catch (Throwable t) {
                    return eitherLeft(t);
                }
            }
        };

        uppercase.apply("mathieu").fold(failure, success);
        uppercase.apply(null).fold(failure, success);
    }

    @Test
    public void testSimpleCurry() {        
        F2<String, String, String> naming = new F2<String, String, String>() {
            @Override
            public String apply(String name, String surname) {
                return name + " : " + surname;
            }
        };
        String named = curry(naming)._("Mathieu")._("Ancelin").invoke();
        Assert.assertEquals(named, "Mathieu : Ancelin");
    }

    @Test
    public void testSimpleMonads() {
        String base = "Hello !";

        Option<String> val = Option.some(base);

        Function<String, String> t = new Function<String, String>() {
            @Override
            public String apply(String value) {
                return value.toUpperCase();
            }
        };

        Function<String, String> t2 = new Function<String, String>() {
            @Override
            public String apply(String value) {
                return value + value;
            }
        };

        String ret1 = val.map(t).getOrElse("fail");
        String ret2 = val.map(t2).getOrElse("fail");
        String ret3 = val.map(t).map(t2).getOrElse("fail");
        Assert.assertEquals(ret1, base.toUpperCase());
        Assert.assertEquals(ret2, base + base);
        Assert.assertEquals(ret3, base.toUpperCase() + base.toUpperCase());

        CheckedAction<Socket> connect = new CheckedAction<Socket>() {
            @Override
            public void apply(Socket socket) throws Throwable {
                if (!socket.connect()) {
                    throw new RuntimeException("socket error");
                }
            }
        };

        CheckedAction<Socket> disconnect = new CheckedAction<Socket>() {
            @Override
            public void apply(Socket socket) throws Throwable {
                if (!socket.disconnect()) {
                    throw new RuntimeException("socket error");
                }
            }
        };

        CheckedAction<BadSocket> connect2 = new CheckedAction<BadSocket>() {
            @Override
            public void apply(BadSocket socket) throws Throwable {
                if (!socket.connect()) {
                    throw new RuntimeException("socket error");
                }
            }
        };

        CheckedAction<BadSocket> disconnect2 = new CheckedAction<BadSocket>() {
            @Override
            public void apply(BadSocket socket) throws Throwable {
                if (!socket.disconnect()) {
                    throw new RuntimeException("socket error");
                }
            }
        };

        CheckedAction<Socket> send = new CheckedAction<Socket>() {
            @Override
            public void apply(Socket socket) throws Throwable {
                if (!socket.send("Hello")) {
                    throw new RuntimeException("socket error");
                }
            }
        };

        CheckedAction<BadSocket> send2 = new CheckedAction<BadSocket>() {
            @Override
            public void apply(BadSocket socket) throws Throwable {
                if (!socket.send("Hello")) {
                    throw new RuntimeException("socket error");
                }
            }
        };

        Option<Socket> socket = Option.some(new Socket());
        Option<BadSocket> socket2 = Option.some(new BadSocket());

        socket.map(connect).map(send).map(disconnect);
        socket2.map(connect2).map(send2).map(disconnect2);

        Assert.assertTrue(socket.isDefined());

        Assert.assertTrue(socket.get().connectCalled);
        Assert.assertTrue(socket.get().sendCalled);
        Assert.assertTrue(socket.get().disconnectCalled);

        Assert.assertTrue(socket2.get().connectCalled);
        Assert.assertFalse(socket2.get().sendCalled);
        Assert.assertFalse(socket2.get().disconnectCalled);
    }

    @Test
    public void testCollections() {
        Collection<String> values = Arrays.asList(new String[]{"Hello", "dude", ",", "how", "are", "you", "today", "!"});
        Collection<String> expected = Arrays.asList(new String[]{"HELLO", "DUDE", ",", "HOW", "ARE", "YOU", "TODAY", "!"});
        Collection<String> result =
            forEach(values).apply(new Function<String, String>() {

                @Override
                public String apply(String t) {
                    return t.toUpperCase();
                }
            }).get();
        Assert.assertEquals(expected, result);
    }

    //@Test
    public void testParCollections() {
        Collection<String> values = Arrays.asList(new String[]{"Hello", "dude", ",", "how", "are", "you", "today", "!"});
        Collection<String> expected = Arrays.asList(new String[]{"HELLO", "DUDE", ",", "HOW", "ARE", "YOU", "TODAY", "!"});
        Collection<String> result =
            forEach(values).parApply(new Function<String, String>() {

                @Override
                public String apply(String t) {
                    return t.toUpperCase();
                }
            }).get();
        Assert.assertEquals(expected, result);
    }

    //@Test
    public void testSpeed() {
        Collection<String> values = Collections.nCopies(100001, "hello");

        long total = 0;
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            forEach(values).apply(new Function<String, String>() {

                @Override
                public String apply(String t) {
                    return t.toUpperCase().toLowerCase().toLowerCase();
                }
            }).get();
            long time = (System.currentTimeMillis() - start);
            total += time;
        }
        System.out.println("time : " + (total / 10));
        total = 0;
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            forEach(values).parApply(new Function<String, String>() {

                @Override
                public String apply(String t) {
                    return t.toUpperCase().toLowerCase().toLowerCase();
                }
            }).get();
            long time = (System.currentTimeMillis() - start);
            total += time;
        }
        System.out.println("time par : " + (total / 10));
    }

    //@Test
    public void testSpeed2() {
        Collection<Integer> values = new ArrayList<Integer>();
        for (int i = 0; i < 100; i++) {
            values.add(i);
        }
        Action<Integer> show = new Action<Integer>() {

            @Override
            public void apply(Integer t) {
                long sleep = 100 + (long) (Math.random() * 10);
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                //System.out.println(t);
            }
        };
        long start = System.currentTimeMillis();
        forEach(values).execute(show).get();
        long time = (System.currentTimeMillis() - start);
        System.out.println("time : " + time);
        start = System.currentTimeMillis();
        forEach(values).parExecute(show).get();
        time = (System.currentTimeMillis() - start);
        System.out.println("time par : " + time);
    }

    @Test
    public void testJoin() {
        Collection<String> values = Arrays.asList(new String[]{"Hello", "dude", "!"});
        String expected1 = "Hello | dude | !";
        String expected2 = "HELLO | DUDE | !";
        String expected3 = "before => HELLO | DUDE | ! <= after";
        String join1 = join(values).with(" | ");
        Assert.assertEquals(expected1, join1);
        String join2 = join(values).labelized(new Function<String, String>() {

            @Override
            public String apply(String t) {
                return t.toUpperCase();
            }
        }).with(" | ");
        Assert.assertEquals(expected2, join2);
        String join3 = join(values).labelized(new Function<String, String>() {

            @Override
            public String apply(String t) {
                return t.toUpperCase();
            }
        }).before("before => ").after(" <= after").with(" | ");
        Assert.assertEquals(expected3, join3);
    }

    @Test
    public void testTransformation() {
        Collection<Person> values = Arrays.asList(new Person[]{new Person("John", "Doe"), new Person("John", "Adams")});
        Collection<String> expected = Arrays.asList(new String[]{"John", "John"});
        String expected1 = "John | John";
        Collection<String> transformed = forEach(values).apply(new Function<Person, String>() {

            @Override
            public String apply(Person t) {
                return t.getName();
            }
        }).get();
        Assert.assertEquals(expected, transformed);
        String join1 = join(values).labelized(new Function<Person, String>() {

            @Override
            public String apply(Person t) {
                return t.getName();
            }
        }).with(" | ");
        Assert.assertEquals(expected1, join1);
    }

    @Test
    public void testCollections2() {
        Collection<Integer> values = Arrays.asList(new Integer[]{1, 2, 3, 4, 5, 6, -8, -2, -1, -28});
        int expectedTotal = 21;
        int expectedCount = 6;
        Sum sum = new Sum();
        forEach(values).filteredBy(greaterThan(0)).execute(sum);
        Assert.assertEquals(expectedTotal, sum.sum());
        Assert.assertEquals(expectedCount, sum.count());
        sum = new Sum();
        forEach(values).filteredBy(greaterThan(0)).filteredBy(lesserThan(6)).execute(sum);
        Assert.assertEquals(15, sum.sum());
        Assert.assertEquals(5, sum.count());
    }

    @Test
    public void testParCollections2() {
        Collection<Integer> values = Arrays.asList(new Integer[]{1, 2, 3, 4, 5, 6, -8, -2, -1, -28});
        int expectedTotal = 21;
        int expectedCount = 6;
        Sum sum = new Sum();
        forEach(values).parFilteredBy(greaterThan(0)).parExecute(sum);
        Assert.assertEquals(expectedTotal, sum.sum());
        Assert.assertEquals(expectedCount, sum.count());
        sum = new Sum();
        forEach(values).parFilteredBy(greaterThan(0)).parFilteredBy(lesserThan(6)).parExecute(sum);
        Assert.assertEquals(15, sum.sum());
        Assert.assertEquals(5, sum.count());
    }

    @Test
    public void testPatternMatching() {
        String value = "foobar";
        Option<String> matched =
                match(value).andExpect(String.class).with(
                caseEquals("one").then(new OneFunction()),
                caseEquals("two").then(new TwoFunction()),
                caseEquals("three").then(new ThreeFunction()),
                otherCases().then(new OtherFunction()));
        Assert.assertEquals(matched.get(), "-1");

        value = "one";
        matched =
                match(value).andExpect(String.class).with(
                caseEquals("one").then(new OneFunction()),
                caseEquals("two").then(new TwoFunction()),
                caseEquals("three").then(new ThreeFunction()),
                otherCases().then(new OtherFunction()));
        Assert.assertEquals(matched.get(), "It's a one");

        value = "two";
        matched =
                match(value).andExpect(String.class).with(
                caseEquals("one").then(new OneFunction()),
                caseEquals("two").then(new TwoFunction()),
                caseEquals("three").then(new ThreeFunction()),
                otherCases().then(new OtherFunction()));
        Assert.assertEquals(matched.get(), "It's a two");

        value = "three";
        matched =
                match(value).andExpect(String.class).with(
                caseEquals("one").then(new OneFunction()),
                caseEquals("two").then(new TwoFunction()),
                caseEquals("three").then(new ThreeFunction()),
                otherCases().then(new OtherFunction()));
        Assert.assertEquals(matched.get(), "It's a three");


    }

    @Test
    public void testPatternMatching2() {
        String value = "one";
        String ret = "-1";
        for (String s : caseEquals("one").match(value)) {
            ret = "It's a one";
        }
        for (String s : caseEquals("two").match(value)) {
            ret = "It's a two";
        }
        for (String s : caseEquals("three").match(value)) {
            ret = "It's a three";
        }
        Assert.assertEquals(ret, "It's a one");

        value = "two";
        ret = "-1";
        for (String s : caseEquals("one").match(value)) {
            ret = "It's a one";
        }
        for (String s : caseEquals("two").match(value)) {
            ret = "It's a two";
        }
        for (String s : caseEquals("three").match(value)) {
            ret = "It's a three";
        }
        Assert.assertEquals(ret, "It's a two");

        value = "three";
        ret = "-1";
        for (String s : caseEquals("one").match(value)) {
            ret = "It's a one";
        }
        for (String s : caseEquals("two").match(value)) {
            ret = "It's a two";
        }
        for (String s : caseEquals("three").match(value)) {
            ret = "It's a three";
        }
        Assert.assertEquals(ret, "It's a three");

        value = "foobar";
        ret = "-1";
        for (String s : caseEquals("one").match(value)) {
            ret = "It's a one";
        }
        for (String s : caseEquals("two").match(value)) {
            ret = "It's a two";
        }
        for (String s : caseEquals("three").match(value)) {
            ret = "It's a three";
        }
        Assert.assertEquals(ret, "-1");
    }

    @Test
    public void testPatternMatching3() {
        String value = "one";
        String ret = "-1";
        for (String s : with(caseEquals("one")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a one";
        }
        for (String s : with(caseEquals("two")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a two";
        }
        for (String s : with(caseEquals("three")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a three";
        }
        Assert.assertEquals(ret, "It's a one");

        value = "two";
        ret = "-1";
        for (String s : with(caseEquals("one")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a one";
        }
        for (String s : with(caseEquals("two")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a two";
        }
        for (String s : with(caseEquals("three")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a three";
        }
        Assert.assertEquals(ret, "It's a two");

        value = "three";
        ret = "-1";
        for (String s : with(caseEquals("one")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a one";
        }
        for (String s : with(caseEquals("two")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a two";
        }
        for (String s : with(caseEquals("three")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a three";
        }
        Assert.assertEquals(ret, "It's a three");

        value = "foobar";
        ret = "-1";
        for (String s : with(caseEquals("one")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a one";
        }
        for (String s : with(caseEquals("two")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a two";
        }
        for (String s : with(caseEquals("three")).and(caseLengthGreater(2)).match(value)) {
            ret = "It's a three";
        }
        Assert.assertEquals(ret, "-1");
    }

    @Test
    public void curryTest() {
        Option<CurryMethod<String>> m = method(this, String.class, "concat",
                String.class, String.class, String.class);
        Option<CurryMethod<String>> m2 = method(this, String.class, "concat",
                Integer.class, String.class, Long.class);
        if (m.isDefined()) {
            String value = curryMethod(m)._("A")._("B")._("C").get();

            String expected = "ABC";
            Assert.assertEquals(expected, value);

            CurryFunction<String> function =
                    curryMethod(m)._("A")._("B");
            String value2 =
                    function._("C").get();
            String value22 =
                    function._("D").get();
            String value222 =
                    function._("E").get();

            Assert.assertEquals(expected, value2);
            Assert.assertEquals("ABD", value22);
            Assert.assertEquals("ABE", value222);

            String value3 =
                    new MyCurryFunction()._("A")._("B")._("C").get();

            Assert.assertEquals(expected, value3);

            CurryFunction<String> function2 =
                    new MyCurryFunction()._("A");
            String value4 =
                    function2._("B")._("C").get();

            Assert.assertEquals(expected, value4);
        } else {
            Assert.fail("Method not defined");
        }
        if (m2.isDefined()) {
            CurryFunction<String> function =
                    curryMethod(m2)._(new Long(3))._("2");
            String value5 =
                    function._(1).get();

            String expected = "123";
            Assert.assertEquals(expected, value5);
        } else {
            Assert.fail("Method not defined");
        }
    }
    private String n = null;

    @Test
    public void curryTest2() {
        Utils u = target(this, Utils.class);

        String value = curryM(u.concat(n, n, n))._("A")._("B")._("C").get();

        String expected = "ABC";
        Assert.assertEquals(expected, value);

        CurryFunction<String> function =
                curryM(u.concat(n, n, n))._("A")._("B");
        String value2 =
                function._("C").get();
        String value22 =
                function._("D").get();
        String value222 =
                function._("E").get();

        Assert.assertEquals(expected, value2);
        Assert.assertEquals("ABD", value22);
        Assert.assertEquals("ABE", value222);

        function =
                curryM(u.concat(0, n, 0L))._(new Long(3))._("2");
        String value5 =
                function._(1).get();

        expected = "123";
        Assert.assertEquals(expected, value5);
    }

    @Test
    public void curryTest3() {
        UtilsTest u = target(this);

        String value = curryM(u.concat(Null.type(String.class), Null.type(String.class), Null.type(String.class)))._("A")._("B")._("C").get();

        String expected = "ABC";
        Assert.assertEquals(expected, value);

        CurryFunction<String> function =
                curryM(u.concat(n, n, n))._("A")._("B");
        String value2 =
                function._("C").get();
        String value22 =
                function._("D").get();
        String value222 =
                function._("E").get();

        Assert.assertEquals(expected, value2);
        Assert.assertEquals("ABD", value22);
        Assert.assertEquals("ABE", value222);

        function =
                curryM(u.concat(0, n, 0L))._(new Long(3))._("2");
        String value5 =
                function._(1).get();

        expected = "123";
        Assert.assertEquals(expected, value5);
    }

    public class MyCurryFunction extends AbstractCurryFunction<String> {

        @Override
        public CurryFunction<String> create(List<Object> args) {
            MyCurryFunction f = new MyCurryFunction();
            f.init(args.toArray(new Object[args.size()]));
            return f;
        }

        @Override
        public String get() {
            return join(args).with("");
        }
    }

    @Override
    public String concat(String s1, String s2, String s3) {
        return s1.concat(s2).concat(s3);
    }

    @Override
    public String concat(Integer s1, String s2, Long s3) {
        return ("" + s1).concat(s2).concat("" + s3);
    }

    public class OneFunction implements MatchCaseFunction<String, String> {

        @Override
        public Option<String> apply(String value) {
            return Option.some("It's a one");
        }
    }

    public class TwoFunction implements MatchCaseFunction<String, String> {

        @Override
        public Option<String> apply(String value) {
            return Option.some("It's a two");
        }
    }

    public class ThreeFunction implements MatchCaseFunction<String, String> {

        @Override
        public Option<String> apply(String value) {
            return Option.some("It's a three");
        }
    }

    public class OtherFunction implements MatchCaseFunction<String, String> {

        @Override
        public Option<String> apply(String value) {
            return Option.some("-1");
        }
    }

    private class Sum implements Action<Integer> {

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
    
    private class Socket {
        
        boolean connectCalled = false;
        boolean sendCalled = false;
        boolean disconnectCalled = false;
        
        public Boolean connect() {
            connectCalled = true;
            System.out.println("Connect socket ...");
            return true;
        }
        
        public Boolean send(String t) {
            sendCalled = true;
            System.out.println("send " + t);
            return true;
        }
        
        public Boolean disconnect() {
            disconnectCalled = true;
            System.out.println("Disconnect socket !");
            return true;
        }
    }
    
    private class BadSocket extends Socket {
        
        @Override
        public Boolean connect() {
            connectCalled = true;
            System.out.println("Connect bad socket ...");
            return false;
        }
        
        @Override
        public Boolean send(String t) {
            sendCalled = true;
            System.out.println("bad send " + t);
            return false;
        }
        
        @Override
        public Boolean disconnect() {
            disconnectCalled = true;
            System.out.println("Disconnect bad socket !");
            return false;
        }
    }
    
//    private class SendFunction implements MRFunction<Socket, Boolean> {
//
//        private String text = "";
//
//        public SendFunction text(String text) {
//            this.text = text;
//            return this;
//        }
//
//        @Override
//        public Tuple<Socket, Boolean> apply(Monad<Socket, ?> monad) {
//
//            Boolean result = (Boolean) monad.unit().get();
//            if (result) {
//                monad.get().send(text);
//            }
//            return new Tuple<UtilsTest.Socket, Boolean>(monad.get(), Boolean.TRUE);
//        }
//    }
//
//    private class SendFunction2 implements MRFunction<BadSocket, Boolean> {
//
//        private String text = "";
//
//        public SendFunction2 text(String text) {
//            this.text = text;
//            return this;
//        }
//
//        @Override
//        public Tuple<BadSocket, Boolean> apply(Monad<BadSocket, ?> monad) {
//            Boolean result = false;
//            if (!monad.error().isDefined()) {
//                result = monad.get().send(text);
//                if (!result)
//                    monad.error("Send error");
//            }
//            return new Tuple<UtilsTest.BadSocket, Boolean>(monad.get(), result);
//        }
//    }

//    @Test
//    public void testMonads() {
//        String base = "Hello !";
//
//        Monad<String, Object> val = Monadic.monad(base);
//
//        MRFunction<String, String> t = new MRFunction<String, String>() {
//
//            @Override
//            public Tuple<String, String> apply(Monad<String, ?> monad) {
//                String result = monad.get().toUpperCase();
//                return new Tuple<String, String>(result, result);
//            }
//        };
//
//        MRFunction<String, String> t2 = new MRFunction<String, String>() {
//
//            @Override
//            public Tuple<String, String> apply(Monad<String, ?> monad) {
//                String result = monad.get() + monad.get();
//                return new Tuple<String, String>(result, result);
//            }
//        };
//
//        String ret1 = val.bind(t).unit().getOrElse("fail");
//        String ret2 = val.bind(t2).unit().getOrElse("fail");
//        String ret3 = val.bind(t).bind(t2).unit().getOrElse("fail");
//        Assert.assertEquals(ret1, base.toUpperCase());
//        Assert.assertEquals(ret2, base + base);
//        Assert.assertEquals(ret3, base.toUpperCase() + base.toUpperCase());
//
//        MRFunction<Socket, Boolean> connect = new MRFunction<Socket, Boolean>() {
//
//            @Override
//            public Tuple<Socket, Boolean> apply(Monad<Socket, ?> monad) {
//                Boolean result = false;
//                if (!monad.error().isDefined()) {
//                    result = monad.get().connect();
//                    if (!result)
//                        monad.error("Connection  denied");
//                }
//                return new Tuple<Socket, Boolean>(monad.get(), result);
//            }
//        };
//
//        MRFunction<Socket, Boolean> disconnect = new MRFunction<Socket, Boolean>() {
//
//            @Override
//            public Tuple<Socket, Boolean> apply(Monad<Socket, ?> monad) {
//                Boolean result = false;
//                if (!monad.error().isDefined()) {
//                    result = monad.get().disconnect();
//                    if (!result)
//                        monad.error("Disconnection error");
//                }
//                return new Tuple<Socket, Boolean>(monad.get(), result);
//            }
//        };
//
//        MRFunction<BadSocket, Boolean> connect2 = new MRFunction<BadSocket, Boolean>() {
//
//            @Override
//            public Tuple<BadSocket, Boolean> apply(Monad<BadSocket, ?> monad) {
//                Boolean result = false;
//                if (!monad.error().isDefined()) {
//                    result = monad.get().connect();
//                    if (!result)
//                        monad.error("Connection error");
//                }
//                return new Tuple<BadSocket, Boolean>(monad.get(), result);
//            }
//        };
//
//        MRFunction<BadSocket, Boolean> disconnect2 = new MRFunction<BadSocket, Boolean>() {
//
//            @Override
//            public Tuple<BadSocket, Boolean> apply(Monad<BadSocket, ?> monad) {
//                Boolean result = false;
//                if (!monad.error().isDefined()) {
//                    result = monad.get().disconnect();
//                    if (!result)
//                        monad.error("Disconnection error");
//                }
//                return new Tuple<BadSocket, Boolean>(monad.get(), result);
//            }
//        };
//
//        SendFunction send = new SendFunction();
//        SendFunction2 send2 = new SendFunction2();
//        Monad<Socket, Object> socket = Monadic.monad(new Socket());
//        Monad<BadSocket, Object> socket2 = Monadic.monad(new BadSocket());
//
//        socket.bind(connect).bind(send.text("Hello")).bind(disconnect);
//        socket2.bind(connect2).bind(send2.text("Hello")).bind(disconnect2);
//
//        Assert.assertTrue(socket.isDefined());
//
//        Assert.assertTrue(socket.get().connectCalled);
//        Assert.assertTrue(socket.get().sendCalled);
//        Assert.assertTrue(socket.get().disconnectCalled);
//
//        Assert.assertTrue(socket2.get().connectCalled);
//        Assert.assertFalse(socket2.get().sendCalled);
//        Assert.assertFalse(socket2.get().disconnectCalled);
//    }
}
