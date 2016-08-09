package controllers;

/**
 * Created by michaelsive on 30/12/14.
 */
public class StatusCodes {
    public static String ERROR = "error";
    public static String SUCCESS = "success";

    protected static String ERROR_BAD_JSON = "The JSON was not formatted correctly. Please read API documentation.";
    protected static String ERROR_UNAUTH = "You are not logged in, or authorised to make this call";
    protected static String ERROR_UNKNOWN = "Something went wrong. Unknown error.";
    protected static String ERROR_LOGIN = "Your email and/or password was entered incorrectly. Please enter the correct details, or select 'Forgot your password'.";
    protected static String ERROR_INVALID_MOBILE = "There are no users registered with the provided phone number.";
    protected static String ERROR_INVALID_TOKEN = "Verification token is invalid or expired";
}
