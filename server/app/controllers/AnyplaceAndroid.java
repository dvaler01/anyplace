package controllers;


import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.AndroidAPKFile;
import utils.AnyplaceServerAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnyplaceAndroid extends Controller {

    public final static String ANDROID_APKS_ROOT_DIRECTORY_LOCAL = "anyplace_android" + File.separatorChar + "apk" + File.separatorChar;
    public final static String ANDROID_APk_DOWNLOAD = AnyplaceServerAPI.ANDROID_API_ROOT + "apk/";

    // the action for the Anyplce Architect
    @Security.Authenticated(Secured.class)
    public static Result getApks(){

        File dirApks = new File(ANDROID_APKS_ROOT_DIRECTORY_LOCAL);
        if( !dirApks.isDirectory() || !dirApks.canExecute() || !dirApks.canRead() ){
            return badRequest("No Android apk available!");
        }
        AndroidAPKFile apk;
        List<AndroidAPKFile> apks = new ArrayList<AndroidAPKFile>();
        for( File fileApk : dirApks.listFiles() ){
            if( !fileApk.isFile() )
                continue;
            apk = new AndroidAPKFile(fileApk);
            apk.setDownloadUrl(ANDROID_APk_DOWNLOAD + apk.getFilePathBasename());
            apks.add(apk);
        }

        Collections.sort(apks, new AndroidAPKFile.AndroidAPKComparator());
        return ok(views.html.anyplace_android.render(apks));
    }

    public static Result downloadApk( String file) {

        File fileApk = new File(ANDROID_APKS_ROOT_DIRECTORY_LOCAL, file);

        System.out.println( "requested: " + fileApk );

            if( !fileApk.exists() || !fileApk.canRead() ){
                return badRequest("Requested APK does not exist on our database!");
            }

            response().setContentType("application/x-download");
            response().setHeader("Content-disposition","attachment; filename="+file);

            return ok(fileApk);
    }

}