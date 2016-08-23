package controllers;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import datasources.CouchbaseDatasource;
import datasources.DatasourceException;
import net.spy.memcached.PersistTo;
import net.spy.memcached.internal.OperationFuture;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import radiomapservercywee.RadioMap;
import utils.AnyResponseHelper;
import utils.AnyplaceServerAPI;
import utils.JsonUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;

//import org.codehaus.jackson.JsonNode;
//import org.codehaus.jackson.node.ArrayNode;
//import org.codehaus.jackson.node.ObjectNode;


public class AnyplaceAdmin extends Controller {

    private static boolean checkAdminCredentials(String user, String pass) {
        return user != null && user.equalsIgnoreCase("lambros") && pass != null && pass.equalsIgnoreCase("fuckcaliforniadream");
    }


    /**
     * Add test accounts in the database
     * TODO - CAREFUL USE ONLY BY LAMBROS
     */
    public static Result addTestAccounts(){
        // IMPORTANT
        response().setHeader("Access-Control-Allow-Origin", "*");      // to allow cross domain requests

        ObjectNode result = Json.newObject();
        Http.RequestBody body = request().body();
        JsonNode json = body.asJson();
        if( json == null ){
            result.put("status", "error");
            result.put("message", "Empty request!" );
            return badRequest(result);
        }

        System.out.println( json.toString() );

        String username = json.findPath("username").textValue();
        String password = json.findPath("password").textValue();

        if( !checkAdminCredentials(username, password) ){
            result.put("status", "error");
            result.put("message", "Invalid credentials (username/password)!" );
            return badRequest(result);
        }

        CouchbaseClient couchbaseClient;
        try {
            couchbaseClient = CouchbaseDatasource.getStaticInstance().getConnection();
        } catch (DatasourceException e) {
            result.put("status", "error");
            result.put("message", "Cannot establish connection to Anyplace database!");
            return badRequest(result);
        }

        // '{"grant_type":"password", "username":"test_username", "password":"test_password",
        // "scope": "test_scope", "client_id":"test_client_id", "client_secret": "test_client_secret"}'
        ObjectNode account = JsonUtils.createObjectNode();
        account.put("auid", "anyaccount_test_auid");
        account.put("username", "test_username");
        account.put("password", "test_password");
        account.put("scope", "test_scope");

        ArrayNode clients = account.putArray("clients");
        ObjectNode client = JsonUtils.createObjectNode();
        client.put("client_id", "test_client_id");
        client.put("client_secret", "test_client_secret");
        client.put("grant_type", "password");
        clients.add(client);

        System.out.println(account.toString());
        OperationFuture<Boolean> db_res = couchbaseClient.add(account.get("auid").textValue(), 0, account.toString(), PersistTo.ONE);

        try {
            if (!db_res.get()) {
                result.put("status", "error");
                result.put("message", "Account already exists or could not be added!");
                return badRequest(result);
            } else {
                // everything is ok
                result.put("status", "success");
                result.put("message", "Successfully added test account!");
                result.put("buid", account.get("auid").textValue());
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Test account addition error: " + e);
            return badRequest(result);
        }

        return ok(result);
    }


    /**
     * DOWNLOAD ALL INFORMATION STORED IN THE DATABASE
     * TODO - CAREFUL USE ONLY BY LAMBROS
     *
     * @return
     */
    public static Result dumpAllDocuments() {

        ObjectNode result = Json.newObject();

        JsonNode json = request().body().asJson();
        if (json == null) {
            result.put("status", "error");
            result.put("message", "Cannot parse request body as JSON!");
            return badRequest(result);
        }
        System.out.println(json.toString());

        String username = json.findPath("username").textValue();
        String password = json.findPath("password").textValue();
        if (!checkAdminCredentials(username, password)) {
            result.put("status", "error");
            result.put("message", "Wrong credentials!!!");
            return badRequest(result);
        }


        File root = new File("couchbase_dumps");
        if (!root.exists()) {
            root.mkdirs();
        }
        if (!root.isDirectory() || !root.canWrite()) {
            System.out.println("Cannot access couchbase dump files directory!");
            result.put("status", "error");
            result.put("message", "Cannot access couchbase dump files directory!");
            return badRequest(result);
        }

        File dumpFile = new File(root, "couchbaseDump_" + System.currentTimeMillis() + ".dat");
        PrintWriter writer;
        try {
            writer = new PrintWriter(dumpFile);
        } catch (FileNotFoundException e) {
            System.out.println("Cannot open file for writing!");
            result.put("status", "error");
            result.put("message", "Cannot open dump file!");
            return badRequest(result);
        }

        writer.println("#format:  KEY__LAMBROS-PETROU__DOCUMENT");

        CouchbaseClient couchbaseClient;
        try {
            couchbaseClient = CouchbaseDatasource.getStaticInstance().getConnection();
        } catch (DatasourceException e) {
            result.put("status", "error");
            result.put("message", "Cannot establish connection to Anyplace database!");
            return badRequest(result);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        int queryLimit = 100000;
        HashMap<String, String> hm;
        int totalFetched = 0;
        int currentFetched;
        do {
            View view = couchbaseClient.getView("admin", "all_documents");
            Query query = new Query();
            query.setIncludeDocs(true);
            query.setDescending(true);
            query.setLimit(queryLimit);
            query.setSkip(totalFetched);
            query.setStale(Stale.FALSE);

            ViewResponse res = couchbaseClient.query(view, query);

            currentFetched = 0;
            for (ViewRow row : res) {
                // handle each raw radio entry
                currentFetched++;
                writer.println(row.getKey() + "__LAMBROS-PETROU__" + row.getDocument().toString());
                System.out.println("id[" + row.getKey() + "]");

            } // end while paginator
            totalFetched += currentFetched;

            Logger.info("total fetched: " + totalFetched);
        } while (currentFetched >= queryLimit);

        // close the file
        writer.flush();
        writer.close();

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        System.out.println("Dumped " + totalFetched + " documents!");
        writer.close();

        result.put("status", "success");
        result.put("message", "Successfully backed up documents in DB! total documents saved[" + totalFetched + "]");
        return ok(result);
    }


    /**
     * FLUSH ALL RADIO MAP INFORMATION STORED IN THE DATABASE
     * TODO - CAREFUL USE ONLY BY LAMBROS
     *
     * @return
     */
    public static Result radioFlush() {

        ObjectNode result = Json.newObject();

        JsonNode json = request().body().asJson();
        if (json == null) {
            result.put("status", "error");
            result.put("message", "Cannot parse request body as JSON!");
            return badRequest(result);
        }
        System.out.println(json.toString());

        String username = json.findPath("username").textValue();
        String password = json.findPath("password").textValue();
        if (!checkAdminCredentials(username, password)) {
            result.put("status", "error");
            result.put("message", "Wrong credentials!!!");
            return badRequest(result);
        }


        CouchbaseClient couchbaseClient;
        try {
            couchbaseClient = CouchbaseDatasource.getStaticInstance().getConnection();
        } catch (DatasourceException e) {
            result.put("status", "error");
            result.put("message", "Cannot establish connection to Anyplace database!");
            return badRequest(result);
        }

        View view = couchbaseClient.getView("admin", "raw_radio_all");
        Query query = new Query();


        // get all radio documents
        Gson gson = new Gson();
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());
        long i = 0;
        for (ViewRow row : res) {
            couchbaseClient.delete(row.getValue());
            System.out.println("id[" + row.getValue() + "]");
            i++;
        }
        System.out.println("Deleted " + i);

        result.put("status", "success");
        result.put("message", "Successfully [" + i + "] raw radio RSS logs from DB! Total [" + res.size() + "]");
        return ok(result);
    }





    /**
     * Returns a link to the radio map that needs to be downloaded according to the specified coordinates
     * @return a link to the radio_map file
     */
    public static Result radioMapConstruction(String radio_folder){
            try {
                File dir = new File( "radiomaps" + File.separatorChar + radio_folder);
                String folder = dir.toString();

                String radiomap_filename = new File (folder + File.separatorChar + "indoor-radiomap.txt").getAbsolutePath();
                String radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt");
                String radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt");
                String radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt");

                // create the radiomap using the input rss log file
                RadioMap rm = new RadioMap( new File(folder), radiomap_filename, "", -110 );
                if( !rm.createRadioMap() ){
                    return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!");
                }

                // fix the paths
                // radiomaps is the folder where the folders reside in
                String api = AnyplaceServerAPI.SERVER_API_ROOT;
                int pos = radiomap_mean_filename.indexOf("radiomaps");
                radiomap_mean_filename = api + radiomap_mean_filename.substring(pos);

                pos = radiomap_rbf_weights_filename.indexOf("radiomaps");
                radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos);

                pos = radiomap_parameters_filename.indexOf("radiomaps");
                radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos);

                // everything is ok
                ObjectNode res = JsonUtils.createObjectNode();
                res.put( "map_url_mean", radiomap_mean_filename );
                res.put( "map_url_weights", radiomap_rbf_weights_filename );
                res.put( "map_url_parameters", radiomap_parameters_filename );

                return AnyResponseHelper.ok(res, "Successfully created radio map.");
            } catch (Exception e) {
                // no exception is expected to be thrown but just in case
                return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly! : " + e.getMessage());
            }
        }
}
