package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import datasources.DatasourceException;
import datasources.ProxyDataSource;
import oauth.provider.v2.granttype.GrantHandlerFactory;
import oauth.provider.v2.models.AccountModel;
import oauth.provider.v2.models.OAuth2Request;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.AnyResponseHelper;
import utils.JsonUtils;
import utils.LPLogger;

import java.util.List;

/**
 * This controller is responsible for the Accounts Admin website
 * and all of the API endpoints the website uses.
 *
 * The whole controller is secured since we want to be accessible
 * only by the website.
 */
@Security.Authenticated(Secured.class)
public class AnyplaceAccounts extends Controller {

    /**
     * Retrieve all the accounts.
     * @return
     */
    public static Result fetchAllAccounts(){
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceAccounts::fetchAllAccounts(): " + json.toString());

        try {
            List<JsonNode> accounts = ProxyDataSource.getIDatasource().getAllAccounts();
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("accounts", JsonUtils.getJsonFromList(accounts));
            return AnyResponseHelper.ok(res, "Successfully retrieved all accounts!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }


    /**
     * Fetches the account with the AUID passed in.
     * The account document is returned in the Json response.
     * @return
     */
    public static Result fetchAccount(String auid){
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceAccounts::fetchAccount():: " + json.toString());

        // check if there is any required parameter missing
        List<String> notFound = JsonUtils.requirePropertiesInJson(json,
                "auid"
        );
        if( !notFound.isEmpty() && ( auid == null || auid.trim().isEmpty() ) ){
            return AnyResponseHelper.requiredFieldsMissing(notFound);
        }

        // if the auid in the route is empty then try to get the one from the POST json body
        if( auid == null || auid.trim().isEmpty() )
            auid = json.path("auid").textValue();
        try {
            JsonNode storedAccount;
            if( (storedAccount = ProxyDataSource.getIDatasource().getFromKeyAsJson(auid)) == null ){
                return AnyResponseHelper.bad_request("Account could not be found!");
            }
            return AnyResponseHelper.ok((ObjectNode)storedAccount, "Successfully created account!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }


    /**
     * Creates a new account with the credentials and nickname passed in.
     * The AUID of the new account is returned in the Json response.
     * @return
     */
//    public static Result addAccount(){
//        // create the Request and check it
//        OAuth2Request anyReq = new OAuth2Request(request(), response());
//        if( !anyReq.assertJsonBody() ){
//            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
//        }
//        JsonNode json = anyReq.getJsonBody();
//        LPLogger.info("AnyplaceAccounts::addAccount():: " + json.toString());
//
//        // check if there is any required parameter missing
//        List<String> notFound = JsonUtils.requirePropertiesInJson(json,
//                "acc_username", "acc_password", "acc_nickname", "acc_email"
//                );
//        if( !notFound.isEmpty() ){
//            return AnyResponseHelper.requiredFieldsMissing(notFound);
//        }
//
//        // TODO - WE SHOULD ENSURE THAT NOT ONLY THE AUID IS UNIQUE BUT ALSO THE USERNAME
//        String acc_nickname = json.path("acc_nickname").textValue();
//        String acc_password = json.path("acc_password").textValue();
//        String acc_username = json.path("acc_username").textValue();
//        String acc_email = json.path("acc_email").textValue();
//        AccountModel newAccount = AccountModel.createInitializedAccount();
//        newAccount.setUsername(acc_username);
//        newAccount.setPassword(acc_password);
//        newAccount.setEmail(acc_email);
//        newAccount.setNickname(acc_nickname);
//        try {
//            if( !ProxyDataSource.getIDatasource().addJsonDocument(newAccount.getAuid(), 0, newAccount.toJson().toString())){
//                return AnyResponseHelper.bad_request("Account could not be added! Try again...");
//            }
//            ObjectNode res = JsonUtils.createObjectNode();
//            res.put("auid", newAccount.getAuid());
//            return AnyResponseHelper.ok(res, "Successfully created account!");
//        } catch (DatasourceException e) {
//            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
//        }
//    }

    /**
     * Deletes the account with the AUID passed in.
     * The result of the action is returned in the Json response.
     * @return
     */
    public static Result deleteAccount(String auid){
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceAccounts::deleteAccount():: " + json.toString());

        // check if there is any required parameter missing
        List<String> notFound = JsonUtils.requirePropertiesInJson(json,
                "auid"
        );
        if( !notFound.isEmpty() && ( auid == null || auid.trim().isEmpty() ) ){
            return AnyResponseHelper.requiredFieldsMissing(notFound);
        }

        // if the auid in the route is empty then try to get the one from the POST json body
        if( auid == null || auid.trim().isEmpty() )
            auid = json.path("auid").textValue();
        try {
            if( !ProxyDataSource.getIDatasource().deleteFromKey(auid) ){
                return AnyResponseHelper.bad_request("Account could not be deleted!");
            }
            return AnyResponseHelper.ok("Successfully deleted account!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }


    /**
     * Updates the account specified by the AUID.
     * The result of the update is returned in the Json response.
     * @return
     */
    public static Result UpdateAccount(String auid){
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceAccounts::updateAccount():: " + json.toString());

        // check if there is any required parameter missing
        // check if there is any required parameter missing
        List<String> notFound = JsonUtils.requirePropertiesInJson(json,
                "auid"
        );
        if( !notFound.isEmpty() && ( auid == null || auid.trim().isEmpty() ) ){
            return AnyResponseHelper.requiredFieldsMissing(notFound);
        }
        // if the auid in the route is empty then try to get the one from the POST json body
        if( auid == null || auid.trim().isEmpty() )
            auid = json.path("auid").textValue();

        try {
            // fetch the stored object
            JsonNode storedAccount;
            if( (storedAccount = ProxyDataSource.getIDatasource().getFromKeyAsJson(auid)) == null ){
                return AnyResponseHelper.bad_request("Account could not be updated! Try again...");
            }

            // apply any change made
            String updateableFields[] = AccountModel.getChangeableProperties();
            for( String s : updateableFields ){
                JsonNode val = json.path(s);
                if( val.isBoolean() ){
                    ((ObjectNode)storedAccount).put(s, val.asBoolean());
                }else{
                    String nv = val.textValue();
                    if( nv == null || nv.trim().isEmpty() )
                        continue;
                    ((ObjectNode)storedAccount).put(s, val.textValue());
                }
            }

            // save the changes
            if( !ProxyDataSource.getIDatasource().replaceJsonDocument(auid, 0, storedAccount.toString()) ){
                return AnyResponseHelper.bad_request("Account could not be updated! Try again...");
            }

            return AnyResponseHelper.ok("Successfully updated account!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Returns the list of clients for this account
     * @param auid The account for which the clients are to be returned
     * @return
     */
    public static Result fetchAccountClients(String auid) {
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceAccounts::fetchAccountClients():: " + json.toString());

        // check if there is any required parameter missing
        List<String> notFound = JsonUtils.requirePropertiesInJson(json,
                "auid"
        );
        if( !notFound.isEmpty() && ( auid == null || auid.trim().isEmpty() ) ){
            return AnyResponseHelper.requiredFieldsMissing(notFound);
        }

        // if the auid in the route is empty then try to get the one from the POST json body
        if( auid == null || auid.trim().isEmpty() )
            auid = json.path("auid").textValue();
        try {
            JsonNode storedAccount;
            if( (storedAccount = ProxyDataSource.getIDatasource().getFromKeyAsJson(auid)) == null ){
                return AnyResponseHelper.bad_request("Account could not be found!");
            }
            JsonNode json_clients = storedAccount.path("clients");
            ObjectNode resp = JsonUtils.createObjectNode();
            resp.put("clients", json_clients);
            return AnyResponseHelper.ok(resp, "Successfully fetched account clients!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }


    /**
     * Adds a new client for this account
     * @param auid The account the new account belongs to
     * @return
     */
    public static Result addAccountClient(String auid) {
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceAccounts::addAccountClient():: " + json.toString());

        // check if there is any required parameter missing
        List<String> notFound = JsonUtils.requirePropertiesInJson(json,
                "auid", "grant_type"
        );
        if( !notFound.isEmpty() && ( auid == null || auid.trim().isEmpty() ) ){
            return AnyResponseHelper.requiredFieldsMissing(notFound);
        }

        // if the auid in the route is empty then try to get the one from the POST json body
        if( auid == null || auid.trim().isEmpty() )
            auid = json.path("auid").textValue();

        String grant_type = json.path("grant_type").textValue();
        String scope = json.path("scope").textValue();
        String redirect_uri = json.path("redirect_uri").textValue();

        if( !GrantHandlerFactory.isGrantTypeSupported(grant_type)){
            return AnyResponseHelper.bad_request("grant_type specified is not supported!");
        }

        try {
            JsonNode storedAccount;
            if( (storedAccount = ProxyDataSource.getIDatasource().getFromKeyAsJson(auid)) == null ){
                return AnyResponseHelper.bad_request("Account could not be found!");
            }
            AccountModel account = new AccountModel(storedAccount);
            account.addNewClient(grant_type, scope, redirect_uri);

            // save the changes
            if( !ProxyDataSource.getIDatasource().replaceJsonDocument(auid, 0, account.toJson().toString()) ){
                return AnyResponseHelper.bad_request("Account could not be updated! Try again...");
            }

            return AnyResponseHelper.ok("Successfully added account client!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }


    /**
     * Fetches the account  client with the AUID and client_id passed in.
     * The client document is returned in the Json response.
     * @return
     */
    public static Result fetchAccountClient(String auid, String client_id){
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceAccounts::fetchAccount():: " + json.toString());

        // check the arguments
        if( ( auid == null || auid.trim().isEmpty() ) ){
            return AnyResponseHelper.bad_request("Invalid account id provided!");
        }
        if( ( client_id == null || client_id.trim().isEmpty() ) ){
            return AnyResponseHelper.bad_request("Invalid client id provided!");
        }

        try {
            JsonNode storedAccount;
            if( (storedAccount = ProxyDataSource.getIDatasource().getFromKeyAsJson(auid)) == null ){
                return AnyResponseHelper.bad_request("Account could not be found!");
            }
            AccountModel account = new AccountModel(storedAccount);
            AccountModel.ClientModel client = account.getClient(client_id);
            if( client == null ){
                return AnyResponseHelper.bad_request("Account client could not be found!");
            }
            return AnyResponseHelper.ok((ObjectNode)client.toJson(), "Successfully fetched account client!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Fetches the account  client with the AUID and client_id passed in.
     * The client document is returned in the Json response.
     * @return
     */
    public static Result deleteAccountClient(String auid, String client_id){
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceAccounts::deleteAccount():: " + json.toString());

        // check the arguments
        if( ( auid == null || auid.trim().isEmpty() ) ){
            return AnyResponseHelper.bad_request("Invalid account id provided!");
        }
        if( ( client_id == null || client_id.trim().isEmpty() ) ){
            return AnyResponseHelper.bad_request("Invalid client id provided!");
        }

        try {
            JsonNode storedAccount;
            if( (storedAccount = ProxyDataSource.getIDatasource().getFromKeyAsJson(auid)) == null ){
                return AnyResponseHelper.bad_request("Account could not be found!");
            }
            AccountModel account = new AccountModel(storedAccount);
            if( !account.deleteClient(client_id) ){
                return AnyResponseHelper.bad_request("Account client could not be found!");
            }
            // save the changes
            if( !ProxyDataSource.getIDatasource().replaceJsonDocument(auid, 0, account.toJson().toString()) ){
                return AnyResponseHelper.bad_request("Account could not be updated! Try again...");
            }

            return AnyResponseHelper.ok("Successfully deleted account client!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

}
