package oauth.provider.v2.granttype;

import oauth.provider.v2.utils.OAuth2Constant;
import oauth.provider.v2.utils.OAuth2Responses;
import oauth.provider.v2.models.*;
import accounts.IAccountService;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Result;
import utils.AnyResponseHelper;

/**
 * This class is the handler that is responsible to handle the requests
 * with grant_type specified as 'password'.
 */
public class PasswordHandler extends AbstractGrantHandler {

    @Override
    public Result handleRequest(OAuth2Request request, IAccountService accountService, AccountModel account) {
        ClientCredentials clientCredentials = request.getCredentials();

        // ensure that username and password exists
        String username = request.getParameter(OAuth2Constant.USERNAME);
        if(StringUtils.isBlank(username)){
            return OAuth2Responses.InvalidRequest("'username' not provided");
        }
        String password = request.getParameter(OAuth2Constant.PASSWORD);
        if(StringUtils.isBlank(password)){
            return OAuth2Responses.InvalidRequest("'password' not provided");
        }

        // ensure that the provided username/password relates to an account
        if(!accountService.validateAccount(account, username, password) ){
            return OAuth2Responses.InvalidGrant("username:password credentials are not valid");
        }

        // now we have to make sure that the scope requested is valid
        // and that is included in the scope granted for the specific account
        // and the specific client currently being used
        String scope = request.getParameter(OAuth2Constant.SCOPE);
        if(StringUtils.isBlank(scope)){
            return OAuth2Responses.InvalidRequest("'scope' not provided");
        }

        // try to issue the new access token
        AuthInfo authInfo = accountService.createOrUpdateAuthInfo(account, clientCredentials.client_id, scope);
        if( authInfo == null ){
            return OAuth2Responses.InvalidGrant("Could not authorize you for this scope");
        }
        //LPLogger.info("before issuing token");
        AccessTokenModel accessTokenModel = issueAccessToken(accountService, authInfo);
        if( accessTokenModel == null ){
            return AnyResponseHelper.internal_server_error("Could not create access token");
        }
        //LPLogger.info("new token: [" + accessTokenModel.getAccessToken() + "]");
        return OAuth2Responses.ValidToken(accessTokenModel);
    }
}
