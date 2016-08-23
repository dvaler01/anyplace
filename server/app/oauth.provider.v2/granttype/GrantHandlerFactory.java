package oauth.provider.v2.granttype;

import oauth.provider.v2.utils.OAuth2Constant;

import java.util.HashMap;
import java.util.Map;

/**
 * This factory is responsible to return the appropriate handler
 * to process the token request specified by the grant_type.
 */
public class GrantHandlerFactory {

    private static Map<String, IGrantHandler> sGrantHandlers;

    static{
        sGrantHandlers = new HashMap<String, IGrantHandler>();
        sGrantHandlers.put(OAuth2Constant.GRANT_TYPE_PASSWORD, new PasswordHandler());
    }

    /**
     * Returns the GrantHandler responsible for the grant type specified.
     * Only 'password' is currently implemented.
     *
     * @param type Grant type needed
     * @return Either the GrantHandler for the specified type or null
     */
    public static IGrantHandler fromGrantType(String type){
        return sGrantHandlers.get(type);
    }

    /**
     * Returns whether the specified grant type is supported by the OAuth2 provider
     * @param type The grant_type in question
     * @return True if the grant_type is supported otherwise False
     */
    public static boolean isGrantTypeSupported(String type){
        return fromGrantType(type) != null;
    }

}
