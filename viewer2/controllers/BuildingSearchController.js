/*
 Building Schema:
 {
 address: "-"
 buid: "building_02e4ebd2-413e-4b27-b3bf-6f7e2ca3640f_1397488807470"
 coordinates_lat: "52.52816874290241"
 coordinates_lon: "13.457137495279312"
 description: "IPSN 2014 Venue"
 is_published: "true"
 name: "Andel's Hotel Berlin, Berlin, Germany"
 url: "-"
 }
 */

app.controller('BuildingSearchController', ['$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService','$timeout', '$q', '$log', function BuildingSearchController ($scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService,$timeout, $q, $log) {

    $scope.gmapService = GMapService;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;

    $scope.creds = {
        username: 'username',
        password: 'password'
    };


    $scope.myBuildings = [];

    $scope.myBuildingsnames = {};
    $scope.data = [];
    $scope.datasz = 0;
    $scope.myallPoisHashT = {};

    $scope.myallEntrances = [];

    $scope.mylastquery = "";
    $scope.myallPois = [];
    var self = this;
    self.querySearch = querySearch;

    function querySearch (query) {
        if (query == ""){
            return ;
        }
        if (query == $scope.mylastquery){
            return $scope.myallPois;
        }
        $scope.anyService.selectedSearchPoi = query;
        setTimeout(
            function(){
                if (query==$scope.anyService.selectedSearchPoi ){
                    $scope.fetchAllPoi(query, $scope.urlCampus);
                }
            },1000);
        $scope.mylastquery = query;
        return $scope.myallPois;
    }

    $scope.fetchAllPoi = function (letters , cuid) {

        var jsonReq = { "access-control-allow-origin": "",    "content-encoding": "gzip",    "access-control-allow-credentials": "true",    "content-length": "17516",    "content-type": "application/json" , "cuid":$scope.urlCampus, "letters":letters, "greeklish":$scope.greeklish };
        var promise = AnyplaceAPIService.retrieveALLPois(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.myallPois = data.pois;

                var sz = $scope.myallPois.length;

                for (var i = sz - 1; i >= 0; i--) {
                    $scope.myallPois[i].buname=$scope.myBuildingsnames[$scope.myallPois[i].buid];
                }

            },
            function (resp) {
                var data = resp.data;
                if (letters=="")
                    _err("Something went wrong while fetching POIs");
            }
        );
    };


    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal && newVal.buid) {
            $scope.mylastquery="";
        }
    });

    $scope.fetchAllBuildings = function () {
        var jsonReq = { "access-control-allow-origin": "",    "content-encoding": "gzip",    "access-control-allow-credentials": "true",    "content-length": "17516",    "content-type": "application/json" , "cuid":$scope.urlCampus};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.allBuildings(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                //var bs = JSON.parse( data.buildings );
                $scope.myBuildings = data.buildings;
                $scope.greeklish = data.greeklish;

                for (var i = 0; i < $scope.myBuildings.length; i++) {

                    var b = $scope.myBuildings[i];

                    $scope.myBuildingsnames[b.buid] = b.name;
                }

            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching buildings.');
            }
        );
    };

    $scope.fetchAllBuildings();

    var _latLngFromBuilding = function (b) {
        if (b && b.coordinates_lat && b.coordinates_lon) {
            return {
                lat: parseFloat(b.coordinates_lat),
                lng: parseFloat(b.coordinates_lon)
            }
        }
        return undefined;
    };

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _calcDistance = function (x1, y1, x2, y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    };

    $scope.orderByName = function (v) {
        return v.name;
    };

    $scope.orderByDistCentre = function (v) {
        if ($scope.anyService.selectedBuilding)
            return v.name;
        var c = $scope.gmapService.gmap.getCenter();
        return _calcDistance(parseFloat(v.coordinates_lat), parseFloat(v.coordinates_lon), c.lat(), c.lng());
    }

}]);
