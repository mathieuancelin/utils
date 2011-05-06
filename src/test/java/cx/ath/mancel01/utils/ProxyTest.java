package cx.ath.mancel01.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mathieu ANCELIN
 */
public class ProxyTest {

    @Test
    public void testCollections() throws InstantiationException, IllegalAccessException {
        Class<?> clazz = Y.getProxyClass(MaClass.class);
        System.out.println(clazz);
        MaClass c = (MaClass) clazz.newInstance();
        MyHandler h = new MyHandler();
        ((Y.CustomProxy) c).setFrom(MaClass.class);
        ((Y.CustomProxy) c).setHandler(h);
        Assert.assertEquals(null, c.getUpper(""));
        Assert.assertTrue(h.pass);
    }

    private static class MyHandler implements InvocationHandler {

        public boolean pass = false;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            pass = true;
            return null;
        }
    }
}
