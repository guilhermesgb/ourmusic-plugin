package com.ourmusic.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.lang.Exception;
import java.lang.Math;


public class OurMusicPlugin extends CordovaPlugin
        implements ConnectionStateCallback, PlayerNotificationCallback {

    protected static final int REQUEST_CODE_LOGIN_DELEGATE = 19204192;
    protected static final int REQUEST_CODE_LOGIN_LAUNCH = 20315203;
    protected static final String CLIENT_ID = "1ad1195a59f646e3a38b656332897055";
    protected static final String REDIRECT_URI = "ourmusic://spotify-callback/";
    protected static final String SPOTIFY_LOGIN_ERROR = "SPOTIFY_LOGIN_ERROR";
    protected static final String PLAYER_LOGIN_ERROR = "PLAYER_LOGIN_ERROR";
    protected static final String EVENT = "event";
    protected static final int CALLBACK_INTERVAL = 1000;

    private Player player;
    private CallbackContext loginCallback;
    private CallbackContext playStopCallback;
    private PlayerState playerState;

    // Functions
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback)
            throws JSONException {
	if(action.equals("login")) {
            loginToSpotify(callback);
            return true;
	} if(action.equals("play") ){
            playSong(args,callback);
            return true;
	} if(action.equals("stop")) {
	    stopSong(callback);
	    return true;
	}
	return false;
    }

    private void stopSong(CallbackContext callback){
	this.playStopCallback = callback;
	try{
	    player.pause();
	} catch (Exception e) {
	    errorCallback(playStopCallback,e.getMessage());
	}
    }

    private void playSong(JSONArray args, final CallbackContext callback) {
	this.playStopCallback = callback;
	try{
	    initializePlayerIfNeeded(args.getString(2),callback);
	    loginToPlayerIfNeeded(args.getString(2));
	} catch(Exception e){}
	if( player != null){
	    try{
		int positionInMs = args.getInt(1);
		String trackUri = args.getString(0);
		if( trackUri.equals(playerState.trackUri) 
		    && Math.abs(positionInMs - playerState.positionInMs) <= 2 * CALLBACK_INTERVAL
		    && !playerState.playing){
		    player.resume();
		} else {
		    player.play(trackUri);
		    player.seekToPosition(positionInMs);
		}
		
	    } catch (Exception e) {
	        errorCallback(playStopCallback,e.getMessage());
	    }
	    Log.i("OurMusicPlugin-play", "mandou tocar");
	}
    }

    private void loginToSpotify(final CallbackContext callback) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Context context = cordova.getActivity().getApplicationContext();
                Toast toast = Toast.makeText(context, "OurMusicPlugin: " +
                        "Will prompt Login to Spotify!", Toast.LENGTH_LONG);
                toast.show();
                Log.i("OurMusicPlugin", "Will prompt Login to Spotify!");
                OurMusicPlugin.this.loginCallback = callback;
                Intent intent = new Intent(context, LoginActivity.class);
                cordova.startActivityForResult(OurMusicPlugin.this, intent, REQUEST_CODE_LOGIN_DELEGATE);
            }
        });
    }

    private void loginToPlayerIfNeeded(String token){
	if(player != null && ! player.isLoggedIn()){
	    try{
		player.login(token);
	    } catch (Exception e) {
		errorCallback(playStopCallback,e.getMessage());
	    }
	}
    }

    private void initializePlayerIfNeeded(String token, final CallbackContext callback){
	if (player != null) return;
	final Context context = cordova.getActivity().getApplicationContext();	
	Config playerConfig = new Config(context, token, CLIENT_ID);
	player = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
		@Override
		public void onInitialized(Player player) {
		    player.addConnectionStateCallback(OurMusicPlugin.this);

		    String message = "Player initialized!";
		    Log.i("OurMusicPlugin", message);
		    Toast toast = Toast.makeText(context,
						 "OurMusicPlugin: " + message, Toast.LENGTH_LONG);
		    toast.show();
                                
		    player.addPlayerNotificationCallback(OurMusicPlugin.this);
		}

		@Override
		public void onError(Throwable throwable) {
		    String error = "Could not initialize player: " + throwable.getMessage();
		    Log.e("OurMusicPlugin", error);
		    errorCallback(callback, PLAYER_LOGIN_ERROR);
		}
	    });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        String error;
        switch (requestCode) {
            case REQUEST_CODE_LOGIN_DELEGATE:
                if ( resultCode == Activity.RESULT_OK ) {
                    final AuthenticationResponse response = (AuthenticationResponse) intent.getParcelableExtra("response");
                    if ( response.getType() == AuthenticationResponse.Type.TOKEN ) {
                        final Context context = cordova.getActivity().getApplicationContext();

                        String message = "User authenticated to Spotify!";
                        Log.i("OurMusicPlugin", message);
                        Toast toast = Toast.makeText(context,
                                "OurMusicPlugin: " + message, Toast.LENGTH_LONG);
                        toast.show();

			initializePlayerIfNeeded(response.getAccessToken(),loginCallback);
			successCallback(loginCallback,response.getAccessToken());
                        return;
                    }
                } else {
                    error = "Login failed: bad result from activity";
                    Log.e("OurMusicPlugin", error);
                    errorCallback(OurMusicPlugin.this.loginCallback,SPOTIFY_LOGIN_ERROR);
                }
                break;
            case REQUEST_CODE_LOGIN_LAUNCH:
                error = "Login failed: bad result from Spotify";
                Log.e("OurMusicPlugin", error);
                errorCallback(OurMusicPlugin.this.loginCallback,SPOTIFY_LOGIN_ERROR);
                break;
            default:
                error = "Login failed: unknown reason";
                Log.e("OurMusicPlugin", error);
                errorCallback(OurMusicPlugin.this.loginCallback,SPOTIFY_LOGIN_ERROR);
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("OurMusicPlugin", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("OurMusicPlugin", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
	Log.d("OurMusicPlugin", "Login failed");
	if(playStopCallback != null){
	    errorCallback(playStopCallback,PLAYER_LOGIN_ERROR);
	} else if( loginCallback != null){
	    errorCallback(loginCallback,PLAYER_LOGIN_ERROR);
	}
    }

    @Override
    public void onTemporaryError() {
        Log.d("OurMusicPlugin", "Temporary error occurred");
    }
 
    @Override
    public void onConnectionMessage(String message) {
        Log.d("OurMusicPlugin", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("OurMusicPlugin", "Playback event received: " + eventType.name());

	boolean startedToPlay = false;
	if ( playerState.playing && !this.playerState.playing){
	    startedToPlay = true;
	}
	this.playerState = playerState;
	if ( startedToPlay ){
	    runPlayerStateUpdater();
	}
	
	JSONObject json = playerStateToJsonObject(this.playerState);
	json.put(EVENT,eventType.toString());
	successCallback(playStopCallback,json);
    }

    private void successCallback(CallbackContext callback, String message){
	PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, message);
	pluginResult.setKeepCallback(true);
	callback.sendPluginResult(pluginResult);
    }

    private void successCallback(CallbackContext callback, JSONObject json){
	PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, json);
	pluginResult.setKeepCallback(true);
	callback.sendPluginResult(pluginResult);
    }

    private void errorCallback(CallbackContext callback, String message){
	PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, message);
	pluginResult.setKeepCallback(true);
	callback.sendPluginResult(pluginResult);
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String message) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
	errorCallback(playStopCallback, errorType.toString());
    }

    private JSONObject playerStateToJsonObject(PlayerState state){
	JSONObject json = new JSONObject();
	json.put("playing",state.playing);
	json.put("durationInMs",state.durationInMs);
	json.put("trackUri",state.trackUri);
	return json;
    }
    
    private void runPlayerStateUpdater(){
	cordova.getThreadPool().execute(new Runnable() {
		public void run() {
		    Player player = OurMusicPlugin.this.player;
		    while (player != null && OurMusicPlugin.this.playerState != null
			   && OurMusicPlugin.this.playerState.playing) {
			try {
			    player.getPlayerState(new PlayerStateCallback() {
				    @Override
				    public void onPlayerState(PlayerState state){
					OurMusicPlugin.this.playerState = state;
					OurMusicPlugin.this.successCallback(OurMusicPlugin.this.playStopCallback,
								       OurMusicPlugin.this.playerStateToJsonObject(state));
				    }
				});
			    Thread.sleep(CALLBACK_INTERVAL);
			} catch (Exception e){
			    Log.d("OurMusicPlugin - runPlayerStateUpdater",e.getMessage());
			}
		    }
		}
	    });
    }

    @Override
    public void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
}
