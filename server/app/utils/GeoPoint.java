package utils;

public class GeoPoint {

    public String lat;
    public String lon;
    public double dlat;
    public double dlon;

    public GeoPoint(){

    }

    public GeoPoint(double lat, double lon){
        this.lat = Double.toString(lat);
        this.lon = Double.toString(lon);
        this.dlat = lat;
        this.dlon = lon;
    }

    public GeoPoint(String lat, String lon){
        try{
            this.dlat = Double.parseDouble(lat);
            this.dlon = Double.parseDouble(lon);
        }catch(Exception e){
            this.dlat = 0.0;
            this.dlon = 0.0;
        }
        this.lat = lat;
        this.lon = lon;
    }

    public String toString(){
        return "lat["+lat+"] lon["+lon+"]";
    }


    public static GeoPoint[] getGeoBoundingBox( double latitude, double longitude, double distance_in_meters ){
        double EARTH_RADIUS = 6378.137; //meters

        double lat = Math.toRadians(latitude);
        double lon = Math.toRadians(longitude);
        double R = EARTH_RADIUS;
        double parallelR = R*Math.cos(lat);
        double distKM = distance_in_meters / 1000.0;

        double lat_min = Math.toDegrees(lat - distKM/R);
        double lat_max = Math.toDegrees(lat + distKM/R);
        double lon_min = Math.toDegrees(lon - distKM/parallelR);
        double lon_max = Math.toDegrees(lon + distKM/parallelR);

        GeoPoint bbox[] = new GeoPoint[]{ new GeoPoint(lat_min, lon_min), new GeoPoint(lat_max, lon_max) };

        return bbox;
    }



    /**
     *
     * @param distance in meters
     * @param bearing  in degrees
     * @return
     */
    public GeoPoint getNewPointFromDistanceBearing( double distance, double bearing ){

        double R = 6378.14; //Radius of the Earth
        double bearRad = Math.toRadians(bearing);
        double distKM = distance / 1000;

        double lat1 = Math.toRadians(Double.parseDouble(lat)); //Current lat point converted to radians
        double lon1 = Math.toRadians(Double.parseDouble(lon)); //Current long point converted to radians

        double lat2 = Math.asin( Math.sin(lat1)*Math.cos(distKM/R) + Math.cos(lat1)*Math.sin(distKM/R)*Math.cos(bearRad));

        double lon2 = lon1 + Math.atan2(Math.sin(bearRad)*Math.sin(distKM/R)*Math.cos(lat1), Math.cos(distKM/R)-Math.sin(lat1)*Math.sin(lat2));

        lat2 = Math.toDegrees(lat2);
        lon2 = Math.toDegrees(lon2);

        return new GeoPoint( lat2, lon2 );
    }

    /**
     * Returns the Euclidean distance between two LatLng points
     * @param lon1
     * @param lat1
     * @param lon2
     * @param lat2
     * @param unit 'K' for kilometers and 'M' for miles
     * @return
     */
    public static double getDistanceBetweenPoints(double lon1, double lat1, double lon2, double lat2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1))
                * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515;
        if (unit.equals("K")) {
            dist = dist * 1.609344;
        } else if (unit.equals("M")) {
            dist = dist * 0.8684;
        }
        return (dist);
    }

}
