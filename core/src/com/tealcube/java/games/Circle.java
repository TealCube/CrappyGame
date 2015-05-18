package com.tealcube.java.games;

import com.badlogic.gdx.math.Vector2;

public class Circle {
    public Vector2 position = new Vector2();
    public int speed;
    public float scale;

    public Circle(int x, int y, int z, int a) {
        this.position.x = x;
        this.position.y = y;
        this.speed = z;
        this.scale = a;
    }
}
