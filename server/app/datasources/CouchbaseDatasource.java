/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): First Author, Second Author, …
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */

package datasources;

import accounts.IAccountService;
import com.avaje.ebeaninternal.server.lib.sql.DataSourceException;
import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;
import com.couchbase.client.TapClient;
import com.couchbase.client.protocol.views.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import db_models.Connection;
import db_models.Poi;
import db_models.RadioMapRaw;
import floor_module.IAlgo;
import net.spy.memcached.PersistTo;
import net.spy.memcached.internal.OperationFuture;
import oauth.provider.v2.models.AccessTokenModel;
import oauth.provider.v2.models.AccountModel;
import oauth.provider.v2.models.AuthInfo;
import oauth.provider.v2.token.TokenService;
import play.Logger;
import play.Play;
import utils.AnyResponseHelper;
import utils.GeoPoint;
import utils.JsonUtils;
import utils.LPLogger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This is my own client interface for the Couchbase database
 */
public class CouchbaseDatasource implements IDatasource, IAccountService {

    private static CouchbaseDatasource sInstance;
    private static Object sLockInstance = new Object();

    /**
     * Creates and returns a Couchbase Datasource instance with the
     * configuration settings from application.conf.
     * <p>
     * Consecutive calls of this method will always return the same instance.
     * Use the createNewInstance() in order to get a newly created instance with
     * your own configuration settings.
     *
     * @return The CouchbaseDatasource object with the default configuration
     */
    public static CouchbaseDatasource getStaticInstance() {
        synchronized (sLockInstance) {
            if (sInstance == null) {
                String hostname = Play.application().configuration().getString("couchbase.hostname");
                String port = Play.application().configuration().getString("couchbase.port");
                String bucket = Play.application().configuration().getString("couchbase.bucket");
                String password = Play.application().configuration().getString("couchbase.password");
                sInstance = CouchbaseDatasource.createNewInstance(hostname, port, bucket, password);
                try {
                    sInstance.init();
                } catch (DatasourceException e) {
                    LPLogger.error("CouchbaseDatasource::getStaticInstance():: Exception while instantiating Couchbase [" + e.getMessage() + "]");
                }
            }
            return sInstance;
        }
    }

    /**
     * Creates and returns a Couchbase Datasource instance with the
     * passed configuration settings. This call should be followed by
     * an INIT() invocation in order to connect the client to the server.
     *
     * @param hostname The hostname of the Couchbase server
     * @param port     The port of the Couchbase server
     * @param bucket   The bucket you want to connect on the Couchbase server
     * @param password The bucket's password
     * @return The constructed CouchbaseDatasource object with the configuration as set
     */
    public static CouchbaseDatasource createNewInstance(String hostname,
                                                        String port,
                                                        String bucket,
                                                        String password) {
        if (hostname == null || port == null
                || bucket == null || password == null) {
            throw new IllegalArgumentException("[null] parameters are not allowed to create a CouchbaseDatasource");
        }
        hostname = hostname.trim();
        port = port.trim();
        bucket = bucket.trim();
        password = password.trim();
        if (hostname.isEmpty() || port.isEmpty()
                || bucket.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Empty string configuration are not allowed to create a CouchbaseDatasource");
        }
        return new CouchbaseDatasource(hostname, port, bucket, password);
    }

    // Couchbase server configuration settings
    private String mHostname;
    private String mPort;
    private String mBucket;
    private String mPassword;

    // Holds the actual CouchbaseClient
    private CouchbaseClient mClient = null;
    private TapClient mTapClient = null;

    // creates a Couchbase datasource object with the specified configuration
    // a connect() call should follow to initialize the datasource

    /**
     * Creates a CouchbaseDatasource with the configuration settings passed in.
     *
     * @param hostname The hostname of the Couchbase server
     * @param port     The port of the Couchbase server
     * @param bucket   The bucket you want to connect on the Couchbase server
     * @param password The bucket's password
     */
    private CouchbaseDatasource(String hostname,
                                String port,
                                String bucket,
                                String password) {
        this.mHostname = hostname;
        this.mPort = port;
        this.mBucket = bucket;
        this.mPassword = password;
    }

    ///////////////////////////////////////////////////////////////////////////////
    // Couchbase Connection
    ///////////////////////////////////////////////////////////////////////////////


    /**
     * Connects to Couchbase based on the passed configuration settings.
     * Sets the internal client to the connected client.
     *
     * @return True if the connection succeeded or exception thrown if not
     * @throws DatasourceException
     */
    private boolean connect() throws DatasourceException {
        /*
        if(1==1){
            LPLogger.info("do not connect to couchbase");
            throw new DatasourceException("Cannot connect to Anyplace Database! [Unknown]");
        }
        */

        Logger.info("Trying to connect to: " + mHostname + ":" + mPort + " bucket[" + mBucket + "] password: " + mPassword);

        List<URI> uris = new LinkedList<URI>();
        uris.add(URI.create(mHostname + ":" + mPort + "/pools"));
        try {
            CouchbaseConnectionFactoryBuilder cfb = new CouchbaseConnectionFactoryBuilder();
            //cfb.setMaxReconnectDelay(10000);
            //cfb.setOpQueueMaxBlockTime(5000);
            //cfb.setOpTimeout(20000);
            //cfb.setShouldOptimize(true);
            //cfb.setViewTimeout(6000);
            //CouchbaseConnectionFactory connFactory = cfb.buildCouchbaseConnection(uris, mBucket, mPassword);
            CouchbaseConnectionFactory connFactory = new CouchbaseConnectionFactory(uris, mBucket, mPassword);

            mClient = new CouchbaseClient(connFactory);
            mTapClient = new TapClient(uris, mBucket, mPassword);
        } catch (java.net.SocketTimeoutException e) {
            // thrown by the constructor on timeout
            LPLogger.error("CouchbaseDatasource::connect():: Error connection to Couchbase: " + e.getMessage());
            throw new DatasourceException("Cannot connect to Anyplace Database [SocketTimeout]!");
        } catch (IOException e) {
            LPLogger.error("CouchbaseDatasource::connect():: Error connection to Couchbase: " + e.getMessage());
            throw new DatasourceException("Cannot connect to Anyplace Database [IO]!");
        } catch (Exception e) {
            LPLogger.error("CouchbaseDatasource::connect():: Error connection to Couchbase: " + e.getMessage());
            throw new DatasourceException("Cannot connect to Anyplace Database! [Unknown]");
        }
        return true;
    }

    /**
     * Disconnect from Couchbase server and sets the internal client to null.
     *
     * @return True if disconnected successfully, otherwise False
     * @throws DatasourceException If the internal client is null,
     *                             thus no connection has occurred exception is thrown.
     */
    public boolean disconnect() throws DatasourceException {
        if (mClient == null) {
            LPLogger.error("CouchbaseDatasource::disconnect():: Trying to disconnect from a null client");
            throw new DatasourceException("Trying to disconnect a NULL client!");
        }
        boolean res = mClient.shutdown(3, TimeUnit.SECONDS);
        this.mClient = null;
        return res;
    }

    /**
     * Returns the internal client object if != null, otherwise it calls connect()
     * and returns the newly connected client.
     *
     * @return The connected client
     * @throws DatasourceException If the client cannot connect to the server exception is thrown
     */
    public CouchbaseClient getConnection() throws DatasourceException {
        if (mClient == null) {
            connect();
        }
        return mClient;
    }

