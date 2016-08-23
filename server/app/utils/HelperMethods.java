package utils;


import org.apache.commons.codec.binary.Base64;
//import org.codehaus.jackson.node.ObjectNode;

import com.fasterxml.jackson.databind.node.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

public class HelperMethods {

    /**
     * Must process username and password and validate user credentials
     * @param username user's username
     * @param password user's password
     * @return True if valid credentials, false otherwise
     */
    public static boolean checkUserCredentials( String username , String password ){
        return username!= null && password != null && username.length() > 0 && password.length() > 0;
    }

    /**
     * Checks for empty or null par and if any of this holds true then an error message is appended inside the ObjectNode.
     * @param par_name The parameter name for the passed in par
     * @param par The actual parameter to check for emptiness or null
     * @param result The ObjectNode into which we will insert an error message if par is empty or null
     * @return True if par is not empty or null, false otherwise
     */
    public static boolean checkEmptyParameter( String par_name, String par, ObjectNode result ){
        if( par == null || par.equals("") ) {
            result.put("status", "error");
            result.put("message", "Missing or Invalid parameter ["+ par_name + "]");
            return false;
        }
        return true;
    }

    /**
     * Checks for empty or null par and if any of this holds true then an error message is appended inside the ObjectNode.
     * @param par_name The parameter name for the passed in par
     * @param par The actual parameter to check for emptiness or null
     * @param result The ObjectNode into which we will insert an error message if par is empty or null
     * @param fields The HashMap into which we will insert the par passed in with field_name as the key
     * @param field_name The key for the inserted pair inside the HashMap fields
     * @return True if par is not empty or null, false otherwise
     */
    public static boolean checkEmptyParameter( String par_name, String par, ObjectNode result, HashMap<String, String> fields, String field_name ){
        if( par == null || par.equals("") ) {
            result.put("status", "error");
            result.put("message", "Missing or Invalid parameter ["+ par_name + "]");
            return false;
        }
        fields.put( field_name, par );
        return true;
    }


    /**
     * Returns a byte representation for the base 64 string passed in
     * @param base64_in
     * @return byte[] for the base64 string input
     */
    public static byte[] decodeBase64( String base64_in ){
        return Base64.decodeBase64( base64_in.getBytes() );
    }

    /**
     * Returns a string representation for the decoded base 64 string passed in
     * @param base64_in
     * @return Decoded String for the base64 string input
     */
    public static String base64ToString( String base64_in ){
        return new String( decodeBase64(base64_in) );
    }


    /**
     * Stores the passed in radio map file inside the server's system for later retrieval if needed and offline processing on the server
     * @param file
     * @return
     */
    public static boolean storeRadioMapToServer( File file ){
        String radio_dir = "radio_maps_raw/";
        File dir = new File(radio_dir);
        dir.mkdirs();

        if( !dir.isDirectory() || !dir.canWrite() || !dir.canExecute() ){
            return false;
        }

        String name = "radiomap_" + LPUtils.generateRandomToken() + System.currentTimeMillis() ;
        File dest_f = new File( radio_dir + name );
        FileOutputStream fout;

        try {
            fout = new FileOutputStream(dest_f);
            Files.copy( file.toPath(), fout );
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return false;
        }

        return true;
    }


    /**
     * Deletes the file passed in. If is a directory then it deletes the contents
     * recursively and then the directory itself
     *
     * @param f The file or the directory to delete
     * @throws IOException When an error occurs while deleting a file
     */
    public static void recDeleteDirFile(File f) throws IOException {
        if( f.isFile() ){
            Files.delete(f.toPath());
        }else if( f.isDirectory() ){
            for( File file : f.listFiles() ){
                if(file.isDirectory()){
                    recDeleteDirFile(file);
                }
                Files.delete(file.toPath());
            }
            Files.delete(f.toPath());
        }
    }





}
