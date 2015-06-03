package com.ourmusic.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;


public class OurMusicPlugin extends CordovaPlugin
      implements ConnectionStateCallback, PlayerNotificationCallback {

    protected static final int REQUEST_CODE_LOGIN_DELEGATE = 19204192;
    protected static final int REQUEST_CODE_LOGIN_LAUNCH = 20315203;
    protected static final String CLIENT_ID = "1ad1195a59f646e3a38b656332897055";
    protected static final String REDIRECT_URI = "ourmusic://spotify-callback/";

    private Player player;
    private CallbackContext loginCallback;
    private CallbackContext playStopCallback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback)
          throws JSONException {
        if ( "login".equals(action) ){
            loginToSpotify(callback);
            return true;
        }
	if ( "play".equals(action) ){
            playSong(args,callback);
            return true;
        }
	
        return false;
    }

    private void playSong(JSONArray args, final CallbackContext callback) {
	OurMusicPlugin.this.playStopCallback = callback;
	player.play(args.getString(0));
	Log.i("OurMusicPlugin-play", "mandou tocar");
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
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

                        Config playerConfig = new Config(context, response.getAccessToken(), CLIENT_ID);
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
				OurMusicPlugin.this.loginCallback.success(response.getAccessToken());
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                String error = "Could not initialize player: " + throwable.getMessage();
                                Log.e("OurMusicPlugin", error);
                                OurMusicPlugin.this.loginCallback.error("OurMusicPlugin: " + error);
                            }
                        });
                    }
                }
                else{
                    String error = "Login failed: bad result from activity";
                    Log.e("OurMusicPlugin", error);
                    OurMusicPlugin.this.loginCallback.error("OurMusicPlugin: " + error);
                }
                break;
            case REQUEST_CODE_LOGIN_LAUNCH:
                String error = "Login failed: bad result from Spotify";
                Log.e("OurMusicPlugin", error);
                OurMusicPlugin.this.loginCallback.error("OurMusicPlugin: " + error);
                break;
            default:
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
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String message) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }

    @Override
    public void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
}
