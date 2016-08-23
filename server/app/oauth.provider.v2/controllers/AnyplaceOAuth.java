package oauth.provider.v2.controllers;

import oauth.provider.v2.utils.OAuth2Constant;
import oauth.provider.v2.utils.OAuth2Responses;
import oauth.provider.v2.granttype.GrantHandlerFactory;
import oauth.provider.v2.granttype.IGrantHandler;
import oauth.provider.v2.models.AccountModel;
import oauth.provider.v2.models.ClientCredentials;
import oauth.provider.v2.models.OAuth2Request;
import accounts.IAccountService;
import accounts.ProxyAccountService;
import org.apache.commons.lang3.StringUtils;
//import org.codehaus.jackson.JsonNode;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AnyResponseHelper;
import utils.LPLogger;

import com.fasterxml.jackson.databind.*;

/**
 * This Controller provides all the endpoints for the OAuth supported
 * by Anyplace.
 *
 * At the moment only OAuth2 is implemented as defined by RFC:
 *  http://tools.ieft.org.html/rfc6749
 *  http://tools.ieft.org.html/rfc6750
 *
 */
public class AnyplaceOAuth extends Controller {

    /**
     * This endpoint interacts with the resource owner in order to obtain
     * authorization grant. The authorization server first authenticates
     * the resource owner, using credentials (HTML Form) or session cookies.
     *
     * TLS/SSL connection is REQUIRED.
     *
     * REQUIRED: GET, [POST]
     *
     * This endpoint is used by grant types:
     *  a) Authorization Code
     *  b) Implicit grant
     * and the field 'response_type' is required with values 'code' or 'token'
     * respectively.
     *
     * @return
     */
    public static Result authorize(){
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceOAuth::authorize():: " + json.toString());

        return AnyResponseHelper.not_found("OAuth grant types using authenticate are not supported!");
    }


    /**
     * This endpoint is used by all the grant types flows except 'implicit grant'
     * since the token is issued directly.
     *
     * TLS/SSL connection is REQUIRED.
     * REQUIRED: POST
     * Required parameters with no value are treated as omitted and unknowns are ignored.
     *
     * ////// REQUEST A TOKEN //////////////
     * Access Token Request (application/x-www-form-urlencoded): RFC 4.3.2
     *  grant_type: 'password'
     *  username: The resource owner username
     *  password: The resource owner password
     *  scope: The scope of the access request (optional)
     *  client_id: The client id issued by the registration service (optional)
     *  client_secret: The client secret issued by the registration service (optional)
     *
     * After successful request the response is like below:
     *
     * HTTP/1.1 200 OK
     * Content-Type: application/json;charset=utf-8
     * Cache-Control: no-store
     * Pragma: no-cache
     *
     * {
     *     "access_token": "access token here",
     *     "token_type": "Bearer",
     *     "expires_in": lifetime in seconds,
     *     "refresh_token": "refresh token here"
     * }
     *
     * ////// REFRESH A TOKEN //////////////
     * Access Token Refresh (application/x-www-form-urlencoded): RFC 4.3.2
     *  grant_type: 'refresh_token'
     *  refresh_token: 'the refresh token issued in the token request'
     *  scope: The scope of the access request (optional)
     *  client_id: The client id issued by the registration service (optional)
     *  client_secret: The client secret issued by the registration service (optional)
     *
     * After successful request the response is like below:
     *
     * HTTP/1.1 200 OK
     * Content-Type: application/json;charset=utf-8
     * Cache-Control: no-store
     * Pragma: no-cache
     *
     * {
     *     "access_token": "access token here",
     *     "token_type": "Bearer",
     *     "expires_in": lifetime in seconds,
     *     "refresh_token": "refresh token here"
     * }
     *
     * @return
     */
    public static Result token(){
        LPLogger.info("AnyplaceOAuth::token():: " + request().body().toString());

        // wrap the Request into our own OAuth2Request
        OAuth2Request anyReq = new OAuth2Request(request(), response());

        // ensure that a grant_type exists
        String grantType = anyReq.getParameter(OAuth2Constant.GRANT_TYPE);
        if(StringUtils.isBlank(grantType)){
            return OAuth2Responses.InvalidRequest("'grant_type' not found");
        }

        // make sure that we can handle the specified grant_type
        IGrantHandler grantHandler = GrantHandlerFactory.fromGrantType(grantType);
        if( grantHandler == null ){
            return OAuth2Responses.UnsupportedGrantType("'grant_type' requested is not supported");
        }

        // ensure that client credentials have been submitted and are valid
        ClientCredentials clientCredentials = anyReq.getCredentials();
        if( StringUtils.isBlank(clientCredentials.client_id) ){
            return OAuth2Responses.InvalidRequest("'client_id' not found");
        }
        if( StringUtils.isBlank(clientCredentials.client_secret) ){
            return OAuth2Responses.InvalidRequest("'client_secret' not found");
        }

        IAccountService accountService = ProxyAccountService.getInstance();
        AccountModel account = accountService.validateClient(clientCredentials.client_id, clientCredentials.client_secret, grantType);
        if( account == null ){
            return OAuth2Responses.InvalidClient("Specified client credentials are not valid");
        }

        // depending on the grant_type the handler will take over the procedure now
        // and return the appropriate response either the token or the error json
        return grantHandler.handleRequest(anyReq, accountService, account);
    }

}