    ///////////////////////////////////////////////////////////////////////////////
    // IDatasource Implementations
    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Initializes the Couchbase connection, thus trying to connect to the server
     * and have a connected connection client available.
     *
     * @return True if the connection succeeded
     * @throws DatasourceException If an error occurred while connecting this exception is thrown
     */
    @Override
    public boolean init() throws DatasourceException {
        try {
            this.connect();
        } catch (DatasourceException e) {
            LPLogger.error("CouchbaseDatasource::init():: " + e.getMessage());
            throw new DatasourceException("Cannot establish connection to Anyplace database!");
        }
        return true;
    }

    @Override
    public boolean addJsonDocument(String key, int expiry, String document) throws DatasourceException {
        // verifies that a connection exists or an exception is thrown
        CouchbaseClient client = getConnection();
        OperationFuture<Boolean> db_res = client.add(key, expiry, document, PersistTo.ONE);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json=null;
        try {
            json = mapper.readTree(document);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (json.get("puid")!=null) {
            String buid = json.get("buid").toString().substring(1, json.get("buid").toString().length() - 1);
            if (allPoisSide.get(buid)!=null){
                List<JsonNode> pois = allPoisSide.get(buid);
                if (json.findValue("pois_type").toString().compareTo("\"None\"")!=0){
                    pois.add(json);
                    allPoisSide.remove(buid);
                    allPoisSide.put(buid,pois);
                }
            }
        }
        try {
            return db_res.get();
        } catch (InterruptedException e) {
            throw new DataSourceException("Document storing interrupted!");
        } catch (ExecutionException e) {
            throw new DataSourceException("Document storing had an exception!");
        }
    }

    @Override
    public boolean replaceJsonDocument(String key, int expiry, String document) throws DatasourceException {
        // verifies that a connection exists or an exception is thrown
        CouchbaseClient client = getConnection();
        OperationFuture<Boolean> db_res = client.replace(key, expiry, document, PersistTo.ONE);
        try {
            return db_res.get();
        } catch (InterruptedException e) {
            throw new DataSourceException("Document replace interrupted!");
        } catch (ExecutionException e) {
            throw new DataSourceException("Document replace had an exception!");
        }
    }

    /**
     * Returns the JSON document from the Couchbase
     *
     * @param key The key of the document required
     * @return Returns the Json document as String, or null if no document exists with the key
     * @throws DatasourceException This exception is only thrown if the connection failed
     */
    @Override
    public boolean deleteFromKey(String key) throws DatasourceException {
        // verifies that a connection exists or an exception is thrown
        CouchbaseClient client = getConnection();
        OperationFuture<Boolean> db_res = client.delete(key, PersistTo.ONE);
        try {
            return db_res.get();
        } catch (InterruptedException e) {
            throw new DataSourceException("Document delete interrupted!");
        } catch (ExecutionException e) {
            throw new DataSourceException("Document delete had an exception!");
        }
    }

    /**
     * Returns the JSON document from the Couchbase
     *
     * @param key The key of the document required
     * @return Returns the Json document as String, or null if no document exists with the key
     * @throws DatasourceException This exception is only thrown if the connection failed
     */
    @Override
    public Object getFromKey(String key) throws DatasourceException {
        // verifies that a connection exists or an exception is thrown
        CouchbaseClient client = getConnection();
        Object db_res = client.get(key);
        return db_res;
    }

    /**
     * The Json document with the specified key is returned as a JsonNode.
     *
     * @param key The key of the document required
     * @return Returns the Json document as JsonNode, or null if no document exists with the key
     * @throws DatasourceException      This exception is only thrown if the connection failed
     * @throws IllegalArgumentException if empty/null key is specified
     */
    @Override
    public JsonNode getFromKeyAsJson(String key) throws DatasourceException {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("No null or empty string allowed as key!");
        }
        Object db_res = getFromKey(key);
        // document does not exist
        if (db_res == null) {
            return null;
        }
        try {
            JsonNode jsonNode = JsonUtils.getJsonTree((String) db_res);
            return jsonNode;
        } catch (IOException e) {
            LPLogger.error("CouchbaseDatasource::getFromKeyAsJson():: Could not convert document from Couchbase into JSON!");
            return null;
        }
    }

    @Override
    public JsonNode buildingFromKeyAsJson(String key) throws DatasourceException {
        // fetch the building
        ObjectNode building = (ObjectNode) getFromKeyAsJson(key);
        if (building == null) {
            return null;
        }
        // fetch the floors
        ArrayNode floors = building.putArray("floors");
        for (JsonNode f : floorsByBuildingAsJson(key)) {
            floors.add(f);
        }
        // fetch the Pois
        ArrayNode pois = building.putArray("pois");
        for (JsonNode p : poisByBuildingAsJson(key)) {
            if (p.path("pois_type").textValue().equals(Poi.POIS_TYPE_NONE))
                continue;
            pois.add(p);
        }
        return building;
    }

    /**
     * The POI with specified key is returned as JsonNode
     *
     * @param key The key of the POI required
     * @return Returns the Json document as JsonNode, or null if no document exists with the key
     * @throws DatasourceException      This exception is only thrown if the connection failed
     * @throws IllegalArgumentException if empty/null key is specified
     */
    @Override
    public JsonNode poiFromKeyAsJson(String key) throws DatasourceException {
        return getFromKeyAsJson(key);
    }

