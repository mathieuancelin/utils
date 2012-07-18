package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.C.EnhancedList;
import cx.ath.mancel01.utils.F.ExceptionWrapper;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Option;
import cx.ath.mancel01.utils.F.Unit;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Data {
    
    public static interface Identifiable<ID> {
        ID getId();
    }
   
    
    public static interface Identifier<A, ID> {
        public ID of(A obj);
    }
    
    public static interface Pagination {}
    
    public static abstract class Page implements Pagination {
        private final int page;
        private final int size;

        public Page(int page) {
            this.page = page;
            this.size = 25;
        }
        
        public Page(int page, int size) {
            this.page = page;
            this.size = size;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }
    }
    
    public static class Full extends Page {

        public Full() {
            super(-1);
        }
    }
    
    public static final Pagination FULL = new Full();
        
    public static class Bocal<A,ID> { 
        
        private final Identifier<A, ID> identifier;
                
        private final EnhancedList<A> store = C.eList();
        
        public static <ID, T extends Identifiable<ID>> Bocal<T, ID> identifiableBocal(final Class<T> clazz) {
            return new Bocal<T, ID>(clazz);
        }
        
        Bocal(final Class<? extends Identifiable<ID>> clazz) {
            this.identifier = new Identifier<A, ID>() {
                public ID of(A obj) {
                    return clazz.cast(obj).getId();
                }
            };
        }
        
        public Bocal() {
            this.identifier = new Identifier<A, ID>() {
                private Class clazz;
                private Field f;
                @Override
                public ID of(A obj) {
                    try {
                        if (clazz == null && f == null) {
                            clazz = obj.getClass();
                            f = clazz.getDeclaredField("id");
                            f.setAccessible(true);
                        }
                        return (ID) f.get(obj); 
                    } catch (Exception e) {
                        throw new ExceptionWrapper(e);
                    }
                }
            };
        }
        
        public Bocal(Identifier<A, ID> identifier) {
            this.identifier = identifier;
        }

	public Option<A> findById(final ID id) {
            return store.find(new F.Function<A, Boolean>() {
                @Override
                public Boolean apply(A item) {
                    return identifier.of(item).equals(id);
                }
            });
	}

	public EnhancedList<A> findAll(Pagination pagination) {
            return paginate(store, pagination);
	}
        
        public EnhancedList<A> findAll() {
            return paginate(store, FULL);
	}

	public EnhancedList<A> findBy(Function<A, Boolean> p, Pagination pagination) {
            return paginate(store.filter(p), pagination);
	}
        
        public EnhancedList<A> findBy(Function<A, Boolean> p) {
            return paginate(store.filter(p), FULL);
	}

	public Option<A> findOneBy(Function<A, Boolean> p) {
            return findBy(p).headOption();
	}

	public void delete(ID id) {
            for (A item : findById(id)) {
                store.rem(item);
            }
	}

	public void delete(EnhancedList<ID> ids) {
            ids.foreach(new Function<ID, Unit>() {
                @Override
                public Unit apply(ID t) {
                    delete(t);
                    return Unit.unit();
                }
            });
	}

	public void delete(final Function<A, Boolean> p) {
            delete(findBy(p).map(new Function<A, ID>() {
                @Override
                public ID apply(A t) {
                    return identifier.of(t);
                }
            }));
	}

	public A save(A item) {
            delete(identifier.of(item));
            store._(item);
            return item;
	}

	public void save(EnhancedList<A> items) {
            items.foreach(new F.Function<A, F.Unit>() {
                @Override
                public Unit apply(A item) {
                    save(item);
                    return Unit.unit();
                }
            });
	}

	public long count(Function<A, Boolean> p) {
            return (long) findBy(p).size();
	}

	public void clear() {
            store.clear();
	}

	public void reset(List<A> withItems) {
            clear();
            counter.set(0);
            store._(withItems);
	}
        
        public void reset() {
            reset(C.<A>eList());
	}

	public long size() {
            return store.size();
	}
        
	private EnhancedList<A> paginate(EnhancedList<A> items, Pagination pagination) {
            for (Full full : M.caseClassOf(Full.class, pagination)) {
                return items;
            }
            for (Page page : M.caseClassOf(Page.class, pagination)) {
                return items.drop((page.getPage() - 1) * page.getSize()).takeLeft(page.getSize());
            }
            throw new RuntimeException("Should not happen");
	}

	private AtomicLong counter = new AtomicLong(0);

	public Long autoIncrement() {
            return counter.getAndIncrement();
	}

	public String UUID() {
            return Long.toString(autoIncrement(), 24);
	}
    }
}

