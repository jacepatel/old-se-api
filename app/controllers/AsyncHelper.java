package controllers;

import play.libs.F;
import play.libs.Json;
import play.mvc.Result;

import java.util.HashMap;

import static play.mvc.Results.ok;

/**
 * Created by michaelsive on 4/03/15.
 */
public class AsyncHelper {
    public static F.Promise<Result> getResultPromise(Object response){
        final Object responseResult = response;
        return F.Promise.promise(
                new F.Function0<Result>() {
                    public Result apply() {
                        return ok(Json.toJson(responseResult));
                    }
                }
        );
    }
}
