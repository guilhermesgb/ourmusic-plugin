package com.ourmusic.plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.net.Uri;
import android.view.Window;
import android.util.Log;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;


public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(OurMusicPlugin.CLIENT_ID,
                AuthenticationResponse.Type.TOKEN, OurMusicPlugin.REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginInBrowser(this, request);
    }

    @Override
    protected void onNewIntent(Intent intent) {
	super.onNewIntent(intent);
    
	Uri uri = intent.getData();
	Log.i("OurMusicPlugin",uri.toString());
	if (uri != null) {
	    AuthenticationResponse response = AuthenticationResponse.fromUri(uri);
Log.i("OurMusicPlugin4", response.getType().toString() + " - " + AuthenticationResponse.Type.TOKEN.toString()) ;        
	    switch (response.getType()) {
            case TOKEN:
		Intent response_intent = new Intent();
		response_intent.putExtra("response",response);
		setResult(Activity.RESULT_OK, response_intent);
		finish();
                break;
		
            default:
		Intent response_intent = new Intent();
		response_intent.putExtra("response",response);
		setResult(Activity.RESULT_OK, response_intent);
		finish();
		break;
	    }
	}
    }
}
