package com.tealcube.java.games.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.tealcube.java.games.CrappyGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "ChromaDodgeOrSomeCrap";
		config.useGL30 = false;
		config.width = 380;
		config.height = 640;
		new LwjglApplication(new CrappyGame(), config);
	}
}
