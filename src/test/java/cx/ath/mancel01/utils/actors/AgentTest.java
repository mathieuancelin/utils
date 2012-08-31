/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.F;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.extra.Agent;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author mathieuancelin
 */
public class AgentTest {
    
    @Test
    public void testAgent() {
        ActorContext ctx = Actors.newContext();
        Agent<Integer> agent = new Agent<Integer>(0, ctx);
        Assert.assertEquals(Integer.valueOf(0), agent.get());
        agent.update(2);
        agent.future().map(new F.Function<Integer, Unit>() {
            @Override
            public Unit apply(Integer t) {
                Assert.assertEquals(Integer.valueOf(2), t);
                return Unit.unit();
            }
        });
        agent.send(new F.Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t) {
                return t * 2;
            }
        });
        agent.future().map(new F.Function<Integer, Unit>() {
            @Override
            public Unit apply(Integer t) {
                Assert.assertEquals(Integer.valueOf(4), t);
                return Unit.unit();
            }
        });
    }
    
}
