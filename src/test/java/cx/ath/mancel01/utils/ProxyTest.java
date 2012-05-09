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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Mathieu ANCELIN
 */
public class ProxyTest {

    @Ignore @Test
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
    
    public static class MaClass {
        public String getUpper(String str) {
            return null;
        }
    }
}
