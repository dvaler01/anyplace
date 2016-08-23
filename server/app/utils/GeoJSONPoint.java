package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeoJSONPoint {

    public double lat,lon;

    public GeoJSONPoint(double lat, double lon){
        this.lat = lat;
        this.lon = lon;
    }

    public JsonNode toGeoJSON(){
        ObjectNode jo = JsonUtils.createObjectNode();
        ArrayNode ja = jo.putArray("coordinates");
        ja.add(this.lat);
        ja.add(this.lon);
        jo.put("type", "Point");
        return jo;
    }

}
