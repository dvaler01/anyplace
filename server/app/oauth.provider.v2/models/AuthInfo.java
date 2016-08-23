package oauth.provider.v2.models;

/**
 * Created by lambros on 2/4/14.
 */
public class AuthInfo {

    private String auid;
    private String client_id;
    private String scope;
    private String refresh_token;

    // NOT USED AT THE MOMENT
    private String code;
    private String redirect_uri;

    public AuthInfo(String auid, String client_id, String scope) {
        this.auid = auid;
        this.client_id = client_id;
        this.scope = scope;
    }

    public String getAuid() {
        return auid;
    }

    public void setAuid(String auid) {
        this.auid = auid;
    }

    public String getClientId() {
        return client_id;
    }

    public void setClientId(String client_id) {
        this.client_id = client_id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void setRefreshToken(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRedirectUri() {
        return redirect_uri;
    }

    public void setRedirectUri(String redirect_uri) {
        this.redirect_uri = redirect_uri;
    }

    @Override
    public String toString() {
        return String.format("AuthInfo: auid[%s] client_id[%s]", this.getAuid(), this.getClientId());
    }
}
