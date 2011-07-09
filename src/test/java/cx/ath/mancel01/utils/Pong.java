package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.Action;
import static cx.ath.mancel01.utils.M.*;

public class Pong extends Actors.NamedActor {
    
    public Pong() {
        super("pong");
    }
    
    public static void main(String... args) {
        Actors.startRemoting("localhost", 8888);
        new Pong().startActor();
    }

    @Override
    public void act() {
        System.out.println("starting pong ...");
        loop(new Action<String>() {
            @Override
            public void apply(String msg) {
                for (String value : with(caseStartsWith("ping")).match(msg)) {
                    System.out.println("PONG");
                    if (sender.isDefined()) {
                        sender.get().send("pong");
                    }
                }
                for (String value : with(caseStartsWith("stop")).match(msg)) {
                    System.out.println("STOP PONG");    
                    stopActor();
                    Actors.stopRemoting();
                }
            }
        });
    }
}
