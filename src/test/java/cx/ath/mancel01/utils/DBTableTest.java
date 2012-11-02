package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.C.EnhancedList;
import static cx.ath.mancel01.utils.DB.*;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Unit;
import java.sql.Connection;
import junit.framework.Assert;
import org.h2.Driver;
import org.junit.Test;

public class DBTableTest {
    
    public static final DB DB = DB(provider(new Driver(), "jdbc:h2:/tmp/testtable", "sa", ""));
    
    @Test
    public void testParser() {
        SimpleLogger.enableTrace(true);
        DB.withConnection(new Function<Connection, Unit>() {
            @Override
            public Unit apply(Connection _) {
                
                Persons._.ddlDelete();
                
                SQL( 
                    "create table persons (\n" + 
                    "ID                    bigint not null,\n" + 
                    "name                  varchar(1000) not null,\n" + 
                    "surname               varchar(1000) not null,\n" + 
                    "age                   bigint not null,\n" + 
                    "cell                  varchar(1000) not null,\n" +
                    "address               varchar(1000) not null,\n" +
                    "email                 varchar(1000) not null,\n" +
                    "constraint pk_person  primary key (id))\n" +
                    ";"
                ).executeUpdate();
                
                Persons._.insertAll(
                    new Person(1L, "John", "Doe", 42L, "0606060606", "Here", "john.doe@gmail.com"),
                    new Person(2L, "John", "Doe", 16L, "0606060606", "Here", "john.doe@gmail.com"),
                    new Person(3L, "John", "Doe", 90L, "0606060606", "Here", "john.doe@gmail.com")
                );                
                
                Assert.assertEquals(3, Persons._.findAll().size());
                Assert.assertEquals(1, Persons._.findAllBetween(18, 80).size());
                Assert.assertEquals(2, Persons._.findAllBetween(18, 100).size());
                Assert.assertEquals(2, Persons._.findAllBetween(10, 80).size());
                Assert.assertEquals(3, Persons._.findAllBetween(10, 100).size());
                Assert.assertEquals(3, Persons._.count());
                
                Persons._.deleteAll();
                
                Assert.assertEquals(0, Persons._.count());
                
                return Unit.unit();
            }
        });
    }
         
    public static class Person extends Model {
        public Long id;
        public String name;
        public String surname;
        public Long age;
        public String cell;
        public String address;
        public String email;
        
        public Person() {}
                
        public Person(Long id, String name, String surname, Long age, 
                String cell, String address, String email) {
            this.id = id;
            this.name = name;
            this.surname = surname;
            this.age = age;
            this.cell = cell;
            this.address = address;
            this.email = email;
        }

        @Override
        public Long getId() { return id; }
    }

    public static class Persons extends Table<Person> {
        
        public final Extractor<Long> id = get(Long.class, "id");
        public final Extractor<String> name = get(String.class, "name");
        public final Extractor<String> surname = get(String.class, "surname");
        public final Extractor<Long> age = get(Long.class, "age");
        public final Extractor<String> cell = get(String.class, "cell");
        public final Extractor<String> address = get(String.class, "address");
        public final Extractor<String> email = get(String.class, "email");

        public static final Persons _ = new Persons().init(Person.class, "persons");

        @Override
        public ExtractorSeq<Person> all() { return seq(id, name, surname, age, cell, address, email); }
        
        public EnhancedList<Person> findAllBetween(int low, int high) {
            return _.filter( _.age.between(low, high) ).list();
        }
    }
}
