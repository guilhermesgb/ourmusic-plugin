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
import com.spotify.sdk.android.player.PlayConfig;
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
    protected static final int CALLBACK_INTERVAL = 1000;

    private Player player;
    private CallbackContext loginCallback;
    private CallbackContext playStopCallback;
    private PlayerState playerState;

    // Plugin functions:
    // 1 - login
    //   Opens a new LoginActivity and waits for an auth_token in the result.
    //   * receives no args
    //   * runs a success callback with the authentication token as plain text
    // 2 - play
    //   * args: trackUri, positionInMs and auth_token respectively
    //   * when playing, constantly runs a success callback passing the current player state as argument
    //   * when a event happens in the player, runs a success callback passing the current player state and the event type as argument
    // 3 - stop
    //   * receives no args
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback)
            throws JSONException {
        if ("login".equals(action)) {
            loginToSpotify(callback);
            return true;
        }
        else if ("play".equals(action)){
            playSong(args, callback);
            return true;
        }
        else if ("stop".equals(action)) {
            stopSong(callback);
            return true;
        }
        return false;
    }

    private void loginToSpotify(final CallbackContext callback) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Context context = cordova.getActivity().getApplicationContext();
                String message = "Will prompt Login to Spotify!";
                Toast.makeText(context, "OurMusicPlugin: " + message, Toast.LENGTH_LONG).show();
                Log.i("OurMusicPlugin", message);
                OurMusicPlugin.this.loginCallback = callback;
                Intent intent = new Intent(context, LoginActivity.class);
                cordova.startActivityForResult(OurMusicPlugin.this,
                    intent, REQUEST_CODE_LOGIN_DELEGATE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_CODE_LOGIN_DELEGATE:
                if (resultCode == Activity.RESULT_OK) {
                    final AuthenticationResponse response =
                            (AuthenticationResponse) intent.getParcelableExtra("response");
                    if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                        final Context context = cordova.getActivity().getApplicationContext();
                        String message = "Logged in to Spotify successfully!";
                        Toast.makeText(context, "OurMusicPlugin: " + message,
                          Toast.LENGTH_LONG).show();
                        Log.i("OurMusicPlugin", message);
                        initializePlayerIfNeeded(response.getAccessToken(), loginCallback);
                        successCallback(loginCallback, response.getAccessToken());
                        return;
                    }
                } else {
                    String error = "Could not login to Spotify (bad response from LoginActivity)!";
                    Log.d("OurMusicPlugin", error);
                }
                break;
            default:
                String error = "Could not login to Spotify (bad response from somewhere)!";
                Log.d("OurMusicPlugin", error);
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
        Context context = cordova.getActivity().getApplicationContext();
        String error = "Could not login to Spotify!";
        Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
        Log.e("OurMusicPlugin", error);
        errorCallback(loginCallback, throwable.getMessage());
    }

    private void initializePlayerIfNeeded(String token, final CallbackContext callback) {
        if (player != null) return;
        final Context context = cordova.getActivity().getApplicationContext();
        Config playerConfig = new Config(context, token, CLIENT_ID);
        player = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
            @Override
            public void onInitialized(Player player) {
                player.addConnectionStateCallback(OurMusicPlugin.this);
                player.addPlayerNotificationCallback(OurMusicPlugin.this);
                String message = "Player initialized!";
                Toast.makeText(context, "OurMusicPlugin: " + message, Toast.LENGTH_SHORT).show();
                Log.i("OurMusicPlugin", message);
            }
            @Override
            public void onError(Throwable throwable) {
                String error = "Player could not initialize properly!";
                Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
                Log.e("OurMusicPlugin", error);
                errorCallback(callback, throwable.getMessage());
            }
        });
    }

    private void loginToPlayerIfNeeded(String token) {
        if (player != null && !player.isLoggedIn()) {
            Context context = cordova.getActivity().getApplicationContext();
            try {
                player.login(token);
                String message = "Player logged in!";
                Toast.makeText(context, "OurMusicPlugin: " + message, Toast.LENGTH_SHORT).show();
                Log.i("OurMusicPlugin", message);
            } catch (Exception e) {
                String error = "Player could not login properly!";
                Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
                Log.e("OurMusicPlugin", error);
                errorCallback(playStopCallback, e.getMessage());
            }
        }
    }

    private void playSong(JSONArray args, final CallbackContext callback) {
        this.playStopCallback = callback;
        try {
            initializePlayerIfNeeded(args.getString(2), playStopCallback);
            loginToPlayerIfNeeded(args.getString(2));
        } catch(Exception e){
            Context context = cordova.getActivity().getApplicationContext();
            String error = "Could not initialize Player due to problems with the arguments";
            Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
            Log.e("OurMusicPlugin", error);
            errorCallback(playStopCallback, e.getMessage());
        }
        if (player != null) {
            Context context = cordova.getActivity().getApplicationContext();
            try {
                int positionInMs = args.getInt(1);
                String trackUri = args.getString(0);
                if (playerState != null && trackUri.equals(playerState.trackUri)
                  && Math.abs(positionInMs - playerState.positionInMs) <= 2 * CALLBACK_INTERVAL
                  && !playerState.playing) {
                    player.resume();
                    String message = "Commanded Player to resume a song!";
                    Toast.makeText(context, "OurMusicPlugin: " + message, Toast.LENGTH_LONG).show();
                    Log.i("OurMusicPlugin", message);
                } else {
                    PlayConfig config = PlayConfig.createFor(trackUri);
                    config.withInitialPosition(positionInMs);
                    player.play(config);
                    String message = "Commanded Player to play a song (seeking to pos " +
                        positionInMs + ")!";
                    Toast.makeText(context, "OurMusicPlugin: " + message, Toast.LENGTH_LONG).show();
                    Log.i("OurMusicPlugin", message);
                }
            } catch (Exception e) {
                String error = "Player could not execute command properly!";
                Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
                Log.e("OurMusicPlugin", error);
                errorCallback(playStopCallback, e.getMessage());
            }
        }
    }

    private void stopSong(CallbackContext callback) {
        this.playStopCallback = callback;
        Context context = cordova.getActivity().getApplicationContext();
        try {
            player.pause();
            String message = "Commanded Player to pause a song!";
            Toast.makeText(context, "OurMusicPlugin: " + message, Toast.LENGTH_LONG).show();
            Log.i("OurMusicPlugin", message);
        } catch (Exception e) {
            String error = "Player could not execute command properly!";
            Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
            Log.e("OurMusicPlugin", error);
            errorCallback(playStopCallback, e.getMessage());
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
        if (playerState.playing && !this.playerState.playing) {
            startedToPlay = true;
        }
        this.playerState = playerState;
        if (startedToPlay) {
            runPlayerStateUpdater();
        }

        JSONObject state = playerStateToJsonObject(this.playerState);
        try {
            state.put("event", eventType.toString());
        } catch (Exception e) {}
        successCallback(playStopCallback, state);
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String message) {
        Context context = cordova.getActivity().getApplicationContext();
        String error = "Playback error received: " + errorType.name() + "; " + message;
        Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
        Log.e("OurMusicPlugin", error);
        errorCallback(playStopCallback, errorType.toString());
    }
    
    private void runPlayerStateUpdater() {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                while (OurMusicPlugin.this.player != null
                        && OurMusicPlugin.this.playerState != null
                        && OurMusicPlugin.this.playerState.playing) {
                    try {
                        OurMusicPlugin.this.player.getPlayerState(new PlayerStateCallback() {
                            @Override
                            public void onPlayerState(PlayerState state) {
                                OurMusicPlugin.this.playerState = state;
                                OurMusicPlugin.this.successCallback(
                                        OurMusicPlugin.this.playStopCallback,
                                        OurMusicPlugin.this.playerStateToJsonObject(state));
                            }
                        });
                        Thread.sleep(CALLBACK_INTERVAL);
                    } catch (Exception e) {
                        Log.d("OurMusicPlugin", "Playback state updater error; " + e.getMessage());
                    }
                }
            }
        });
    }

    private JSONObject playerStateToJsonObject(PlayerState state) {
        JSONObject json = new JSONObject();
        try {
            json.put("playing", state.playing);
            json.put("trackUri", state.trackUri);
            json.put("positionInMs", state.positionInMs);
            json.put("durationInMs", state.durationInMs);
        } catch (Exception e) {}
        return json;
    }

    private void successCallback(CallbackContext callback, String message) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, message);
        pluginResult.setKeepCallback(true);
        callback.sendPluginResult(pluginResult);
    }

    private void successCallback(CallbackContext callback, JSONObject json) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, json);
        pluginResult.setKeepCallback(true);
        callback.sendPluginResult(pluginResult);
    }

    private void errorCallback(CallbackContext callback, String message) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, message);
        pluginResult.setKeepCallback(true);
        callback.sendPluginResult(pluginResult);
    }

    @Override
    public void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
}