module.exports = {
    login : function(success, error) {
	cordova.exec(function(message){
            success(message);
        }, function(error){
            error(error);
        },"OurMusicPlugin", "login", []);
    },
    play : function(uri,success, error) {
	cordova.exec(function(message){
            success(message);
        }, function(error){
            error(error);
        },"OurMusicPlugin", "play", [uri]);
    },
    pause : function(success, error) {
	cordova.exec(function(message){
            success(message);
        }, function(error){
            error(error);
        },"OurMusicPlugin", "pause", []);
    }
};
