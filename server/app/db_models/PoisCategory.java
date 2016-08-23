package db_models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import utils.JsonUtils;
import utils.LPUtils;

import java.io.IOException;
import java.util.HashMap;

public class PoisCategory extends AbstractModel{

    private JsonNode json;

    private String[] admins = {"112997031510415584062_google"};

    public PoisCategory(HashMap<String, String> hm){
        this.fields = hm;
    }

    public PoisCategory(){
        fields.put("poistypeid", "");
        fields.put("poistype","");
        fields.put("owner_id","");
        fields.put("types","[]");
    }

    public PoisCategory(JsonNode json){

        fields.put("owner_id", json.path("owner_id").textValue());

        fields.put("poistypeid",json.path("cuid").textValue());

        fields.put("poistype", json.path("name").textValue());

        this.json = json;
    }



    public PoisCategory(JsonNode json, String owner) {
        this(json);
        fields.put("owner_id", owner);
    }


    public String getId(){
        String poistypeid;
        if( (poistypeid = fields.get("poistypeid")) == null || poistypeid.equals("") ){

            String finalId = LPUtils.getRandomUUID()+ "_"+ System.currentTimeMillis();
            fields.put("poistypeid", "poistypeid_" + finalId );
            poistypeid = fields.get("poistypeid");

            ((ObjectNode)this.json).put("poistypeid", poistypeid);
        }
        return poistypeid;
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
