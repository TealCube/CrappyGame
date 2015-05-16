package com.tealcube.java.games.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.tealcube.java.games.AdsController;
import com.tealcube.java.games.CrappyGame;

public class AndroidLauncher extends AndroidApplication implements AdsController {

    private static final String INTERSTITIAL_UNIT_ID = "ca-app-pub-5519384153835422/6795093799";
    private static final String TRACKING_ID = "UA-63024465-1";
    private InterstitialAd interstitialAd;
    private Tracker tracker;

    @Override public boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return (ni != null && ni.isConnected());
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        initialize(new CrappyGame(this), config);
        setupAds();
        tracker = getTracker();
    }

    public void setupAds() {
        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(INTERSTITIAL_UNIT_ID);

        requestInterstitialAd();
    }

    @Override public void showInterstitialAd(final Runnable then) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (!isWifiConnected() || !interstitialAd.isLoaded()) {
                    return;
                }
                if (then != null) {
                    interstitialAd.setAdListener(new AdListener() {
                        @Override public void onAdClosed() {
                            Gdx.app.postRunnable(then);
                            AdRequest.Builder builder = new AdRequest.Builder();
                            AdRequest ad = builder.build();
                            interstitialAd.loadAd(ad);
                        }
                    });
                }
                interstitialAd.show();
            }
        });
    }

    @Override public void requestInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("YOUR_DEVICE_HASH").build();

        interstitialAd.loadAd(adRequest);
    }

    private synchronized Tracker getTracker() {
        if (tracker == null) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            tracker = analytics.newTracker(TRACKING_ID);
            tracker.enableAdvertisingIdCollection(true);
            tracker.setSessionTimeout(300L);
            tracker.enableAutoActivityTracking(true);
        }
        return tracker;
    }

}
