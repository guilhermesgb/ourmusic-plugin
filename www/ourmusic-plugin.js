module.exports = {
    login: function(success, error) {
	cordova.exec(function(message) {
            success(message);
        }, function(message) {
            error(message);
        }, "OurMusicPlugin", "login", []);
    },
    play: function(trackUri, position, token, success, error) {
	cordova.exec(function(playerState) {
            success(playerState);
        }, function(message) {
            error(message);
        }, "OurMusicPlugin", "play", [trackUri, position, token]);
    },
    pause: function(token, success, error) {
	cordova.exec(function(playerState) {
            success(playerState);
        }, function(message) {
            error(message);
        }, "OurMusicPlugin", "pause", [token]);
    }
};
