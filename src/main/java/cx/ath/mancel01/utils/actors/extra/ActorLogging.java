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

package cx.ath.mancel01.utils.actors.extra;

import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.SimpleLogger;
import cx.ath.mancel01.utils.actors.Actors;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.extra.ActorLogging.Debug;
import cx.ath.mancel01.utils.actors.extra.ActorLogging.Info;
import cx.ath.mancel01.utils.actors.extra.ActorLogging.LoggerAdapter;
import cx.ath.mancel01.utils.actors.extra.ActorLogging.Warning;

public class ActorLogging {

    public static interface LoggerAdapter {

        void error(String name, String message, Object... printable);

        void warning(String name, String message, Object... printable);

        void info(String name, String message, Object... printable);

        void debug(String name, String message, Object... printable);
    }

    public static interface AsyncLogger {

        void error(String message, Object... printable);

        void warning(String message, Object... printable);

        void info(String message, Object... printable);

        void debug(String message, Object... printable);
    }

    public static class LoggerMessage {

        public final String name;
        public final String message;
        public final Object[] printable ;

        public LoggerMessage(String name, String message, Object... printable) {
            this.name = name;
            this.message = message;
            this.printable = printable;
        }
    }

    public static class Error extends LoggerMessage {

        public Error(String name, String message, Object... printable) {
            super(name, message, printable);
        }
    }

    public static class Warning extends LoggerMessage {

        public Warning(String name, String message, Object... printable) {
            super(name, message, printable);
        }
    }

    public static class Info extends LoggerMessage {

        public Info(String name, String message, Object... printable) {
            super(name, message, printable);
        }
    }

    public static class Debug extends LoggerMessage {

        public Debug(String name, String message, Object... printable) {
            super(name, message, printable);
        }
    }

    public static AsyncLogger asyncLog(String name, ActorContext context) {
        return new AsyncLoggerClient(context, name);
    }
    
    private static class AsyncLoggerClient implements AsyncLogger {
    
        private final ActorContext ctx;
        private final Actor queueRef;
        private final String name;

        public AsyncLoggerClient(ActorContext ctx, String name) {
            this.ctx = ctx;
            this.name = name;
            queueRef = ctx.create(new LoggerQueue(), LOGGER_BEHAVIOR_KEY);
        }

        @Override
        public void error(String message, Object... printable) {
            queueRef.tell(new Error(name, message, printable));
        }

        @Override
        public void warning(String message, Object... printable) {
            queueRef.tell(new Warning(name, message, printable));
        }

        @Override
        public void info(String message, Object... printable) {
            queueRef.tell(new Info(name, message, printable));
        }

        @Override
        public void debug(String message, Object... printable) {
            queueRef.tell(new Debug(name, message, printable));
        }
    }

    private static final String LOGGER_BEHAVIOR_KEY = "ActorContextOnlyLogger";

    private static Function<Unit, LoggerAdapter> adapterFactory = new Function<Unit, LoggerAdapter>() {
        @Override
        public LoggerAdapter apply(Unit unit) {
                return new SimpleAsyncLogger();
        }
    };

    public static void setLoggerAdapter(Function<Unit, LoggerAdapter> factory) {
        ActorLogging.adapterFactory = factory;
    }

    private static class LoggerQueue implements Behavior {

        private final LoggerAdapter logger = ActorLogging.adapterFactory.apply(Unit.unit());

        @Override
        public Effect apply(Object t, Actors.Context ctx) {
            for (Error it : M.caseClassOf(Error.class, t)) {
                logger.error(it.name, it.message, it.printable);
            }
            for (Warning it : M.caseClassOf(Warning.class, t)) {
                logger.warning(it.name, it.message, it.printable);
            }
            for (Info it : M.caseClassOf(Info.class, t)) {
                logger.info(it.name, it.message, it.printable);
            }
            for (Debug it : M.caseClassOf(Debug.class, t)) {
                logger.debug(it.name, it.message, it.printable);
            }
            return Actors.CONTINUE;
        }
    };

    public static class SimpleAsyncLogger implements LoggerAdapter {

        @Override
        public void error(String name, String message, Object... printable) {
            SimpleLogger.error(name + " : " + message, printable);
        }

        @Override
        public void warning(String name, String message, Object... printable) {
            SimpleLogger.error(name + " : " + message, printable);
        }

        @Override
        public void info(String name, String message, Object... printable) {
            SimpleLogger.info(name + " : " + message, printable);
        }

        @Override
        public void debug(String name, String message, Object... printable) {
            SimpleLogger.trace(name + " : " + message, printable);
        }
    }
}
