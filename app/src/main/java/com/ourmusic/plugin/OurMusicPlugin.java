package com.ourmusic.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaInterface;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;


public class OurMusicPlugin extends CordovaPlugin
      implements ConnectionStateCallback, PlayerNotificationCallback {

    protected static final int REQUEST_CODE_LOGIN_DELEGATE = 19204192;
    protected static final int REQUEST_CODE_LOGIN_LAUNCH = 20315203;
    protected static final String CLIENT_ID = "1ad1195a59f646e3a38b656332897055";
    protected static final String REDIRECT_URI = "ourmusic://spotify-callback/";

    private Player player;
    private CallbackContext callback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback)
          throws JSONException {
        if ( "login".equals(action) ){
            loginToSpotify(callback);
            return true;
        }
        return false;
    }

    private void loginToSpotify(final CallbackContext callback) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                OurMusicPlugin.this.callback = callback;
                Intent intent = new Intent(cordova.getActivity().getApplicationContext(), LoginActivity.class);
                cordova.startActivityForResult(OurMusicPlugin.this, intent, REQUEST_CODE_LOGIN_DELEGATE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_CODE_LOGIN_DELEGATE:
                if ( resultCode == Activity.RESULT_OK ) {
                    AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
                    if ( response.getType() == AuthenticationResponse.Type.TOKEN ) {
                        Config playerConfig = new Config(cordova.getActivity().getApplicationContext(),
                                response.getAccessToken(), CLIENT_ID);
                        player = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                            @Override
                            public void onInitialized(Player player) {
                                player.addConnectionStateCallback(OurMusicPlugin.this);
                                player.addPlayerNotificationCallback(OurMusicPlugin.this);
                                player.play("spotify:track:2TpxZ7JUBn3uw46aR7qd6V");
                                String message = "Player initialized!";
                                OurMusicPlugin.this.callback.success("OurMusicPlugin: " + message);
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                String error = "Could not initialize player: " + throwable.getMessage();
                                Log.e("OurMusicPlugin", error);
                                OurMusicPlugin.this.callback.error("OurMusicPlugin: " + error);
                            }
                        });
                    }
                }
                else{
                    String error = "Login failed: bad result from activity";
                    Log.e("OurMusicPlugin", error);
                    OurMusicPlugin.this.callback.error("OurMusicPlugin: " + error);
                }
                break;
            case REQUEST_CODE_LOGIN_LAUNCH:
                String error = "Login failed: bad result from Spotify";
                Log.e("OurMusicPlugin", error);
                OurMusicPlugin.this.callback.error("OurMusicPlugin: " + error);
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
}