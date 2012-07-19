package cx.ath.mancel01.utils;

public class Lenses {
       
    public static class Lens<A, B> implements F.Function<A, B> {
        
        private final F.Function<A, B> get;
        
        private final F.Function<F.Tuple<A, B>, A> set;

        public Lens(F.Function<A, B> get, F.Function<F.Tuple<A, B>, A> set) {
            this.get = get;
            this.set = set;
        }

        @Override
        public B apply(A whole) {
            return get.apply(whole);
        }
        
        public A updated(A whole, B part) {
            return set.apply(new F.Tuple<A, B>(whole, part));
        }
        
        public A mod(A a, F.Function<B, B> f) {
            return set.apply(new F.Tuple<A, B>(a, f.apply(apply(a))));
        }
        
        public A modify(A a, F.Function<B, B> f) {
            return mod(a, f);
        }
        
        public <C> Lens<A, C> andThen(Lens<B, C> that) {
            return that.compose(this);
        }
        
        public <C> Lens<C, B> compose(final Lens<C, A> that) {
            return new Lens<C, B>(new F.Function<C, B>() {
                @Override
                public B apply(C c) {
                    return get.apply(that.get.apply(c));
                }
            }, new F.Function<F.Tuple<C, B>, C>() {
                @Override
                public C apply(final F.Tuple<C, B> t) {
                    return that.mod(t._1, new F.Function<A, A>() {
                        @Override
                        public A apply(A a) {
                            return set.apply(new F.Tuple<A, B>(a, t._2));
                        }
                    });
                }
            });            
        }
        
        public State<A, B> toState() {
            return State.state(new F.Function<A, F.Tuple<A, B>>() {
                @Override
                public F.Tuple<A, B> apply(A a) {
                    return new F.Tuple<A, B>(a, get.apply(a));
                }
            });
        }
        
        public <C> State<A, B> mods(F.Function<B, B> f) {
            return State.state(new F.Function<A, F.Tuple<A, B>>() {
                @Override
                public F.Tuple<A, B> apply(A t) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        } 
        
        public State<A, F.Unit> change(final B b) {
            return State.state(new F.Function<A, F.Tuple<A, F.Unit>>() {
                @Override
                public F.Tuple<A, F.Unit> apply(A t) {
                    return new F.Tuple<A, F.Unit>(set.apply(new F.Tuple<A, B>(t, b)), F.Unit.unit());
                }
            });
        }
        
        public <C> State<A, C> flatMap(final F.Function<B, State<A, C>> f) {
            return State.state(new F.Function<A, F.Tuple<A, C>>() {
                @Override
                public F.Tuple<A, C> apply(A t) {
                    return f.apply(get.apply(t)).apply(t);
                }
            });
        }
        
        public <C> State<A, C> map(final F.Function<B, C> f) {
            return State.state(new F.Function<A, F.Tuple<A, C>>() {
                @Override
                public F.Tuple<A, C> apply(A t) {
                    return new F.Tuple<A, C>(t, f.apply(get.apply(t)));
                }
            });
        }
        
        public static <A> Lens<A, A> self() {
            F.Function<A, A> identity = identity();
            return new Lens<A, A>(identity, new  F.Function<F.Tuple<A, A>, A>() {
                @Override
                public A apply(F.Tuple<A, A> t) {
                    return t._2;
                }
            });
        }
        
        public static <A> Lens<A, F.Unit> trivial() {
            return new Lens<A, F.Unit>(new F.Function<A, F.Unit>() {
                @Override
                public F.Unit apply(A t) {
                    return F.Unit.unit();
                }
            }, new  F.Function<F.Tuple<A, F.Unit>, A>() {
                @Override
                public A apply(F.Tuple<A, F.Unit> t) {
                    return t._1;
                }
            });
        }
        
        public static <A, B> State<A, B> asState(Lens<A, B> lens) {
            return lens.toState();
        }
        
        public static <A, B> Lens<F.Tuple<A, B>, A> first() {
            return new Lens<F.Tuple<A, B>, A>(new F.Function<F.Tuple<A, B>, A>() {
                @Override
                public A apply(F.Tuple<A, B> t) {
                    return t._1;
                }
            }, new F.Function<F.Tuple<F.Tuple<A, B>, A>, F.Tuple<A, B>>() {
                @Override
                public F.Tuple<A, B> apply(F.Tuple<F.Tuple<A, B>, A> t) {
                    return new F.Tuple<A, B>(t._2, t._1._2);
                }
            });
        }
        
        public static <A, B> Lens<F.Tuple<A, B>, B> second() {
            return new Lens<F.Tuple<A, B>, B>(new F.Function<F.Tuple<A, B>, B>() {
                @Override
                public B apply(F.Tuple<A, B> t) {
                    return t._2;
                }
            }, new F.Function<F.Tuple<F.Tuple<A, B>, B>, F.Tuple<A, B>>() {
                @Override
                public F.Tuple<A, B> apply(F.Tuple<F.Tuple<A, B>, B> t) {
                    return new F.Tuple<A, B>(t._1._1, t._2);
                }
            });
        }
        
        private static <A> F.Function<A, A> identity() {
            return new F.Function<A, A>() {
                @Override
                public A apply(A t) {
                    return t;
                }
            };
        }
    }
    
    public static abstract class State<S, A> implements F.Function<S, F.Tuple<S, A>> {
        
        public <B> State<S, B> map(final F.Function<A, B> f) {
            final State<S,A> thisState = this;
            return state(new F.Function<S, F.Tuple<S, B>>() {
                @Override
                public F.Tuple<S, B> apply(S t) {
                    F.Tuple<S, A> tuple = thisState.apply(t);
                    return new F.Tuple<S, B>(tuple._1, f.apply(tuple._2));
                }
            });
        }
        
        public <B> State<S, B> flatMap(final F.Function<A, State<S, B>> f) {
            final State<S,A> thisState = this;
            return state(new F.Function<S, F.Tuple<S, B>>() {
                @Override
                public F.Tuple<S, B> apply(S t) {
                    F.Tuple<S, A> tuple = thisState.apply(t);
                    return f.apply(tuple._2).apply(tuple._1);
                }
            });
        }
        
        public  static <S, A> State<S, A> state(final F.Function<S, F.Tuple<S, A>> f) {
            return new State<S, A>() {
                @Override
                public F.Tuple<S, A> apply(S t) {
                    return f.apply(t);
                }
            };
        }
        
        public static <S> State<S, S> init() {
            return state(new F.Function<S, F.Tuple<S, S>>() {
                @Override
                public F.Tuple<S, S> apply(S t) {
                    return new F.Tuple<S, S>(t, t);
                }
            });
        }
        
        public static <S> State<S, F.Unit> modify(final F.Function<S, S> f) {
            State<S, S> init = init();
            return init.flatMap(new F.Function<S, State<S, F.Unit>>() {
                @Override
                public State<S, F.Unit> apply(final S s) {
                    return state(new F.Function<S, F.Tuple<S, F.Unit>>() {
                        @Override
                        public F.Tuple<S, F.Unit> apply(S t) {
                            return new F.Tuple<S, F.Unit>(f.apply(s), F.Unit.unit());
                        }
                    });
                }
            });
        }
        
        public static <S> State<S, F.Unit> put(final S s) {
            return state(new F.Function<S, F.Tuple<S, F.Unit>>() {
                @Override
                public F.Tuple<S, F.Unit> apply(S t) {
                    return new F.Tuple<S, F.Unit>(s, F.Unit.unit());
                }
            });
        }
    }
}
