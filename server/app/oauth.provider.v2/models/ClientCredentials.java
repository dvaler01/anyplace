package oauth.provider.v2.models;

/**
 * Created by lambros on 2/4/14.
 */
public class ClientCredentials {

    public String client_id;
    public String client_secret;

    public ClientCredentials(String id, String secret){
        this.client_id = id;
        this.client_secret = secret;
    }

}
