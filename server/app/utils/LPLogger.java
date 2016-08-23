package utils;

import play.Logger;

/**
 * Custom logger class to make logging easier.
 * I will try to use this everywhere so I can concentrate changing
 * this class to allow logging in files
 *
 */
public class LPLogger {

    public static void error(String tag, String message, Exception e){
        Logger.error(String.format("LPLogger::Error[%s]: %s [%s]", tag, message, e.getMessage()), e);
    }

    public static void error(String message){
        Logger.error(String.format("LPLogger::Error:: %s", message));
    }

    public static void info(String tag, String message, Exception e){
        Logger.info(String.format("LPLogger::Info[%s]: %s [%s]", tag, message, e.getMessage()), e);
    }

    public static void info(String message){
        Logger.info(String.format("LPLogger::Info:: %s", message));
    }

    public static void debug(String tag, String message, Exception e){
        Logger.debug(String.format("LPLogger::Debug[%s]: %s [%s]", tag, message, e.getMessage()), e);
    }

    public static void debug(String message){
        Logger.debug(String.format("LPLogger::Debug:: %s", message));
    }

}
