package cx.ath.mancel01.utils;

import static cx.ath.mancel01.utils.L.*;

import org.junit.Assert;
import org.junit.Test;


public class ClosureTest {

    @Test
    public void testApp() {
        
        class Hello {{ System.out.println("Hello !"); }}
        
        class HelloDude extends Act<String> {{ System.out.println("Hello " + in() + "!"); }}
        
        class UpperCase extends Func<String, String> {{ out = in().toUpperCase(); }}
        
        Act hello = newSimpleClosure(Hello.class);
        
        Act<String> helloDude = newSimpleClosure(HelloDude.class);
        
        Func<String, String> upper = newClosure(UpperCase.class);

        Assert.assertEquals( F.Option.some("mathieu").map(helloDude).getOrElse("fail"), "mathieu" );
        
        Assert.assertEquals( F.Option.some("mathieu").map(upper).getOrElse("fail"), "MATHIEU");
    }
}
