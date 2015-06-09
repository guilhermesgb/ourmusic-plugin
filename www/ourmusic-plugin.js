module.exports = {
    login : function(success, error) {
	cordova.exec(function(message){
            success(message);
        }, function(error){
            error(error);
        },"OurMusicPlugin", "login", []);
    },
    play : function(uri, time, token, success, error) {
	cordova.exec(function(message){
            success(message);
        }, function(error){
            error(error);
        },"OurMusicPlugin", "play", [uri, time, token]);
    },
    pause : function(success, error) {
	cordova.exec(function(message){
            success(message);
        }, function(error){
            error(error);
        },"OurMusicPlugin", "pause", []);
    }
};
