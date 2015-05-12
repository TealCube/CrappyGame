package com.tealcube.java.games;

public interface FacebookService {

    boolean isLoggedIn();

    void login();

    void logout();

    void postBrag();

    boolean isAppInstalled();

}