    @Override
    public List<JsonNode> tempAllPoisWithoutUrl() throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "all_pois_without_url");
        Query query = new Query();
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<JsonNode> poisByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "pois_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                json.remove("geometry");
//                json.remove("image");
//                json.remove("is_door");
//                json.remove("is_published");
//                json.remove("floor_name");
//                json.remove("url");
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<JsonNode> poisByBuildingIDAsJson(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "pois_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);

        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                json.remove("geometry");
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }


    @Override
    public List<HashMap<String, String>> poisByBuildingFloorAsMap(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "pois_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
        for (ViewRow row : res) {
            result.add(JsonUtils.getHashMapStrStr(row.getDocument().toString()));
        }
        return result;
    }

    @Override
    public List<JsonNode> poisByBuildingAsJson(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "pois_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        List<JsonNode> pois = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                json.remove("geometry");
//                json.remove("image");
//                json.remove("is_door");
//                json.remove("is_published");
//                json.remove("floor_name");
//                json.remove("url");
                if (json.findValue("pois_type").toString().compareTo("\"None\"")!=0){
                    pois.add(json);
                }
                //"pois_type":"None"
            } catch (IOException e) {
                // skip this one since not a valid Json object
            }
        }
        return pois;
    }

    public String lastletters = "";
    public ArrayList<ArrayList<String>> wordsELOT = new ArrayList<>();
    @Override
    public List<JsonNode> poisByBuildingAsJson2GR(String cuid,String letters) throws DatasourceException {

        List<JsonNode> pois;

        List<JsonNode> pois2 = new ArrayList<JsonNode>();

        pois=allPoisbycuid.get(cuid);


        String words [] = letters.split(" ");

        boolean flag ,flag2,flag3 ;

        if (letters.compareTo(lastletters)!=0){
            lastletters=letters;
            wordsELOT = new ArrayList<>();
            for (int j=0; j<words.length; j++) {
                wordsELOT.add(greeklishTogreekList(words[j].toLowerCase()));
            }
        }
        for (JsonNode json: pois) {
            flag = true;
            flag2 = true;
            flag3 = true;
            int j=0;
            for (String w:words){
                if (!((json.get("name").toString().toLowerCase().contains(w.toLowerCase())
                        ||json.get("description").toString().toLowerCase().contains(w.toLowerCase())))){
                    flag = false;
                }
                String greeklish = greeklishTogreek(w.toLowerCase());
                if (!((json.get("name").toString().toLowerCase().contains(greeklish)
                        ||json.get("description").toString().toLowerCase().contains(greeklish)))){
                    flag2 = false;
                }

                if (wordsELOT.size()!=0){
                    ArrayList<String> wordsELOT2 = new ArrayList<>();
                    wordsELOT2 = wordsELOT.get(j++);
                    if (wordsELOT2.size()==0){
                        flag3 = false;
                    }
                    else {
                        for (String greeklishELOT:wordsELOT2) {
                            if (!((json.get("name").toString().toLowerCase().contains(greeklishELOT)
                                    || json.get("description").toString().toLowerCase().contains(greeklishELOT)))) {
                                flag3 = false;
                            } else {
                                flag3 = true;
                                break;
                            }
                        }
                    }
                }
                else flag3=false;

                if (!flag3 && !flag && !flag2) break;
            }
            if (flag || flag2 || flag3){
                pois2.add(json);
            }
        }
        return pois2;
    }

    public List<JsonNode> poisByBuildingAsJson2(String cuid,String letters) throws DatasourceException {

        List<JsonNode> pois;

        List<JsonNode> pois2 = new ArrayList<JsonNode>();

        pois=allPoisbycuid.get(cuid);

        String words [] = letters.split(" ");

        boolean flag  ;

        for (JsonNode json: pois) {
            flag = true;
            int j=0;
            for (String w:words){
                if (!((json.get("name").toString().toLowerCase().contains(w.toLowerCase())
                        ||json.get("description").toString().toLowerCase().contains(w.toLowerCase())))){
                    flag = false;
                }

                if (!flag) break;
            }
            if (flag){
                pois2.add(json);
            }
        }
        return pois2;
    }

    public List<JsonNode> poisByBuildingAsJson3(String buid,String letters) throws DatasourceException {

        List<JsonNode> pois;

        List<JsonNode> pois2 = new ArrayList<JsonNode>();
        if (allPoisSide.get(buid)!=null){
            pois=allPoisSide.get(buid);
        }
        else{
            pois=poisByBuildingAsJson(buid);
        }

        String words [] = letters.split(" ");

        boolean flag ,flag2,flag3 ;

        if (letters.compareTo(lastletters)!=0){
            lastletters=letters;
            wordsELOT = new ArrayList<>();
            for (int j=0; j<words.length; j++) {
                wordsELOT.add(greeklishTogreekList(words[j].toLowerCase()));
                //System.out.println(greeklishTogreekList(words[j].toLowerCase()).toString());
            }
        }
        for (JsonNode json: pois) {
            flag = true;
            flag2 = true;
            flag3 = true;
            int j=0;
            for (String w:words){
                if (!((json.get("name").toString().toLowerCase().contains(w.toLowerCase())
                        ||json.get("description").toString().toLowerCase().contains(w.toLowerCase())))){
                    flag = false;
                }
                String greeklish = greeklishTogreek(w.toLowerCase());
                if (!((json.get("name").toString().toLowerCase().contains(greeklish)
                        ||json.get("description").toString().toLowerCase().contains(greeklish)))){
                    flag2 = false;
                }

                if (wordsELOT.size()!=0){
                    ArrayList<String> wordsELOT2 = new ArrayList<>();
                    wordsELOT2 = wordsELOT.get(j++);
                    if (wordsELOT2.size()==0){
                        flag3 = false;
                    }
                    else {
                        for (String greeklishELOT:wordsELOT2) {
                            if (!((json.get("name").toString().toLowerCase().contains(greeklishELOT)
                                    || json.get("description").toString().toLowerCase().contains(greeklishELOT)))) {
                                flag3 = false;
                            } else {
                                flag3 = true;
                                break;
                            }
                        }
                    }
                }
                else flag3=false;

                if (!flag3 && !flag && !flag2) break;
            }
            if (flag || flag2 || flag3){
                pois2.add(json);
            }
        }
        return pois2;
    }

    public String greeklishTogreek(String greeklish){

        char[] myChars = greeklish.toCharArray();

        for (int i=0; i<greeklish.length(); i++){

            switch (myChars[i]) {
                case 'a':  myChars[i] = 'α';
                    break;
                case 'b':  myChars[i] = 'β';
                    break;
                case 'c':  myChars[i] = 'ψ';
                    break;
                case 'd':  myChars[i] = 'δ';
                    break;
                case 'e':  myChars[i] = 'ε';
                    break;
                case 'f':  myChars[i] = 'φ';
                    break;
                case 'g':  myChars[i] = 'γ';
                    break;
                case 'h':  myChars[i] = 'η';
                    break;
                case 'i':  myChars[i] = 'ι';
                    break;
                case 'j':  myChars[i] = 'ξ';
                    break;
                case 'k':  myChars[i] = 'κ';
                    break;
                case 'l':  myChars[i] = 'λ';
                    break;
                case 'm':  myChars[i] = 'μ';
                    break;
                case 'n':  myChars[i] = 'ν';
                    break;
                case 'o':  myChars[i] = 'ο';
                    break;
                case 'p':  myChars[i] = 'π';
                    break;
                case 'q':  myChars[i] = ';';
                    break;
                case 'r':  myChars[i] = 'ρ';
                    break;
                case 's':  myChars[i] = 'σ';
                    break;
                case 't':  myChars[i] = 'τ';
                    break;
                case 'u':  myChars[i] = 'θ';
                    break;
                case 'v':  myChars[i] = 'ω';
                    break;
                case 'w':  myChars[i] = 'ς';
                    break;
                case 'x':  myChars[i] = 'χ';
                    break;
                case 'y':  myChars[i] = 'υ';
                    break;
                case 'z':  myChars[i] = 'ζ';
                    break;
                default:
                    break;
            }
        }
        return String.valueOf(myChars);
    }

    public ArrayList<String> greeklishTogreekList(String greeklish){

        ArrayList<String> words = new ArrayList<>();
        words.add("");

        char[] myChars = greeklish.toCharArray();

        for (int i=0; i<greeklish.length(); i++){
            int size = words.size();
            for (int j=0; j<size; j++){
                String myword="";
                myword = words.get(j);
                if (myChars[i]=='a'){
                    words.add(myword + "α");
                }
                else if(myChars[i]=='b'){
                    words.add(myword + "β");
                    words.add(myword + "μπ");
                }
                else if(myChars[i]=='c'){
                    if (i<greeklish.length()-1){
                        if(myChars[i+1]=='h') {
                            words.add(myword + "χ");
                        }
                    }
                    words.add(myword + "γ");
                }
                else if(myChars[i]=='d'){
                    words.add(myword + "δ");
                    words.add(myword + "ντ");
                }
                else if(myChars[i]=='e'){
                    words.add(myword + "ε");
                    words.add(myword + "αι");
                    words.add(myword + "ι");
                    words.add(myword + "η");
                }
                else if(myChars[i]=='f'){
                    words.add(myword + "φ");
                }
                else if(myChars[i]=='g'){
                    words.add(myword + "γ");
                    words.add(myword + "γγ");
                    words.add(myword + "γκ");
                }
                else if(myChars[i]=='h'){
                    if (myword.length()>0 && myword.charAt(myword.length()-1)=='θ'){
                        words.add(myword);
                        continue;
                    }
                    if (myword.length()>0 && myword.charAt(myword.length()-1)=='χ'){
                        words.add(myword);
                        continue;
                    }
                    words.add(myword + "χ");
                    words.add(myword + "η");
                }
                else if(myChars[i]=='i'){
                    words.add(myword + "ι");
                    words.add(myword + "η");
                    words.add(myword + "υ");
                    words.add(myword + "οι");
                    words.add(myword + "ει");
                }
                else if(myChars[i]=='j'){
                    words.add(myword + "ξ");
                }
                else if(myChars[i]=='k'){
                    if (i<greeklish.length()-1){
                        if(myChars[i+1]=='s') {
                            words.add(myword + "ξ");
                        }
                    }
                    words.add(myword + "κ");
                }
                else if(myChars[i]=='l'){
                    words.add(myword + "λ");
                }
                else if(myChars[i]=='m'){
                    words.add(myword + "μ");
                }
                else if(myChars[i]=='n'){
                    words.add(myword + "ν");
                }
                else if(myChars[i]=='o'){
                    words.add(myword + "ο");
                    words.add(myword + "ω");
                }
                else if(myChars[i]=='p'){
                    if (i<greeklish.length()-1){
                        if(myChars[i+1]=='s') {
                            words.add(myword + "ψ");
                        }
                    }
                    words.add(myword + "π");
                }
                else if(myChars[i]=='q'){
                    words.add(myword + ";");
                }
                else if(myChars[i]=='r'){
                    words.add(myword + "ρ");
                }
                else if(myChars[i]=='s'){
                    if (myword.length()>0 && myword.charAt(myword.length()-1)=='ξ'){
                        words.add(myword);
                        continue;
                    }
                    if (myword.length()>0 && myword.charAt(myword.length()-1)=='ψ'){
                        words.add(myword);
                        continue;
                    }
                    words.add(myword + "σ");
                    words.add(myword + "ς");
                }
                else if(myChars[i]=='t'){
                    if (i<greeklish.length()-1){
                        if(myChars[i+1]=='h') {
                            words.add(myword + "θ");
                        }
                    }
                    words.add(myword + "τ");
                }
                else if(myChars[i]=='u'){
                    words.add(myword + "υ");
                    words.add(myword + "ου");
                }
                else if(myChars[i]=='v'){
                    words.add(myword + "β");
                }
                else if(myChars[i]=='w'){
                    words.add(myword + "ω");
                }
                else if(myChars[i]=='x'){
                    words.add(myword + "χ");
                    words.add(myword + "ξ");
                }
                else if(myChars[i]=='y'){
                    words.add(myword + "υ");
                }
                else if(myChars[i]=='z'){
                    words.add(myword + "ζ");
                }
            }
            for (int j=0; j<size; j++) {
                words.remove(0);
            }
        }
        return words;
    }

    @Override
    public List<HashMap<String, String>> poisByBuildingAsMap(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "pois_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        List<HashMap<String, String>> pois = new ArrayList<HashMap<String, String>>();
        for (ViewRow row : res) {
            pois.add(JsonUtils.getHashMapStrStr(row.getDocument().toString()));
        }
        return pois;
    }

    @Override
    public List<JsonNode> floorsByBuildingAsJson(String buid) throws DatasourceException {
        List<JsonNode> floors = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "floor_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        if (res.getErrors().size() > 0) {
            throw new DatasourceException("Error retrieving floors from database!");
        }

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                floors.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return floors;
    }

    @Override
    public List<JsonNode> connectionsByBuildingAsJson(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "connection_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        JsonNode json;
        List<JsonNode> conns = new ArrayList<JsonNode>();
        for (ViewRow row : res) {
            try {
                json = JsonUtils.getJsonTree(row.getDocument().toString());
                if (json.path("edge_type").textValue().equalsIgnoreCase(Connection.EDGE_TYPE_OUTDOOR))
                    continue;
                conns.add(json);
            } catch (IOException e) {
                // skip this one since not a valid Json object
            }
        }
        return conns;
    }

    @Override
    public List<HashMap<String, String>> connectionsByBuildingAsMap(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "connection_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        HashMap<String, String> hm;
        List<HashMap<String, String>> conns = new ArrayList<HashMap<String, String>>();
        for (ViewRow row : res) {
            hm = JsonUtils.getHashMapStrStr(row.getDocument().toString());
            if (hm.get("edge_type").equalsIgnoreCase(Connection.EDGE_TYPE_OUTDOOR))
                continue;
            conns.add(hm);
        }
        return conns;
    }

    @Override
    public List<JsonNode> connectionsByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "connection_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();
        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<JsonNode> connectionsByBuildingAllFloorsAsJson(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "connection_by_buid_all_floors");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();
        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<String> deleteAllByBuilding(String buid) throws DatasourceException {
        List<String> all_items_failed = new ArrayList<String>();
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "all_by_buid");
        Query query = new Query();
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        for (ViewRow row : res) {
            String id = row.getValue();
            // we have the id so try to delete it

            OperationFuture<Boolean> db_res = couchbaseClient.delete(id, PersistTo.ONE);
            try {
                if (db_res.get().booleanValue() == false) {
                    all_items_failed.add(id);
                } else {
                    // document deleted just fine
                }
            } catch (Exception e) {
                all_items_failed.add(id);
            }
        }
        return all_items_failed;
    }

    @Override
    public List<String> deleteAllByFloor(String buid, String floor_number) throws DatasourceException {
        List<String> all_items_failed = new ArrayList<String>();
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "all_by_floor");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        for (ViewRow row : res) {
            String id = row.getValue();
            // we have the id so try to delete it
            OperationFuture<Boolean> db_res = couchbaseClient.delete(id, PersistTo.ONE);
            try {
                if (db_res.get().booleanValue() == false) {
                    all_items_failed.add(id);
                } else {
                    // document deleted just fine
                }
            } catch (Exception e) {
                all_items_failed.add(id);
            }
        }
        return all_items_failed;
    }

    @Override
    public List<String> deleteAllByConnection(String cuid) throws DatasourceException {
        List<String> all_items_failed = new ArrayList<String>();
        if (!this.deleteFromKey(cuid)) {
            all_items_failed.add(cuid);
        }
        return all_items_failed;
    }

    @Override
    public List<String> deleteAllByPoi(String puid,String buid) throws DatasourceException {
        List<String> all_items_failed = new ArrayList<String>();
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "all_by_pois");
        Query query = new Query();
        query.setKey(puid);
        ViewResponse res = couchbaseClient.query(view, query);
        for (ViewRow row : res) {
            String id = row.getValue();
            // we have the id so try to delete it
            OperationFuture<Boolean> db_res = couchbaseClient.delete(id, PersistTo.ONE);
            try {
                if (db_res.get().booleanValue() == false) {
                    all_items_failed.add(id);
                } else {
                    // document deleted just fine
                    if (allPoisSide.get(buid)!=null){
                        allPoisSide.remove(buid);
                        List<JsonNode> pois = poisByBuildingAsJson(buid);
                        allPoisSide.put(buid,pois);
                    }
                }
            } catch (Exception e) {
                all_items_failed.add(id);
            }
        }
        return all_items_failed;
    }

    @Override
    public List<JsonNode> getRadioHeatmap() throws DatasourceException {
        List<JsonNode> points = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("radio", "radio_new_campus_experiment");
        Query query = new Query();
        query.setGroup(true);
        query.setReduce(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getKey());
                json.put("weight", row.getValue());
                points.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return points;
    }

    @Override
    public List<JsonNode> getRadioHeatmapByBuildingFloor(String buid, String floor) throws DatasourceException {
        List<JsonNode> points = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("radio", "radio_heatmap_building_floor");
        Query query = new Query();
        query.setGroup(true);
        query.setReduce(true);
        query.setRangeStart(ComplexKey.of(buid, floor));
        query.setRangeEnd(ComplexKey.of(buid, floor, ComplexKey.emptyObject(), ComplexKey.emptyObject()));

        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                System.out.println(row);
                json = (ObjectNode) JsonUtils.getJsonTree("{}");
                String k = row.getKey();
                System.out.println(k);
                k = k.substring(1, k.length() - 1);
                String[] array = k.split(",");
                json.put("x", array[2].substring(1, array[2].length() - 1));
                json.put("y", array[3].substring(1, array[3].length() - 1));
                json.put("w", row.getValue());
                System.out.println(json.toString());
                points.add(json);
                System.out.println();
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return points;
    }


    @Override
    public List<JsonNode> getRadioHeatmapByBuildingFloor2(String lat,String lon,String buid,String floor,int range) throws DatasourceException {
        List<JsonNode> points = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("radio", "radio_heatmap_building_floor");
        Query query = new Query();
        query.setGroup(true);
        query.setReduce(true);
        query.setRangeStart(ComplexKey.of(buid, floor));
        query.setRangeEnd(ComplexKey.of(buid, floor, ComplexKey.emptyObject(), ComplexKey.emptyObject()));


        GeoPoint bbox[] = GeoPoint.getGeoBoundingBox(Double.parseDouble(lat), Double.parseDouble(lon), range); // 50 meters radius


        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree("{}");
                String k = row.getKey();
                k = k.substring(1, k.length() - 1);
                String[] array = k.split(",");
                String x =  array[2].substring(1, array[2].length() - 1);
                String y = array[3].substring(1, array[3].length() - 1);
                if (Double.parseDouble(x)>=bbox[0].dlat&&Double.parseDouble(x)<=bbox[1].dlat
                        &&Double.parseDouble(y)>=bbox[0].dlon&&Double.parseDouble(y)<=bbox[1].dlon){
                    json.put("x",x);
                    json.put("y", y);
                    json.put("w", row.getValue());
                    points.add(json);
                }
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return points;
    }


    @Override
    public List<JsonNode> getRadioHeatmapBBox(String lat,String lon,String buid,String floor,int range) throws DatasourceException {
        List<JsonNode> points = new ArrayList<JsonNode>();
        HashMap<List<String>,Integer> point = new HashMap<List<String>,Integer>();
        List<List<String>> xy = new ArrayList<List<String>>();

        CouchbaseClient couchbaseClient = getConnection();
        SpatialView view = couchbaseClient.getSpatialView("radio", "radio_heatmap_bbox_byxy");
        Query query = new Query();
        query.setIncludeDocs(true);

        GeoPoint bbox[] = GeoPoint.getGeoBoundingBox(Double.parseDouble(lat), Double.parseDouble(lon), range); // 50 meters radius
        query.setBbox(bbox[0].dlat, bbox[0].dlon, bbox[1].dlat, bbox[1].dlon);
        System.out.println(bbox[0].dlat+"  "+ bbox[0].dlon+"  "+ bbox[1].dlat+"  "+bbox[1].dlon);

        ViewResponse res = couchbaseClient.query(view, query);

        System.out.println("couchbase results: " + res.size());

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());

                if (json.findValue("buid").toString().compareTo("\""+buid+"\"")==0 && json.findValue("floor").toString().compareTo("\""+floor+"\"")==0){

                    List<String> p = new ArrayList<String>();
                    String x = json.findValue("x").toString();
                    String y = json.findValue("y").toString();

                    p.add(x.substring(1, x.length() - 1));
                    p.add(y.substring(1, y.length() - 1));
                    if (point.containsKey(p)){
                        point.put(p,point.get(p)+1);
                    }
                    else {
                        point.put(p,1);
                        xy.add(p);
                    }
                }
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        while (!xy.isEmpty()){
            List<String> p = xy.remove(0);
            try {
                json = (ObjectNode) JsonUtils.getJsonTree("{}");
                String w = ""+ point.get(p);
                json.put("x", p.get(0));
                json.put("y", p.get(1));
                json.put("w", w);
                points.add(json);
            } catch (IOException e) {
                //skip this NOT-JSON document
            }
        }
        return points;
    }


    @Override
    public List<JsonNode> getRadioHeatmapBBox2(String lat,String lon,String buid,String floor,int range) throws DatasourceException {
        List<JsonNode> points = new ArrayList<JsonNode>();
        HashMap<List<String>,Integer> point = new HashMap<List<String>,Integer>();

        CouchbaseClient couchbaseClient = getConnection();
        SpatialView view = couchbaseClient.getSpatialView("radio", "radio_heatmap_bbox_byxy");
        Query query = new Query();
        query.setIncludeDocs(true);

        GeoPoint bbox[] = GeoPoint.getGeoBoundingBox(Double.parseDouble(lat), Double.parseDouble(lon), range); // 50 meters radius
        query.setBbox(bbox[0].dlat, bbox[0].dlon, bbox[1].dlat, bbox[1].dlon);

        System.out.println(bbox[0].dlat+"  "+ bbox[0].dlon+"  "+ bbox[1].dlat+"  "+bbox[1].dlon);

        ViewResponse res = couchbaseClient.query(view, query);

        System.out.println("couchbase results: " + res.size());

        ObjectNode json,json2;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                if (json.findValue("buid").toString().compareTo("\""+buid+"\"")==0 && json.findValue("floor").toString().compareTo("\""+floor+"\"")==0){
                    List<String> p = new ArrayList<String>();
                    String x = json.findValue("x").toString();
                    String y = json.findValue("y").toString();
                    p.add(x.substring(1, x.length() - 1));
                    p.add(y.substring(1, y.length() - 1));
                    if (point.containsKey(p)){
                        continue;
                    }
                    else {
                        json2 = (ObjectNode) JsonUtils.getJsonTree("{}");
                        json2.put("x", p.get(0));
                        json2.put("y", p.get(1));
                        points.add(json2);
                        point.put(p,1);
                    }
                }
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return points;
    }

    @Override
    public List<JsonNode> getAllBuildings() throws DatasourceException {
        List<JsonNode> buildings = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "building_all");
        Query query = new Query();
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("geometry");
                json.remove("owner_id");
                json.remove("co_owners");
