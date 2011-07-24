package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.Actors.ActorRef;
import java.util.ArrayList;
import java.util.List;

public class Master extends Actors.NamedActor {
    
    public Master() {
        super("master");
    }
    
    public static void main(String... args) {
        Actors.startRemoting("localhost", 8080);
        new Master().startActor();
    }
    
    @Override
    public void act() {
        System.out.println("starting broadcasting ...");
        final ActorRef slave1 = Actors.remote("localhost", 8081).forName("slave1");
        final ActorRef slave2 = Actors.remote("localhost", 8082).forName("slave2");
        final ActorRef slave3 = Actors.remote("localhost", 8083).forName("slave3");
        List<ActorRef> slaves = new ArrayList<ActorRef>();
        slaves.add(slave1);
        slaves.add(slave2);
        slaves.add(slave3);
        Actors.Broadcaster broadcaster = new Actors.Broadcaster(slaves);
        for (int i = 0; i < 10; i++) {
            broadcaster.send("Hello " + i);
        }
        broadcaster.send(new Actors.PoisonPill());
        stopActor();
        Actors.stopRemoting();
        System.exit(0);
    }
}
