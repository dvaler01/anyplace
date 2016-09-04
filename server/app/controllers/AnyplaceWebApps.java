package controllers;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import java.io.File;

/**
 * This controller is responsible to serve the web applications
 */
public class AnyplaceWebApps extends Controller {

    public static Result AddTrailingSlash() {
        return movedPermanently(request().path() + "/");
    }

    // the action for the Anyplace Architect
//    @Security.Authenticated(Secured.class)
//    public static Result serveArchitect(String file) {
//        File archiDir = new File("web_apps/anyplace_architect");
//        return serveFile(archiDir, file);
//    }

    // @Security.Authenticated(Secured.class)
    public static Result serveArchitect2(String file) {
        File archiDir = new File("public/anyplace_architect");
        return serveFile(archiDir, file);
    }

    public static Result servePortal(String file) {
        File viewerDir = new File("web_apps/anyplace_portal");
        return serveFile(viewerDir, file);
    }

    public static Result servePortalTos(String file) {
        File viewerDir = new File("web_apps/anyplace_portal/tos");
        return serveFile(viewerDir, file);
    }

    public static Result servePortalPrivacy(String file) {
        File viewerDir = new File("web_apps/anyplace_portal/privacy");
        return serveFile(viewerDir, file);
    }

    public static Result servePortalMail(String file) {
        File viewerDir = new File("web_apps/anyplace_portal/mail");
        return serveFile(viewerDir, file);
    }

    // the action for the Anyplace Viewer
    public static Result serveViewer() {

        String agentInfo = request().getHeader("user-agent");

        String mode = request().getQueryString("mode");

        File viewerDir = null;
        if (mode == null || !mode.equalsIgnoreCase("widget")) {

            String bid = request().getQueryString("buid");
            String pid = request().getQueryString("selected");
            String floor = request().getQueryString("floor");
            String campus = request().getQueryString("cuid");
            if (null == bid) {
                bid = "";
            }
            if (null == pid) {
                pid = "";
            }
            if(null == floor) {
                floor = "";
            }
            if(null == campus || campus=="") {
                campus = "";
                viewerDir = new File("public/anyplace_viewer");
            }
            else{
                viewerDir = new File("public/anyplace_viewer2");
            }

        }



        return serveFile(viewerDir, "index.html");
    }

    /**
     * @author KG
     * <p>
     * Viewer 2 for testing
     */
    public static Result serveViewer2(String file) {

        String agentInfo = request().getHeader("user-agent");

        String mode = request().getQueryString("mode");
//        String fromHost = request().getHeader("host");

        if (mode == null || !mode.equalsIgnoreCase("widget")) {

            String bid = request().getQueryString("buid");
            String pid = request().getQueryString("selected");
            String floor = request().getQueryString("floor");
            if (null == bid) {
                bid = "";
            }
            if (null == pid) {
                pid = "";
            }
            if(null == floor) {
                floor = "";
            }

            // important order, windows phone 8.1 contains android & iphone in useragent
            // http://msdn.microsoft.com/en-us/library/ie/hh869301(v=vs.85).aspx
//            if (agentInfo.toLowerCase().contains("windows phone")) {
//                String wpUrl = "anyplace-dmsl-getnavigation:to?bid=" + bid + "&pid=" + pid;
//                return redirect(wpUrl);
//            } else if (agentInfo.toLowerCase().contains("android")) {
////                String androidUrl = "http://anyplace.rayzit.com/getnavigation?poid=" + pid + "&buid=" + bid + "&floor=" + floor;
////                return ok(new Html(new StringBuilder("<html><head><script>window.top.location" +
////                        ".replace('" + androidUrl + "')</script></head></html>")));
//            }
        }

        File viewerDir = new File("public/anyplace_viewer2");
        return serveFile(viewerDir, file);
    }

    // the action for the Anyplace Developers
    public static Result serveDevelopers(String file) {
        File devsDir = new File("web_apps/anyplace_developers");
        return serveFile(devsDir, file);
    }


    public static String parseCookieForUsername(Http.Request request) {
        String cookie = request.cookie("PLAY_SESSION").value();
        String data = cookie.substring(cookie.indexOf('-') + 1);
        String pairs[] = data.split("\u0000");
        //String pairs[] = data.split("&");// play 2.1.3+
        for (String pair : pairs) {
            String dat[] = pair.split("%3A");
            //String dat[] = pair.split("=");// play 2.1.3+
            String key = dat[0];
            String value = dat[1];
            if (key.equals("username")) {
                return value;
            }
        }
        return "";
    }

    // The action that serves the Admin website
    @Security.Authenticated(Secured.class)
    public static Result serveAdmin(String file) {
        File accountsDir = new File("web_apps/anyplace_accounts");
        return serveFile(accountsDir, file);
    }


    /**
     * HELPER METHODS
     */
    public static Result serveFile(File appDir, String file) {
        if (file == null) {
            return notFound();
        }
        if (file.trim().isEmpty() || file.trim().equals("index.html")) {
            file = "index.html";
            //System.out.println(request().cookie("PLAY_SESSION").value());
            //response().setCookie("nickname", parseCookieForUsername(request()));

            // update in 2.2.1 Play forces Content-Disposition: attachment
            // when type File is given to Result, so we need to override it.
            response().setHeader("Content-Disposition", "inline");
        }

        File reqFile = new File(appDir, file);
        //System.out.println("webapp: " + reqFile.getAbsolutePath().toString());
        if (!reqFile.exists() || !reqFile.canRead()) {
            return notFound();
        }

        return ok(reqFile);
    }

}
