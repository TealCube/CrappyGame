package com.tealcube.java.games;

public interface AdsController {
    boolean isWifiConnected();

    void showInterstitialAd(Runnable then);
    // vfrgvcdfrgbfd

    void requestInterstitialAd();
}