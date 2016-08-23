package utils;


import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class GeoJSONMultiPoint {

    public List<GeoPoint> points;

    public GeoJSONMultiPoint( GeoPoint... points ){
        this.points = new ArrayList<GeoPoint>();
        for( GeoPoint p : points ){
            this.points.add(p);
        }
    }

    public JSONObject toGeoJSON() throws JSONException {
        JSONArray ja, jall=ja = new JSONArray();;
        for( GeoPoint p : this.points ){
            ja = new JSONArray();
            ja.put(p.dlat);
            ja.put(p.dlon);
            jall.put( ja );
        }
        JSONObject jo = new JSONObject();
        jo.put("type", "MultiPoint");
        jo.put("coordinates", jall);
        return jo;
    }

}
