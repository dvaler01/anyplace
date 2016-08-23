package utils;


import java.io.File;

public class AnyplaceServerAPI {

    public static final String SERVER_ADDRESS = "https://anyplace.rayzit.com";

    public static final String SERVER_PORT = "443";
    public static final String SERVER_FULL_URL = SERVER_ADDRESS + ":" + SERVER_PORT;

    public static final String SERVER_API_ROOT = SERVER_FULL_URL + File.separatorChar + "anyplace" + File.separatorChar;

    public static final String ANDROID_API_ROOT = SERVER_FULL_URL + File.separatorChar + "android" + File.separatorChar;

}
