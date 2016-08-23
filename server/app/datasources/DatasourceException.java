package datasources;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Exceptions used by IDatasource implementations
 * should extend this exception
 */
public class DatasourceException extends Exception {

    public DatasourceException(String msg){
        super(msg);
    }

    String getStacktrace(){
        StringWriter sw = new StringWriter();
        this.printStackTrace(new PrintWriter(sw));
        sw.flush();
        return sw.toString();
    }

}
