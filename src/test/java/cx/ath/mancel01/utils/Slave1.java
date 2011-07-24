package cx.ath.mancel01.utils;



import cx.ath.mancel01.utils.F.Action;

public class Slave1 extends Actors.NamedActor {
    
    public Slave1(String name) {
        super(name);
    }
    
    public static void main(String... args) {
        Actors.startRemoting("localhost", 8081);
        new Slave1("slave1").startActor();
    }

    @Override
    public void act() {
        loop(new Action<String>() {
            @Override
            public void apply(String msg) {
                System.out.println("slave 1 received : " + msg);
            }
        });
        Actors.stopRemoting();
        System.exit(0);
    }
}
