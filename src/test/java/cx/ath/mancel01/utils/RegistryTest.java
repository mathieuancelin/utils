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

import cx.ath.mancel01.utils.F.Action;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.Registry.BeanEvent;
import cx.ath.mancel01.utils.Registry.BeanEventType;
import cx.ath.mancel01.utils.Registry.BeanListener;
import cx.ath.mancel01.utils.Registry.BeanListenerRegistration;
import cx.ath.mancel01.utils.Registry.BeanReference;
import cx.ath.mancel01.utils.Registry.BeanRegistration;
import org.junit.Assert;
import org.junit.Test;

public class RegistryTest {
    
    private int listenerCounter = 0;
    
    private int listenerStringCounter = 0;

    @Test
    public void testApp() {
        SimpleLogger.enableTrace(true);
        SimpleLogger.enableColors(true);
        BeanListenerRegistration reg1 = Registry.registerListener(new BeanListener() {
            @Override
            public void onEvent(BeanEvent evt) {
                if (evt.type() == BeanEventType.BEAN_REGISTRATION) {
                    listenerCounter++;
                } else {
                    listenerCounter--;
                }
            }
        });
        BeanListenerRegistration reg2 = Registry.registerListener(new BeanListener<String>() {
            @Override
            public void onEvent(BeanEvent evt) {
                if (evt.type() == BeanEventType.BEAN_REGISTRATION) {
                    listenerStringCounter++;
                } else {
                    listenerStringCounter--;
                }
            }
        }, String.class);
        String instanceString = "Hello";
        Integer instanceInt = new Integer(42);
        BeanRegistration<String> string = Registry.register(String.class, instanceString);
        BeanRegistration<Integer> integer = Registry.register(Integer.class, instanceInt);
        Registry.status();
        Assert.assertEquals(listenerCounter, 2);
        Assert.assertEquals(listenerStringCounter, 1);
        Assert.assertSame(string.instance(), instanceString);
        Assert.assertSame(integer.instance(), instanceInt);
        Assert.assertSame(string.reference().instance(), instanceString);
        Assert.assertSame(integer.reference().instance(), instanceInt);
        Assert.assertSame(Registry.instance(String.class), instanceString);
        Assert.assertSame(Registry.instance(Integer.class), instanceInt);
        string.unregister();
        integer.unregister();
        Assert.assertEquals(listenerCounter, 0);
        Assert.assertEquals(listenerStringCounter, 0);
        reg1.unregistrer();
        reg2.unregistrer();
        BeanRegistration<String> string1 = Registry.register(String.class, "40");
        BeanRegistration<String> string2 = Registry.register(String.class, "41");
        BeanRegistration<String> string3 = Registry.register(String.class, "42");
        Registry.instances(String.class).foreach(new Function<String, Unit>() {
            @Override
            public Unit apply(String t) {
                Assert.assertNotNull(t);
                Assert.assertEquals(2, t.length());
                SimpleLogger.info(t);
                return Unit.unit();
            }
        });
        Registry.references(String.class).foreach(new Function<BeanReference<String>, Unit>() {
            @Override
            public Unit apply(BeanReference<String> t) {
                SimpleLogger.info(t.optional().getOrElse("No value"));
                Assert.assertTrue(t.optional().isDefined());
                Assert.assertNotSame("No value", t.optional().getOrElse("No value"));
                return Unit.unit();
            }
        });
        Registry.instances().foreach(new Function<Object, Unit>() {
            @Override
            public Unit apply(Object t) {
                Assert.assertNotNull(t);
                Assert.assertEquals(2, t.toString().length());
                SimpleLogger.info(t.toString());
                return Unit.unit();
            }
        });
        Registry.optionalReference(String.class).optional().map(new Action<String>() {

            @Override
            public void apply(String t) {
                Assert.assertNotNull(t);
                Assert.assertEquals(2, t.length());
                SimpleLogger.info(t);
            }
        });
        Registry.reference(String.class).map(new Action<BeanReference<String>>() {

            @Override
            public void apply(BeanReference<String> t) {
                Assert.assertTrue(t.optional().isDefined());
                Assert.assertNotSame("No value", t.optional().getOrElse("No value"));
                SimpleLogger.info(t.optional().getOrElse("No value"));            
            }
        });
        Registry.references().foreach(new Function<BeanReference<?>, Unit>() {

            @Override
            public Unit apply(BeanReference<?> t) {
                Assert.assertTrue(t.optional().isDefined());
                SimpleLogger.info(t.optional().get().toString());
                return Unit.unit();
            }
        });
        string1.unregister();
        string2.unregister();
        string3.unregister();
    }
}
