package oauth.provider.v2.models;

import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.*;

//import org.codehaus.jackson.JsonNode;
//import org.codehaus.jackson.node.ArrayNode;
//import org.codehaus.jackson.node.ObjectNode;
import utils.JsonUtils;
import utils.LPUtils;
import utils.PasswordService;

import java.util.*;

/**
 * Created by lambros on 2/4/14.
 */
public class AccountModel {

    /**
     * Defines a registered client for this account
     */
    public static class ClientModel{

        public ClientModel(String client_id, String client_secret,
                           String grant_type, String scope, String redirect_uri){
            this.client_id = client_id;
            this.client_secret = client_secret;
            this.grant_type = grant_type;
            this.scope = scope;
            this.redirect_uri = redirect_uri;
        }

        // the client credentials
        String client_id;
        String client_secret;
        // the grant type available for this client
        String grant_type;
        // the redirect uri for this client
        String redirect_uri;
        // the restricted scope of this client
        String scope;

        public String getClientId() {
            return client_id;
        }

        public String getClientSecret() {
            return client_secret;
        }

        public String getGrantType() {
            return grant_type;
        }

        public String getRedirectUri() {
            return redirect_uri;
        }

        public String getScope() {
            return scope;
        }

        public JsonNode toJson(){
            ObjectNode json = JsonUtils.createObjectNode();
            json.put("client_id", client_id);
            json.put("client_secret", client_secret);
            json.put("grant_type", grant_type);
            json.put("scope", scope);
            json.put("redirect_uri", redirect_uri);
            return json;
        }
    }
    ////////////////////////////////////////////////////////

    /**
     * STATIC FACTORIES
     */
    public static AccountModel createEmptyAccount(){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put("clients", JsonUtils.getJsonFromList(Collections.emptyList()));
        return new AccountModel(json);
    }
    public static AccountModel createInitializedAccount(){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put("clients", JsonUtils.getJsonFromList(Collections.emptyList()));
        json.put("auid", generateNewAuid());
        json.put("username", "");
        json.put("password", "");
        json.put("scope", "");
        json.put("nickname", "");
        json.put("email", "");
        json.put("isadmin", false);
        return new AccountModel(json);
    }

    /**
     * Static helpers
     */
    private static String generateNewAuid(){
        return "account_" + LPUtils.hashStringHex(
                LPUtils.generateRandomToken()
                        + System.currentTimeMillis()
                        + LPUtils.getRandomUUID());
    }

    private static String generateNewClientId(String auid){
        return "client_" + LPUtils.hashStringHex(
                LPUtils.getRandomUUID() + auid + System.currentTimeMillis() );
    }

    private static String generateNewClientSecret(String auid, String client_id){
        return "secret_" + LPUtils.hashStringBase64(
                client_id + LPUtils.generateRandomToken() );
    }

    private static String[] CHANGEABLE_PROPERTIES = new String[]{ "nickname", "scope", "email", "isadmin" };
    public static String[] getChangeableProperties(){
        return CHANGEABLE_PROPERTIES;
    }


    /**
     * Account instance
     */

    private ObjectNode mJson;

    private String auid;
    private String username;
    private String password;
    private String scope;

    private String nickname;
    private String email;
    private boolean isadmin;

    private List<ClientModel> clients;

    public AccountModel(JsonNode json){
        this.mJson = (ObjectNode)json;
        this.auid = json.path("auid").textValue();
        this.username = json.path("username").textValue();
        this.password = json.path("password").textValue();
        this.scope = json.path("scope").textValue();
        this.clients = new ArrayList<ClientModel>();
        for( final JsonNode client : json.path("clients") ){
            this.clients.add(new AccountModel.ClientModel(
                    client.path("client_id").textValue(),
                    client.path("client_secret").textValue(),
                    client.path("grant_type").textValue(),
                    client.path("scope").textValue(),
                    client.path("redirect_uri").textValue())
            );
        }
        this.nickname = json.path("nickname").textValue();
        this.email = json.path("email").textValue();
        this.isadmin = json.path("isadmin").asBoolean();
        if( this.auid == null || this.auid.trim().isEmpty() ){
            this.auid = generateNewAuid();
            this.mJson.put("auid", this.auid);
        }
    }

    public String getNickname(){ return nickname; }
    public void setNickname(String nickname){ this.nickname = nickname; }

    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }

    public boolean isAdmin(){ return isadmin; }
    public void setAdmin(boolean isAdmin){ this.isadmin = isAdmin; }

    public String getAuid() { return auid; }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getScope() {
        return scope;
    }
    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean validateScope(String scope, String client_id){
        // TODO - check against the global user scope
        if( scope == null ){
            return false;
        }
        String scopes[] = scope.split(" ");
        String accountScopes[] = this.getScope().split(" ");

        // if more scopes requested reject
        if( scopes.length > accountScopes.length ){
            return false;
        }

        // check the scopes
        for( String s : scopes ){
            boolean matched = false;
            for( String sa : accountScopes ){
                if( s.equalsIgnoreCase(sa) ){
                    matched = true;
                    break;
                }
            }
            if( !matched )
                return false;
        }

        // TODO - check against the client specific scopes
        return true;
    }

    public boolean deleteClient(String client_id){
        for( int i=0,sz=this.clients.size(); i<sz; i++ ){
            if( this.clients.get(i).client_id.equals(client_id) ){
                this.clients.remove(i);
                return true;
            }
        }
        return false;
    }

    public ClientModel getClient(String client_id){
        for( ClientModel cm : this.clients ){
            if( cm.client_id.equals(client_id) ){
                return cm;
            }
        }
        return null;
    }

    // DOES NOT GUARANTEE UNIQUENESS FOR NOW
    public void addNewClient(String grant_type, String scope, String redirect_uri){
        String client_id = generateNewClientId(this.getAuid());
        String client_secret = generateNewClientSecret(auid, client_id);
        this.clients.add( new ClientModel(client_id, client_secret, grant_type, scope, redirect_uri) );
    }

    @Override
    public String toString() {
        return String.format("AccountModel: uaid[%s]", this.getAuid());
    }

    public JsonNode toJson(){
        ObjectNode json = JsonUtils.createObjectNode();
        json.put("doctype","account");
        json.put("auid", this.auid);
        json.put("username", this.username);
        json.put("password", PasswordService.createHash(this.password));
        json.put("scope", this.scope);
        json.put("nickname", this.nickname);
        json.put("email", this.email);
        json.put("isadmin", this.isadmin);
        ArrayNode clientsNode = json.putArray("clients");
        for( ClientModel cm : this.clients ){
            clientsNode.add(cm.toJson());
        }
        this.mJson = json;
        return this.mJson;
    }

}
