package db_models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import utils.LPUtils;

import java.util.HashMap;

public class MagneticPath extends AbstractModel {

    private JsonNode json;

    public MagneticPath(HashMap<String, String> hm) {
        this.fields = hm;
    }

    public MagneticPath() {
        fields.put("lat_a", "");
        fields.put("lng_a", "");

        fields.put("lat_b", "");
        fields.put("lng_b", "");

        fields.put("floor_num", "");
        fields.put("buid", "");
        fields.put("mpuid", "");

        fields.put("doctype", "magnetic_path");
    }

    public MagneticPath(JsonNode json) {
        fields.put("lat_a", json.path("lat_a").textValue());
        fields.put("lng_a", json.path("lng_a").textValue());

        fields.put("lat_b", json.path("lat_b").textValue());
        fields.put("lng_b", json.path("lng_b").textValue());

        fields.put("floor_num", json.path("floor_num").textValue());
        fields.put("buid", json.path("buid").textValue());
        fields.put("mpuid", json.path("mpuid").textValue());

        fields.put("doctype", "magnetic_path");

        this.json = json;
    }

    public String getId() {
        String id;
        if ((id = fields.get("mpuid")) == null || id.equals("")) {
            id = "mpath_" + LPUtils.getRandomUUID() + "_" + System.currentTimeMillis();
        }
        return id;
    }

    public String toValidCouchJson() {
        getId(); // to initialize it if not initialized

        Gson gson = new Gson();
        return gson.toJson(this.getFields());
    }

    @Override
    public String toCouchGeoJSON() {
        return toValidCouchJson();
    }

    public String toString() {
        return this.toValidCouchJson();
    }

}
