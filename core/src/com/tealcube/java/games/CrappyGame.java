package com.tealcube.java.games;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
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
    static final int BASE_PLAYER_SPEED = 48;
    static final int BASE_BARRIER_SPEED = 48;
    static final int WORLD_WIDTH = 9000;
    static final int WORLD_HEIGHT = 16000;
    static final int MAX_RIGHT_BOUNDS = 6800;
    static final int MAX_LEFT_BOUNDS = 800;
    static final int PLAYER_SCALE = 1350;
    static Preferences preferences;

    private float RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
    private float LEFT_BOUNDS = MAX_LEFT_BOUNDS;

    private Texture colorShiftBkg;
    private Texture TClogo;
    private Texture retry;
    private Texture effects;
    private Sound TCload;
    private Music music;


    private float shadowcreep;
    private int player_x;
    private int player_y;
    private int playerspeed;
    private int barrierspeed;
    private int bkgShift;
    private int lastRandom;
    private int lastBarrier;
    private float splashTimer;
    private float faderShaderTimer;
    int highscore = 0;
    int score = 0;

    GameState gameState;
    ShapeRenderer shapeRenderer;
    SpriteBatch batch;
    Viewport viewport;
    OrthographicCamera camera;
    BitmapFont font;

    Array<Barrier> barriers = new Array<Barrier>();
    Array<Circlez> circles = new Array<Circlez>();


    @Override
    public void create() {
        gameState = GameState.TCSplash;
        splashTimer = 1;
        camera = new OrthographicCamera();
        viewport = new StretchViewport(9000, 16000, camera);
        viewport.apply();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("impact.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 256;
        parameter.characters = "1234567890";
        font = generator.generateFont(parameter);

        player_y = WORLD_HEIGHT / 6;

        colorShiftBkg = new Texture("shifter.png");
        TClogo = new Texture("TClogo.png");
        effects = new Texture("bkgcircle.png");
        retry = new Texture("retry.png");

        TCload = Gdx.audio.newSound(Gdx.files.internal("TCload.wav"));
        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
        music.setLooping(true);

        for (int i = 0; i < 25; i++) {
            circles.add(new Circlez(MathUtils.random(-2000,8400), MathUtils.random(0, 18000), MathUtils.random(5,30), MathUtils.random(800,3000)));
        }

        preferences = Gdx.app.getPreferences("ChromaDodge");

        if (!preferences.contains("highScore")) {
            preferences.putInteger("highScore", 0);
        }

        batch = new SpriteBatch();
        resetWorld();
    }


    // Grab screen adjusted X value
    private float grabX() {
        int width = Gdx.graphics.getWidth();
        float x = Gdx.input.getX();
        x = 9000 * (x / (float) width);
        Gdx.app.log("[INFO]", "CLICKED X: " + x);
        return x;
    }

    // Grab screen adjusted Y value
    private float grabY() {
        int height = Gdx.graphics.getHeight();
        float y = Gdx.input.getY();
        y = 16000-(16000 * (y / (float) height));
        Gdx.app.log("[INFO]", "CLICKED Y: " + y);
        return y;
    }


    // Changes the saved highscore.
    public static void setHighScore(int val) {
        preferences.putInteger("highScore", val);
        preferences.flush();
    }

    // Gets the highscore, if you hadn't figured that out.
    public static int getHighScore() {
        return preferences.getInteger("highScore");
    }


    // Resets the world when you hit retry or w/e
    private void resetWorld() {
        int tempRandom;
        int barrierLoc;

        highscore = getHighScore();
        score = 0;
        playerspeed = BASE_PLAYER_SPEED;
        barrierspeed = BASE_BARRIER_SPEED;

        player_x = WORLD_WIDTH/2 - PLAYER_SCALE/2;
        player_y = WORLD_HEIGHT / 6;

        shadowcreep = -100 + (((float) score/(Math.max((float) highscore, 25F)))*200);
        RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
        LEFT_BOUNDS = MAX_LEFT_BOUNDS;
        barriers.clear();

        for (int i = 0; i < 10; i++) {
            tempRandom = lastRandom + MathUtils.random(1, 5);
            if (tempRandom > 5) {
                tempRandom -= 6;
            }
            lastRandom = tempRandom;
            barrierLoc = 8400 + tempRandom * -850;
            barriers.add(new Barrier(barrierLoc, 16100 + i * 5500));
            lastBarrier = 16000 + i * 5500;
        }
    }


    // Moves Barriers and sets colision bounds
    private void moveBarriers() {
        lastBarrier -= barrierspeed;
        for (Barrier r : barriers) {
            r.position.y -= barrierspeed;
            if (!r.counted) {
                // Sets the max X bounding to the current position of the barrier
                // that should be in range of the player now. If the player is
                // 'collided' with the texture, it is because its X value is too
                // high or too low
                if (r.position.y <= player_y+1350) {
                    RIGHT_BOUNDS = r.position.x - 1320;
                    LEFT_BOUNDS = r.position.x - 3250;
                    // Once it is past the player, it should add one to the score, and
                    // change the counted value. It also resets the bounds to the sides
                    // of the screen. After this step, the barrier does literally
                    // nothing but move downwards until it is cleared.
                    if (r.position.y <= (player_y-1350)) {
                        RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
                        LEFT_BOUNDS = MAX_LEFT_BOUNDS;
                        r.counted = true;
                        score++;
                        shadowcreep = -100 + (((float) score/(Math.max((float) highscore, 25F)))*200);
                        //barrierspeed = (score % 3 == 0 ? barrierspeed + 5 : barrierspeed);
                        //playerspeed = (score % 4 == 0 ? playerspeed + 5 : playerspeed);
                    }
                }
            } else {
                if (r.position.y <= -1350) {
                    Gdx.app.log("[INFO]", "LAST BARRIER WAS AT: " + lastBarrier);
                    r.position.y = lastBarrier + 5500;
                    lastBarrier += 5500;
                    Gdx.app.log("[INFO]", "MOVED LOWEST BARRIER TO: " + lastBarrier);
                    int tempRandom = lastRandom + MathUtils.random(1, 5);
                    if (tempRandom > 5) {
                        tempRandom -= 6;
                    }
                    lastRandom = tempRandom;
                    r.position.x = 8300 + tempRandom * -850;
                    r.counted = false;
                }
            }
        }
    }


    // Moves Barriers and sets colision bounds
    private void moveCircles() {
        for (Circlez r : circles) {
            if (gameState == GameState.Start) {
                r.position.y = r.position.y - (r.speed/2);
            } else {
                r.position.y = r.position.y - r.speed;
            }
            if (r.position.y < -3000) {
                r.position.y = 19000;
                r.position.x = -2000 + MathUtils.random(0, 8400);
                r.scale = MathUtils.random(800,3000);
                r.speed = MathUtils.random(5,30);
            }
        }

    }


    // World update. Makes stuff happen.
    private void updateWorld() {
        if (gameState == GameState.TCSplash) {
            if (splashTimer < 90) {
                splashTimer++;
                if (splashTimer == 10) {
                    TCload.play();
                }
            } else {
                gameState = GameState.Start;
                TClogo.dispose();
                TCload.dispose();
                //music.play();
            }
        }

        if (gameState == GameState.Start) {
            moveCircles();
            if (faderShaderTimer > 0.0F) {
                faderShaderTimer -= 0.1F;
                if (faderShaderTimer < 0.0F) {
                    faderShaderTimer = 0.0F;
                }
            } else if (Gdx.input.justTouched()) {
                gameState = GameState.Running;
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
            moveBarriers();
            moveCircles();
            bkgShift += 6;

            // Collision detection. Rather than using squares to detect, the
            // simplicity of the game allows me to merely compare three numbers
            // for psudo collisions.
            if (player_x < LEFT_BOUNDS || player_x > RIGHT_BOUNDS) {
                //music.stop();
                Gdx.app.log("[INFO]", "SCORE: " + score);
                Gdx.app.log("[INFO]", "HIGHSCORE: " + highscore);
                if (score > highscore) {
                    Gdx.app.log("[INFO]", "NEW HIGHSCORE: " + highscore + "-> " + score);
                    setHighScore(score);
                }
                gameState = GameState.GameOver;
                return;
            }
        }

        if (gameState == GameState.GameOver) {
            moveBarriers();
            moveCircles();
            player_y -= BASE_BARRIER_SPEED;
            if (faderShaderTimer >= 1.0F) {
                if (Gdx.input.justTouched()) {
                    float x = grabX();
                    float y = grabY();

                    // Replay Button
                    if (x > 1500 && x < 7500 && y > 7000 && y < 9200) {
                        gameState = GameState.Start;
                        //music.play();
                        resetWorld();
                        return;
                    }

                    // Facebook share button

                    // Tweet button

                    // Main Menu Button
                    //if (x > 1500 && x < 7500 && y > 3000 && y < 5000) {
                    //    resetWorld();
                    //    gameState = GameState.MainMenu;
                    //    return;
                    //}
                }
            }
            if (faderShaderTimer < 1.0F) {
                faderShaderTimer += 0.1F;
                if (faderShaderTimer > 1.0F) {
                    faderShaderTimer = 1.0F;
                }
            }
        }
    }


    private void drawSplash() {
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 1);
        shapeRenderer.rect(0, 0, 9000, 16000);
        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
        Color c = batch.getColor();
        batch.setColor(c.r, c.g, c.b, 1f);
        if (splashTimer < 10) {
            batch.setColor(c.r, c.g, c.b, splashTimer/10);
        }
        batch.draw(TClogo, 3000, 6500, 3000, 3000);
        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }


    private void drawMainMenu() {
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 1);
        shapeRenderer.rect(0, 0, 9000, 16000);
        shapeRenderer.end();
    }


    private void drawGameplay() {
        // Set up batch
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        // Draw background (includes text)
        batch.draw(colorShiftBkg, -bkgShift, -bkgShift, WORLD_WIDTH * 5, WORLD_HEIGHT * 5);
        for (Circlez circle : circles) {
            batch.draw(effects, circle.position.x, circle.position.y, circle.scale, circle.scale);
        }

        // Draws score text that's part of the background
        if (faderShaderTimer < 1) {
            font.setScale(12, 12);
            font.setColor(0, 0, 0, 0.3F);
            font.drawMultiLine(batch, "" + score, 4500 + shadowcreep / 2, 12940, 0, BitmapFont.HAlignment.CENTER);
            font.setColor(1, 1, 1, 1);
            font.drawMultiLine(batch, "" + score, 4500, 13000, 0, BitmapFont.HAlignment.CENTER);
        }

        // End batch. Disable Blend.
        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        //Setup ShapeRenderer
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        // Shadow of shapes
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(player_x + shadowcreep, player_y - 100, PLAYER_SCALE, PLAYER_SCALE);
        shapeRenderer.rect(shadowcreep, 0, 900, 16000);
        shapeRenderer.rect(9000 + shadowcreep, 0, -900, 16000);
        for (Barrier barrier : barriers) {
            shapeRenderer.rect(barrier.position.x + shadowcreep - 9300, barrier.position.y - 100, 6000, PLAYER_SCALE);
            shapeRenderer.rect(barrier.position.x + shadowcreep, barrier.position.y - 100, 6000, PLAYER_SCALE);
        }

        //Main Shapes
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(player_x, player_y, PLAYER_SCALE, PLAYER_SCALE);
        shapeRenderer.rect(0, 0, 900, 16000);
        shapeRenderer.rect(9000, 0, -900, 16000);
        for (Barrier barrier : barriers) {
            shapeRenderer.rect(barrier.position.x - 9300, barrier.position.y, 6000, PLAYER_SCALE);
            shapeRenderer.rect(barrier.position.x, barrier.position.y, 6000, PLAYER_SCALE);
        }
        if (gameState == GameState.GameOver) {
            shapeRenderer.setColor(0, 0, 0, faderShaderTimer / 3);
            shapeRenderer.rect(0, 0, 9000, 16000);
        }

        // End ShapeRenderer. Disable Blend.
        shapeRenderer.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }


    private void drawGameOver() {
        shapeRenderer = new ShapeRenderer();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Scoring feild background
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1500, 9400 - (14300 * (1 - faderShaderTimer)), 6000, 4900);
        shapeRenderer.setColor(0.8F, 0.8F, 0.8F, 1);
        shapeRenderer.rect(1800, 10700 - (14300 * (1 - faderShaderTimer)), 5400, 3300);
        shapeRenderer.rect(2200, 9800 - (14300 * (1 - faderShaderTimer)), 5000, 500);
        shapeRenderer.circle(2150, 10050 - (14300 * (1 - faderShaderTimer)), 450);

        // Retry button background
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1500, 7050 - (14300 * (1 - faderShaderTimer)), 6000, 2200);

        // Twitter button background
        shapeRenderer.setColor(0.9F, 0.9F, 1F, 1);
        shapeRenderer.rect(1500, 4700 - (14300 * (1 - faderShaderTimer)), 2850, 2200);

        // Facebook button background
        shapeRenderer.setColor(0.7F, 0.7F, 1F, 1);
        shapeRenderer.rect(4650, 4700 - (14300 * (1 - faderShaderTimer)), 2850, 2200);

        // Main Menu Button
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1500, 2350 - (14300 * (1 - faderShaderTimer)), 6000, 2200);


        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        if (faderShaderTimer > 0) {
            font.setScale(12, 12);
            font.setColor(1, 1, 1, 1);
            font.drawMultiLine(batch, "" + score, 4500,13500-(14000*(1-faderShaderTimer)), 0, BitmapFont.HAlignment
                .CENTER);
        }

        batch.draw(retry, 2400, 7200-(14300*(1-faderShaderTimer)), 4000, 1700);

        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }


    // Draw event for the renderer to use.
    private void mainDraw() {
        if (gameState == GameState.GameOver || gameState == GameState.Running || gameState == GameState.Start) {
            drawGameplay();
            if (gameState != GameState.Running) {
                drawGameOver();
            }
            return;
        }

        if (gameState == GameState.TCSplash) {
            drawSplash();
            return;
        }

        if (gameState == GameState.MainMenu) {
            drawMainMenu();
        }
    }


    @Override
    public void render() {
        camera.update();

        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        updateWorld();
        mainDraw();
    }


    @Override
    public void dispose() {
        TClogo.dispose();
        effects.dispose();
        retry.dispose();
        colorShiftBkg.dispose();

        music.dispose();
        TCload.dispose();

        font.dispose();
        batch.dispose();
        shapeRenderer.dispose();
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


    static class Circlez {
        Vector2 position = new Vector2();
        int speed;
        float scale;

        public Circlez(int x, int y, int z, int a) {
            this.position.x = x;
            this.position.y = y;
            this.speed = z;
            this.scale = a;
        }
    }

    enum GameState {
        TCSplash, MainMenu, Options, Unlocks, Start, Running, GameOver
    }
}
