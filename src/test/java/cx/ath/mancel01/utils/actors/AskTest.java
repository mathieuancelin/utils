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

package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.Concurrent.Promise;
import cx.ath.mancel01.utils.F;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.Failure;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;

public class AskTest {
    
    public static final CountDownLatch latch = new CountDownLatch(2);
    
    public static final ExecutorService service = Executors.newCachedThreadPool();
    
    public static final String TESTVALUE = "Hello Asker !!!";
    
    public static enum Start { IT }
        
    @Test
    public void askTest() throws Exception {
        final Actor testActor = Actors.newContext().create(TEST, "TESTACTOR", service);
        System.out.println("Asking for something ...");
        Promise<String> promise = testActor.ask(Start.IT);
        promise.map(new F.Function<String, Unit>() {
            @Override
            public Unit apply(String t) {
                System.out.println("Received what I've asked for : '" + t + "'");
                Assert.assertEquals(TESTVALUE, t);
                latch.countDown();
                return Unit.unit();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(latch.getCount(), 0);
        testActor.tell(Poison.PILL);
    }
    
    public static final Behavior TEST = new Behavior() {
        @Override
        public Effect apply(Object t, Actors.Context ctx) {
            for (Start it : M.caseClassOf(Start.class, t)) {
                try {
                    Thread.sleep(2000);
                    latch.countDown();
                    ctx.from.tell(TESTVALUE);
                } catch (InterruptedException ex) {
                    ctx.from.tell(Failure.FAIL);
                }
            }   
            return Actors.DIE;
        }
    };
}
