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
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;

public class ScheduledTest {
    
    public static final CountDownLatch latch = new CountDownLatch(25);
    public static final CountDownLatch latch2 = new CountDownLatch(1);

    @Test
    public void testSchedule() throws Exception {
        ActorContext ctx = Actors.newContext();
        Actor sched = ctx.create(SCHEDULED, "SCHEDULED");
        ctx.scheduleOnce(3, TimeUnit.SECONDS, sched, Once.INSTANCE);
        ctx.schedule(200, TimeUnit.MILLISECONDS, sched, Tick.INSTANCE);
        latch2.await();
        latch.await();
        Assert.assertEquals(latch.getCount(), 0);
        Assert.assertEquals(latch2.getCount(), 0);
        sched.tell(Poison.PILL);
    }
    
    public static enum Tick { INSTANCE }
    
    public static enum Once { INSTANCE }
    
    public static final Actors.Behavior SCHEDULED = new Actors.Behavior() {
        
        @Override
        public Actors.Effect apply(Object t, Actors.Context ctx) {
            if (latch.getCount() == 0) {
                return Actors.DIE;
            }
            for (Tick s : caseClassOf(Tick.class, t)) {
                System.out.println(new Date());
                latch.countDown();
            }
            for (Once s : caseClassOf(Once.class, t)) {
                System.out.println("Once happened !!!");
                latch2.countDown();
            }
            return Actors.CONTINUE;
        }
    };
}
