package oauth.provider.v2.utils;

import com.fasterxml.jackson.databind.node.*;

import oauth.provider.v2.models.AccessTokenModel;
//import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Result;
import play.mvc.Results;
import utils.JsonUtils;

/**
 * Created by lambros on 2/4/14.
 */
public class OAuth2Responses {

    // TODO - ADD IN EVERY RESPONSE THE error_description and error_uri fields

    public static Result InvalidRequest(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.INVALID_REQUEST);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.badRequest(json);
    }

    public static Result InvalidClient(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.INVALID_CLIENT);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.unauthorized(json);
    }

    public static Result UnauthorizedClient(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.UNAUTHORIZED_CLIENT);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.unauthorized(json);
    }

    public static Result RedirectUriMismatch(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.NOT_MATCH_REDIRECT_URI);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.unauthorized(json);
    }

    public static Result UnsupportedResponseType(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.UNSUPPORTED_RESPONSE_TYPE);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.badRequest(json);
    }

    public static Result InvalidGrant(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.INVALID_GRANT);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.unauthorized(json);
    }

    public static Result UnsupportedGrantType(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.UNSUPPORTED_GRANT_TYPE);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.badRequest(json);
    }

    public static Result InvalidScope(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.INVALID_SCOPE);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.unauthorized(json);
    }

    public static Result InvalidToken(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.INVALID_TOKEN);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.unauthorized(json);
    }

    public static Result ExpiredToken(){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.EXPIRED_TOKEN);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, "The token has expired!");
        return Results.unauthorized(json);
    }

    public static Result InsufficientScope(String msg){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ERROR, OAuth2ErrorConstant.INSUFFICIENT_SCOPE);
        json.put(OAuth2Constant.ERROR_DESCRIPTION, msg);
        return Results.unauthorized(json);
    }

    public static Result ValidToken(AccessTokenModel tokenModel){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put(OAuth2Constant.ACCESS_TOKEN, tokenModel.getAccessToken());
        json.put(OAuth2Constant.TOKEN_TYPE, tokenModel.getTokenType());
        json.put(OAuth2Constant.EXPIRES_IN, tokenModel.getExpiresIn());
        json.put(OAuth2Constant.REFRESH_TOKEN, tokenModel.getRefreshToken());
        return Results.ok(json);
    }

}
