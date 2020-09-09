package com.tealcube.chromadodge;

public interface AdsController {
    boolean isWifiConnected();

    void showInterstitialAd(Runnable then);
    // vfrgvcdfrgbfd

    void requestInterstitialAd();
}