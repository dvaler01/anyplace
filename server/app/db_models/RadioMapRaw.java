package db_models;


import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.*;

import com.google.gson.Gson;
//import org.codehaus.jackson.JsonNode;
//import org.codehaus.jettison.json.JSONException;
//import org.codehaus.jettison.json.JSONObject;
import utils.GeoJSONPoint;
import utils.JsonUtils;

import java.io.IOException;
import java.util.HashMap;

public class RadioMapRaw extends AbstractModel{

    public RadioMapRaw( HashMap<String, String> h ){
        this.fields = h;
    }

    public RadioMapRaw( String timestamp, String x, String y, String heading, String MAC_addr, String rss ){
        fields.put("timestamp", timestamp);
        fields.put("x", x );
        fields.put("y", y );
        fields.put("heading", heading );
        fields.put("MAC", MAC_addr);
        fields.put("rss", rss);
        fields.put("floor", "-");
    }

    public RadioMapRaw( String timestamp, String x, String y, String heading, String MAC_addr, String rss, String floor ){
        fields.put("timestamp", timestamp);
        fields.put("x", x );
        fields.put("y", y );
        fields.put("heading", heading );
        fields.put("MAC", MAC_addr);
        fields.put("rss", rss);
        fields.put("floor", floor);
    }

    public RadioMapRaw( String timestamp, String x, String y, String heading, String MAC_addr, String rss, String floor,String strongestWifi ){
        fields.put("timestamp", timestamp);
        fields.put("x", x );
        fields.put("y", y );
        fields.put("heading", heading );
        fields.put("MAC", MAC_addr);
        fields.put("rss", rss);
        fields.put("floor", floor);
        fields.put("strongestWifi", strongestWifi);
    }

    public RadioMapRaw( String timestamp, String x, String y, String heading, String MAC_addr, String rss, String floor,String strongestWifi, String buid ){
        fields.put("timestamp", timestamp);
        fields.put("x", x );
        fields.put("y", y );
        fields.put("heading", heading );
        fields.put("MAC", MAC_addr);
        fields.put("rss", rss);
        fields.put("floor", floor);
        fields.put("strongestWifi", strongestWifi);
        fields.put("buid", buid);
    }

    public String getId(){
        return fields.get("x") + fields.get("y") + fields.get("heading") + fields.get("timestamp") + fields.get("MAC");
    }

    public String toValidCouchJson(){
        getId(); // to initialize it if not initialized

        Gson gson = new Gson();
        return gson.toJson( this.getFields() );
    }

    public String toCouchGeoJSON(){
        StringBuilder sb = new StringBuilder();

        ObjectNode json = null;
        try {

            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());
            json.put("geometry", new GeoJSONPoint(Double.parseDouble(fields.get("x")),
                    Double.parseDouble(fields.get("y"))).toGeoJSON());

        } catch (IOException e) {
            e.printStackTrace();
        }

        sb.append( json.toString() );

        return sb.toString();
    }

    public String toString(){
        return this.toValidCouchJson();
    }

    public String toRawRadioMapRecord(){
        StringBuilder sb = new StringBuilder();
        sb.append(fields.get("timestamp")); sb.append(" ");
        sb.append(fields.get("x")); sb.append(" ");
        sb.append(fields.get("y")); sb.append(" ");
        sb.append(fields.get("heading")); sb.append(" ");
        sb.append(fields.get("MAC")); sb.append(" ");
        sb.append(fields.get("rss")); sb.append(" ");
        sb.append(fields.get("floor"));
        return sb.toString();
    }

    public static  String toRawRadioMapRecord(HashMap<String,String> hm){
        StringBuilder sb = new StringBuilder();
        sb.append(hm.get("timestamp")); sb.append(" ");
        sb.append(hm.get("x")); sb.append(" ");
        sb.append(hm.get("y")); sb.append(" ");
        sb.append(hm.get("heading")); sb.append(" ");
        sb.append(hm.get("MAC")); sb.append(" ");
        sb.append(hm.get("rss")); sb.append(" ");
        sb.append(hm.get("floor"));
        return sb.toString();
    }

    public static  String toRawRadioMapRecord(JsonNode json){
        StringBuilder sb = new StringBuilder();
        sb.append(json.path("timestamp").textValue()); sb.append(" ");
        sb.append(json.path("x").textValue()); sb.append(" ");
        sb.append(json.path("y").textValue()); sb.append(" ");
        sb.append(json.path("heading").textValue()); sb.append(" ");
        sb.append(json.path("MAC").textValue()); sb.append(" ");
        sb.append(json.path("rss").textValue()); sb.append(" ");
        sb.append(json.path("floor").textValue());
        return sb.toString();
    }

}
