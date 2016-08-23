package oauth.provider.v2.token;

import oauth.provider.v2.utils.OAuth2Constant;
import oauth.provider.v2.models.AccessTokenModel;
import oauth.provider.v2.models.AuthInfo;
import utils.LPLogger;
import utils.LPUtils;

import java.util.Date;

/**
 * The service responsible to create unique tokens and client ids.
 */
public class TokenService {

    // Used by the secure encryption/decryption algorithms
    private final static String SECURE_PASSWORD_PHASE1 = "anyplaceT_O_K_3_n-P@@"; // 21 length
    private final static String SECURE_PASSWORD_PHASE2 = "anyCALI.*_O_K_3_n-P$@";


    /**
     * Creates a new access token according to the specified AuthInfo.
     * The token structure is as follows:
     *
     * PHASE 1 ::
     * signature = <auid>]<scope>]<client_id>]<timestamp>
     * random = random string generated
     *
     * PHASE 2 ::
     * encrypted_token = secureEncrypt( random + "." + signature )
     *
     * @param authInfo
     * @return
     */
    public static AccessTokenModel createNewAccessToken(AuthInfo authInfo){
        long timeNow = new Date().getTime();
        String plainText = authInfo.getAuid() + "]"
                + authInfo.getScope() + "]"
                + authInfo.getClientId() + "]"
                + String.valueOf(timeNow);
        LPLogger.info("plaintext    : >" + plainText + "<");
        String randomString = LPUtils.generateRandomToken();
        LPLogger.info("randomstr PH1: >" + randomString + "<");
        String encrypted_token = LPUtils.secureEncrypt(SECURE_PASSWORD_PHASE1, randomString + "." + plainText);
        LPLogger.info("encrypted PH2: >" + encrypted_token + "<");
        return new AccessTokenModel(encrypted_token, OAuth2Constant.TOKEN_TYPE_BEARER,
                OAuth2Constant.EXPIRES_IN_DEFAULT_VALUE,
                authInfo.getRefreshToken(), authInfo.getScope(),
                authInfo.getAuid(), authInfo.getClientId(), timeNow);
    }

    /**
     * Creates a new access token according to the specified AuthInfo.
     * The token structure is as follows:
     *
     * PHASE 1 ::
     * signature = <auid>]<scope>]<client_id>]<timestamp>
     * encryptedSignature = secureEncrypt(signature)
     *
     * random = random string generated
     *
     * PHASE 2 ::
     * encrypted_token = secureEncrypt( random + "." + encryptedSignature )
     *
     * @param authInfo
     * @return
     */
    public static AccessTokenModel createNewAccessToken2(AuthInfo authInfo){
        long timeNow = new Date().getTime();
        String plainText = authInfo.getAuid() + "]"
                + authInfo.getScope() + "]"
                + authInfo.getClientId() + "]"
                + String.valueOf(timeNow);
        LPLogger.info("plaintext    : >" + plainText + "<");
        String signature = LPUtils.secureEncrypt(SECURE_PASSWORD_PHASE1, plainText);
        LPLogger.info("signature PH1: >" + signature + "<");
        String randomString = LPUtils.generateRandomToken();
        LPLogger.info("randomstr PH1: >" + randomString + "<");
        String encrypted_token = LPUtils.secureEncrypt(SECURE_PASSWORD_PHASE2, randomString + "." +signature);
        LPLogger.info("encrypted PH2: >" + encrypted_token + "<");
        return new AccessTokenModel(encrypted_token, OAuth2Constant.TOKEN_TYPE_BEARER,
                OAuth2Constant.EXPIRES_IN_DEFAULT_VALUE,
                authInfo.getRefreshToken(), authInfo.getScope(),
                authInfo.getAuid(), authInfo.getClientId(), timeNow);
    }


    /**
     * Return a new Client Id
     */
    public static String createNewClientId(){
        return LPUtils.getRandomUUID();
    }

}
