module.exports = {
    __login__ : function(success, error) {
	cordova.exec(function(message){
            console.log(message);
            success();
        }, function(error){
            console.log(error);
            error();
        },"OurMusicPlugin", "login", []);
    }
};