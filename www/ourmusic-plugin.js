module.exports = {
    login : function(success, error) {
	cordova.exec(function(message){
            console.log(message);
            success(message);
        }, function(error){
            console.log(error);
            error(error);
        },"OurMusicPlugin", "login", []);
    },
    play : function(uri,success, error) {
	cordova.exec(function(message){
            console.log(message);
            success(message);
        }, function(error){
            console.log(error);
            error(error);
        },"OurMusicPlugin", "play", [uri]);
    }
};
