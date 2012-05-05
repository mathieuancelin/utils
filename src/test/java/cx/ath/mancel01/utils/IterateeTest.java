package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.Iteratees.Cont;
import cx.ath.mancel01.utils.Iteratees.Done;
import cx.ath.mancel01.utils.Iteratees.El;
import cx.ath.mancel01.utils.Iteratees.EOF;
import cx.ath.mancel01.utils.Iteratees.Input;
import cx.ath.mancel01.utils.Iteratees.Iteratee;
import cx.ath.mancel01.utils.Iteratees.Enumerator;
import cx.ath.mancel01.utils.Iteratees.IterateeException;
import cx.ath.mancel01.utils.Iteratees.Output;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import junit.framework.Assert;
import org.junit.Test;

public class IterateeTest {
    
    @Test
    public void testIterateeOnList() {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> list = Arrays.asList(new String[]{"Mathieu", "Kevin", "Jeremy"});
        for (String s : new ListEnumerator<String>(list)
                    .applyOn(new ListIteratee())) {
            System.out.println(s);
            Assert.assertEquals(s, "MathieuKevinJeremy");
            latch.countDown();
        }
        Assert.assertEquals(0, latch.getCount());
    }
    
    @Test
    public void testIterateeOnLong() {
        CountDownLatch latch = new CountDownLatch(501);
        for (Unit u : new LongEnumerator().applyOn(new LongIteratee(latch))) {
            latch.countDown();
        }
        Assert.assertEquals(0, latch.getCount());
    }

    public static class ListIteratee extends Iteratee<String, String> {
        
        private StringBuilder builder = new StringBuilder();

        @Override
        public String get() {
            return builder.toString();
        }

        @Override
        public Function<Input<String>, Output<String, String>> handler() {
            return new Function<Input<String>, Output<String, String>>() {

                @Override
                public Output<String, String> apply(Input<String> input) {
                    for (El e : M.caseClassOf(El.class, input)) {
                        El<String> el = (El<String>) e;
                        for (String s : el.get()) {
                            builder.append(s);
                        }
                        return new Cont<String, String>(this);
                    }
                    for (EOF e : M.caseClassOf(EOF.class, input)) {
                        return new Done<String, String>(builder.toString());
                    }
                    throw new IterateeException("Something went wrong"); 
                }
            };
        }
    }
    
    public static class LongIteratee extends Iteratee<Long, Unit> {
        
        private final CountDownLatch latch;

        public LongIteratee(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Unit get() {
            return Unit.unit();
        }

        @Override
        public Function<Input<Long>, Output<Long, Unit>> handler() {
            return new Function<Input<Long>, Output<Long, Unit>>() {
                @Override
                public Output<Long, Unit> apply(Input<Long> input) {
                    for (El e : M.caseClassOf(El.class, input)) {
                        El<Long> el = (El<Long>) e;
                        for (Long l : el.get()) {
                            if (l > 500) {
                                return new Done<Long, Unit>(Unit.unit());
                            }
                            System.out.println(l);
                            latch.countDown();
                        }
                        return new Cont<Long, Unit>(this);
                    }
                    throw new RuntimeException("Something went wrong");    
                }
            };            
        }
    }
    
    public static class ListEnumerator<T> extends Enumerator<T> {
        private final Iterator<T> it;
        public ListEnumerator(List<T> names) {
            it = names.iterator();
        }
        @Override
        public Input<T> next() {
            if (it.hasNext()) {
                return new Iteratees.El<T>(it.next());
            } else {
                return new EOF<T>();
            }
        }
    }
    
    public static class LongEnumerator extends Enumerator<Long> {
        private Long current = 0L;
        @Override
        public Input<Long> next() {
            current = current + 1L;
            return new El<Long>(current);
        }
    }
}
