module.exports = {
    login: function(success, error) {
	cordova.exec(function(message) {
            success(message);
        }, function(error) {
            error(error);
        }, "OurMusicPlugin", "login", []);
    },
    play: function(trackUri, position, token, success, error) {
	cordova.exec(function(playerState) {
            success(playerState);
        }, function(error) {
            error(error);
        }, "OurMusicPlugin", "play", [trackUri, position, token]);
    },
    pause: function(playerState, error) {
	cordova.exec(function(playerState) {
            success(playerState);
        }, function(error) {
            error(error);
        }, "OurMusicPlugin", "pause", []);
    }
};
