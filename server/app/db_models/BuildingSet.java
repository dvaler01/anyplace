package db_models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import utils.GeoJSONPoint;
import utils.JsonUtils;
import utils.LPUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class BuildingSet extends AbstractModel{

    private JsonNode json;
    private double lat;
    private double lng;

    private String[] admins = {"112997031510415584062_google"};

    public BuildingSet(HashMap<String, String> hm){
        this.fields = hm;
    }

    public BuildingSet(){
        fields.put("owner_id", "");
        fields.put("cuid","");
        fields.put("name","");
        fields.put("description","");
        fields.put("greeklish","");
        fields.put("buids","[]");
    }

    public BuildingSet(JsonNode json){

        fields.put("owner_id", json.path("owner_id").textValue());

        fields.put("cuid",json.path("cuid").textValue());

        fields.put("name", json.path("name").textValue());

        fields.put("greeklish", json.path("greeklish").textValue());

        fields.put("description", json.path("description").textValue());

        this.json = json;
    }



    public BuildingSet(JsonNode json, String owner) {
        this(json);
        fields.put("owner_id", owner);
    }


    public String getId(){
        String cuid;
        if( (cuid = fields.get("cuid")) == null || cuid.equals("") ){

            String finalId = LPUtils.getRandomUUID()+ "_"+ System.currentTimeMillis();
            fields.put("cuid", "cuid_" + finalId );
            cuid = fields.get("cuid");

            ((ObjectNode)this.json).put("cuid", cuid);
        }
        return cuid;
    }

    public String toValidCouchJson(){
        getId(); // initialize id if not initialized

        Gson gson = new Gson();
        return gson.toJson( this.getFields() );
    }

    public String toCouchGeoJSON(){
        StringBuilder sb = new StringBuilder();
        ((ObjectNode) json).remove("access_token");
        sb.append(this.json.toString());
        return sb.toString();
    }

    public String changeOwner(String newOwnerId) {
        StringBuilder sb = new StringBuilder();
        ObjectNode json = null;
        try {
            this.fields.put("owner_id", newOwnerId);
            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());
        } catch (IOException e) {
            e.printStackTrace();
        }
        sb.append(json.toString());
        return sb.toString();
    }

    public String toString(){
        return toValidCouchJson();
    }

}
