package com.tealcube.java.games;

import com.badlogic.gdx.math.Vector2;

public class Barrier {

    public Vector2 position = new Vector2();
    public boolean counted;
    public boolean activated;

    public Barrier(int x, int y) {
        this.position.x = x;
        this.position.y = y;
    }

}
