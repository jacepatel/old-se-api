import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import controllers.TrucksController;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Result;

/**
 * Created by michaelsive on 29/01/15.
 */
public class UsersControllerTest {

    @Test
    public void getActiveTrucks() {
        running(fakeApplication(), new Runnable() {
            public void run() {
                JPA.withTransaction(new play.libs.F.Callback0() {
                    public void invoke() {
                        Result result = TrucksController.userActiveTrucks();
                        assertThat(status(result)).isEqualTo(OK);
                        assertThat(contentAsString(result)).contains("trucks");
                    }
                });
            }
        });
    }
}