//                json.remove("is_published");
//                json.remove("url");
//                json.remove("address");
                buildings.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return buildings;
    }

    public HashMap<String,List<JsonNode>> allPoisSide = new HashMap<String,List<JsonNode>>();

    public HashMap<String,List<JsonNode>> allPoisbycuid = new HashMap<String,List<JsonNode>>();

    @Override
    public List<JsonNode> getBuildingSet(String cuid2) throws DatasourceException {
        List<JsonNode> buildingSet = new ArrayList<JsonNode>();
        List<JsonNode> allPois = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "get_campus");
        Query query = new Query();
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());

                String cuid = json.toString();

                json.remove("owner_id");
                json.remove("description");
                if (cuid.contains(cuid2)){
                    json.remove("cuid");
                    buildingSet.add(json);
                    break;
                }
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        //allPoisSide.put(cuid2,);
        if (allPoisbycuid.get(cuid2)==null){
            System.out.println("LOAD CUID:"+cuid2);
            for (int i = 0; i < buildingSet.get(0).get("buids").size(); i++) {
                String buid = buildingSet.get(0).get("buids").get(i).toString().substring(1, buildingSet.get(0).get("buids").get(i).toString().length() - 1);
                if (allPoisSide.get(buid)!=null){
                    List<JsonNode> pois = allPoisSide.get(buid);
                    allPois.addAll(pois);
                }
                else{
                    List<JsonNode> pois = poisByBuildingAsJson(buid);
                    allPoisSide.put(buid, pois);
                    allPois.addAll(pois);
                }
            }
            allPoisbycuid.put(cuid2,allPois);
        }
        return buildingSet;
    }

    @Override
    public Boolean BuildingSetsCuids(String cuid2) throws DatasourceException {

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "get_campus");
        Query query = new Query();
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());

                String cuid = json.get("cuid").toString();
                cuid=cuid.substring(1, cuid.length() - 1);

                if (cuid.compareTo(cuid2)==0){
                    return true;
                }
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return false;
    }


    @Override
    public List<JsonNode> getAllBuildingsByOwner(String oid) throws DatasourceException {
        List<JsonNode> buildings = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "building_all_by_owner");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(oid);
        query.setKey(key);
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results BYowner: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("geometry");
                json.remove("owner_id");
                json.remove("co_owners");
