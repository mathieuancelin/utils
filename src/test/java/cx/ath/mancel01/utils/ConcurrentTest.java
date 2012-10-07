package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.Concurrent.PromiseCountDownLatch;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Unit;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author mathieuancelin
 */
public class ConcurrentTest {
    
    public static final String INITIAL_VALUE = "NOTHING";
    public static final String EXPECTED_VALUE = "EVERYTHING";
    
    private String value = INITIAL_VALUE;
    
    public void changeValue() {
        value = EXPECTED_VALUE;
    }
    
    @Test
    public void testLatch() {
        PromiseCountDownLatch latch = new PromiseCountDownLatch(10);
        System.out.println(latch);
        latch.onRedeem(new Function<PromiseCountDownLatch, F.Unit>() {
            @Override
            public Unit apply(PromiseCountDownLatch t) {
                changeValue();
                return Unit.unit();
            }
        });
        for (int i = 0; i < 10; i++) {
            latch.countDown();
            System.out.println(latch);
        } 
        Assert.assertEquals(EXPECTED_VALUE, value);
    }
}
