package com.tealcube.java.games;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class CrappyGame extends ApplicationAdapter {
    static final int WORLD_WIDTH = 9000;
    static final int WORLD_HEIGHT = 16000;
    static final int MAX_RIGHT_BOUNDS = 6800;
    static final int MAX_LEFT_BOUNDS = 800;

    private float RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
    private float LEFT_BOUNDS = MAX_LEFT_BOUNDS;

    private Texture colorShiftBkg;
    private int shadowcreep = -(((WORLD_WIDTH/4)-3838)/15);
    private int player_x;
    private int player_y;
    private int playerspeed;
    private int bkgShift;
    private int lastRandom;
    private float faderShaderTimer;
    String scoreMessage;
    int score = 0;

    ShapeRenderer shapeRenderer;
    SpriteBatch batch;
    Viewport viewport;
    OrthographicCamera camera;
    BitmapFont font;

    Array<Barrier> barriers = new Array<Barrier>();

    GameState gameState = GameState.Start;

    Music music;


    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new StretchViewport(9000, 16000, camera);
        viewport.apply();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("impact.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 256;
        parameter.characters = "1234567890";
        font = generator.generateFont(parameter);

        player_x = WORLD_WIDTH / 4;
        player_y = WORLD_HEIGHT / 6;

        scoreMessage = "" + score;

        colorShiftBkg = new Texture("shifter.png");

        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
        music.setLooping(true);
        music.play();

        batch = new SpriteBatch();
        resetWorld();
    }


    // Resets the world when you hit retry or w/e
    private void resetWorld() {
        int tempRandom;
        int barrierLoc;

        faderShaderTimer = 0;
        score = 0;
        player_x = WORLD_WIDTH / 4;
        shadowcreep = -((player_x-3838)/15);
        RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
        LEFT_BOUNDS = MAX_LEFT_BOUNDS;
        barriers.clear();

        for (int i = 0; i < 25; i++) {
            tempRandom = lastRandom + MathUtils.random(1, 5);
            if (tempRandom > 5) {
                tempRandom -= 6;
            }
            lastRandom = tempRandom;
            barrierLoc = 8300 + tempRandom * -850;
            barriers.add(new Barrier(barrierLoc, 16000 + i * 5500));
        }
    }


    // Moves Barriers and sets colision bounds
    private void moveBarriers() {
        for (Barrier r : barriers) {
            r.position.y = r.position.y - 50;
            if (!r.counted) {
                // Sets the max X bounding to the current position of the barrier
                // that should be in range of the player now. If the player is
                // 'collided' with the texture, it is because its X value is too
                // high or too low
                if (r.position.y < 4000) {
                    RIGHT_BOUNDS = r.position.x - 140;
                    LEFT_BOUNDS = r.position.x - 3300;
                    // Once it is past the player, it should add one to the score, and
                    // change the counted value. It also resets the bounds to the sides
                    // of the screen. After this step, the barrier does literally
                    // nothing but move downwards until it is cleared.
                    if (r.position.y < 1800) {
                        RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
                        LEFT_BOUNDS = MAX_LEFT_BOUNDS;
                        r.counted = true;
                        score++;
                    }
                }
            }
        }
    }


    // World update. Makes stuff happen.
    private void updateWorld() {
        if (gameState == GameState.MainMenu) {
            playerspeed = 5;
        }

        if (gameState == GameState.Options) {
            playerspeed = 5;
        }

        if (gameState == GameState.Unlocks) {
            playerspeed = 5;
        }
        if (gameState == GameState.Start) {
            // Touch to start the game
            if (Gdx.input.justTouched()) {
                gameState = GameState.Running;
                playerspeed = 50;
                return;
            }
        }

        if (gameState == GameState.Running) {
            //Reverses Player's movement direction
            if (Gdx.input.justTouched()) {
                playerspeed = playerspeed * -1;
            }

            // Moves stuff
            player_x += playerspeed;
            shadowcreep = -((player_x-3838)/15);
            moveBarriers();
            bkgShift += 6;

            // Collision detection. Rather than using squares to detect, the
            // simplicity of the game allows me to merely compare three numbers
            // for psudo collisions.
            if (player_x < LEFT_BOUNDS || player_x > RIGHT_BOUNDS) {
                gameState = GameState.GameOver;
                Gdx.app.log("LOGGGG", "ENTERED GAMEOVERSTATE");
                return;
            }
        }

        if (gameState == GameState.GameOver) {
            // Placeholder until a gameover screen with buttons is completed.
            if (faderShaderTimer >= 1.0F) {
                if (Gdx.input.justTouched()) {
                    gameState = GameState.Start;
                    resetWorld();
                    return;
                }
            }
            if (faderShaderTimer < 1.0F) {
                Gdx.app.log("LOGGGG", "FADER IS NOW AT:" + faderShaderTimer);
                faderShaderTimer += 0.1F;
            }
        }
    }

    // Draw event for the renderer to use.
    private void drawWorld() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
        batch.draw(colorShiftBkg, -bkgShift, -bkgShift, WORLD_WIDTH * 5, WORLD_HEIGHT * 5);
        font.setScale(12, 12);
        font.setColor(0, 0, 0, 0.3F);
        font.drawMultiLine(batch, "" + score, 4500 + shadowcreep, 12900, 0, BitmapFont.HAlignment.CENTER);
        font.setColor(1, 1, 1, 1);
        font.drawMultiLine(batch, "" + score, 4500, 13000, 0, BitmapFont.HAlignment.CENTER);
        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        // Shadow of shapes
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(player_x + shadowcreep, player_y - 100, 1350, 1350);
        shapeRenderer.rect(shadowcreep, 0, 900, 16000);
        shapeRenderer.rect(9000 + shadowcreep, 0, -900, 16000);
        for (Barrier barrier : barriers) {
            shapeRenderer.rect(barrier.position.x + shadowcreep - 9300, barrier.position.y - 100, 6000, 1350);
            shapeRenderer.rect(barrier.position.x + shadowcreep, barrier.position.y - 100, 6000, 1350);
        }

        //Main Shapes
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(player_x, player_y, 1350, 1350);
        shapeRenderer.rect(0, 0, 900, 16000);
        shapeRenderer.rect(9000, 0, -900, 16000);
        for (Barrier barrier : barriers) {
            shapeRenderer.rect(barrier.position.x - 9300, barrier.position.y, 6000, 1350);
            shapeRenderer.rect(barrier.position.x, barrier.position.y, 6000, 1350);
        }
        if (gameState == GameState.GameOver) {
            shapeRenderer.setColor(0, 0, 0, faderShaderTimer / 3);
            shapeRenderer.rect(0, 0, 9000, 16000);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }


    @Override
    public void render() {
        camera.update();

        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        updateWorld();
        drawWorld();
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
    }


    static class Barrier {
        Vector2 position = new Vector2();
        boolean counted;

        public Barrier(int x, int y) {
            this.position.x = x;
            this.position.y = y;
        }
    }

    enum GameState {
        MainMenu, Options, Unlocks, Start, Running, GameOver
    }
}
