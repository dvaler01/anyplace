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

import com.fasterxml.jackson.databind.JsonNode;
import floor_module.IAlgo;
import utils.GeoPoint;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Our proxy service that hides the actual database back-end implementation
 * we use in order to allow easy transition of back-ends and for testing.
 */
public class ProxyDataSource implements IDatasource {

    private static ProxyDataSource sInstance;

    public static ProxyDataSource getInstance() {
        if (sInstance == null) {
            sInstance = new ProxyDataSource();
        }
        return sInstance;
    }

    public static IDatasource getIDatasource() {
        return getInstance();
    }

    /////////////////////////////////////////////////////////////////////

    private CouchbaseDatasource mCouchbase;
    private IDatasource mActiveDatabase;

    // we set the constructor private to disallow object creation
    private ProxyDataSource() {
        initCouchbase();
        setActiveDatabase(this.mCouchbase);
    }

    private void initCouchbase() {
        /*
        String hostname = Play.application().configuration().getString("couchbase.hostname");
        String port = Play.application().configuration().getString("couchbase.port");
        String bucket = Play.application().configuration().getString("couchbase.bucket");
        String password = Play.application().configuration().getString("couchbase.password");
        this.mCouchbase = CouchbaseDatasource.createNewInstance(hostname,port,bucket,password);
        try {
            this.mCouchbase.init();
        } catch (DatasourceException e) {
            LPLogger.error("ProxyDataSource::initCouchbase():: Exception while instantiating Couchbase [" + e.getMessage() + "]");
        }
        */
        this.mCouchbase = CouchbaseDatasource.getStaticInstance();
    }

    private void setActiveDatabase(IDatasource ds) {
        this.mActiveDatabase = ds;
    }

    //////////////////////////////////////////////////////////////////////
    //  IDatasource Methods implementation
    //////////////////////////////////////////////////////////////////////

    @Override
    public boolean init() throws DatasourceException {
        return true;
    }

