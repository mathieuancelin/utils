package cx.ath.mancel01.utils;



import cx.ath.mancel01.utils.F.Action;

public class Slave3 extends Actors.NamedActor {
    
    public Slave3(String name) {
        super(name);
    }
    
    public static void main(String... args) {
        Actors.startRemoting("localhost", 8083);
        new Slave3("slave3").startActor();
    }

    @Override
    public void act() {
        loop(new Action<String>() {
            @Override
            public void apply(String msg) {
                System.out.println("slave 3 received : " + msg);
            }
        });
        Actors.stopRemoting();
        System.exit(0);
    }
}
