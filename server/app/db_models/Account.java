package db_models;

import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.*;

import com.google.gson.Gson;
//import org.codehaus.jackson.JsonNode;
//import org.codehaus.jackson.node.ObjectNode;
//import org.codehaus.jettison.json.JSONException;
//import org.codehaus.jettison.json.JSONObject;
import utils.GeoJSONPoint;
import utils.JsonUtils;
import utils.LPUtils;

import java.io.IOException;
import java.util.HashMap;

public class Account extends AbstractModel {

    private JsonNode json;

    public Account(HashMap<String, String> hm) {
        this.fields = hm;
    }

    public Account() {
        fields.put("name", "");
        fields.put("owner_id", "");
        fields.put("type", "");
        fields.put("doc_type", "account");
    }

    public Account(JsonNode json) {
        fields.put("name", json.path("name").textValue());
        fields.put("owner_id", json.path("owner_id").textValue());
        fields.put("type", json.path("type").textValue());
        fields.put("doc_type", "account");

        this.json = json;
    }

    public String getId() {
        String puid = fields.get("owner_id");
        return puid;
    }

    public String toValidCouchJson() {
        Gson gson = new Gson();
        return gson.toJson(this.getFields());
    }

    public String toCouchGeoJSON() {
        StringBuilder sb = new StringBuilder();

        ObjectNode json = null;
        try {
            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());
        } catch (IOException e) {
            e.printStackTrace();
        }

        sb.append(json.toString());

        return sb.toString();
    }


    public String toString() {
        return toValidCouchJson();
    }

}
