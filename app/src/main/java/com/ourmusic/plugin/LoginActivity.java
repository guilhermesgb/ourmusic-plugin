package com.ourmusic.plugin;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;


public class LoginActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_login);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(OurMusicPlugin.CLIENT_ID,
                AuthenticationResponse.Type.TOKEN, OurMusicPlugin.REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, OurMusicPlugin.REQUEST_CODE_LOGIN_LAUNCH, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case OurMusicPlugin.REQUEST_CODE_LOGIN_LAUNCH:
                if ( resultCode == Activity.RESULT_OK ){
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
                else {
                    setResult(Activity.RESULT_CANCELED, intent);
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, intent);
                break;
        }
    }
}