    @Override
    public boolean addJsonDocument(String key, int expiry, String document) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.addJsonDocument(key, expiry, document);
    }

    @Override
    public boolean replaceJsonDocument(String key, int expiry, String document) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.replaceJsonDocument(key, expiry, document);
    }

    @Override
    public boolean deleteFromKey(String key) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.deleteFromKey(key);
    }

    @Override
    public Object getFromKey(String key) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getFromKey(key);
    }

    @Override
    public boolean deleteRadiosInBox() throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.deleteRadiosInBox();
    }


    @Override
    public JsonNode getFromKeyAsJson(String key) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getFromKeyAsJson(key);
    }

    @Override
    public JsonNode buildingFromKeyAsJson(String key) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.buildingFromKeyAsJson(key);
    }

    @Override
    public JsonNode poiFromKeyAsJson(String key) throws DatasourceException {
        return getFromKeyAsJson(key);
    }

    @Override
    public List<JsonNode> tempAllPoisWithoutUrl() throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.tempAllPoisWithoutUrl();
    }

    @Override
    public List<JsonNode> poisByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.poisByBuildingFloorAsJson(buid, floor_number);
    }

    @Override
    public List<JsonNode> poisByBuildingIDAsJson(String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.poisByBuildingIDAsJson(buid);
    }

    @Override
    public List<HashMap<String, String>> poisByBuildingFloorAsMap(String buid, String floor_number) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.poisByBuildingFloorAsMap(buid, floor_number);
    }

    @Override
    public List<JsonNode> poisByBuildingAsJson(String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.poisByBuildingAsJson(buid);
    }

    @Override
    public List<JsonNode> poisByBuildingAsJson2(String buid,String letters) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.poisByBuildingAsJson2(buid,letters);
    }

    @Override
    public List<JsonNode> poisByBuildingAsJson2GR(String buid,String letters) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.poisByBuildingAsJson2GR(buid,letters);
    }

    @Override
    public List<JsonNode> poisByBuildingAsJson3(String buid,String letters) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.poisByBuildingAsJson3(buid,letters);
    }

    @Override
    public List<HashMap<String, String>> poisByBuildingAsMap(String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.poisByBuildingAsMap(buid);
    }

    @Override
    public List<JsonNode> floorsByBuildingAsJson(String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.floorsByBuildingAsJson(buid);
    }

    @Override
    public List<JsonNode> connectionsByBuildingAsJson(String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.connectionsByBuildingAsJson(buid);
    }

    @Override
    public List<HashMap<String, String>> connectionsByBuildingAsMap(String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.connectionsByBuildingAsMap(buid);
    }

    @Override
    public List<JsonNode> connectionsByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.connectionsByBuildingFloorAsJson(buid, floor_number);
    }

    @Override
    public List<JsonNode> connectionsByBuildingAllFloorsAsJson(String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.connectionsByBuildingAllFloorsAsJson(buid);
    }

    @Override
    public List<String> deleteAllByBuilding(String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.deleteAllByBuilding(buid);
    }

    @Override
    public List<String> deleteAllByFloor(String buid, String floor_number) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.deleteAllByFloor(buid, floor_number);
    }

    @Override
    public List<String> deleteAllByConnection(String cuid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.deleteAllByConnection(cuid);
    }

    @Override
    public List<String> deleteAllByPoi(String puid,String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.deleteAllByPoi(puid,buid);
    }

    @Override
    public List<JsonNode> getRadioHeatmap() throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getRadioHeatmap();
    }

    @Override
    public List<JsonNode> getRadioHeatmapByBuildingFloor(String buid, String floor) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getRadioHeatmapByBuildingFloor(buid, floor);
    }

    @Override
    public List<JsonNode> getRadioHeatmapByBuildingFloor2(String lat,String lon,String buid,String floor,int range) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getRadioHeatmapByBuildingFloor2(lat, lon, buid, floor, range);
    }


    @Override
    public List<JsonNode> getRadioHeatmapBBox(String lat,String lon,String buid,String floor,int range) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getRadioHeatmapBBox(lat,lon,buid,floor,range);
    }

    @Override
    public List<JsonNode> getRadioHeatmapBBox2(String lat,String lon,String buid,String floor,int range) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getRadioHeatmapBBox2(lat,lon,buid,floor,range);
    }

    @Override
    public List<JsonNode> getBuildingSet(String cuid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getBuildingSet(cuid);
    }

    @Override
    public Boolean BuildingSetsCuids(String cuid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.BuildingSetsCuids(cuid);
    }

    @Override
    public List<JsonNode> getAllBuildings() throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getAllBuildings();
    }

    @Override
    public List<JsonNode> getAllBuildingsByOwner(String oid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getAllBuildingsByOwner(oid);
    }

    @Override
    public List<JsonNode> getAllBuildingsetsByOwner(String oid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getAllBuildingsetsByOwner(oid);
    }

    @Override
    public List<JsonNode> getAllPoisTypesByOwner(String oid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getAllPoisTypesByOwner(oid);
    }

    @Override
    public List<JsonNode> getAllBuildingsByBucode(String bucode) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getAllBuildingsByBucode(bucode);
    }


    @Override
    public JsonNode getBuildingByAlias(String alias) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getBuildingByAlias(alias);
    }

    @Override
    public List<JsonNode> getAllBuildingsNearMe(double lat, double lng) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getAllBuildingsNearMe(lat, lng);
    }

    @Override
    public long dumpRssLogEntriesSpatial(FileOutputStream outFile, GeoPoint[] bbox, String floor_number) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.dumpRssLogEntriesSpatial(outFile, bbox, floor_number);
    }

    @Override
    public long dumpRssLogEntriesByBuildingFloor(FileOutputStream outFile, String buid, String floor_number) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.dumpRssLogEntriesByBuildingFloor(outFile, buid, floor_number);
    }

    @Override
    public long dumpRssLogEntriesByBuildingFloorBbox(FileOutputStream outFile, String buid, String floor_number,String range, String lat,String lon) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.dumpRssLogEntriesByBuildingFloorBbox(outFile, buid, floor_number,range, lat,lon);
    }

    @Override
    public List<JsonNode> getAllAccounts() throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.getAllAccounts();
    }


    ///////////////////////////////////////////////////////////////////////
    // Private Helper Methods
    ///////////////////////////////////////////////////////////////////////

    /**
     * Checks whether the Active Datasource is valid.
     * If not it throws an exception otherwise does nothing.
     *
     * @throws DatasourceException
     */
    public void _checkActiveDatasource() throws DatasourceException {
        if (this.mActiveDatabase == null) {
            throw new DatasourceException("No active Datasource exists!");
        }
    }

    @Override
    public boolean predictFloor(IAlgo algo, GeoPoint[] bbox, String[] strongestMACs) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.predictFloor(algo, bbox, strongestMACs);
    }

    @Override
    public List<JsonNode> magneticPathsByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.magneticPathsByBuildingFloorAsJson(buid, floor_number);
    }

    @Override
    public List<JsonNode> magneticPathsByBuildingAsJson(String buid) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.magneticPathsByBuildingAsJson(buid);
    }

    @Override
    public List<JsonNode> magneticMilestonesByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        _checkActiveDatasource();
        return mActiveDatabase.magneticMilestonesByBuildingFloorAsJson(buid, floor_number);
    }

}
