package oauth.provider.v2.granttype;

import oauth.provider.v2.models.AccountModel;
import oauth.provider.v2.models.OAuth2Request;
import accounts.IAccountService;
import play.mvc.Result;

public interface IGrantHandler {

    /**
     * Handle a request to issue a token and issue it.
     * This method should be implemented for each grant type of OAuth2
     * specification. For instance, the procedure uses an AccountService
     * instance to access the accounts database. Each grant type has
     * some validation rule and according to the request satisfying those rules
     * either a valid token will be returned or an error.
     *
     * @param request The current request being processed
     * @param accountService The instance of the Accounts Service database
     * @return a Result containing the token or the error
     */
    public Result handleRequest(OAuth2Request request, IAccountService accountService, AccountModel account);

}
