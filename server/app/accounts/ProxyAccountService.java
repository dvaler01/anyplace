package accounts;


import datasources.CouchbaseDatasource;
import oauth.provider.v2.models.AccessTokenModel;
import oauth.provider.v2.models.AccountModel;
import oauth.provider.v2.models.AuthInfo;

/**
 * This class acts as a proxy to the User Service implementation.
 */
public class ProxyAccountService implements IAccountService {

    private static ProxyAccountService sInstance;
    public static ProxyAccountService getInstance(){
        if( sInstance == null ){
            sInstance = new ProxyAccountService();
        }
        return sInstance;
    }

    private ProxyAccountService(){
        mCouchbase = CouchbaseDatasource.getStaticInstance();
    }

    private CouchbaseDatasource mCouchbase;

    /*********************************************************************************
     * GENERAL ACCOUNT MANIPULATION
     */



    /*********************************************************************************
     * OAUTH 2 - FLOWS BELOW
     */

    @Override
    public AccountModel validateClient(String clientId, String clientSecret, String grantType) {
        return mCouchbase.validateClient(clientId, clientSecret, grantType);
    }

    @Override
    public boolean validateAccount(AccountModel account, String username, String password) {
        return mCouchbase.validateAccount(account, username, password);
    }

    @Override
    public AuthInfo createOrUpdateAuthInfo(AccountModel account, String clientId, String scope) {
        return mCouchbase.createOrUpdateAuthInfo(account, clientId, scope);
    }

    @Override
    public AuthInfo getAuthInfoByRefreshToken(String refreshToken) {
        return mCouchbase.getAuthInfoByRefreshToken(refreshToken);
    }

    @Override
    public AccessTokenModel createOrUpdateAccessToken(AuthInfo authInfo) {
        return mCouchbase.createOrUpdateAccessToken(authInfo);
    }
}
