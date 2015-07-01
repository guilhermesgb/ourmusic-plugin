package com.ourmusic.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.net.Uri;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
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
import java.lang.Override;


public class OurMusicPlugin extends CordovaPlugin
        implements ConnectionStateCallback, PlayerNotificationCallback {

    interface PlayerInitializedCallback {
        public void playerInitializedSuccessfully();
    }

    protected static final int REQUEST_CODE_LOGIN_DELEGATE = 19204192;
    protected static final int REQUEST_CODE_LOGIN_LAUNCH = 20315203;
    protected static final String CLIENT_ID = "a86d7ad4269d4a6ea18b167c1f5b811d";
    protected static final String REDIRECT_URI = "ourmusic://spotify-callback/";
    protected static final int CALLBACK_INTERVAL = 1000;
    protected static final String PLAYER_INITIALIZED_CODE = "PLAYER_INITIALIZED";

    private Player player;
    private CallbackContext loginCallback;
    private CallbackContext playPauseCallback;
    private PlayerInitializedCallback playerInitializedCallback;
    private PlayerState playerState;

    // Plugin functions:
    // 1 - login
    //   Opens a web browser and returns a spotify code (first step in authentication flow).
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
        else if ("pause".equals(action)) {
            pauseSong(args, callback);
            return true;
        }
        return false;
    }

    private void loginToSpotify(final CallbackContext callback) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                OurMusicPlugin.this.loginCallback = callback;

                Context context = cordova.getActivity().getApplicationContext();
                String message = "Will prompt Login to Spotify!";
                Toast.makeText(context, "OurMusicPlugin: " + message, Toast.LENGTH_LONG).show();
                Log.i("OurMusicPlugin", message);

                AuthenticationRequest.Builder builder =
                        new AuthenticationRequest.Builder(OurMusicPlugin.CLIENT_ID,
                                AuthenticationResponse.Type.CODE, OurMusicPlugin.REDIRECT_URI);
                builder.setScopes(new String[]{"user-read-private", "streaming"});
                builder.setShowDialog(true);
                AuthenticationRequest request = builder.build();

                AuthenticationClient.openLoginInBrowser(cordova.getActivity(), request);
            }
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final Context context = cordova.getActivity().getApplicationContext();

        Uri uri = intent.getData();
        if (uri != null) {
            AuthenticationResponse response = AuthenticationResponse.fromUri(uri);
            if (response.getType() == AuthenticationResponse.Type.CODE) {
                String message = "Logged in to Spotify successfully!";
                Toast.makeText(context, "OurMusicPlugin: " + message, Toast.LENGTH_LONG).show();
                Log.i("OurMusicPlugin", message);
                successCallback(loginCallback, response.getCode());
                return;
            }
        }
        String error = "Could not get Spotify code";
        Log.d("OurMusicPlugin", error);
        Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
        errorCallback(loginCallback, error);
    }

    @Override
    public synchronized void onLoggedIn() {
        Log.d("OurMusicPlugin", "Player logged in");
        this.playerInitializedCallback.playerInitializedSuccessfully();
    }

    @Override
    public void onLoggedOut() {
        Log.d("OurMusicPlugin", "Player logged out");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Context context = cordova.getActivity().getApplicationContext();
        Spotify.destroyPlayer(this);
        player = null;
        String error = "Could not login to Spotify Player!";
        Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
        Log.e("OurMusicPlugin", error);
        errorCallback(playPauseCallback, throwable.getMessage());
    }

    private void initializePlayerIfNeeded(String token, final CallbackContext callbackContext,
            final PlayerInitializedCallback playerInitializedCallback) {
        this.playerInitializedCallback = playerInitializedCallback;
        Log.i("OurMusicPlugin", "THE TOKEN RECEIVED IS: " + token);
        if (player != null) {
            this.playerInitializedCallback.playerInitializedSuccessfully();
            return;
        }

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
                errorCallback(callbackContext, throwable.getMessage());
            }
        });
    }

    private void playSong(final JSONArray args, final CallbackContext callback) {
        this.playPauseCallback = callback;
        try {
            initializePlayerIfNeeded(args.getString(2), playPauseCallback, new PlayerInitializedCallback(){
                public void playerInitializedSuccessfully() {
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
                        try {
                            player.getPlayerState(new PlayerStateCallback() {
                                @Override
                                public void onPlayerState(PlayerState state) {
                                    OurMusicPlugin.this.playerState = state;
                                    OurMusicPlugin.this.successCallback(
                                            OurMusicPlugin.this.playPauseCallback,
                                            OurMusicPlugin.this.playerStateToJsonObject(state, "PLAY_PRESSED"));
                                }
                            });
                        } catch (Exception e) {
                            String error = "Error retriving playback state!";
                            Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
                            Log.e("OurMusicPlugin", error);
                            errorCallback(playPauseCallback, e.getMessage());
                            Log.d("OurMusicPlugin", "Error retriving playback state; " + e.getMessage());
                        }
                    } catch (Exception e) {
                        String error = "Player could not execute command properly!";
                        Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
                        Log.e("OurMusicPlugin", error);
                        errorCallback(playPauseCallback, e.getMessage());
                    }
                }
            });
        } catch(Exception e){
            Context context = cordova.getActivity().getApplicationContext();
            String error = "Could not initialize Player due to problems with the arguments";
            Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
            Log.e("OurMusicPlugin", error);
            errorCallback(playPauseCallback, e.getMessage());
        }
    }

    private void pauseSong(final JSONArray args, final CallbackContext callback) {
        this.playPauseCallback = callback;
        try {
            initializePlayerIfNeeded(args.getString(0), playPauseCallback, new PlayerInitializedCallback() {
                public void playerInitializedSuccessfully() {
                    Context context = cordova.getActivity().getApplicationContext();
                    try {
                        player.pause();
                        String message = "Commanded Player to pause a song!";
                        Toast.makeText(context, "OurMusicPlugin: " + message, Toast.LENGTH_LONG).show();
                        Log.i("OurMusicPlugin", message);
                        try {
                            player.getPlayerState(new PlayerStateCallback() {
                                @Override
                                public void onPlayerState(PlayerState state) {
                                    OurMusicPlugin.this.playerState = state;
                                    OurMusicPlugin.this.successCallback(
                                            OurMusicPlugin.this.playPauseCallback,
                                            OurMusicPlugin.this.playerStateToJsonObject(state,"PAUSE_PRESSED"));
                                }
                            });
                        } catch (Exception e) {
                            String error = "Error retriving playback state!";
                            Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
                            Log.e("OurMusicPlugin", error);
                            errorCallback(playPauseCallback, e.getMessage());
                            Log.d("OurMusicPlugin", "Error retriving playback state; " + e.getMessage());
                        }
                    } catch (Exception e) {
                        String error = "Player could not execute command properly!";
                        Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
                        Log.e("OurMusicPlugin", error);
                        errorCallback(playPauseCallback, e.getMessage());
                    }
                }
            });
        } catch(Exception e){
            Context context = cordova.getActivity().getApplicationContext();
            String error = "Could not initialize Player due to problems with the arguments";
            Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
            Log.e("OurMusicPlugin", error);
            errorCallback(playPauseCallback, e.getMessage());
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
    public synchronized void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("OurMusicPlugin", "Playback event received: " + eventType.name());

        this.playerState = playerState;
        if (eventType == EventType.PLAY) {
            runPlayerStateUpdater();
        }

        JSONObject state = playerStateToJsonObject(this.playerState, eventType.toString());
        successCallback(playPauseCallback, state);
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String message) {
        Context context = cordova.getActivity().getApplicationContext();
        String error = "Playback error received: " + errorType.name() + "; " + message;
        Toast.makeText(context, "OurMusicPlugin: " + error, Toast.LENGTH_LONG).show();
        Log.e("OurMusicPlugin", error);
        errorCallback(playPauseCallback, errorType.toString());
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
				synchronized(OurMusicPlugin.this){
				    if(OurMusicPlugin.this.playerState != null && OurMusicPlugin.this.playerState.playing){
					OurMusicPlugin.this.playerState = state;
					OurMusicPlugin.this.successCallback(
									    OurMusicPlugin.this.playPauseCallback,
									    OurMusicPlugin.this.playerStateToJsonObject(state,"PLAYING"));
				    }
				}
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

    private JSONObject playerStateToJsonObject(PlayerState state, String event) {
        JSONObject json = new JSONObject();
        try {
            json.put("playing", state.playing);
            json.put("trackUri", state.trackUri);
            json.put("positionInMs", state.positionInMs);
            json.put("durationInMs", state.durationInMs);
	    json.put("event", event);
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
