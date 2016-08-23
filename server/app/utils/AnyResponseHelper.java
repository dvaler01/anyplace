package utils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.mvc.Result;
import play.mvc.Results;

import java.util.List;

/**
 * This Helper class helps me construct responses for the different
 * controllers in a unified way across the API and easy modification
 * for massive updates.
 *
 */
public class AnyResponseHelper {

    public static final String CANNOT_PARSE_BODY_AS_JSON = "Cannot parse request body as Json object!";

    /**
     * Anyplace supported HTTP Responses
     */
    public enum Response{
        BAD_REQUEST, OK, FORBIDDEN, UNAUTHORIZED_ACCESS, INTERNAL_SERVER_ERROR, NOT_FOUND
    }

    public static Result ok(ObjectNode json, String msg){
        return createResultResponse(Response.OK, json, msg);
    }

    public static Result bad_request(ObjectNode json, String msg){
        return createResultResponse(Response.BAD_REQUEST, json, msg);
    }

    public static Result forbidden(ObjectNode json, String msg){
        return createResultResponse(Response.FORBIDDEN, json, msg);
    }

    public static Result unauthorized(ObjectNode json, String msg){
        return createResultResponse(Response.UNAUTHORIZED_ACCESS, json, msg);
    }

    public static Result internal_server_error(ObjectNode json, String msg){
        return createResultResponse(Response.INTERNAL_SERVER_ERROR, json, msg);
    }

    public static Result not_found(ObjectNode json, String msg){
        return createResultResponse(Response.NOT_FOUND, json, msg);
    }

    public static Result ok(String msg){
        return createResultResponse(Response.OK, null, msg);
    }

    public static Result bad_request(String msg){
        return createResultResponse(Response.BAD_REQUEST, null, msg);
    }

    public static Result forbidden(String msg){
        return createResultResponse(Response.FORBIDDEN, null, msg);
    }

    public static Result unauthorized(String msg){
        return createResultResponse(Response.UNAUTHORIZED_ACCESS, null, msg);
    }

    public static Result internal_server_error(String msg){
        return createResultResponse(Response.INTERNAL_SERVER_ERROR, null, msg);
    }

    public static Result not_found(String msg){
        return createResultResponse(Response.NOT_FOUND, null, msg);
    }


    /**
     * Returns a Result suited for requests with missing required properties or fields.
     * @param missing The List with the missing names of the properties missing
     * @return The result that should be returned by a controller action
     */
    public static Result requiredFieldsMissing(List<String> missing){
        if( missing == null ){
            throw new IllegalArgumentException("No null List of missing keys allowed!");
        }
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        ArrayNode error_messages = json.putArray("error_messages");
        for( String s : missing ){
            error_messages.add(String.format("Missing or Invalid parameter:: [%s]", s));
        }
        // TODO - should change the response to contain all the fields missing
        return createResultResponse(Response.BAD_REQUEST, json,
                String.format("Missing or Invalid parameter:: [%s]", missing.get(0)));
    }


    /**
     * Creates the play.mvc.Result object that a controller action should return.
     *
     * @param r The HTTP response enum to return
     * @param json The response's JSON object that is being returned
     * @param message The message to put into the returned JSON object
     * @return The play.mvc.Result with the 'json' object extended with 'status', 'status_code' and 'message'
     */
    private static Result createResultResponse(Response r, ObjectNode json, String message){
        if( json == null ){
            json = JsonNodeFactory.instance.objectNode();
        }
        switch(r){
            case BAD_REQUEST:
                json.put("status", "error");
                json.put("message", message);
                json.put("status_code", 400);
                return Results.badRequest(json);
            case OK:
                json.put("status", "success");
                json.put("message", message);
                json.put("status_code", 200);
                return Results.ok(json);
            case FORBIDDEN:
                json.put("status", "error");
                json.put("message", message);
                json.put("status_code", 401);
                return Results.forbidden(json);
            case UNAUTHORIZED_ACCESS:
                json.put("status", "error");
                json.put("message", message);
                json.put("status_code", 403);
                return Results.unauthorized(json);
            case INTERNAL_SERVER_ERROR:
                json.put("status", "error");
                json.put("message", message);
                json.put("status_code", 500);
                return Results.internalServerError(json);
            case NOT_FOUND:
                json.put("status", "error");
                json.put("message", message);
                json.put("status_code", 404);
                return Results.notFound(json);
            default:
                json.put("status", "error");
                json.put("message", "Unknown Action");
                json.put("status_code", 403);
                return Results.badRequest(json);
        }
    }






}
