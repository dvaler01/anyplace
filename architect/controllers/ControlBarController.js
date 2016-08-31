/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Data Management Systems Laboratory (DMSL)
 Department of Computer Science, University of Cyprus, Nicosia, CYPRUS,
 dmsl@cs.ucy.ac.cy, http://dmsl.cs.ucy.ac.cy/

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

app.controller('ControlBarController', ['$scope', '$rootScope', 'AnyplaceService', 'AnyplaceAPIService', function ($scope, $rootScope, AnyplaceService, AnyplaceAPIService) {

    $scope.isAuthenticated = false;

    $scope.signInType = "google";

    $scope.gAuth = {};
    $scope.person = undefined;

    $scope.creds = {
        username: 'username',
        password: 'password'
    };

    $scope.owner_id = undefined;
    $scope.displayName = undefined;

    var self = this; //to be able to reference to it in a callback, you could use $scope instead


    $scope.setAuthenticated = function (bool) {
        $scope.isAuthenticated = bool;
    };

    $scope.showFullControls = true;

    $scope.toggleFullControls = function () {
        $scope.showFullControls = !$scope.showFullControls;
    };

    var apiClientLoaded = function () {
        gapi.client.plus.people.get({userId: 'me'}).execute(handleEmailResponse);
    };

    var handleEmailResponse = function (resp) {
        $scope.personLookUp(resp);
    };

    $scope.showGoogleID = function () {
        if (!$scope.person) {
            return;
        }
        AnyplaceService.addAlert('success', 'Your Google ID is: ' + $scope.person.id);
    };

    $scope.showGoogleAuth = function () {
        if (!$scope.gAuth) {
            return;
        }
        AnyplaceService.addAlert('success', 'access_token: ' + $scope.gAuth.access_token);
    };

    $scope.onSignIn = function (googleUser) {

        if ($scope.getCookie("username")=="") {
            $scope.setCookie("username", "true", 365);
            location.reload();
        }

        //location.reload();
        $scope.setAuthenticated(true);

        $scope.gAuth = gapi.auth2.getAuthInstance().currentUser.get().getAuthResponse();

        $scope.gAuth.access_token = $scope.gAuth.id_token;

        app.access_token = $scope.gAuth.id_token;

        $scope.personLookUp(googleUser);
    }


    $scope.onSignInFailure = function () {
        console.log('Sign-in state: Error');
    }

    window.onSignIn = $scope.onSignIn;
    window.onSignInFailure = $scope.onSignInFailure;

    $scope.personLookUp = function (resp) {
        $scope.person = resp.getBasicProfile();
        $scope.person.id = $scope.person.getId();
        $scope.person.displayName = $scope.person.getName();
        // compose user id
        $scope.owner_id = $scope.person.id + '_' + $scope.signInType;
        $scope.displayName = $scope.person.displayName;

        if ($scope.person && $scope.person.id) {
            $scope.$broadcast('loggedIn', []);
        }

        var promise = AnyplaceAPIService.signAccount({
            name: $scope.person.displayName,
            type: "google"
        });

        promise.then(
            function (resp) {
            },
            function (resp) {
            }
        );
    };

    $scope.signOut = function () {
        $scope.setCookie("username", "", 365);
        var auth2 = gapi.auth2.getAuthInstance();
        auth2.signOut().then(function () {
            console.log('User signed out.');
        });
        $scope.isAuthenticated = false;

        $scope.$broadcast('loggedOff', []);
        $scope.gAuth = {};
        $scope.owner_id = undefined;
        $scope.person = undefined;


    };

    $scope.getCookie = function(cname) {
        var name = cname + "=";
        var ca = document.cookie.split(';');
        for(var i = 0; i <ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0)==' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) == 0) {
                return c.substring(name.length,c.length);
            }
        }
        return "";
    }

    $scope.setCookie = function(cname, cvalue, exdays) {
        var d = new Date();
        d.setTime(d.getTime() + (exdays*24*60*60*1000));
        var expires = "expires="+d.toUTCString();
        document.cookie = cname + "=" + cvalue + "; " + expires;
    }

    $scope.tab = 1;

    $scope.setTab = function (num) {
        $scope.tab = num;
    };

    $scope.isTabSet = function (num) {
        return $scope.tab === num;
    };

}]);
