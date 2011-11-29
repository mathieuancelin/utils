package cx.ath.mancel01.utils;


import org.junit.Assert;
import org.junit.Test;

import static cx.ath.mancel01.utils.Registry.*;
import static cx.ath.mancel01.utils.F.*;

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
                SimpleLogger.info(t);
                return Unit.unit();
            }
        });
        Registry.references(String.class).foreach(new Function<BeanReference<String>, Unit>() {
            @Override
            public Unit apply(BeanReference<String> t) {
                SimpleLogger.info(t.optional().getOrElse("No value"));
                return Unit.unit();
            }
        });
    }
}
