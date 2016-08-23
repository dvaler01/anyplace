package controllers;

import play.mvc.Call;
import play.mvc.Result;
import security.User;
import play.data.Form;
import play.mvc.Controller;
import utils.CORSHelper;

import static play.data.Form.form;

public class ApplicationAnyplace extends Controller {

    // displays the login form for the architect
    public static Result index() {
        return redirect(routes.Assets.at("/public/anyplace_viewer", "index.html"));
    }

    public static Result indexAny(String any) {
        return redirect(routes.Assets.at("/public/anyplace_viewer", "index.html"));
    }

    /**
     * AUTHORIZATION SESSION
     */
    public static class Login {
        public String username;
        public String password;
        public String admin_mode;

        public String validate() {
            if (!User.authenticate(username, password)) {
                return "Invalid user or password";
            }
            return null;
        }
    }

    // displays the login form for the architect
    public static Result login() {
        // IMPORTANT - to allow cross domain requests
        //response().setHeader("Access-Control-Allow-Origin", CORSHelper.checkOriginFromRequest(request()));
        // allows session cookies to be transferred
        //response().setHeader("Access-Control-Allow-Credentials", "true");
        return ok(views.html.anyplace_login.render(form(Login.class)));
    }

    // validates the username and password
    public static Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return badRequest(views.html.anyplace_login.render(loginForm));
        } else {
            session().clear();
            session("username", loginForm.get().username);
            if (loginForm.get().admin_mode.equalsIgnoreCase("architect"))
                return redirect(routes.AnyplaceWebApps.serveAdmin("index.html"));
            else if (loginForm.get().admin_mode.equalsIgnoreCase("android"))
                return redirect(routes.AnyplaceAndroid.getApks());
            else if (loginForm.get().admin_mode.equalsIgnoreCase("admin"))
                return redirect(routes.AnyplaceWebApps.serveAdmin("index.html"));
            else if (loginForm.get().admin_mode.equalsIgnoreCase("architect2"))
                return redirect(routes.AnyplaceWebApps.serveAdmin("index.html"));
        }
        return badRequest(views.html.anyplace_login.render(loginForm));
    }

    // logs the user out
    public static Result logout() {
        session().clear();
        flash("success", "You've been logged out");
        return redirect(routes.ApplicationAnyplace.login());
    }


    /**
     * PREFLIGHT REQUESTS HEADERS
     */

    public static Result checkPreFlight(String cuid) {
        CORSHelper.setCORSHeadersForAPI(response(), request());
        return ok();
    }

    public static Result checkPreFlight() {
        CORSHelper.setCORSHeadersForAPI(response(), request());
        return ok();
    }

    public static Result checkPreFlightFloorplans(String buid, String floor) {
        CORSHelper.setCORSHeadersForAPI(response(), request());
        return ok();
    }


    public static Result checkPreFlightAccount() {
        CORSHelper.setCORSHeadersForAccounts(response(), request());
        return ok();
    }

    public static Result checkPreFlightAccountAuid(String auid) {
        CORSHelper.setCORSHeadersForAccounts(response(), request());
        return ok();
    }

    public static Result checkPreFlightAccountClients(String auid, String cid) {
        CORSHelper.setCORSHeadersForAccounts(response(), request());
        return ok();
    }
}
