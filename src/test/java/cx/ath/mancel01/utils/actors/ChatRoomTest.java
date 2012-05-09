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

import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;

public class ChatRoomTest {
        
    private static CountDownLatch userlatch = new CountDownLatch(12);
    
    public static final Actors.ActorContext actCtx = Actors.newContext();
    
    @Test
    public void testChatRoom() throws Exception {
        User user1 = new User("maurice");
        User user2 = new User("john");
        User user3 = new User("pete");
        Actor room = actCtx.create(new ChatRoom(), "chatroom");
        try {
            room.tell(new Suscribe(user1));
            room.tell(new Suscribe(user2));
            room.tell(new Suscribe(user3));
            Thread.sleep(200);
            user1.send("Hello guys!", room);
            Thread.sleep(200);
            user2.send("Hello too!", room);
            Thread.sleep(200);
            user3.send("Hello user1!", room);
            Thread.sleep(200);
            room.tell(new Unsuscribe(user1));
            Thread.sleep(200);
            room.tell(new Unsuscribe(user2));
            Thread.sleep(200);
            room.tell(new Unsuscribe(user3));
            Thread.sleep(200);
            userlatch.await();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        room.tell(StopSession.INSTANCE);
        room.tell(Poison.PILL);
    }
    
    public static enum StopSession { INSTANCE }
    
    public static class ChatRoom implements Actors.Behavior {
        
        private Map<User, Actor> session = new HashMap<User, Actor>();
        
        @Override
        public Effect apply(Object msg, Context ctx) {
            for (Suscribe value : M.caseClassOf(Suscribe.class, msg)) {
                System.out.println(value.user.name + " suscribed ....");
                userlatch.countDown();
                session.put(value.getUser(), actCtx.create(new UserActor(value.getUser()), value.getUser().name));
            }
            for (Unsuscribe value : M.caseClassOf(Unsuscribe.class, msg)) {
                System.out.println(value.user.name + " unsuscribed ....");
                userlatch.countDown();
                session.get(value.getUser()).tell(Poison.PILL);
                session.remove(value.getUser());
            }
            for (Post value : M.caseClassOf(Post.class, msg)) {
                for (User user : session.keySet()) {
                    if (!user.equals(value.user)) {
                        session.get(user).tell(value, ctx.me);
                    }
                }
            }
            for (StopSession value : M.caseClassOf(StopSession.class, msg)) {
                for (Actor actor : session.values()) {
                    actor.tell(Poison.PILL);
                }
            }
            return Actors.CONTINUE;
        }
   }
    
    public static class UserActor implements Actors.Behavior {
        
        private final User user;

        public UserActor(User user) {
            this.user = user;
        }
        
        @Override
        public Actors.Effect apply(Object t, Actors.Context ctx) {
            for (Post post : M.caseClassOf(Post.class, t)) {
                System.out.println(user.name + " receive " + post.msg);
                userlatch.countDown();
            }
            return Actors.CONTINUE;
        }
    };
    
    public static class Suscribe {
        
        private final User user;

        public Suscribe(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }
    }
    
    public static class Unsuscribe {
        
        private final User user;

        public Unsuscribe(User user) {
            this.user = user;
        }
        
        public User getUser() {
            return user;
        }
    }
    
    public static class Post {
        
        private final String msg;
        
        private final User user;

        public Post(String msg, User user) {
            this.msg = msg;
            this.user = user;
        }

        public String getMsg() {
            return msg;
        }

        public User getUser() {
            return user;
        }
    }
    
    public static class User {
        private final String name;

        public User(String name) {
            this.name = name;
        }
        
        public void send(String msg, Actor room) {
            System.out.println(name + " sent " + msg);
            room.tell(new Post(msg, this));
        }
    }
}
