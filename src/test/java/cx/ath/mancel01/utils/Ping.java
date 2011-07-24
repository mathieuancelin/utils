package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.Action;
import cx.ath.mancel01.utils.Actors.Actor;
import cx.ath.mancel01.utils.Actors.ActorRef;
import static cx.ath.mancel01.utils.M.*;

public class Ping extends Actors.NamedActor {
    
    public static int ping = 20;
    
    public Ping() {
        super("ping");
    }
    
    public static void main(String... args) throws Exception {
        Actors.startRemoting("localhost", 8889);
        Actor ping = new Ping().startActor();
    }  

    @Override
    public void act() {
        System.out.println("starting ping ...");
        final ActorRef pongActor = Actors.remote("localhost", 8888).forName("pong");
        pongActor.send("ping", asRemoteURL());
        System.out.println("PING");
        loop(new Action<String>() {
            @Override
            public void apply(String msg) {
                for (String value : with(caseStartsWith("pong")).match(msg)) {
                    System.out.println("PING");
                    if (ping == 0) {
                        pongActor.send(new Actors.PoisonPill(), asRemoteURL());
                        System.out.println("STOP PING");
                        stopActor();
                    } else {
                        pongActor.send("ping", asRemoteURL());
                        ping--;
                    }
                }
            }
        });
        Actors.stopRemoting();
        System.exit(0);
    }
}
