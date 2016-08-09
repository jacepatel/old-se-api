
import models.User;
import org.junit.*;
import org.mindrot.jbcrypt.BCrypt;
import play.db.jpa.JPA;
import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;
import static org.junit.Assert.*;

/**
 * Created by michaelsive on 12/01/15.
 */
public class UsersTest {
    private User user;
    private Long userId;

    @Before
    public void setupUser(){
        running(fakeApplication(), new Runnable() {
            public void run() {
                JPA.withTransaction(new play.libs.F.Callback0() {
                    public void invoke() {
                        user = User.findByEmail("mikesive@gmail.com");
                        if (user == null){
                            User user = new User();
                            user.firstName = "Mike";
                            user.lastName = "Sive";
                            user.mobNumber = "+61419721862";
                            user.email = "mikesive@gmail.com";
                            user.passwordHash = BCrypt.hashpw("password", BCrypt.gensalt());
                            user.save();
                        }
                        userId = user.userId;
                    }
                });
            }
        });
    }

    @Test
    public void findById() {
        running(fakeApplication(), new Runnable() {
            public void run() {
                JPA.withTransaction(new play.libs.F.Callback0() {
                    public void invoke() {
                        User user = JPA.em().find(User.class, userId);
                        assertThat(user.firstName).isEqualTo("Mike");
                        assertThat(user.lastName).isEqualTo("Sive");
                    }
                });
            }
        });
    }

    @Test
    public void findByEmail(){
        running(fakeApplication(), new Runnable() {
            public void run() {
                JPA.withTransaction(new play.libs.F.Callback0() {
                    public void invoke() {
                        User user = User.findByEmail("mikesive@gmail.com");
                        assertThat(user.firstName).isEqualTo("Mike");
                        assertThat(user.lastName).isEqualTo("Sive");
                    }
                });
            }
        });
    }

    @Test
    public void authorize(){
        running(fakeApplication(), new Runnable() {
            public void run() {
                JPA.withTransaction(new play.libs.F.Callback0() {
                    public void invoke() {
                        User user = User.findByEmail("mikesive@gmail.com");
                        assertTrue(user.authPass("password"));
                    }
                });
            }
        });
    }

    @Test
    public void authorizeFail(){
        running(fakeApplication(), new Runnable() {
            public void run() {
                JPA.withTransaction(new play.libs.F.Callback0() {
                    public void invoke() {
                        User user = User.findByEmail("mikesive@gmail.com");
                        assertFalse(user.authPass("shmassword"));
                    }
                });
            }
        });
    }

    @Test
    public void getFullName(){
        running(fakeApplication(), new Runnable() {
            public void run() {
                JPA.withTransaction(new play.libs.F.Callback0() {
                    public void invoke() {
                        User user = User.findByEmail("mikesive@gmail.com");
                        assertThat(user.fullName()).isEqualTo("Mike Sive");
                    }
                });
            }
        });
    }
}
