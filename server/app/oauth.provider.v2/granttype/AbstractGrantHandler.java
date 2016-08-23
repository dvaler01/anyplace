package oauth.provider.v2.granttype;

import oauth.provider.v2.models.AccessTokenModel;
import oauth.provider.v2.models.AuthInfo;
import accounts.IAccountService;

public abstract class AbstractGrantHandler implements IGrantHandler {

    /**
     * Issue an Access Token and relating information and return it.
     * Actually, the creation of the access token is handled by the
     * account service and a valid created AccessTokenModel is returned.

     * @param accountService The Account Service that will handle the account update and token creation
     * @param authInfo The information regarding the new token to be issued
     * @return The created access token model or null if failed
     */
    protected AccessTokenModel issueAccessToken(IAccountService accountService, AuthInfo authInfo){
        return accountService.createOrUpdateAccessToken(authInfo);
    }



}
