package datasources;

import com.fasterxml.jackson.databind.*;

import floor_module.IAlgo;
import utils.GeoPoint;

import javax.xml.crypto.Data;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;

/**
 * The main interface that every Database implementation client should implement
 * <p>
 * This is the object being used in all the APIs
 */
public interface IDatasource {

    /**
     * The implementation should make all the initializations
     * either in the Constructor or this method.
     *
     * @return True if successful otherwise False
     * @throws DatasourceException
     */
    boolean init() throws DatasourceException;

    /**
     * Inserts the passed document into the database with the first parameter
     * being the retrieval key and the second being the expire time in seconds.
     *
     * @param key      The key for the stored document
     * @param expiry   Expiration time in seconds
     * @param document The document to be stored in Json format
     * @return True if the insertion succeeded otherwise false
     * @throws DatasourceException Thrown when a connection error occurs
     */
    boolean addJsonDocument(String key, int expiry, String document) throws DatasourceException;

    /**
     * Replaces the passed document into the database with the first parameter
     * being the retrieval key and the second being the expire time in seconds.
     *
     * @param key      The key for the stored document
     * @param expiry   Expiration time in seconds
     * @param document The document to be replaced with in Json format
     * @return True if the replace succeeded otherwise false
     * @throws DatasourceException Thrown when a connection error occurs
     */
    boolean replaceJsonDocument(String key, int expiry, String document) throws DatasourceException;

    /**
     * According to the key this method should return the object that corresponds
     * inside the database. Object is chosen to allow generality.
     *
     * @param key The key of the document to be deleted
     * @return The object that is stored based on @param key or NULL if not found
     * @throws DatasourceException In case there is an error while reading the database
     */
    public boolean deleteFromKey(String key) throws DatasourceException;

    /**
     * According to the key this method should return the object that corresponds
     * inside the database. Object is chosen to allow generality.
     *
     * @param key The key of the document required
     * @return The object that is stored based on @param key or NULL if not found
     * @throws DatasourceException In case there is an error while reading the database
     */
    Object getFromKey(String key) throws DatasourceException;

    /**
     * According to the key this method should return a jackson.JsonNode object
     * that corresponds to a document inside the database.
     *
     * @param key The key of the document required
     * @return The object that is stored based on @param key in JsonNode format or NULL if not found
     * @throws DatasourceException In case there is an error while reading the database
     */
    JsonNode getFromKeyAsJson(String key) throws DatasourceException;


    /**
     * According to the key this method should return a jackson.JsonNode object
     * that corresponds to the building that has key the parameter.
     * The returned information should contain all floors and Pois of the building.
     *
     * @param key The key of the building required
     * @return The object that is stored based on @param key in JsonNode format or NULL if not found
     * @throws DatasourceException In case there is an error while reading the database
     */
    JsonNode buildingFromKeyAsJson(String key) throws DatasourceException;

    /**
     * According to the key this method should return a jackson.JsonNode object
     * that corresponds to the POI that has key the parameter.
     *
     * @param key The key of the POI required
     * @return The object that is stored based on @param key in JsonNode format or NULL if not found
     * @throws DatasourceException In case there is an error while reading the database
     */
    JsonNode poiFromKeyAsJson(String key) throws DatasourceException;

    /**
     * Will Attempt to get all pois that dont have the url attribute that is needed for the android
     * app to work
     *
     * @return
     * @throws DatasourceException
     */
    List<JsonNode> tempAllPoisWithoutUrl() throws DatasourceException;

