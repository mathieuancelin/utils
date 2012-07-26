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

import cx.ath.mancel01.utils.Concurrent;
import cx.ath.mancel01.utils.F;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.CreationnalContextImpl;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

public class RemoteActors {
    
    private static class RemoteActorMessage implements Serializable {
        String toName;
        String toCtx;
        String remoteFrom;
        String remoteCtx;
        String remoteHost;
        String remotePort;
        Object toMsg;
    }
    
    public static class RemoteActor extends SimpleChannelUpstreamHandler implements Actor {

        private final String host;
        private final int port;
        private final String name;
        private final String context;
        private ChannelFuture channel;
        private ClientBootstrap bootstrap;
        private final RemoteCreationnalContextImpl ctx;
        
        public static RemoteActor ref(String context, String name, String host, int port, RemoteCreationnalContextImpl ctx) {
            String key = context + name + host + port;
            if (!ctx.refs.containsKey(key)) {
                ctx.refs.putIfAbsent(key, new RemoteActor(context, name, host, port, ctx));
            }
            return ctx.refs.get(key);
        }

        private RemoteActor(String context, String name, String host, int port, RemoteCreationnalContextImpl ctx) {
            this.host = host;
            this.port = port;
            this.name = name;
            this.ctx = ctx;
            this.context = context;
            bootstrap = new ClientBootstrap(
                    new NioClientSocketChannelFactory(
                    ctx.clientServerExecutor,
                    ctx.clientBossServerExecutor));
            final SimpleChannelUpstreamHandler handler = this;
            bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    return Channels.pipeline(
                        new ObjectEncoder(),
                        new ObjectDecoder(), handler);
                }
            });
            channel = bootstrap.connect(new InetSocketAddress(host, port));
        }
        
        public void close() {
            channel.awaitUninterruptibly(1, TimeUnit.SECONDS);
            channel.getChannel().disconnect();
            //bootstrap.releaseExternalResources();
        }
        
        private void sendToRemoteActor(Object msg, Actor from) {
            try {             
                RemoteActorMessage message = new RemoteActorMessage();
                message.toName = name;
                message.toMsg = msg;
                message.toCtx = context;
                message.remoteFrom = from.id();
                message.remoteCtx = ctx.id;
                message.remoteHost = ctx.host;
                message.remotePort = ctx.port;
                channel.awaitUninterruptibly().getChannel().write(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, ExceptionEvent e) {
            System.err.println("RemoteActor exception : " + e.getCause());
            e.getChannel().close();
        }

        @Override
        public String id() {
            return "remoteactor://" + host + ":" + String.valueOf(port) + "/" + context + "/" + name;
        }

        @Override
        public String toString() {
            return id();
        }
        
        @Override
        public boolean buzy() {
            return false; // TODO : find a better way
        }
        
        @Override
        public <T> Concurrent.Promise<T> ask(Object message) {
            final String promiseActorName =  UUID.randomUUID().toString();
            final Concurrent.Promise<T> promise = new Concurrent.Promise<T>();
            promise.onRedeem(new F.Action<Concurrent.Promise<T>>() {
                @Override
                public void apply(Concurrent.Promise<T> t) {
                    ctx.scheduleOnce(1, TimeUnit.SECONDS, new Runnable() {
                        @Override
                        public void run() {
                            ((CreationnalContextImpl) ctx).getActors().remove(promiseActorName);
                        }
                    });
                }
            });
            Actor promiseActor = ctx.create(new Actors.Behavior() {
                @Override
                public Actors.Effect apply(Object a, Actors.Context b) {
                    promise.apply((T) a);
                    return Actors.DIE;
                }
            }, promiseActorName);
            tell(message, promiseActor);
            return promise;
        }

        @Override
        public void tell(Object message) {
            sendToRemoteActor(message, Actors.Sink.INSTANCE);
        }

        @Override
        public void tell(Object message, Actor from) {
            sendToRemoteActor(message, from);
        }
    }
    
    private static class RemoteActorHandler extends SimpleChannelUpstreamHandler {

        private final RemoteCreationnalContextImpl context;

        public RemoteActorHandler(RemoteCreationnalContextImpl context) {
            this.context = context;
        }
        
        @Override
        public void messageReceived(
                ChannelHandlerContext ctx, MessageEvent e) {
            if (e.getMessage() instanceof RemoteActorMessage) {
                RemoteActorMessage message = (RemoteActorMessage) e.getMessage();
                Actor from = RemoteActor.ref(message.toCtx, message.remoteFrom, 
                        message.remoteHost, Integer.valueOf(message.remotePort), context);
                try {
                    Actor target = context.lookup(message.toName);
                    if (target != null) {
                        target.tell(message.toMsg, from);
                    } else {
                        Actors.Sink.INSTANCE.tell(message.toMsg, from);
                    }
                } catch (Exception ex) {
                }
            }
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, ExceptionEvent e) {
            System.err.println("RemoteActorHandler exception : " + e.getCause());
            e.getChannel().close();
        }
    }
        
    public static interface RemoteActorContext extends ActorContext {
        
        void startRemoting(String host, int port);
        
        void stopRemoting();
    }
    
    static class RemoteCreationnalContextImpl extends CreationnalContextImpl implements RemoteActorContext {
        
        private ServerBootstrap bootstrap;
        private Channel channel;
        private String host;
        private String port;
        private ExecutorService remotingServerExecutor = 
                Executors.newCachedThreadPool();
        private ExecutorService bossServerExecutor = 
                Executors.newCachedThreadPool();
        private ExecutorService clientServerExecutor = 
                Executors.newCachedThreadPool();
        private ExecutorService clientBossServerExecutor = 
                Executors.newCachedThreadPool();
        private final Properties props;
        private final RemoteActorHandler rah;
        private final ConcurrentHashMap<String, RemoteActor> refs = 
                new ConcurrentHashMap<String, RemoteActor>();

        public RemoteCreationnalContextImpl(String id, Properties props) {
            super(id);
            this.props = props;
            rah = new RemoteActorHandler(this);
        }
        
        public RemoteCreationnalContextImpl(String id, ExecutorService service, Properties props) {
            super(id, service);
            this.props = props;
            rah = new RemoteActorHandler(this);
        }
        
        @Override
        public Actor lookup(String name) {
            String value = props.getProperty(name);
            try {
                if (value != null) {
                    if (!value.toLowerCase().startsWith("local")) {
                        value = value.replace("remoteactor://", "").replace("localactor://", "");
                        String hos = value.split(":")[0];
                        String rest = value.split(":")[1];
                        String por = rest.split("/")[0];
                        String context = rest.split("/")[1];
                        String nam = rest.split("/")[2];
                        return RemoteActor.ref(context, nam, hos, Integer.valueOf(por), this);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return super.lookup(name);
        }

        @Override
        public void startRemoting(String host, int port) {
            this.host = host;
            this.port = String.valueOf(port);
            System.out.print("Starting remoting of " + " @" + host + ":" + port + " ... ");
            try {
                bootstrap = new ServerBootstrap(
                        new NioServerSocketChannelFactory(
                        remotingServerExecutor,
                        bossServerExecutor));
                bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                    @Override
                    public ChannelPipeline getPipeline() throws Exception {
                        return Channels.pipeline(
                            new ObjectEncoder(),
                            new ObjectDecoder(),
                            rah);
                    }
                });
                channel = bootstrap.bind(new InetSocketAddress(host, port));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                System.out.println(" DONE !");
            }
        }

        @Override
        public void stopRemoting() {
            System.out.print("Stopping remoting of " + " @" + host + ":" + port + " ... ");
            try {
                for (RemoteActor act : refs.values()) {
                    act.close();
                }
            } catch(Throwable t) { t.printStackTrace(); }
            try {
                channel.unbind().awaitUninterruptibly(1, TimeUnit.SECONDS);
            } catch(Throwable t) { t.printStackTrace(); }
            try {
                //bootstrap.releaseExternalResources();
            } catch(Throwable t) { t.printStackTrace(); }
            try {
                remotingServerExecutor.shutdown();
                bossServerExecutor.shutdown();
                clientServerExecutor.shutdown();
                clientBossServerExecutor.shutdown();
                remotingServerExecutor.shutdownNow();
                bossServerExecutor.shutdownNow();
                clientServerExecutor.shutdownNow();
                clientBossServerExecutor.shutdownNow();
            } catch(Throwable t) { t.printStackTrace(); }
            remotingServerExecutor = Executors.newCachedThreadPool();
            bossServerExecutor = Executors.newCachedThreadPool();
            clientServerExecutor = Executors.newCachedThreadPool();
            clientBossServerExecutor = Executors.newCachedThreadPool();
            System.out.println("DONE !");
        }

        @Override
        public String toString() {
            return "[REMOTE] " + super.toString();
        }        
    }
    
    private final static ConcurrentHashMap<String, RemoteActorContext> CTXS = new ConcurrentHashMap<String, RemoteActorContext>();

    public static RemoteActorContext newContext(Properties props) {
        return newContext(UUID.randomUUID().toString(), props);        
    } 
    
    public static RemoteActorContext newContext(ExecutorService service, Properties props) {
        return newContext(UUID.randomUUID().toString(), service, props);        
    } 
    
    public static RemoteActorContext newContext(String id, Properties props) {
        if (!CTXS.containsKey(id)) {
            RemoteCreationnalContextImpl c = new RemoteCreationnalContextImpl(id, props);
            CTXS.putIfAbsent(id, c);
        }
        return CTXS.get(id);       
    } 
    
    public static RemoteActorContext newContext(String id, ExecutorService service, Properties props) {
        if (!CTXS.containsKey(id)) {
            RemoteCreationnalContextImpl c = new RemoteCreationnalContextImpl(id, service, props);
            CTXS.putIfAbsent(id, c);
        }
        return CTXS.get(id);
    } 
}
