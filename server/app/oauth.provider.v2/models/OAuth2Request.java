package oauth.provider.v2.models;

import com.fasterxml.jackson.databind.JsonNode;
import oauth.provider.v2.utils.OAuth2Constant;
import play.mvc.Http;
import utils.CORSHelper;
import utils.LPUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class will provide easy tools to handle HTTP Requests
 * in order to easily integrate OAuth2 implementation.
 *
 */
public class OAuth2Request {

    Http.Request mRequest;
    Http.Response mResponse;
    Http.RequestBody mBody;

    JsonNode mJsonBody;
    Map<String, String[]> mFormBody;

    /**
     * Creates an OAuthRequest from the request and the response objects.
     * It tries to convert the body into a JsonNode.
     * If not a Json conversion is possible then a FormUrlEncoded transformation.
     * If not that either the body remains as is.
     * CORS is enabled by default with this constructor.
     *
     * @param request The request object that invoked this call
     * @param response The response object that will be returned
     */
    public OAuth2Request(Http.Request request, Http.Response response){
        this( request, response, true );
    }

    /**
     * Creates an OAuthRequest from the request and the response objects.
     * It tries to convert the body into a JsonNode.
     * If not a Json conversion is possible then a FormUrlEncoded transformation.
     * If not that either the body remains as is.
     *
     * @param request The request object that invoked this call
     * @param response The response object that will be returned
     * @param enableCORS If True enables CORS, sets the Access-Control-Allow-Origin=*
     */
    public OAuth2Request(Http.Request request, Http.Response response, boolean enableCORS){
        this.mRequest = request;
        this.mResponse = response;
        //this.mResponse.setContentType("application/json; charset=utf-8");
        if( enableCORS ){
            // IMPORTANT - to allow cross domain requests
            this.mResponse.setHeader("Access-Control-Allow-Origin", CORSHelper.checkOriginFromRequest(this.mRequest));
            // allows session cookies to be transferred
            this.mResponse.setHeader("Access-Control-Allow-Credentials", "true");
        }
        this.mBody = this.mRequest.body();
        if( !assertJsonBody() ){
            assertFormUrlEncodedBody();
        }
    }

    /**
     * Make sure that the body can be parsed as a valid Json object.
     * If not already converted it tries to convert the body into Json first.
     * The transformed Json body can be later retrieved with @getJsonBody()
     *
     * @return True if the body parsed as Json, False otherwise
     */
    public boolean assertJsonBody(){
        if( this.mJsonBody == null )
            this.mJsonBody = this.mBody.asJson();
        return this.mJsonBody != null;
    }

    /**
     * Checks if the body has been parsed as Json.
     * @return True if there is a parsed Json body, False otherwise
     */
    public boolean hasJsonBody(){
        return this.mJsonBody != null;
    }

    /**
     * If the body has been parsed as Json using @assertJsonBody() that Json object
     * is returned, otherwise a call to that method is invoked and then the object
     * is returned.
     * @return The JsonNode of the body parsed as Json, or null if not possible.
     */
    public JsonNode getJsonBody(){
        if( this.mJsonBody == null )
            assertJsonBody();
        return this.mJsonBody;
    }


    /**
     * Make sure that the body can be parsed as a valid Form Url encoded map.
     * The transformed Json body can be later retrieved with @getFormEncodedBody()
     *
     * @return True if the body parsed as FormUrlEncoded, False otherwise
     */
    public boolean assertFormUrlEncodedBody(){
        this.mFormBody = this.mBody.asFormUrlEncoded();
        if( this.mFormBody == null ){
            return false;
        }
        return true;
    }

    /**
     * Checks if the body has been parsed as Json.
     * @return True if there is a parsed Json body, False otherwise
     */
    public boolean hasFormEncodedBody(){
        return this.mJsonBody != null;
    }

    /**
     * If the body has been parsed as Form using @assertFormUrlEncodedBody() that Map object
     * is returned, otherwise a call to that method is invoked and then the object
     * is returned.
     * @return The body parsed as Map<String, String[]>, or null if not possible.
     */
    public Map<String, String[]> getFormEncodedBody(){
        return this.mFormBody;
    }

    /**
     * If the body can be parsed as MultipartFormData then we return
     * the multipart data representation of the body.
     *
     * @return MultipartFormData object or null
     */
    public Http.MultipartFormData getMultipartFormData(){
        return this.mRequest.body().asMultipartFormData();
    }


    public String getHeader(String header){
        if( header == null || header.trim().isEmpty() ){
            throw new IllegalArgumentException("No null/empty headers allowed!");
        }
        return this.mRequest.getHeader(header);
    }

    public String setHeader(String header, String value){
        if( header == null || value == null
                || header.trim().isEmpty() || value.trim().isEmpty()){
            throw new IllegalArgumentException("No null/empty headers allowed!");
        }
        String old = this.mResponse.getHeaders().get(header);
        this.mResponse.setHeader(header, value);
        return old;
    }

    public String getParameter(String property){
        if( hasJsonBody() ){
            return this.mJsonBody.path(property).textValue();
        }else if( hasFormEncodedBody() ){
            return this.mFormBody.get(property)[0];
        }
        return null;
    }


    ///////////////////////////////////////////////////////////////////////////////////
    // OAUTH RELATED METHODS
    ///////////////////////////////////////////////////////////////////////////////////

    private final static Pattern REGEXP_BASIC = Pattern.compile("^\\s*(Basic)\\s+(.*)$");

    public ClientCredentials getCredentials(){
        String header = this.getHeader(OAuth2Constant.HEADER_AUTHORIZATION);
        // we found the credentials in the authorization header
        if( header != null ){
            Matcher matcher = REGEXP_BASIC.matcher(header);
            if( matcher.find() ){
                String decoded = LPUtils.decodeBase64String(matcher.group(2));
                if( decoded.indexOf(':') > 0 ){
                    // we have a client_id:client_secret combo
                    String credential[] = decoded.split(":", 2);
                    return new ClientCredentials(credential[0], credential[1]);
                }
            }
        }
        // we will try in the parameters list since the header does not contain the info
        String client_id = this.getParameter(OAuth2Constant.CLIENT_ID);
        String client_secret = this.getParameter(OAuth2Constant.CLIENT_SECRET);
        return new ClientCredentials(client_id, client_secret);
    }




}
