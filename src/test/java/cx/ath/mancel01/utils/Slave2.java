package cx.ath.mancel01.utils;



import cx.ath.mancel01.utils.F.Action;

public class Slave2 extends Actors.NamedActor {
    
    public Slave2(String name) {
        super(name);
    }
    
    public static void main(String... args) {
        Actors.startRemoting("localhost", 8082);
        new Slave2("slave2").startActor();
    }

    @Override
    public void act() {
        loop(new Action<String>() {
            @Override
            public void apply(String msg) {
                System.out.println("salve 2 received : " + msg);
            }
        });
        Actors.stopRemoting();
        System.exit(0);
    }
}
