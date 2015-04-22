package com.tealcube.java.games.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.tealcube.java.games.CrappyGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
	    LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
	    cfg.title = "ChromaDodgeOrSomeCrap";
	    cfg.useGL30 = false;
	    cfg.width = 380;
	    cfg.height = 640;
	    new LwjglApplication(new CrappyGame(), cfg);
	}
}
