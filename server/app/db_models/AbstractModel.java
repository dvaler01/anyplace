package db_models;

import java.util.HashMap;


public abstract class AbstractModel {

    HashMap<String, String> fields;

    public AbstractModel(){
        fields = new HashMap<String,String>();
    }

    public HashMap<String, String> getFields(){
        return fields;
    }

    public void setFields(HashMap<String, String> f){
        this.fields = f;
        getId();
    }

    public abstract String getId();

    public abstract String toValidCouchJson();

    public abstract String toCouchGeoJSON();

}
