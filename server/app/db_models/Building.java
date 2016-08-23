package db_models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import utils.GeoJSONPoint;
import utils.JsonUtils;
import utils.LPUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class Building extends AbstractModel{

    private JsonNode json;
    private double lat;
    private double lng;

    private String[] admins = {"112997031510415584062_google"};

    public Building(HashMap<String,String> hm){
        this.fields = hm;
    }

    public Building(){
        fields.put("username_creator", "");
        fields.put("buid","");
        fields.put("is_published","");
        fields.put("name","");
        fields.put("description","");
        fields.put("url","");
        fields.put("address","");

        fields.put("coordinates_lat", "");
        fields.put("coordinates_lon", "");

        fields.put("bucode","");
        fields.put("poistypeid","");
    }

    public Building(JsonNode json){
        fields.put("username_creator", json.path("username_creator").textValue());

        // The id and the type of the owner that created the building
        fields.put("owner_id", json.path("owner_id").textValue());

        fields.put("buid",json.path("buid").textValue());
        fields.put("is_published", json.path("is_published").textValue());
        fields.put("name", json.path("name").textValue());
        fields.put("description", json.path("description").textValue());
        fields.put("url", json.path("url").textValue());
        fields.put("address", json.path("address").textValue());

        fields.put("coordinates_lat", json.path("coordinates_lat").textValue());
        fields.put("coordinates_lon", json.path("coordinates_lon").textValue());

        fields.put("bucode", json.path("bucode").textValue());
        fields.put("poistypeid", json.path("poistypeid").textValue());

        this.json = json;
        this.lat = Double.parseDouble(json.path("coordinates_lat").textValue());
        this.lng = Double.parseDouble(json.path("coordinates_lon").textValue());
    }

    public Building(JsonNode json, String owner) {
        this(json);
        fields.put("owner_id", owner);
    }


    public String getId(){
        String buid;
        if( (buid = fields.get("buid")) == null || buid.equals("") ){
            // create the id for the new building
            /*
            String rnd = LPUtils.generateRandomToken();
            String finalId = rnd + "_" + fields.get("coordinates_lat")
                    + String.valueOf(System.currentTimeMillis())
                    + fields.get("coordinates_lon");
            fields.put("buid", "building_" + LPUtils.encodeBase64String(finalId) );
                    */
            String finalId = LPUtils.getRandomUUID()+ "_"+ System.currentTimeMillis();
            fields.put("buid", "building_" + finalId );
            buid = fields.get("buid");

            ((ObjectNode)this.json).put("buid", buid);
        }
        return buid;
    }

    public String toValidCouchJson(){
        getId(); // initialize id if not initialized

        Gson gson = new Gson();
        return gson.toJson( this.getFields() );
    }

    public String toCouchGeoJSON(){
        StringBuilder sb = new StringBuilder();

        ObjectNode json = null;
        try {

            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());
            json.put("geometry", new GeoJSONPoint(Double.parseDouble(fields.get("coordinates_lat")),
                    Double.parseDouble(fields.get("coordinates_lon"))).toGeoJSON());

            if(json.get("co_owners") == null) {
                ArrayNode ja = json.putArray("co_owners");

                for(int i = 0; i < admins.length; i++) {
                    ja.add(admins[i]);
                }
            }

            json.remove("username");

        } catch (IOException e) {
            e.printStackTrace();
        }

        sb.append( json.toString() );

        return sb.toString();
    }

    public String appendCoOwners(JsonNode jsonReq) {

        StringBuilder sb = new StringBuilder();

        ObjectNode json = null;
        try {

            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());

            if(json.get("owner_id") == null || !json.get("owner_id").equals(jsonReq.get("owner_id"))) {
                return json.toString();
            }

            ArrayNode ja = json.putArray("co_owners");

            for(int i = 0; i < admins.length; i++) {
                ja.add(admins[i]);
            }

            Iterator<JsonNode> it = jsonReq.path("co_owners").elements();

            while(it.hasNext()) {
                JsonNode curr = it.next();
                if(curr.textValue() != null) {
                    ja.add(curr.textValue());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        sb.append(json.toString());

        return sb.toString();
    }

    public String changeOwner(String newOwnerId) {
        StringBuilder sb = new StringBuilder();
        ObjectNode json = null;
        try {
            this.fields.put("owner_id", newOwnerId);
            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());
            ArrayNode ja = json.putArray("co_owners");
            for (int i = 0; i < admins.length; i++) {
                ja.add(admins[i]);
            }
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
