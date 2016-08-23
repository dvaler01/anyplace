package oauth.provider.v2.models;

import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.*;

//import org.codehaus.jackson.JsonNode;
//import org.codehaus.jackson.node.ObjectNode;
import utils.JsonUtils;

public class AccessTokenModel {

    public static String generateTokenId(String access_token){
        return String.format("oauth2_token_v1_%s", access_token);
    }

    // information to be returned from the /token endpoint
    private String access_token;
    private String token_type;
    private long expires_in;
    private String refresh_token;

    private String scope;

    // information to be kept in the database
    private String tuid;
    private String auid;
    private String client_id;
    private long createdAt;


    public AccessTokenModel(String access_token, String token_type,
                            long expires_in, String refresh_token,
                            String scope, String auid, String client_id, long createdAt){
        this.access_token = access_token;
        this.token_type = token_type;
        this.expires_in = expires_in;
        this.refresh_token = refresh_token;
        this.scope = scope;
        this.auid = auid;
        this.client_id = client_id;
        this.createdAt = createdAt;
        this.tuid = generateTokenId(this.access_token);
    }


    public String getAccessToken() {
        return access_token;
    }

    public String getTokenType() {
        return token_type;
    }

    public long getExpiresIn() {
        return expires_in;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public String getScope() {
        return scope;
    }

    public String getTuid() {
        return tuid;
    }

    public String getAuid() { return auid; }

    public String getClientId() {
        return client_id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString(){
        return this.getTuid();
    }

    public JsonNode toJson(){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put("token_type", this.getTokenType());
        json.put("access_token", this.getAccessToken());
        json.put("expires_in", this.getExpiresIn());
        json.put("refresh_token", this.getRefreshToken());

        json.put("tuid", this.getTuid());
        json.put("scope", this.getScope());
        json.put("client_id", this.getClientId());
        json.put("auid", this.getAuid());
        json.put("created_at", this.getCreatedAt());
        return json;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccessTokenModel that = (AccessTokenModel) o;

        if (createdAt != that.createdAt) return false;
        if (expires_in != that.expires_in) return false;
        if (!access_token.equals(that.access_token)) return false;
        if (!client_id.equals(that.client_id)) return false;
        if (!refresh_token.equals(that.refresh_token)) return false;
        if (!scope.equals(that.scope)) return false;
        if (!token_type.equals(that.token_type)) return false;
        if (!tuid.equals(that.tuid)) return false;
        if (!auid.equals(that.auid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = access_token.hashCode();
        result = 31 * result + token_type.hashCode();
        result = 31 * result + (int) (expires_in ^ (expires_in >>> 32));
        result = 31 * result + refresh_token.hashCode();
        result = 31 * result + scope.hashCode();
        result = 31 * result + tuid.hashCode();
        result = 31 * result + auid.hashCode();
        result = 31 * result + client_id.hashCode();
        result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
        return result;
    }
}
