/*
 *  Copyright 2011 Mathieu ANCELIN
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
package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.ExceptionWrapper;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.actors.Actors;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class Concurrent {

    public static class Promise<V> implements Future<V>, F.Action<V> {

        private final CountDownLatch taskLock = new CountDownLatch(1);
        
        private boolean cancelled = false;
        
        private List<F.Action<Promise<V>>> callbacks = new ArrayList<F.Action<Promise<V>>>();
        
        private boolean invoked = false;
        
        private V result = null;
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return cancelled;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return invoked;
        }

        public V getOrNull() {
            return result;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            taskLock.await();
            return result;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            taskLock.await(timeout, unit);
            return result;
        }

        @Override
        public void apply(V result) {
            synchronized (this) {
                if (!invoked) {
                    invoked = true;
                    this.result = result;
                    taskLock.countDown();
                } else {
                    return;
                }
            }
            for (F.Action<Promise<V>> callback : callbacks) {
                callback.apply(this);
            }
        }

        public void onRedeem(F.Action<Promise<V>> callback) {
            synchronized (this) {
                if (!invoked) {
                    callbacks.add(callback);
                }
            }
            if (invoked) {
                callback.apply(this);
            }
        }
        
        public <B> Promise<B> map(final Function<V, B> map) {
            final Promise<B> promise = new Promise<B>();
            this.onRedeem(new F.Action<Promise<V>>() {
                @Override
                public void apply(Promise<V> t) {
                    try {
                        promise.apply(map.apply(t.get()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            return promise;
        }
        public <B> Promise<B> flatmap(final Function<V, Promise<B>> map) {
            final Promise<B> promise = new Promise<B>();
            this.onRedeem(new F.Action<Promise<V>>() {
                @Override
                public void apply(Promise<V> t) {
                    try {
                        promise.apply(map.apply(t.get()).get());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            return promise;
        }
        public static <T> Promise<List<T>> waitAll(final Promise<T>... promises) {
            return waitAll(Arrays.asList(promises));
        }

        public static <T> Promise<List<T>> waitAll(final Collection<Promise<T>> promises) {
            final CountDownLatch waitAllLock = new CountDownLatch(promises.size());
            final Promise<List<T>> result = new Promise<List<T>>() {

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    boolean r = true;
                    for (Promise<T> f : promises) {
                        r = r & f.cancel(mayInterruptIfRunning);
                    }
                    return r;
                }

                @Override
                public boolean isCancelled() {
                    boolean r = true;
                    for (Promise<T> f : promises) {
                        r = r & f.isCancelled();
                    }
                    return r;
                }

                @Override
                public boolean isDone() {
                    boolean r = true;
                    for (Promise<T> f : promises) {
                        r = r & f.isDone();
                    }
                    return r;
                }

                @Override
                public List<T> get() throws InterruptedException, ExecutionException {
                    waitAllLock.await();
                    List<T> r = new ArrayList<T>();
                    for (Promise<T> f : promises) {
                        r.add(f.get());
                    }
                    return r;
                }

                @Override
                public List<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    waitAllLock.await(timeout, unit);
                    return get();
                }
            };
            final F.Action<Promise<T>> action = new F.Action<Promise<T>>() {
                @Override
                public void apply(Promise<T> completed) {
                    waitAllLock.countDown();
                    if (waitAllLock.getCount() == 0) {
                        try {
                            result.apply(result.get());
                        } catch (Exception e) {
                            throw new ExceptionWrapper(e);
                        }
                    }
                }
            };
            for (Promise<T> f : promises) {
                f.onRedeem(action);
            }
            return result;
        }

        public static <T> Promise<T> waitAny(final Promise<T>... futures) {
            final Promise<T> result = new Promise<T>();
            final F.Action<Promise<T>> action = new F.Action<Promise<T>>() {
                @Override
                public void apply(Promise<T> completed) {
                    synchronized (this) {
                        if (result.isDone()) {
                            return;
                        }
                    }
                    result.apply(completed.getOrNull());
                }
            };
            for (Promise<T> f : futures) {
                f.onRedeem(action);
            }
            return result;
        }
        
        public static <T> Promise<T> future(final F.Callable<T> callable) {
            final ActorContext context = Actors.newContext();
            return context.now(callable);
        }
        
        public static <T> Promise<T> future(long in, TimeUnit unit, F.Callable<T> callable) {
            final ActorContext context = Actors.newContext();
            return context.scheduleOnce(in, unit, callable);
        }
        
        public static <T> Promise<T> future(ActorContext context, final F.Callable<T> callable) {
            return context.now(callable);
        }
        
        public static <T> Promise<T> future(ActorContext context, long in, TimeUnit unit, F.Callable<T> callable) {
            return context.scheduleOnce(in, unit, callable);
        }
    }
}