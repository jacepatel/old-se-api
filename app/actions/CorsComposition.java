package actions;

import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.Response;
import play.mvc.SimpleResult;
import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class CorsComposition {

	@With(CorsAction.class)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Cors {
		String value() default "http://localhost:8000";
	}

	public static class CorsAction extends Action<Cors> {

		@Override
		public Promise<SimpleResult> call(Context context) throws Throwable {
			Response response = context.response();
			String domain = context.current().request().getHeader("Origin");
			if (domain != null) {
				response.setHeader("Access-Control-Allow-Origin", domain);
			}
			else {
				response.setHeader("Access-Control-Allow-Origin", "*");
			}
			response.setHeader("Access-Control-Allow-Credentials", "true");
			response.setHeader("Cache-Control", "no-cache");

			if (context.request().method().equals("OPTIONS")) {
				response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");
				response.setHeader("Access-Control-Max-Age", "3600");
				response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, X-Auth-Token, JWT-Token");
                response.setHeader("Access-Control-Expose-Headers", "JWT-Token");
				return delegate.call(context);
			}
            response.setHeader("Access-Control-Expose-Headers", "JWT-Token");
			response.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, X-Auth-Token");
			return delegate.call(context);
		}
	}
}
