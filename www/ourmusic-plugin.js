window.login = function(success, error) {
    cordova.exec(function(message){
           window.alert(message);
           success();
        }, function(error){
           window.alert(error);
           error();
        }, "login", []);
};
