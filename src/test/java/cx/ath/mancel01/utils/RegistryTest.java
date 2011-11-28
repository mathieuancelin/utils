package cx.ath.mancel01.utils;


import cx.ath.mancel01.utils.Registry.BeanEvent;
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
        Registry.BeanListenerRegistration reg1 = Registry.registerListener(new Registry.BeanListener() {
            @Override
            public void onEvent(BeanEvent evt) {
                if (evt.type() == Registry.BeanEventType.BEAN_REGISTRATION) {
                    listenerCounter++;
                } else {
                    listenerCounter--;
                }
            }
        });
        Registry.BeanListenerRegistration reg2 = Registry.registerListener(new Registry.BeanListener<String>() {
            @Override
            public void onEvent(BeanEvent evt) {
                if (evt.type() == Registry.BeanEventType.BEAN_REGISTRATION) {
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
    }
}
