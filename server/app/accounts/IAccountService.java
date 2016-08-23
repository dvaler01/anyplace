package accounts;


import oauth.provider.v2.models.AccessTokenModel;
import oauth.provider.v2.models.AccountModel;
import oauth.provider.v2.models.AuthInfo;

/**
 *
 * <p>This interface provides the necessary functions to support
 * the different flows for the OAuth2 server flows.</p>
 *
 * <p>Methods are provided in order to store and create each information
 * regarding the OAuth2 specification (RFC6479).</p>
 *
 * <p>Each Grant Type requires different method invocations from here.</p>
 *
 * <p>
 * Refresh Token Grant:<br />
 *   <ul>
 *   <li>validateClient(clientId, clientSecret, grantType)</li>
 *   <li>getAuthInfoByRefreshToken(refreshToken)</li>
 *   <li>createOrUpdateAccessToken(authInfo)</li>
 *   </ul>
 * </p>
 *
 * <p>
 * Resource Owner Password Credentials Grant:<br />
 *   <ul>
 *   <li>validateClient(clientId, clientSecret, grantType)</li>
 *   <li>validateAccount(accountModel, username, password)</li>
 *   <li>createOrUpdateAuthInfo(auid, clientId, scope)</li>
 *   <li>createOrUpdateAccessToken(authInfo)</li>
 *   </ul>
 * </p>
 *
 */
public interface IAccountService {


    /*********************************************************************************
     * OAUTH 2 - FLOWS BELOW
     */

    /**
     * Validate the client and return the result.
     * This method is called at first for all grant types.
     * You should check whether the client specified by clientId value exists
     * or not, whether the client secret is valid or not, and whether
     * the client supports the grant type or not. If there are other things
     * to have to check, they must be implemented in this method.
     *
     * @param clientId The client ID.
     * @param clientSecret The client secret string.
     * @param grantType The grant type string which the client required.
     * @return True if the client is valid.
     */
    AccountModel validateClient(String clientId, String clientSecret, String grantType);

    /**
     * Validates the Account with the provided username/password.
     * The AccountModel passed in should be the returned result of
     * validateClient() above.
     *
     * @param account The returned account from validateClient()
     * @param username The username provided by the user
     * @param password The password provided by the user
     * @return True if the credentials provided are valid against the account
     */
    boolean validateAccount(AccountModel account, String username, String password);

    /**
     * Create or update an Authorization information.
     * This method is used when the authorization information should be created
     * or updated directly against receiving of the request in case of Client
     * Credential grant or Resource Owner Password Credential grant.
     * If the null value is returned from this method as the result, the error
     * type "invalid_grant" will be sent to the client.
     *
     * @param account The account that needs to be updated
     * @param clientId The client ID.
     * @param scope The scope string.
     * @return The created or updated the information about authorization.
     */
    AuthInfo createOrUpdateAuthInfo( AccountModel account, String clientId, String scope);

    /**
     * Create or update an Access token.
     * This method is used for all grant types. The access token is created or
     * updated based on the authInfo's property values. In generally, this
     * method never failed, because all validations should be passed before
     * this method is called.
     * @param authInfo The instance which has the information about authorization.
     * @return The created or updated access token instance or null if failed.
     */
    AccessTokenModel createOrUpdateAccessToken(AuthInfo authInfo);

    /**
     * Retrieve the authorization information by the refresh token string.
     * This method is used to re-issue an access token with the refresh token.
     * The authorization information which has already been stored into your
     * database should be specified by the refresh token. If you want to define
     * the expiration of the refresh token, you must check it in this
     * implementation. If the refresh token is not found, the refresh token is
     * invalid or there is other reason which the authorization information
     * should not be returned, this method must return the null value as the
     * result.
     * @param refreshToken The refresh token string.
     * @return The authorization information instance.
     */
    AuthInfo getAuthInfoByRefreshToken(String refreshToken);




}