    /**
     * A list of all the POIs in the building floor specified will be returned.
     * Each POI is a JsonNode object.
     *
     * @param buid         The building Id
     * @param floor_number The floor number
     * @return List<JsonNode> with all the POIs of the specified floor, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<JsonNode> poisByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException;


    /**
     * A list of all the POIs in the building floor specified will be returned.
     * Each POI is a JsonNode object.
     *
     * @param buid         The building Id
     * @return List<JsonNode> with all the POIs of the specified floor, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<JsonNode> poisByBuildingIDAsJson(String buid) throws DatasourceException;


    /**
     * A list of all the POIs in the building floor specified will be returned.
     * Each POI is a HashMap<String, String> object.
     *
     * @param buid         The building Id
     * @param floor_number The floor number
     * @return List<HashMap<String, String>> with all the POIs of the specified floor, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<HashMap<String, String>> poisByBuildingFloorAsMap(String buid, String floor_number) throws DatasourceException;

    /**
     * A list of all the POIs in the building specified will be returned.
     * Each POI is a JsonNode object.
     *
     * @param buid The building Id
     * @return List<JsonNode> with all the POIs, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<JsonNode> poisByBuildingAsJson(String buid) throws DatasourceException;
    /**
     * A list of all the POIs in the building specified will be returned.
     * Each POI is a JsonNode object.
     *
     * @param buid The building Id
     * @return List<JsonNode> with all the POIs, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<JsonNode> poisByBuildingAsJson2(String buid,String letters) throws DatasourceException;

    /**
     * A list of all the POIs in the building specified will be returned.
     * Each POI is a JsonNode object.
     *
     * @param buid The building Id
     * @return List<JsonNode> with all the POIs, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<JsonNode> poisByBuildingAsJson3(String buid,String letters) throws DatasourceException;

    /**
     * A list of all the POIs in the building specified will be returned.
     * Each POI is a JsonNode object.
     *
     * @param cuid The building Id
     * @return List<JsonNode> with all the POIs, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<JsonNode> poisByBuildingAsJson2GR(String cuid,String letters) throws DatasourceException;



    /**
     * A list of all the POIs in the building specified will be returned.
     * Each POI is a HashMap<String, String> object.
     *
     * @param buid The building Id
     * @return List<HashMap<String, String>> with all the POIs, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<HashMap<String, String>> poisByBuildingAsMap(String buid) throws DatasourceException;

    /**
     * Returns all the floors of a building stored in the database in a list
     *
     * @param buid The building of which we want the floors
     * @return A list of JsonNodes representing all the stored floors  or empty list
     * @throws DatasourceException When there is a connection error
     */
    List<JsonNode> floorsByBuildingAsJson(String buid) throws DatasourceException;


    /**
     * A list of all the connections in the building specified will be returned.
     * Each Connection is a JsonNode object.
     *
     * @param buid The building Id
     * @return List<JsonNode> with all the connections, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<JsonNode> connectionsByBuildingAsJson(String buid) throws DatasourceException;

    /**
     * A list of all the Connections in the building specified will be returned.
     * Each Connection is a HashMap<String, String> object.
     *
     * @param buid The building Id
     * @return List<HashMap<String, String>> with all the connections, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<HashMap<String, String>> connectionsByBuildingAsMap(String buid) throws DatasourceException;

    /**
     * A list of all the connections in the building specified will be returned.
     * Each Connection is a JsonNode object.
     *
     * @param buid         The building Id
     * @param floor_number The floor number
     * @return List<JsonNode> with all the connections, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<JsonNode> connectionsByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException;

    /**
     * A list of all the connections in the building specified will be returned.
     * Each Connection is a JsonNode object.
     *
     * @param buid         The building Id
     * @return List<JsonNode> with all the connections, or an empty list
     * @throws DatasourceException Thrown when error occurs reading from the database
     */
    List<JsonNode> connectionsByBuildingAllFloorsAsJson(String buid) throws DatasourceException;


    /**
     * Tries to delete everything stored for a specific building
     * inside the database. (Building, POI, Floors, Connections)
     *
     * @param buid The building we want to delete
     * @return A list with the keys/ids of the items failed to delete
     * @throws DatasourceException Thrown when there is an exception error
     */
    List<String> deleteAllByBuilding(String buid) throws DatasourceException;

    /**
     * Tries to delete everything stored for a specific floor
     * inside the database. (POI, Floor, Connections)
     *
     * @param buid         The building of the floor
     * @param floor_number The floor we want to delete
     * @return A list with the keys/ids of the items failed to delete
     * @throws DatasourceException Thrown when there is an exception error
     */
    List<String> deleteAllByFloor(String buid, String floor_number) throws DatasourceException;

    /**
     * Tries to delete everything stored for a specific connection
     * inside the database. (Connections)
     *
     * @param cuid The connection id
     * @return A list with the keys/ids of the items failed to delete
     * @throws DatasourceException Thrown when there is an exception error
     */
    List<String> deleteAllByConnection(String cuid) throws DatasourceException;

    /**
     * Tries to delete everything stored for a specific poi
     * inside the database. (POI, Connections)
     *
     * @param puid The building of the floor
     * @return A list with the keys/ids of the items failed to delete
     * @throws DatasourceException Thrown when there is an exception error
     */
    List<String> deleteAllByPoi(String puid,String buid) throws DatasourceException;

    List<JsonNode> getRadioHeatmap() throws DatasourceException;

    List<JsonNode> getRadioHeatmapByBuildingFloor(String buid, String floor) throws DatasourceException;

