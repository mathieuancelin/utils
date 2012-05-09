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

import cx.ath.mancel01.utils.L.Act;
import cx.ath.mancel01.utils.L.Func;
import static cx.ath.mancel01.utils.L.newClosure;
import static cx.ath.mancel01.utils.L.newSimpleClosure;
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
