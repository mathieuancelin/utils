package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.Data.Bocal;
import cx.ath.mancel01.utils.F.Function;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class BocalTest {  
    
    public static class User {

        public String name;
        public String email;
        
        public static final Bocal<User, String> bocal = new Bocal<User, String>(new Data.Identifier<User, String>() {
            @Override
            public String of(User obj) {
                return obj.email;
            }
        });

        public User(String email, String name) {
            this.name = name;
            this.email = email;
        }

        @Override
        public String toString() {
            return "User{" + "name=" + name + ", email=" + email + '}';
        }
    }

    public static class Project {
        
        public static final Bocal<Project, String> bocal = new Bocal<Project, String>();
        
        public Long id = bocal.autoIncrement();
        public String name;
        public String owner;
        public Collection<String> members = C.eList();

        public Project(String name, String owner) {
            this.name = name;
            this.owner = owner;
        }

        public Project(String name, String owner, Collection<String> members) {
            this.name = name;
            this.owner = owner;
            this.members = members;
        }

        @Override
        public String toString() {
            return "Project{" + "id=" + id + ", name=" + name + ", owner=" + owner + ", members=" + members + '}';
        }
        
    }
    

    @Before
    public void before() {
        User.bocal.reset(C.eList(
            new User("bob@gmail.com", "Bob D."),
            new User("nicolas@gmail.com", "Nicolas S."),
            new User("jean@gmail.com", "Jean CV.")));
        Project.bocal.reset(C.eList(
            new Project("Secret", "jean@gmail.com"),
            new Project("Ariane", "nicolas@gmail.com", 
                C.eList("bob@gmail.com", "jean@gmail.com"))));
    }

    @Test
    public void findAll() {
        Assert.assertEquals(3, User.bocal.findAll().size());
    }
    
    @Test
    public void findUserbyemail() {
        Assert.assertTrue(User.bocal.findById("bob@gmail.com").isDefined());
        Assert.assertFalse(User.bocal.findById("kiki@gmail.com").isDefined());
    }
    
    @Test
    public void findUserbynamelike() {
        Assert.assertTrue(User.bocal.findOneBy(new Function<User, Boolean>() {

            @Override
            public Boolean apply(User input) {
                return input.name.contains("Jean");
            }
        }).isDefined());
        Assert.assertFalse(User.bocal.findOneBy(new Function<User, Boolean>() {

            @Override
            public Boolean apply(User input) {
                return input.name.contains("Guillaume");
            }
        }).isDefined());
    }
    
    @Test
    public void findallUserswhoownatleastoneproject() {
        Assert.assertEquals(2, User.bocal.findBy(new Function<User, Boolean>() {

            @Override
            public Boolean apply(final User user) {
                return Project.bocal.findOneBy(new Function<Project, Boolean>() {

                    @Override
                    public Boolean apply(Project p) {
                        return p.owner.equals(user.email);
                    }
                }).isDefined();
            }
        }).size());
    }
    @Test
    public void findallProjectforJean() {
        final String jean = "jean@gmail.com";
        Assert.assertEquals(Project.bocal.findBy(new Function<Project, Boolean>() {

            @Override
            public Boolean apply(Project project) {
                return project.owner.contains(jean) || project.members.contains(jean);
            }
        }).size(), 2);
    }
    @Test
    public void findallProjectforBob() {
        final String bob = "bob@gmail.com";
        Assert.assertEquals(Project.bocal.findBy(new Function<Project, Boolean>() {

            @Override
            public Boolean apply(Project project) {
                return project.owner.contains(bob) || project.members.contains(bob);
            }
        }).size(), 1);
    }
    @Test
    public void createAUser() {
        Assert.assertEquals(User.bocal.findAll().size(), 3);
        User.bocal.save(new User("toto@gmail.com", "Toto D."));
        Assert.assertEquals(User.bocal.findAll().size(), 4);
    }
    @Test
    public void updateAUser() {
        Assert.assertEquals(User.bocal.findAll().size(), 3);
        User.bocal.save(new User("bob@gmail.com", "Bob M."));
        Assert.assertEquals(User.bocal.findAll().size(), 3);
        Assert.assertTrue(User.bocal.findById("bob@gmail.com").isDefined());
    }
    @Test
    public void deleteSomeProjects() {
        Assert.assertEquals(Project.bocal.findAll().size(), 2);
        Project.bocal.delete(new Function<Project, Boolean>() {

            @Override
            public Boolean apply(Project project) {
                return project.members.isEmpty();
            }
        });
        Assert.assertEquals(Project.bocal.findAll().size(), 1);
    }
}
