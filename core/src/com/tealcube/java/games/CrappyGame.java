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
    private static final int BASE_PLAYER_SPEED = 42;
    private static final int BASE_BARRIER_SPEED = 48;
    private static final int WORLD_WIDTH = 9000;
    private static final int WORLD_HEIGHT = 16000;
    private static final int MAX_RIGHT_BOUNDS = 6800;
    private static final int MAX_LEFT_BOUNDS = 800;
    private static final int PLAYER_SCALE = 1350;
    private static Preferences preferences;

    private float RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
    private float LEFT_BOUNDS = MAX_LEFT_BOUNDS;

    private Texture colorShiftBkg;
    private Texture TClogo;
    private Texture square;
    private Texture shadow;
    private Texture effects;
    private Sound TCload;
    private Music music1;
    private Music music2;
    private Music music3;
    private Music menumusic;
    private Music gameovermusic;

    private float shadowcreep;
    private int player_x;
    private int player_y;
    private int playerspeed;
    private int barrierspeed;
    private int bkgShift;
    private int lastRandom;
    private int lastBarrier;
    private int rotator;
    private int track;
    private float splashTimer;
    private float faderShaderTimer;
    private int highscore = 0;
    private int score = 0;
    private boolean ads = true;

    private GameState gameState;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private Viewport viewport;
    private OrthographicCamera camera;
    private BitmapFont font;

    private Array<Barrier> barriers = new Array<Barrier>();
    private Array<Circlez> circles = new Array<Circlez>();

    private AdsController adsController;

    public CrappyGame(AdsController adsController){
        if (adsController != null) {
            this.adsController = adsController;
        } else {
            ads = false;
        }
    }


    @Override
    public void create() {
        gameState = GameState.SPLASH;
        splashTimer = 1;
        camera = new OrthographicCamera();
        viewport = new StretchViewport(9000, 16000, camera);
        viewport.apply();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Fjalla.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 256;
        parameter.characters = "aBbcCdDEefGgHhIijklmMNnOoPpqrRSsTtWuvy1234567890:!";
        font = generator.generateFont(parameter);

        player_y = WORLD_HEIGHT / 6;

        // Splash Assets
        TClogo = new Texture("TClogo.png");
        TCload = Gdx.audio.newSound(Gdx.files.internal("TCload.wav"));
        TCload.setVolume(0, 0.5F);

        // Gameplay Assets
        colorShiftBkg = new Texture("shifter.png");
        effects = new Texture("bkgcircle.png");

        // Menu Assets
        square = new Texture("square.png");
        shadow = new Texture("shadow.png");

        menumusic = Gdx.audio.newMusic(Gdx.files.internal("odyssey.mp3"));
        music1 = Gdx.audio.newMusic(Gdx.files.internal("bensound-memories.mp3"));
        music2 = Gdx.audio.newMusic(Gdx.files.internal("dream-culture.mp3"));
        music3 = Gdx.audio.newMusic(Gdx.files.internal("bensound-goinghigher.mp3"));
        gameovermusic = Gdx.audio.newMusic(Gdx.files.internal("easy-lemon.mp3"));

        menumusic.setVolume(0.8F);
        music1.setVolume(0.7F);
        music1.setVolume(0.7F);
        music1.setVolume(0.7F);
        gameovermusic.setVolume(0.9F);

        menumusic.setLooping(true);
        music1.setLooping(true);
        music2.setLooping(true);
        music3.setLooping(true);

        for (int i = 0; i < 25; i++) {
            circles.add(new Circlez(MathUtils.random(-2000,8400), MathUtils.random(0, 18000), MathUtils.random(5,30), MathUtils.random(800,3000)));
        }

        preferences = Gdx.app.getPreferences("ChromaDodge");

        if (!preferences.contains("highscore")) {
            preferences.putInteger("highscore", 0);
        }
        if (!preferences.contains("music")) {
            preferences.putInteger("music", 0);
        }
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
        preferences.putInteger("highscore", val);
        preferences.flush();
    }

    // Gets the highscore, if you hadn't figured that out.
    public static int getHighScore() {
        return preferences.getInteger("highscore");
    }

    // Changes the saved track.
    public static void setMusic(int val) {
        preferences.putInteger("music", val);
        preferences.flush();
    }

    // Gets the music, if you hadn't figured that out.
    public static int getMusic() {
        return preferences.getInteger("music");
    }

    // Resets the world when you hit retry or w/e
    private void resetWorld() {
        int tempRandom;
        int barrierLoc;

        track = getMusic();
        highscore = getHighScore();
        score = 0;
        playerspeed = BASE_PLAYER_SPEED;
        barrierspeed = BASE_BARRIER_SPEED;

        player_x = WORLD_WIDTH/2 - PLAYER_SCALE/2;
        player_y = WORLD_HEIGHT / 6;

        bkgShift = 0;
        shadowcreep = -100;

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
            barriers.add(new Barrier(barrierLoc, 16100 + i * 5750));
            lastBarrier = 16000 + i * 5750;
        }
    }

    // DANCE, PLAYER, DANCE!!
    private void movePlayer() {
        if (Gdx.input.justTouched()) {
            playerspeed = playerspeed * -1;
        }
        player_x += playerspeed;
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
                    LEFT_BOUNDS = r.position.x - 3300;
                    // Once it is past the player, it should add one to the score, and
                    // change the counted value. It also resets the bounds to the sides
                    // of the screen. After this step, the barrier does literally
                    // nothing but move downwards until it is cleared.
                    if (r.position.y <= (player_y-1350)) {
                        RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
                        LEFT_BOUNDS = MAX_LEFT_BOUNDS;
                        r.counted = true;
                        score++;
                        if (score < (Math.max(highscore, 25F))) {
                            shadowcreep = -100 + (((float) score / (Math.max((float) highscore, 25F))) * 200);
                        } else {
                            shadowcreep = 100;
                        }
                        //barrierspeed = (score % 3 == 0 ? barrierspeed + 5 : barrierspeed);
                        //playerspeed = (score % 4 == 0 ? playerspeed + 5 : playerspeed);
                    }
                }
            }
            if (r.position.y <= -1350) {
                Gdx.app.log("[INFO]", "LAST BARRIER WAS AT: " + lastBarrier);
                r.position.y = lastBarrier + 5750;
                lastBarrier += 5750;
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

    // Moves Barriers and sets colision bounds
    private void moveCircles() {
        for (Circlez r : circles) {
            if (gameState == GameState.START || gameState == GameState.MAIN_MENU || gameState == GameState.OPTIONS) {
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

    // Handles the splash opening
    private void doSplash() {
        if (splashTimer < 90) {
            splashTimer++;
            if (splashTimer == 10) {
                TCload.play();
            }
        } else {
            gameState = GameState.MAIN_MENU;
            TClogo.dispose();
            TCload.dispose();
            menumusic.play();
        }
    }

    // Check to see if you lose/set highscore
    private void checkGameOver() {
        // Left and right bounds are editedby barriers - this checks and sets highscore if collision
        if (player_x < LEFT_BOUNDS || player_x > RIGHT_BOUNDS) {
            //music.stop();
            Gdx.app.log("[INFO]", "SCORE: " + score);
            Gdx.app.log("[INFO]", "HIGHSCORE: " + highscore);
            if (score > highscore) {
                Gdx.app.log("[INFO]", "NEW HIGHSCORE: " + highscore + "-> " + score);
                setHighScore(score);
            }
            switch (track) {
                case 0: music1.stop();
                    break;
                case 1: music2.stop();
                    break;
                case 2: music3.stop();
                    break;
            }
            gameovermusic.play();
            gameState = GameState.GAME_OVER;
            if (ads) {
                if(adsController.isWifiConnected()) {
                    adsController.showBannerAd();
                }
            }
        }
    }

    // World update. Makes stuff happen.
    private void updateWorld() {
        rotator++;
        if (rotator == 360) {
            rotator = 0;
        }
        if (gameState == GameState.SPLASH) {
            doSplash();
            return;
        }

        if (gameState == GameState.MAIN_MENU) {
            shadowcreep = 100;
            moveCircles();
            if (Gdx.input.justTouched()) {
                float x = grabX();
                float y = grabY();
                // Play Button 1500, 5000, 6000, 2200
                if (x > 1500 && x < 7500 && y > 6900 && y < 8700) {
                    gameState = GameState.START;
                    menumusic.stop();
                    resetWorld();
                    return;
                }
                // Options button 1500, 4750, 6000, 1800
                if (x > 1500 && x < 7500 && y > 4750 && y < 6550) {
                    gameState = GameState.OPTIONS;
                    return;
                }
                // Remove Ads Button

            }
            if (faderShaderTimer > 0.0F) {
                faderShaderTimer -= 0.1F;
                if (faderShaderTimer < 0.0F) {
                    faderShaderTimer = 0.0F;
                }
            }
        }

        if (gameState == GameState.OPTIONS) {
            shadowcreep = 100;
            moveCircles();
            if (Gdx.input.justTouched()) {
                float x = grabX();
                float y = grabY();
                // TRACK1
                if (x > 1500 && x < 7500 && y > 6900 && y < 8400) {
                    track = 0;
                    setMusic(0);
                    menumusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    music1.play();
                    return;
                }
                // TRACK2
                if (x > 1500 && x < 7500 && y > 5250 && y < 6750) {
                    track = 1;
                    setMusic(1);
                    menumusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    music2.play();
                    return;
                }
                // TRACK3
                if (x > 1500 && x < 7500 && y > 3600 && y < 5100) {
                    track = 2;
                    setMusic(2);
                    menumusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    music3.play();
                    return;
                }
                // BACK1
                if (x > 1500 && x < 7500 && y > 1950 && y < 3450) {
                    gameState = GameState.MAIN_MENU;
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    menumusic.play();
                    return;
                }
            }
        }

        // shapeRenderer.rect(1500, 6900, 6000, 1500);
        // shapeRenderer.rect(1500, 5250, 6000, 1500);
        // shapeRenderer.rect(1500, 3600, 6000, 1500);
        // shapeRenderer.rect(1500, 1950, 6000, 1500);
        if (gameState == GameState.START) {
            moveCircles();
            if (faderShaderTimer > 0.0F) {
                faderShaderTimer -= 0.1F;
                if (faderShaderTimer < 0.0F) {
                    faderShaderTimer = 0.0F;
                }
            } else if (Gdx.input.justTouched()) {
                gameState = GameState.RUNNING;
                switch (track) {
                    case 0: music1.play();
                        break;
                    case 1: music2.play();
                        break;
                    case 2: music3.play();
                        break;
                }
                return;
            }
        }

        if (gameState == GameState.RUNNING) {
            movePlayer();
            moveBarriers();
            moveCircles();
            bkgShift += 8;
            if (bkgShift > 150000) {
                bkgShift = -15000;
            }
            checkGameOver();
        }

        if (gameState == GameState.GAME_OVER) {
            if (bkgShift > 1) {
                bkgShift-= 150+(bkgShift/25);
            }
            moveBarriers();
            moveCircles();
            player_y -= barrierspeed;
            if (faderShaderTimer >= 1.0F) {
                if (Gdx.input.justTouched()) {
                    float x = grabX();
                    float y = grabY();

                    // Replay Button
                    if (x > 1500 && x < 7500 && y > 6050 && y < 8250) {
                        if (ads) {adsController.hideBannerAd();}
                        gameState = GameState.START;
                        gameovermusic.stop();
                        resetWorld();
                        return;
                    }

                    // Main Menu Button
                    if (x > 1500 && x < 7500 && y > 3700 && y < 5900) {
                        if (ads) {adsController.hideBannerAd();}
                        gameState = GameState.MAIN_MENU;
                        menumusic.play();
                        gameovermusic.stop();
                        resetWorld();
                        return;
                    }
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

        batch = new SpriteBatch();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        Color c = batch.getColor();
        batch.setColor(c.r, c.g, c.b, 1f);
        if (splashTimer < 10) {
            batch.setColor(c.r, c.g, c.b, splashTimer / 10);
        }
        batch.draw(TClogo, 3000, 6500, 3000, 3000);
        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }

    private void drawMainMenu() {
        shapeRenderer.dispose();
        shapeRenderer = new ShapeRenderer();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(1500 + shadowcreep, 6900 - shadowcreep, 6000, 1800);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1500, 6900, 6000, 1800);

        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(1500 + shadowcreep, 4750 - shadowcreep, 6000, 1800);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1500, 4750, 6000, 1800);

        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(2500 + shadowcreep, 2600 - shadowcreep, 4000, 1800);
        shapeRenderer.setColor(0.95F, 0.95F, 0.6F, 1);
        shapeRenderer.rect(2500, 2600, 4000, 1800);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        batch.dispose();
        batch = new SpriteBatch();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        font.setScale(8, 8);
        font.setColor(0, 0, 0, 0.3F);
        font.draw(batch, "Chroma", 1190, 13300);
        font.draw(batch, "Dodge", 3120, 11400);
        font.setColor(1, 1, 1, 1);
        font.draw(batch, "Chroma", 1090, 13400);
        font.draw(batch, "Dodge", 3020, 11500);

        font.setScale(5, 5);
        font.setColor(0, 0, 0, 0.4F);
        font.draw(batch, "Play", 3530, 8380);
        font.draw(batch, "Options", 2710, 6250);

        batch.draw(shadow, 2040 + 100, 10570 - 100, 2, 2, 4, 4, 300, 300, rotator, 0, 0, 4, 4, false, false);
        batch.draw(square, 2040, 10570, 2, 2, 4, 4, 300, 300, rotator, 0, 0, 4, 4, false, false);

        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }

    private void drawOptions() {
        shapeRenderer.dispose();
        shapeRenderer = new ShapeRenderer();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // sound1
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(1500 + shadowcreep, 6900 - shadowcreep, 6000, 1500);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 0) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(1500, 6900, 6000, 1500);

        // sound2
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(1500 + shadowcreep, 5250 - shadowcreep, 6000, 1500);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 1) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(1500, 5250, 6000, 1500);

        // sound3
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(1500 + shadowcreep, 3600 - shadowcreep, 6000, 1500);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 2) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(1500, 3600, 6000, 1500);

        //main menu button
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(1500 + shadowcreep, 1950 - shadowcreep, 6000, 1500);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1500, 1950, 6000, 1500);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        batch.dispose();
        batch = new SpriteBatch();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        font.setScale(8, 8);
        font.setColor(0, 0, 0, 0.3F);
        font.draw(batch, "Chroma", 1190, 13300);
        font.draw(batch, "Dodge", 3120, 11400);
        font.setColor(1, 1, 1, 1);
        font.draw(batch, "Chroma", 1090, 13400);
        font.draw(batch, "Dodge", 3020, 11500);

        font.setScale(5, 5);
        font.setColor(0, 0, 0, 0.4F);
        font.draw(batch, "Track 1", 2850, 8200);
        font.draw(batch, "Track 2", 2850, 6550);
        font.draw(batch, "Track 3", 2850, 4900);
        font.draw(batch, "Back", 3430, 3250);

        batch.draw(shadow, 2040 + 100, 10570 - 100, 2, 2, 4, 4, 300, 300, rotator, 0, 0, 4, 4, false, false);
        batch.draw(square, 2040, 10570, 2, 2, 4, 4, 300, 300, rotator, 0, 0, 4, 4, false, false);

        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }

    private void drawGameplay() {
        batch.dispose();
        batch = new SpriteBatch();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        // Draw background (includes text)
        batch.draw(colorShiftBkg, 0, -bkgShift, WORLD_WIDTH, WORLD_HEIGHT * 10);
        for (Circlez circle : circles) {
            batch.draw(effects, circle.position.x, circle.position.y, circle.scale, circle.scale);
        }

        // Draws score text that's part of the background
        if (gameState != GameState.MAIN_MENU && gameState != GameState.OPTIONS) {
            if (faderShaderTimer < 1) {
                font.setScale(12, 12);
                font.setColor(0, 0, 0, 0.3F);
                font.drawMultiLine(batch, "" + score, 4500 + shadowcreep, 12900, 0, BitmapFont.HAlignment.CENTER);
                font.setColor(1, 1, 1, 1);
                font.drawMultiLine(batch, "" + score, 4500, 13000, 0, BitmapFont.HAlignment.CENTER);
            }
        }

        // End batch. Disable Blend.
        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        //Setup ShapeRenderer
        shapeRenderer.dispose();
        shapeRenderer = new ShapeRenderer();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        // Shadow of shapes
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        if (gameState != GameState.MAIN_MENU && gameState != GameState.OPTIONS) {
            shapeRenderer.rect(player_x + shadowcreep, player_y - 100, PLAYER_SCALE, PLAYER_SCALE);
        }
        shapeRenderer.rect(shadowcreep, 0, 900, 16000);
        shapeRenderer.rect(9000 + shadowcreep, 0, -900, 16000);
        for (Barrier barrier : barriers) {
            shapeRenderer.rect(barrier.position.x + shadowcreep - 9300, barrier.position.y - 100, 6000, PLAYER_SCALE);
            shapeRenderer.rect(barrier.position.x + shadowcreep, barrier.position.y - 100, 6000, PLAYER_SCALE);
        }

        //Main Shapes
        shapeRenderer.setColor(1, 1, 1, 1);
        if (gameState != GameState.MAIN_MENU && gameState != GameState.OPTIONS) {
            shapeRenderer.rect(player_x, player_y, PLAYER_SCALE, PLAYER_SCALE);
        }
        shapeRenderer.rect(0, 0, 900, 16000);
        shapeRenderer.rect(9000, 0, -900, 16000);
        for (Barrier barrier : barriers) {
            shapeRenderer.rect(barrier.position.x - 9300, barrier.position.y, 6000, PLAYER_SCALE);
            shapeRenderer.rect(barrier.position.x, barrier.position.y, 6000, PLAYER_SCALE);
        }

        // End ShapeRenderer. Disable Blend.
        shapeRenderer.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }

    private void drawGameOver() {
        shapeRenderer.dispose();
        shapeRenderer = new ShapeRenderer();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Shadow when in gameover
        shapeRenderer.setColor(0, 0, 0, faderShaderTimer / 3);
        shapeRenderer.rect(0, 0, 9000, 16000);

        // Scoring feild background
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1500, 8400 - (14300 * (1 - faderShaderTimer)), 6000, 4900);
        shapeRenderer.setColor(0.7F, 0.7F, 0.7F, 1);
        shapeRenderer.rect(1800, 9700 - (14300 * (1 - faderShaderTimer)), 5400, 3300);
        shapeRenderer.rect(2200, 8660 - (14300 * (1 - faderShaderTimer)), 5000, 780);
        shapeRenderer.circle(2150, 9050 - (14300 * (1 - faderShaderTimer)), 450);

        // Retry button background
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1500, 6050 - (14300 * (1 - faderShaderTimer)), 6000, 2200);

        // Main Menu Button
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1500, 3700 - (14300 * (1 - faderShaderTimer)), 6000, 2200);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        batch.dispose();
        batch = new SpriteBatch();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        font.setScale(6, 6);
        font.setColor(0, 0, 0, 0.4F);
        font.draw(batch, "Retry", 2970, 7850 - (14000 * (1 - faderShaderTimer)));
        font.draw(batch, "Back", 3120, 5500 - (14000 * (1 - faderShaderTimer)));
        font.setScale(2.3F, 2.3F);
        font.setColor(1, 1, 1, 1);
        if (score <= highscore){
            font.draw(batch, "Highscore: " + highscore, 2720, 9310 - (14000 * (1 - faderShaderTimer)));
        } else {
            font.draw(batch, "NEW HIGHSCORE!", 2720, 9310 - (14000 * (1 - faderShaderTimer)));
        }

        batch.draw(square, 2130, 9050 - (14000 * (1 - faderShaderTimer)), 2, 2, 4, 4, 130, 130, rotator, 0, 0, 4, 4,
                   false, false);

        if (faderShaderTimer > 0) {
            font.setScale(12, 12);
            font.setColor(1, 1, 1, 1);
            font.drawMultiLine(batch, "" + score, 4500,12600-(14000*(1-faderShaderTimer)), 0, BitmapFont.HAlignment
                .CENTER);
        }

        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }

    // Draw event for the renderer to use.
    private void mainDraw() {
        if (gameState != GameState.SPLASH) {
            drawGameplay();
        } else {
            drawSplash();
            return;
        }

        if (gameState == GameState.MAIN_MENU) {
            drawMainMenu();
        }

        if (gameState == GameState.OPTIONS) {
            drawOptions();
        }

        if (faderShaderTimer != 0.0F) {
            drawGameOver();
        }
    }

    @Override
    public void render() {
        camera.update();

        Gdx.gl.glClearColor(0.273F, 0.602F, 0.906F, 0.91F);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        updateWorld();
        mainDraw();
    }

    @Override
    public void dispose() {
        TClogo.dispose();
        effects.dispose();
        square.dispose();
        shadow.dispose();
        colorShiftBkg.dispose();

        menumusic.dispose();
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

    class Barrier {
        Vector2 position = new Vector2();
        boolean counted;

        Barrier(int x, int y) {
            this.position.x = x;
            this.position.y = y;
        }
    }

    class Circlez {
        Vector2 position = new Vector2();
        int speed;
        float scale;

        Circlez(int x, int y, int z, int a) {
            this.position.x = x;
            this.position.y = y;
            this.speed = z;
            this.scale = a;
        }
    }

    enum GameState {
        SPLASH, MAIN_MENU, OPTIONS, START, RUNNING, GAME_OVER
    }
}
