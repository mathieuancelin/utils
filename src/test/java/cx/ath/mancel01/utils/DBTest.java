package cx.ath.mancel01.utils;

import static cx.ath.mancel01.utils.DB.*;
import cx.ath.mancel01.utils.Data.Identifiable;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Unit;
import java.sql.Connection;
import java.util.*;
import junit.framework.Assert;
import org.h2.Driver;
import org.junit.Test;

public class DBTest {
    
    @Test
    public void testParser() {
        SimpleLogger.enableTrace(true);
        DB.withConnection(new Function<Connection, Unit>() {
            @Override
            public Unit apply(Connection _) {
                SQL("drop table if exists persons;").executeUpdate();
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
                SQL("insert into persons values ( {id}, {name}, {surname}, {age}, {cell}, {address}, {email} );" )
                    .on(pair("id", 1), pair("name", "John"), pair("surname", "Doe"), 
                        pair("age", 42), pair("cell", "0606060606"), pair("address", "Here"), 
                        pair("email", "john.doe@gmail.com")).executeUpdate();
                SQL("insert into persons values ( {id}, {name}, {surname}, {age}, {cell}, {address}, {email} );" )
                    .on(pair("id", 2), pair("name", "John"), pair("surname", "Doe"), 
                        pair("age", 16), pair("cell", "0606060606"), pair("address", "Here"), 
                        pair("email", "john.doe@gmail.com")).executeUpdate();
                SQL("insert into persons values ( {id}, {name}, {surname}, {age}, {cell}, {address}, {email} );" )
                    .on(pair("id", 3), pair("name", "John"), pair("surname", "Doe"), 
                        pair("age", 90), pair("cell", "0606060606"), pair("address", "Here"), 
                        pair("email", "john.doe@gmail.com")).executeUpdate();
                return Unit.unit();
            }
        });
        
        setParser( Person.personParser );
        Assert.assertEquals(3, Person.findAll().size());
        Assert.assertEquals(1, Person.findAllBetween(18, 80).size());
        Assert.assertEquals(2, Person.findAllBetween(18, 100).size());
        Assert.assertEquals(2, Person.findAllBetween(10, 80).size());
        Assert.assertEquals(3, Person.findAllBetween(10, 100).size());
        
        setParser( Person.personParserRefl );
        Assert.assertEquals(3, Person.findAll().size());
        Assert.assertEquals(1, Person.findAllBetween(18, 80).size());
        Assert.assertEquals(2, Person.findAllBetween(18, 100).size());
        Assert.assertEquals(2, Person.findAllBetween(10, 80).size());
        Assert.assertEquals(3, Person.findAllBetween(10, 100).size());
        
        Assert.assertEquals(3, Person.count());
        
        DB.withConnection(new Function<Connection, Unit>() {
            @Override
            public Unit apply(Connection _) {
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
    
    public static SQLParser<Person> parser;

    public static SQLParser<Person> getParser() {
        return DBTest.parser;
    }

    public static void setParser(SQLParser<Person> p) {
        DBTest.parser = p;
    }
    
    public static final DB DB = DB(provider(new Driver(), "jdbc:h2:/tmp/test", "sa", ""));
     
    public static class Person implements Identifiable<Long> {

        public Long id;
        public String name;
        public String surname;
        public Long age;
        public String cell;
        public String address;
        public String email;
        
        public static final SQLParser personParserRefl = parser(Person.class,
            get(Long.class, "id"),
            get(String.class, "name"),
            get(String.class, "surname"),
            get(Long.class, "age"),
            get(String.class, "cell"),
            get(String.class, "address"),
            get(String.class, "email")
        ).mapWithFieldsReflection();

        public static final SQLParser personParser = parser(Person.class,
            get(Long.class, "id"),
            get(String.class, "name"),
            get(String.class, "surname"),
            get(Long.class, "age"),
            get(String.class, "cell"),
            get(String.class, "address"),
            get(String.class, "email")
        ).map(new Function<TypedContainer, Person>() {
            @Override
            public Person apply(TypedContainer _) {
                return new Person(
                    _.lng("id"),
                    _.str("name"),
                    _.str("surname"),
                    _.lng("age"),
                    _.str("cell"),
                    _.str("address"),
                    _.str("email")
                );
            }
        });
        
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

        public static int count() {
            return DB.withConnection(new Function<Connection, Integer>() {
                @Override
                public Integer apply(Connection _) {
                    return SQL("select count(*) as p from persons")
                            .asSingleOpt(integerParser("p")).getOrElse(0);
                }
            });
        }
        
        public static List<Person> findAll() {
            return DB.withConnection(new Function<Connection, List<Person>>() {
                @Override
                public List<Person> apply(Connection _) {
                    return 
                        SQL("SELECT id, name, surname, age, cell, address, email FROM Persons")
                            .asList( getParser() );
                }
            });
        }
        
        public static List<Person> findAllBetween(final int low, final int high) {
            return DB.withConnection(new Function<Connection, List<Person>>() {
                @Override
                public List<Person> apply(Connection conn) {
                    return sql(conn, 
                        "SELECT id, name, surname, age, cell, address, email " +
                        "FROM Persons WHERE age > {low} AND age < {high}")
                            .on( pair("low", low), pair("high", high) )
                            .asList( getParser() );
                }
            });
        }
        
        @Override
        public String toString() {
            return "Person{" + "id=" + id + ", name=" + name 
                + ", surname=" + surname + ", age=" + age 
                + ", cell=" + cell + ", address=" + address 
                + ", email=" + email + '}';
        }

        @Override
        public Long getId() {
            return id;
        }
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
        public ExtractorSeq all() { return seq(id)._(name)._(surname)._(age)._(cell)._(address)._(email); }
        
        public List<Person> findAllBetween(final int low, final int high) {
            return sql("SELECT id, name, surname, age, cell, address, email " +
                "FROM Persons WHERE age > {low} AND age < {high}")
                    .on( pair("low", low), pair("high", high) ).asList( parser );
        }
    }
}
