package cx.ath.mancel01.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import cx.ath.mancel01.utils.C.Function;
import cx.ath.mancel01.utils.F.Option;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Small Actors library for multi-threaded programming models.
 * 
 * @author Mathieu ANCELIN
 */
public class Actors {

    private static final ConcurrentHashMap<String, ActorRef> actors =
            new ConcurrentHashMap<String, ActorRef>();
    
    private static HttpServer remotingServer;
    
    private static final ExecutorService remotingServerExecutor = Executors.newCachedThreadPool();

    private static ActorRef getActor(String name) {
        return actors.get(name);
    }

    private static void register(String name, ActorRef actor) {
        actors.putIfAbsent(name, actor);
    }

    private static void unregister(String name) {
        actors.remove(name);
    }

    public static Option<ActorRef> forName(String name) {
        return Option.maybe(Actors.getActor(name));
    }

    public static void shutdownAll() {
        Actor.executor.shutdownNow();
    }

    public static void startRemoting(String host, int port) {
        try {
            remotingServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            remotingServer.setExecutor(remotingServerExecutor);
            remotingServer.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange he) throws IOException {
                    System.out.println("req ...");
                    String body = slurpBody(he.getRequestBody());
                    System.out.println(body);
                    String[] params = body.split("&");
                    String remoteName = "";
                    String remoteFrom = "";
                    String remoteHost = "";
                    String remotePort = "";
                    String remoteMsg = "";
                    for (String param : params) {
                        if (param.startsWith("remote-name")) {
                            remoteName = param.replace("remote-name=", "");
                        }
                        if (param.startsWith("remote-from")) {
                            remoteFrom = param.replace("remote-from=", "");
                        }
                        if (param.startsWith("remote-host")) {
                            remoteHost = param.replace("remote-host=", "");
                        }
                        if (param.startsWith("remote-port")) {
                            remotePort = param.replace("remote-port=", "");
                        }
                        if (param.startsWith("remote-msg")) {
                            remoteMsg = param.replace("remote-msg=", "");
                        }
                    }
                    Option<ActorRef> ref = forName(remoteName);
                    if (ref.isDefined()) {
                        try {
                            ref.get().send(fromString(remoteMsg), 
                                    "remote://" + remoteHost
                                    + ":" + remotePort
                                    + "/" + remoteFrom);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    he.sendResponseHeaders(200, 0);
                    he.getResponseBody().flush();
                    he.close();
                    System.out.println("end req");
                }
                
                private String slurpBody(InputStream body) {
                    if (body == null)
                        return "null";
                    try {
                        StringBuilder out = new StringBuilder();
                        byte[] b = new byte[4096];
                        for (int n; (n = body.read(b)) != -1;) {
                            out.append(new String(b, 0, n));
                        }
                        return out.toString();
                    } catch (IOException ex) {
                        return "IO/error";
                    }
                }
            });
            remotingServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ActorStore remote(final String host, final int port) {
        return new ActorStore() {
            @Override
            public ActorRef forName(final String name) {
                return new RemoteActorRef(name, host, port);
            }
        };
    }

    public static interface ActorStore {

        ActorRef forName(String name);
    }

    public static interface ActorRef {

        void send(Object msg);

        void send(Object msg, String from);
    }

    public static interface LocalActorRef extends ActorRef {

        void send(Object msg, ActorRef from);
    }

    public static abstract class Actor extends Thread implements LocalActorRef {

        private static final BlockingQueue<Runnable> tasks = new ArrayBlockingQueue<Runnable>(500);
        private static ExecutorService executor = Executors.newCachedThreadPool();

        static {
            Runtime.getRuntime().addShutdownHook(
                new Thread() {

                    @Override
                    public void run() {
                        executor.shutdownNow();
                        remotingServer.stop(0);
                        remotingServerExecutor.shutdownNow();
                    }
                });
        }

        private static class Message {

            Object payload;
            Option<ActorRef> sender;

            public Message(Object payload, Option<ActorRef> sender) {
                this.payload = payload;
                this.sender = sender;
            }
        }
        private ConcurrentLinkedQueue<Message> mailbox =
                new ConcurrentLinkedQueue<Message>();
        private Function<Object> react;
        private AtomicBoolean started = new AtomicBoolean(false);
        private AtomicBoolean buzy = new AtomicBoolean(false);
        protected Option<ActorRef> sender = Option.none();
        protected final String uuid;

        public Actor() {
            uuid = UUID.randomUUID().toString();
            Actors.register(uuid, this);
        }

        public void unregister() {
            Actors.unregister(uuid);
        }

        protected void before() {
        }

        protected void after() {
        }

        @Override
        public void run() {
            mailbox.clear();
            started.compareAndSet(false, true);
            before();
            act();
            after();
            started.compareAndSet(true, false);
        }

        public abstract void act();

        public final Actor me() {
            return this;
        }

        /**
         * Method to loop the reception function to read all messages
         * in the mailbox one by one. 
         * Once launch, the loop will end with the stopActor method
         * or with a poison pill.
         */
        public final void loop(Function react) {
            this.react = react;
            while (started.get()) {
                Message ret = mailbox.poll();
                if (ret != null) {
                    if (ret.payload.getClass().equals(PoisonPill.class)) {
                        stopActor();
                    } else {
                        sender = ret.sender;
                        buzy.compareAndSet(false, true);
                        react.apply(ret.payload);
                        buzy.compareAndSet(true, false);
                        sender = Option.none();
                    }
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        /**
         * Read only one message in the mailbox with the function.
         */
        public final void react(Function<Object> react) {
            this.react = react;
            Message ret = mailbox.poll();
            if (ret != null) {
                if (ret.payload.getClass().equals(PoisonPill.class)) {
                    stopActor();
                } else {
                    sender = ret.sender;
                    buzy.compareAndSet(false, true);
                    react.apply(ret.payload);
                    buzy.compareAndSet(true, false);
                    sender = Option.none();
                }
            }
        }

        @Override
        public final void send(Object msg) {
            Option<ActorRef> opt = Option.none();
            mailbox.add(new Message(msg, opt));
        }

        @Override
        public final void send(Object msg, ActorRef from) {
            Option<ActorRef> opt = Option.maybe(from);
            mailbox.add(new Message(msg, opt));
        }

        @Override
        public final void send(Object msg, String from) {
            if (from.startsWith("remote://")) {
                String url = from.replace("remote://", "");
                String host = url.split(":")[0];
                String rest = url.split(":")[1];
                String port = rest.split("/")[0];
                String name = rest.split("/")[1];
                send(msg, new RemoteActorRef(name, host, Integer.valueOf(port)));
            } else {
                send(msg, Actors.getActor(from));
            }
        }

        public final Actor stopActor() {
            started.compareAndSet(true, false);
            return this;
        }

        public final Actor startActor() {
            started.compareAndSet(false, true);
            executor.submit(this);
            return this;
        }
    }
    
    public static class RemoteActorRef implements ActorRef {
        
        private final String host;
        
        private final int port;
        
        private final String name;

        public RemoteActorRef(String name, String host, int port) {
            this.host = host;
            this.port = port;
            this.name = name;
        }
        
        @Override
        public void send(Object msg) {
            sendToRemoteActor(msg, "blackhole");
        }

        @Override
        public void send(Object msg, String from) {
            sendToRemoteActor(msg, from);
        }

        private void sendToRemoteActor(Object msg, String from) {
            try {
                if (!msg.getClass().equals(String.class)) {
                    throw new RuntimeException("can't send anything than string");
                }
                String data = URLEncoder.encode("remote-name", "UTF-8") + "=" + URLEncoder.encode(name, "UTF-8");
                data += "&" + URLEncoder.encode("remote-msg", "UTF-8") + "=" + URLEncoder.encode(Actors.toString(msg), "UTF-8");
                data += "&" + URLEncoder.encode("remote-from", "UTF-8") + "=" + URLEncoder.encode(from, "UTF-8");
                data += "&" + URLEncoder.encode("remote-host", "UTF-8") + "=" + URLEncoder.encode(remotingServer.getAddress().getHostName(), "UTF-8");
                data += "&" + URLEncoder.encode("remote-port", "UTF-8") + "=" + URLEncoder.encode(remotingServer.getAddress().getPort() + "", "UTF-8");                            
                URL url = new URL("http://" + host + ":" + port + "/");
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.flush();
                conn.connect();
                wr.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class LoadBalancer {

        private final List<Actor> actors;
        private Iterator<Actor> it;

        public LoadBalancer(List<Actor> actors) {
            this.actors = actors;
            this.it = actors.iterator();
        }

        public final void send(Object msg) {
            if (msg.getClass().equals(Broadcast.class)) {
                Broadcast b = (Broadcast) msg;
                broadcast(b.message, b.from.getOrElse(null));
            } else {
                chooseAndSend(msg, null);
            }
        }

        public final void send(Object msg, Actor from) {
            if (msg.getClass().equals(Broadcast.class)) {
                Broadcast b = (Broadcast) msg;
                broadcast(b.message, b.from.getOrElse(from));
            } else {
                chooseAndSend(msg, from);
            }
        }

        private void chooseAndSend(Object msg, Actor from) {
            if (!it.hasNext()) {
                it = actors.iterator();
            }
            Actor a = it.next();
            if (!a.buzy.get()) {
                a.send(msg, from);
            } else {
                boolean sent = false;
                for (Actor bis : actors) {
                    if (!bis.buzy.get()) {
                        a.send(msg, from);
                        sent = true;
                    }
                }
                if (!sent) {
                    a.send(msg, from);
                }
            }
        }

        private void broadcast(Object message, Actor from) {
            for (Actor actor : actors) {
                actor.send(message, from);
            }
        }
    }

    public static class Broadcaster {

        private final List<Actor> actors;

        public Broadcaster(List<Actor> actors) {
            this.actors = actors;
        }

        public final void send(Object msg) {
            for (Actor actor : actors) {
                actor.send(msg);
            }
        }

        public final void send(Object msg, Actor from) {
            for (Actor actor : actors) {
                actor.send(msg, from);
            }
        }
    }

    public static abstract class NamedActor extends Actor {

        protected final String name;

        public NamedActor(String name) {
            super();
            this.name = name;
            Actors.unregister(uuid);
            Actors.register(name, this);
        }

        public String name() {
            return name;
        }

        @Override
        public void unregister() {
            Actors.unregister(name);
        }
    }

    public static class Broadcast {

        private final Object message;
        private final Option<Actor> from;

        public Broadcast(Object message, Option<Actor> from) {
            this.message = message;
            this.from = from;
        }

        public Broadcast(Object message) {
            this.message = message;
            this.from = Option.none();
        }

        public Option<Actor> from() {
            return from;
        }

        public Object message() {
            return message;
        }
    }

    public static class PoisonPill {}

    public static class Kill {}
    
    private static Object fromString(String s) 
            throws IOException ,ClassNotFoundException {
        byte [] data = s.getBytes("UTF-8");
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o  = ois.readObject();
        ois.close();
        return o;
    }
    private static String toString(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return new String(baos.toByteArray());
    }
}