    List<JsonNode> getRadioHeatmapByBuildingFloor2(String lat,String lon,String buid,String floor,int range) throws DatasourceException;

    List<JsonNode> getRadioHeatmapBBox(String lat,String lon,String buid,String floor,int range) throws DatasourceException;

    List<JsonNode> getRadioHeatmapBBox2(String lat,String lon,String buid,String floor,int range) throws DatasourceException;

    /**
     * Returns all the buildings stored in the database in a list
     *
     * @return A list of JsonNodes representing all the stored buildings
     * @throws DatasourceException When there is a connection error
     */
    List<JsonNode> getAllBuildings() throws DatasourceException;

    /**
     * Returns the building set json
     *
     * @return A JsonNode representing the correct cuid
     * @throws DatasourceException When there is a connection error
     */
    List<JsonNode> getBuildingSet(String cuid) throws DatasourceException;

    /**
     * Returns buildings sets ids
     *
     * @return A JsonNode representing the correct cuid
     * @throws DatasourceException When there is a connection error
     */
    Boolean BuildingSetsCuids(String cuid) throws DatasourceException;

    /**
     * @return A list of JsonNodes representing all the stored buildings of the user
     * @throws DatasourceException When there is a connection error
     * @author KG
     * Returns all the buildings that belong to a user, stored in the database in a list
     */
    List<JsonNode> getAllBuildingsByOwner(String oid) throws DatasourceException;

    List<JsonNode> getAllBuildingsetsByOwner(String oid) throws DatasourceException;

    List<JsonNode> getAllPoisTypesByOwner(String oid) throws DatasourceException;

    List<JsonNode> getAllBuildingsByBucode(String bucode) throws DatasourceException;

    /**
     * @param alias
     * @return
     * @throws DatasourceException
     * @author KG
     */
    public JsonNode getBuildingByAlias(String alias) throws DatasourceException;

    /**
     * Returns all the buildings stored in the database in a list
     *
     * @return A list of JsonNodes representing all the stored buildings
     * @throws DatasourceException When there is a connection error
     */
    List<JsonNode> getAllBuildingsNearMe(double lat, double lng) throws DatasourceException;

    /**
     * Using the bounding box passed in as parameter, this function will
     * write all the rss log entries in the db that are inside the bounded area
     * into the outFile.
     *
     * @param outFile      The file path of the output file that will contain the rss entries
     * @param bbox         The spatial bounding box for the query
     * @param floor_number The floor number of the user trying to download radio map
     * @return Number of valid entries written into the file
     * @throws DatasourceException When there is a connection error
     */
    long dumpRssLogEntriesSpatial(FileOutputStream outFile, GeoPoint bbox[], String floor_number) throws DatasourceException;

    /**
     * Using the floor_num and buid, this function will
     * write all the rss log entries in the db that are from that floor in that building
     *
     * @param outFile      The file path of the output file that will contain the rss entries
     * @param buid         building id
     * @param floor_number The floor number of the user trying to download radio map
     * @return Number of valid entries written into the file
     * @throws DatasourceException When there is a connection error
     */
    long dumpRssLogEntriesByBuildingFloor(FileOutputStream outFile, String buid, String floor_number) throws DatasourceException;

    /**
     * Using the floor_num and buid, this function will
     * write all the rss log entries in the db that are from that floor in that building
     *
     * @param outFile      The file path of the output file that will contain the rss entries
     * @param buid         building id
     * @param floor_number The floor number of the user trying to download radio map
     * @return Number of valid entries written into the file
     * @throws DatasourceException When there is a connection error
     */
    long dumpRssLogEntriesByBuildingFloorBbox(FileOutputStream outFile, String buid, String floor_number,String range, String lat,String lon) throws DatasourceException;


    /*****
     * ACCOUNTS METHODS
     */

    /**
     * Returns all the accounts registered with our service in
     * a list of JsonNodes.
     *
     * @return List of JsonNodes that represent the accounts
     * @throws DatasourceException
     */
    List<JsonNode> getAllAccounts() throws DatasourceException;

    boolean predictFloor(IAlgo algo, GeoPoint[] bbox, String[] strongestMACs) throws DatasourceException;

    boolean deleteRadiosInBox() throws DatasourceException;

    List<JsonNode> magneticPathsByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException;
    List<JsonNode> magneticPathsByBuildingAsJson(String buid) throws DatasourceException;
    List<JsonNode> magneticMilestonesByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException;

}
