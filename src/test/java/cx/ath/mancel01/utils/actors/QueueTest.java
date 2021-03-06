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

import static cx.ath.mancel01.utils.M.caseClassOf;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import junit.framework.Assert;
import org.junit.Test;

public class QueueTest {
    
    public static final CountDownLatch produce = new CountDownLatch(200);
    public static final CountDownLatch consume = new CountDownLatch(200);
    
    public static final ExecutorService service = Executors.newCachedThreadPool();
    
    public static final class Run{}
        
    @Test
    public void queueTest() throws Exception {
        final Actor consumer = Actors.newContext().create(CONSUME, "CONSUME", service);
        consumer.tell(new Run());
        int i = 0;
        while (produce.getCount() > 0) {
            System.out.println("Produce ticket " + i);
            consumer.tell("Consuming " + produce.getCount());
            produce.countDown();
            i++;
        }
        System.out.println("[PRODUCE] Waiting for consumer to finish ...");
        consume.await();
        System.out.println("[PRODUCE] Done Waiting ;-)");
        Assert.assertEquals(produce.getCount(), 0);
        Assert.assertEquals(consume.getCount(), 0);
        consumer.tell(Poison.PILL);
    }
    
    public static final Behavior CONSUME = new Behavior() {
        
        @Override
        public Effect apply(Object t, Actors.Context ctx) {
            for (Run run : caseClassOf(Run.class, t)) {
                try {
                    System.out.println("[CONSUME] Waiting for producer to finish ...");
                    produce.await();
                    System.out.println("[CONSUME] Done Waiting ;-)");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            if (consume.getCount() == 0) {
                return Actors.DIE;
            }
            for (String s : caseClassOf(String.class, t)) {
                System.out.println(s);
                consume.countDown();
            }
            return Actors.CONTINUE;
        }
    };
}