//                json.remove("is_published");
//                json.remove("url");
//                json.remove("address");
                buildings.add(json);
            } catch (Exception e) {
                // skip this NOT-JSON document
            }
        }
        return buildings;
    }


    @Override
    public List<JsonNode> getAllBuildingsetsByOwner(String oid) throws DatasourceException {
        List<JsonNode> buildingsets = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "cuid_all_by_owner");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(oid);
        query.setKey(key);
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results campus: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                buildingsets.add(json);
            } catch (Exception e) {
                // skip this NOT-JSON document
            }
        }
        return buildingsets;
    }



    @Override
    public List<JsonNode> getAllPoisTypesByOwner(String oid) throws DatasourceException {
        List<JsonNode> poistypes = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "all_pois_types");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(oid);
        query.setKey(key);
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results pois types: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                poistypes.add(json);
            } catch (Exception e) {
                // skip this NOT-JSON document
            }
        }
        return poistypes;
    }


    @Override
    public List<JsonNode> getAllBuildingsByBucode(String bucode) throws DatasourceException {
        List<JsonNode> buildings = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "building_all_by_bucode");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(bucode);
        query.setKey(key);
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("geometry");
                json.remove("owner_id");
                json.remove("co_owners");
//                json.remove("is_published");
//                json.remove("url");
//                json.remove("address");
                buildings.add(json);
            } catch (Exception e) {
                // skip this NOT-JSON document
            }
        }
        return buildings;
    }

    @Override
    public List<JsonNode> getAllBuildingsNearMe(double lat, double lng) throws DatasourceException {
        List<JsonNode> buildings = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        SpatialView view = couchbaseClient.getSpatialView("nav", "building_coordinates");
        Query query = new Query();
        query.setIncludeDocs(true);

        GeoPoint bbox[] = GeoPoint.getGeoBoundingBox(lat, lng, 50); // 50 meters radius
        //query.setBbox(-180, -90, 180, 90);
        query.setBbox(bbox[0].dlat, bbox[0].dlon, bbox[1].dlat, bbox[1].dlon);

        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("geometry");
                json.remove("owner_id");
                json.remove("co_owners");
//                json.remove("is_published");
//                json.remove("url");
//                json.remove("address");
                buildings.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return buildings;
    }

    @Override
    public JsonNode getBuildingByAlias(String alias) throws DatasourceException {
        JsonNode jsn = null;

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "building_by_alias");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(alias);
        query.setKey(key);
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        if (!res.iterator().hasNext()) {
            return null;
        }
        try {
            jsn = (ObjectNode) JsonUtils.getJsonTree(res.iterator().next().getDocument().toString());
        } catch (IOException ioe) {

        }

        return jsn;
    }

    @Override
    public long dumpRssLogEntriesSpatial(FileOutputStream outFile, GeoPoint[] bbox, String floor_number) throws DatasourceException {
        PrintWriter writer = new PrintWriter(outFile);
        CouchbaseClient couchbaseClient = getConnection();
        SpatialView view;
        Query query;

        int queryLimit = 5000;
        int totalFetched = 0;
        int currentFetched;
        int floorFetched = 0;
        JsonNode rssEntry;

        view = couchbaseClient.getSpatialView("radio", "raw_radio");
        do {
            query = new Query();
            //query.setDescending(true);
            query.setLimit(queryLimit);
            query.setSkip(totalFetched);
            //query.setStale(Stale.FALSE);
            query.setBbox(bbox[0].dlat, bbox[0].dlon, bbox[1].dlat, bbox[1].dlon);

            ViewResponse res = couchbaseClient.query(view, query);
            currentFetched = 0;
            for (ViewRow row : res) {
                // handle each raw radio entry
                currentFetched++;
                try {
                    rssEntry = JsonUtils.getJsonTree(row.getValue());
                } catch (IOException e) {
                    // skip documents not in Json-format
                    continue;
                }
                if (!rssEntry.path("floor").textValue().equals(floor_number)) {
                    // skip rss logs of another floor
                    continue;
                }
                floorFetched++;
                // write this entry into the file
                writer.println(RadioMapRaw.toRawRadioMapRecord(rssEntry));
            } // end while paginator
            totalFetched += currentFetched;

            LPLogger.info("total fetched: " + totalFetched);
        } while (currentFetched >= queryLimit && floorFetched < 100000);

        // flush and close the files
        writer.flush();
        writer.close();
        return floorFetched;
    }

    @Override
    public long dumpRssLogEntriesByBuildingFloor(FileOutputStream outFile, String buid, String floor_number) throws DatasourceException {
        PrintWriter writer = new PrintWriter(outFile);
        CouchbaseClient couchbaseClient = getConnection();
        View view;
        Query query;
// multi key
        // couch view
        int queryLimit = 10000;
        int totalFetched = 0;
        int currentFetched;
        JsonNode rssEntry;

        ComplexKey key = ComplexKey.of(buid, floor_number);

        view = couchbaseClient.getView("radio", "raw_radio_building_floor");
        do {
            query = new Query();
            query.setKey(key);
            query.setIncludeDocs(true);
            //query.setDescending(true);
            query.setLimit(queryLimit);
            query.setSkip(totalFetched);
            //query.setStale(Stale.FALSE);

            ViewResponse res = couchbaseClient.query(view, query);

            if (res == null)
                return totalFetched;

            currentFetched = 0;
            for (ViewRow row : res) {
                // handle each raw radio entry
                currentFetched++;
                try {
                    rssEntry = JsonUtils.getJsonTree(row.getDocument().toString());
                } catch (IOException e) {
                    // skip documents not in Json-format
                    continue;
                }
                // write this entry into the file
                writer.println(RadioMapRaw.toRawRadioMapRecord(rssEntry));
            } // end while paginator
            totalFetched += currentFetched;

            LPLogger.info("total fetched: " + totalFetched);

            // basically, ==
        } while (currentFetched >= queryLimit);

        // flush and close the files
        writer.flush();
        writer.close();
        return totalFetched;
    }

    @Override
    public long dumpRssLogEntriesByBuildingFloorBbox(FileOutputStream outFile, String buid, String floor_number,String range, String lat,String lon) throws DatasourceException {
        PrintWriter writer = new PrintWriter(outFile);
        CouchbaseClient couchbaseClient = getConnection();
        View view;
        Query query;
// multi key
        // couch view
        int queryLimit = 10000;
        int totalFetched = 0;
        int currentFetched;
        JsonNode rssEntry;

        ComplexKey key = ComplexKey.of(buid, floor_number);

        GeoPoint bbox[] = GeoPoint.getGeoBoundingBox(Double.parseDouble(lat), Double.parseDouble(lon), Integer.parseInt(range)); // 50 meters radius


        view = couchbaseClient.getView("radio", "raw_radio_building_floor");
        do {
            query = new Query();
            query.setKey(key);
            query.setIncludeDocs(true);
            //query.setDescending(true);
            query.setLimit(queryLimit);
            query.setSkip(totalFetched);
            //query.setStale(Stale.FALSE);

            ViewResponse res = couchbaseClient.query(view, query);

            if (res == null)
                return totalFetched;

            currentFetched = 0;
            for (ViewRow row : res) {
                boolean flag = true;
                // handle each raw radio entry
                currentFetched++;
                try {
                    rssEntry = JsonUtils.getJsonTree(row.getDocument().toString());
                    String x = rssEntry.get("x").toString();
                    x=x.substring(1, x.length() - 1);
                    String y = rssEntry.get("y").toString();
                    y=y.substring(1, y.length() - 1);
                    if (!(Double.parseDouble(x)>=bbox[0].dlat&&Double.parseDouble(x)<=bbox[1].dlat
                            &&Double.parseDouble(y)>=bbox[0].dlon&&Double.parseDouble(y)<=bbox[1].dlon)){
                        flag = false;
                    }
                } catch (IOException e) {
                    // skip documents not in Json-format
                    continue;
                }

                // write this entry into the file
                if (flag){
                    writer.println(RadioMapRaw.toRawRadioMapRecord(rssEntry));
                }
            } // end while paginator
            totalFetched += currentFetched;

            LPLogger.info("total fetched: " + totalFetched);

            // basically, ==
        } while (currentFetched >= queryLimit);

        // flush and close the files
        writer.flush();
        writer.close();
        return totalFetched;
    }

    /**
     * Can be helpful in case we want to iterate through all the rss logs again

     private static double getD(double x1, double y1, double x2, double y2) {
     return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
     }

     @Override
     public long addBuidToRssLogs() throws DatasourceException {
     List<JsonNode> buildings = null;
     try {
     buildings = ProxyDataSource.getIDatasource().getAllBuildings();
     } catch (DatasourceException e) {
     return -1;
     }

     List<JsonNode> maxFloors = new ArrayList<JsonNode>();
     List<Double> maxFloorsVal = new ArrayList<Double>();
     List<Double> maxFloorsCentreX = new ArrayList<Double>();
     List<Double> maxFloorsCentreY = new ArrayList<Double>();

     for (JsonNode jn : buildings) {
     String buid = jn.get("buid").textValue();

     if (buid == null)
     continue;

     List<JsonNode> floors = null;

     try {
     floors = ProxyDataSource.getIDatasource().floorsByBuildingAsJson(buid);
     } catch (DatasourceException e) {
     LPLogger.error("Err: can't get floors for buid: " + buid);
     continue;
     }

     if (floors == null || floors.size() <= 0)
     continue;

     JsonNode maxFloor = null;
     double maxD = Double.MIN_VALUE;

     for (JsonNode fjn : floors) {
     double top_x = fjn.get("top_right_lat").asDouble();
     double top_y = fjn.get("top_right_lng").asDouble();
     double bot_x = fjn.get("bottom_left_lat").asDouble();
     double bot_y = fjn.get("bottom_left_lng").asDouble();

     double d = getD(top_x, top_y, bot_x, bot_y);

     if (d >= maxD) {
     maxD = d;
     maxFloor = fjn;
     }
     }

     maxFloors.add(maxFloor);
     maxFloorsVal.add(maxD);
     maxFloorsCentreX.add((maxFloor.get("top_right_lat").asDouble() + maxFloor.get("bottom_left_lat").asDouble()) / 2);
     maxFloorsCentreY.add((maxFloor.get("top_right_lng").asDouble() + maxFloor.get("bottom_left_lng").asDouble()) / 2);
     }


     CouchbaseClient couchbaseClient = getConnection();
     View view;
     Query query;
     // multi key
     // couch view
     int queryLimit = 10000;
     int totalFetched = 0;
     int currentFetched;
     JsonNode rssEntry;

     view = couchbaseClient.getView("radio", "radio_without_buid");
     do {
     query = new Query();
     query.setIncludeDocs(true);
     query.setLimit(queryLimit);
     query.setSkip(totalFetched);

     ViewResponse res = couchbaseClient.query(view, query);

     if (res == null)
     return totalFetched;

     String belongsTo = null;
     double prev_x = 0;
     double prev_y = 0;

     currentFetched = 0;
     for (ViewRow row : res) {
     // handle each raw radio entry
     currentFetched++;

     try {
     rssEntry = JsonUtils.getJsonTree(row.getDocument().toString());
     } catch (IOException e) {
     // skip documents not in Json-format
     continue;
     }

     double p_x = rssEntry.get("x").asDouble();
     double p_y = rssEntry.get("y").asDouble();

     if (true) {
     ((ObjectNode) rssEntry).put("buid", "username_1373876832005");
     replaceJsonDocument(row.getId(), 0, rssEntry.toString());
     } else if (p_x == prev_x && p_y == prev_y && belongsTo != null) {
     ((ObjectNode) rssEntry).put("buid", belongsTo);
     replaceJsonDocument(row.getId(), 0, rssEntry.toString());
     } else {

     prev_x = p_x;
     prev_y = p_y;

     List<Double> candidateFloorsVal = new ArrayList<Double>();
     List<JsonNode> candidateFloors = new ArrayList<JsonNode>();

     for (int fl = 0; fl < maxFloors.size(); fl++) {
     JsonNode maxFloor = maxFloors.get(fl);
     double maxD = maxFloorsVal.get(fl);

     double c_x = maxFloorsCentreX.get(fl);
     double c_y = maxFloorsCentreY.get(fl);

     double p_d = getD(c_x, c_y, p_x, p_y);

     if (p_d <= maxD / 2) {
     candidateFloorsVal.add(p_d);
     candidateFloors.add(maxFloor);
     }
     }

     if (candidateFloors.size() > 0) {
     double minV = Double.MAX_VALUE;
     int minI = -1;
     for (int i = 0; i < candidateFloorsVal.size(); i++) {
     if (candidateFloorsVal.get(i) <= minV) {
     minV = candidateFloorsVal.get(i);
     minI = i;
     }
     }

     belongsTo = candidateFloors.get(minI).get("buid").textValue();

     ((ObjectNode) rssEntry).put("buid", belongsTo);
     replaceJsonDocument(row.getId(), 0, rssEntry.toString());
     } else {
     belongsTo = null;
     }
     }

     } // end while paginator
     totalFetched += currentFetched;

     LPLogger.info("total fetched: " + totalFetched);

     // basically, ==
     } while (currentFetched >= queryLimit);

     return totalFetched;
     }

     */

    /**
     * ***************************************************************
     * ACCOUNTS METHODS
     */

    @Override
    public List<JsonNode> getAllAccounts() throws DatasourceException {
        List<JsonNode> accounts = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("accounts", "accounts_all");
        Query query = new Query();
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        if (res.getErrors().size() > 0) {
            throw new DatasourceException("Error retrieving accounts from database!");
        }

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                // try to avoid documents that are fetched without documents
                if (row.getDocument() == null)
                    continue;
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("doctype");
                accounts.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return accounts;
    }


    /**
     * ***************************************************************
     * OAUTH2 - IAccountService Implementation
     */

    @Override
    public AccountModel validateClient(String clientId, String clientSecret, String grantType) {
        CouchbaseClient couchbaseClient;
        try {
            couchbaseClient = getConnection();
        } catch (DatasourceException e) {
            LPLogger.error("CouchbaseDatasource::validateClient():: Exception while getting connection in DB");
            return null;
        }

        View view = couchbaseClient.getView("accounts", "accounts_by_client_id");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(clientId);
        ViewResponse res = couchbaseClient.query(view, query);
        JsonNode accountJson = null;
        JsonNode clients;
        boolean found = false;
        for (ViewRow row : res) {
            try {
                accountJson = JsonUtils.getJsonTree(row.getDocument().toString());
            } catch (IOException e) {
                return null;
            }
            clients = accountJson.path("clients");
            for (final JsonNode client : clients) {
                if (client.path("client_id").textValue().equals(clientId)
                        && client.path("client_secret").textValue().equals(clientSecret)
                        && client.path("grant_type").textValue().equals(grantType)) {
                    found = true;
                    break;
                }
            }
        }

        if (found) {
            /*
            AccountModel account = new AccountModel();
            account.setAuid(accountJson.path("auid").getTextValue());
            account.setPassword(accountJson.path("password").getTextValue());
            account.setUsername(accountJson.path("username").getTextValue());
            account.setScope(accountJson.path("scope").getTextValue());
            for( final JsonNode client : accountJson.path("clients") ){
                account.addClient(new AccountModel.ClientModel(
                        client.path("client_id").getTextValue(),
                        client.path("client_secret").getTextValue(),
                        client.path("grant_type").getTextValue(),
                        client.path("scope").getTextValue(),
                        "" // redirect_uri
                ));
            }
            account.setNickname(accountJson.path("nickname").getTextValue());
            account.setEmail(accountJson.path("email").getTextValue());
            */
            AccountModel account = new AccountModel(accountJson);
            return account;
        }
        /*
        AccountModel account = new AccountModel();
        account.setPassword("test_password");
        account.setUsername("test_username");
        account.addClient(new AccountModel.ClientModel("test_client_id","test_client_secret","password", "test_scope", ""));
        account.setAuid("test_auid");
        return account;
        */
        return null;
    }

    @Override
    public boolean validateAccount(AccountModel account, String username, String password) {
        if (account == null)
            return false;
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        // TODO- here maybe we should add Base64 decoding for the credentials
        // TODO- or even maybe salting them
        if (!username.equals(account.getUsername())) {
            return false;
        }
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        // TODO- here maybe we should add Base64 decoding for the credentials
        // TODO- or even maybe salting them
        if (!password.equals(account.getPassword())) {
            return false;
        }
        return true;
    }

    @Override
    public AuthInfo createOrUpdateAuthInfo(AccountModel account, String clientId, String scope) {
        // validate the scopes
        if (!account.validateScope(scope, clientId)) {
            return null;
        }
        // TODO - maybe the database here first in order to check
        // TODO - if there is already a refresh token available
        // TODO - IMPORTANT - no refresh token is issued at the moment
        return new AuthInfo(account.getAuid(), clientId, scope);
    }

    @Override
    public AccessTokenModel createOrUpdateAccessToken(AuthInfo authInfo) {
        CouchbaseClient client;
        try {
            client = getConnection();
        } catch (DatasourceException e) {
            LPLogger.error("CouchbaseDatasource::createOrUpdateAccessToken():: Exception while opening connection to store new token in DB");
            return null;
        }
        do {
            AccessTokenModel tokenModel = TokenService.createNewAccessToken(authInfo);
            OperationFuture<Boolean> db_res = client.add(tokenModel.getTuid(), (int) tokenModel.getExpiresIn(), tokenModel.toJson().toString(), PersistTo.ONE);
            try {
                if (!db_res.get()) {
                    continue;
                } else {
                    return tokenModel;
                }
            } catch (Exception e) {
                LPLogger.error("CouchbaseDatasource::createOrUpdateAccessToken():: Exception while storing new token in DB");
                return null;
            }
        } while (true);
    }

    @Override
    public boolean deleteRadiosInBox() throws DatasourceException {

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("radio", "tempview");
        Query query = new Query();
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);

        for (ViewRow row : res) {
            deleteFromKey(row.getKey());
        }

        return true;
    }

    @Override
    public AuthInfo getAuthInfoByRefreshToken(String refreshToken) {
        // TODO - not used yet since we do not issue refresh tokens
        return null;
    }

    @Override
    public boolean predictFloor(IAlgo algo, GeoPoint[] bbox, String[] strongestMAC)
            throws DatasourceException {
        //predictFloorSlow(algo, bbox);
        return predictFloorFast(algo, bbox, strongestMAC);
    }

    private boolean predictFloorFast(IAlgo algo, GeoPoint[] bbox,
                                     String[] strongestMACs) throws DatasourceException {

        String designDoc = "floor";
        String viewName = "group_wifi";
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView(designDoc, viewName);
        int totalFetched = 0;

        for (String strongestMAC : strongestMACs) {
            Query query = new Query();
            ComplexKey startkey = ComplexKey.of(strongestMAC, bbox[0].dlat,
                    bbox[0].dlon, null);
            ComplexKey endkey = ComplexKey.of(strongestMAC, bbox[1].dlat,
                    bbox[1].dlon, "\u0fff");

            query.setRange(startkey, endkey);
            ViewResponse response = couchbaseClient.query(view, query);

            String _timestamp = "";
            String _floor = "0";
            ArrayList<JsonNode> bucket = new ArrayList<JsonNode>(10);
            for (ViewRow row : response) {
                try {
                    String timestamp = row.getKey();
                    JsonNode value = JsonUtils.getJsonTree(row.getValue());

                    if (!_timestamp.equals(timestamp)) {
                        if (!_timestamp.equals("")) {
                            algo.proccess(bucket, _floor);
                        }

                        bucket.clear();
                        _timestamp = timestamp;
                        _floor = value.get("floor").textValue();
                    }

                    bucket.add(value);
                    totalFetched++;
                } catch (IOException e) {
                    // skip documents not in Json-format
                    continue;
                }

            }
        }

        LPLogger.info("total fetched: " + totalFetched);
        if (totalFetched > 10) {
            return true;
        } else {
            return false;
        }

    }

    /*
        MAGNETIC
     */

    @Override
    public List<JsonNode> magneticPathsByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("magnetic", "mpaths_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<JsonNode> magneticPathsByBuildingAsJson(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("magnetic", "mpaths_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<JsonNode> magneticMilestonesByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("magnetic", "mmilestones_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

}
