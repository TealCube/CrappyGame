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
    private static final int BASE_PLAYER_SPEED = 21;
    private static final int BASE_BARRIER_SPEED = 24;
    private static final int WORLD_WIDTH = 4500;
    private static final int WORLD_HEIGHT = 7500;
    private static final int MAX_RIGHT_BOUNDS = 3400;
    private static final int MAX_LEFT_BOUNDS = 400;
    private static final int PLAYER_SCALE = 675;
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
        viewport = new StretchViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
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
            circles.add(new Circlez(MathUtils.random(-1000,4200), MathUtils.random(0, 9000), MathUtils.random(5,30),
                                    MathUtils.random(400,1500)));
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
        x = WORLD_WIDTH * (x / (float) width);
        Gdx.app.log("[INFO]", "CLICKED X: " + x);
        return x;
    }

    // Grab screen adjusted Y value
    private float grabY() {
        int height = Gdx.graphics.getHeight();
        float y = Gdx.input.getY();
        y = WORLD_HEIGHT-(WORLD_HEIGHT * (y / (float) height));
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
        shadowcreep = -50;

        RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
        LEFT_BOUNDS = MAX_LEFT_BOUNDS;
        barriers.clear();

        for (int i = 0; i < 10; i++) {
            tempRandom = lastRandom + MathUtils.random(1, 5);
            if (tempRandom > 5) {
                tempRandom -= 6;
            }
            lastRandom = tempRandom;
            barrierLoc = 4200 + tempRandom * -425;
            barriers.add(new Barrier(barrierLoc, WORLD_HEIGHT + 100 + i * 2875));
            lastBarrier = WORLD_HEIGHT + i * 2875;
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
                if (r.position.y <= player_y+PLAYER_SCALE) {
                    RIGHT_BOUNDS = r.position.x - 660;
                    LEFT_BOUNDS = r.position.x - 1650;
                    Gdx.app.log("[INFO]", "Right bounds set to: " + RIGHT_BOUNDS);
                    Gdx.app.log("[INFO]", "Left bounds set to: " + LEFT_BOUNDS);
                    // Once it is past the player, it should add one to the score, and
                    // change the counted value. It also resets the bounds to the sides
                    // of the screen. After this step, the barrier does literally
                    // nothing but move downwards until it is cleared.
                    if (r.position.y <= (player_y-PLAYER_SCALE)) {
                        RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
                        LEFT_BOUNDS = MAX_LEFT_BOUNDS;
                        Gdx.app.log("[INFO]", "Bounds Reset");
                        r.counted = true;
                        score++;
                        if (score < (Math.max(highscore, 25F))) {
                            shadowcreep = -50 + (((float) score / (Math.max((float) highscore, 25F))) * 100);
                        } else {
                            shadowcreep = 50;
                        }
                        //barrierspeed = (score % 3 == 0 ? barrierspeed + 5 : barrierspeed);
                        //playerspeed = (score % 4 == 0 ? playerspeed + 5 : playerspeed);
                    }
                }
            }
            if (r.position.y <= -PLAYER_SCALE) {
                Gdx.app.log("[INFO]", "LAST BARRIER WAS AT: " + lastBarrier);
                r.position.y = lastBarrier + 2875;
                lastBarrier += 2875;
                Gdx.app.log("[INFO]", "MOVED LOWEST BARRIER TO: " + lastBarrier);
                int tempRandom = lastRandom + MathUtils.random(1, 5);
                if (tempRandom > 5) {
                    tempRandom -= 6;
                }
                lastRandom = tempRandom;
                r.position.x = 4150 + tempRandom * -425;
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
            if (r.position.y < -1500) {
                r.position.y = 9500;
                r.position.x = -1000 + MathUtils.random(0, 4200);
                r.scale = MathUtils.random(400,1500);
                r.speed = MathUtils.random(2,10);
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
                case 0:
                    music1.stop();
                    break;
                case 1:
                    music2.stop();
                    break;
                case 2:
                    music3.stop();
                    break;
            }
            gameovermusic.play();
            gameState = GameState.GAME_OVER;
            if (ads) {
                adsController.showBannerAd();
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
            shadowcreep = 50;
            moveCircles();
            if (Gdx.input.justTouched()) {
                float x = grabX();
                float y = grabY();
                // Play Button 1500, 5000, 6000, 2200
                if (x > 750 && x < 3750 && y > 3450 && y < 4350) {
                    gameState = GameState.START;
                    menumusic.stop();
                    resetWorld();
                    return;
                }
                // Options button 1500, 4750, 6000, 1800
                if (x > 750 && x < 3750 && y > 2375 && y < 3275) {
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
            shadowcreep = 50;
            moveCircles();
            if (Gdx.input.justTouched()) {
                float x = grabX();
                float y = grabY();
                // TRACK1
                if (x > 750 && x < 3750 && y > 3450 && y < 4200) {
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
                if (x > 750 && x < 3750 && y > 2625 && y < 3375) {
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
                if (x > 750 && x < 7500 && y > 1800 && y < 2550) {
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
                if (x > 750 && x < 7500 && y > 975 && y < 1725) {
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
            bkgShift += 4;
            if (bkgShift > 75000) {
                bkgShift = -7500;
            }
            checkGameOver();
        }

        if (gameState == GameState.GAME_OVER) {
            if (bkgShift > 1) {
                bkgShift-= 75+(bkgShift/25);
            }
            moveBarriers();
            moveCircles();
            player_y -= barrierspeed;
            if (faderShaderTimer >= 1.0F) {
                if (Gdx.input.justTouched()) {
                    float x = grabX();
                    float y = grabY();

                    // Replay Button
                    if (x > 750 && x < 3750 && y > 3025 && y < 4125) {
                        if (ads) {
                            adsController.hideBannerAd();
                        }
                        gameState = GameState.START;
                        gameovermusic.stop();
                        resetWorld();
                        return;
                    }

                    // Main Menu Button
                    if (x > 750 && x < 1850 && y > 1850 && y < 2950) {
                        if (ads) {
                            adsController.hideBannerAd();
                        }
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
        shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
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
        batch.draw(TClogo, 1500, 3250, 1500, 1500);
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
        shapeRenderer.rect(750 + shadowcreep, 3050 - shadowcreep, 3000, 900);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(750, 3050, 3000, 900);

        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowcreep, 1975 - shadowcreep, 3000, 900);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(750, 1975, 3000, 900);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        batch.dispose();
        batch = new SpriteBatch();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        font.setScale(4, 4);
        font.setColor(0, 0, 0, 0.3F);
        font.draw(batch, "Chroma", 595, 6150);
        font.draw(batch, "Dodge", 1560, 5200);
        font.setColor(1, 1, 1, 1);
        font.draw(batch, "Chroma", 545, 6200);
        font.draw(batch, "Dodge", 1510, 5250);

        font.setScale(2.5F, 2.5F);
        font.setColor(0, 0, 0, 0.4F);
        font.draw(batch, "Play", 1765, 3790);
        font.draw(batch, "Options", 1355, 2725);

        batch.draw(shadow, 1020 + 50, 4830 - 50, 2, 2, 4, 4, 150, 150, rotator, 0, 0, 4, 4, false, false);
        batch.draw(square, 1030, 4830, 2, 2, 4, 4, 150, 150, rotator, 0, 0, 4, 4, false, false);

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
        shapeRenderer.rect(750 + shadowcreep, 3450 - shadowcreep, 3000, 750);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 0) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(750, 3450, 3000, 750);

        // sound2
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowcreep, 2625 - shadowcreep, 3000, 750);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 1) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(750, 2625, 3000, 750);

        // sound3
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowcreep, 1800 - shadowcreep, 3000, 750);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 2) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(750, 1800, 3000, 750);

        //main menu button
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowcreep, 975 - shadowcreep, 3000, 750);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(750, 975, 3000, 750);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        batch.dispose();
        batch = new SpriteBatch();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        font.setScale(4, 4);
        font.setColor(0, 0, 0, 0.3F);
        font.draw(batch, "Chroma", 595, 6150);
        font.draw(batch, "Dodge", 1560, 5200);
        font.setColor(1, 1, 1, 1);
        font.draw(batch, "Chroma", 545, 6200);
        font.draw(batch, "Dodge", 1510, 5250);

        font.setScale(2, 2);
        font.setColor(0, 0, 0, 0.4F);
        font.draw(batch, "Track 1", 1425, 4100);
        font.draw(batch, "Track 2", 1425, 3275);
        font.draw(batch, "Track 3", 1425, 2450);
        font.draw(batch, "Back", 1715, 1625);

        batch.draw(shadow, 1020 + 50, 4830 - 50, 2, 2, 4, 4, 150, 150, rotator, 0, 0, 4, 4, false, false);
        batch.draw(square, 1020, 4830, 2, 2, 4, 4, 150, 150, rotator, 0, 0, 4, 4, false, false);

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
                font.setScale(5, 5);
                font.setColor(0, 0, 0, 0.3F);
                font.drawMultiLine(batch, "" + score, 2250 + shadowcreep, 5950, 0, BitmapFont.HAlignment.CENTER);
                font.setColor(1, 1, 1, 1);
                font.drawMultiLine(batch, "" + score, 2250, 6000, 0, BitmapFont.HAlignment.CENTER);
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
            shapeRenderer.rect(player_x + shadowcreep, player_y - 50, PLAYER_SCALE, PLAYER_SCALE);
        }
        shapeRenderer.rect(shadowcreep, 0, 450, WORLD_HEIGHT);
        shapeRenderer.rect(4500 + shadowcreep, 0, -450, WORLD_HEIGHT);
        for (Barrier barrier : barriers) {
            shapeRenderer.rect(barrier.position.x + shadowcreep - 4659, barrier.position.y - 50, 3000, PLAYER_SCALE);
            shapeRenderer.rect(barrier.position.x + shadowcreep, barrier.position.y - 50, 3000, PLAYER_SCALE);
        }

        //Main Shapes
        shapeRenderer.setColor(1, 1, 1, 1);
        if (gameState != GameState.MAIN_MENU && gameState != GameState.OPTIONS) {
            shapeRenderer.rect(player_x, player_y, PLAYER_SCALE, PLAYER_SCALE);
        }
        shapeRenderer.rect(0, 0, 450, WORLD_HEIGHT);
        shapeRenderer.rect(WORLD_WIDTH, 0, -450, WORLD_HEIGHT);
        for (Barrier barrier : barriers) {
            shapeRenderer.rect(barrier.position.x - 4650, barrier.position.y, 3000, PLAYER_SCALE);
            shapeRenderer.rect(barrier.position.x, barrier.position.y, 3000, PLAYER_SCALE);
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
        shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);

        // Scoring feild background
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(750, 4150 - (7150 * (1 - faderShaderTimer)), 3000, 2450);
        shapeRenderer.setColor(0.7F, 0.7F, 0.7F, 1);
        shapeRenderer.rect(950, 4350 - (7150 * (1 - faderShaderTimer)), 2600, 2050);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(950, 4650 - (7150 * (1 - faderShaderTimer)), 2600, 150);

        shapeRenderer.setColor(0.7F, 0.7F, 0.7F, 1);
        shapeRenderer.circle(1070, 4480 - (7150 * (1 - faderShaderTimer)), 230);

        // Retry button background
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(750, 2900 - (7150 * (1 - faderShaderTimer)), 3000, 1100);

        // Main Menu Button
        shapeRenderer.rect(750, 1650 - (7150 * (1 - faderShaderTimer)), 3000, 1100);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        batch.dispose();
        batch = new SpriteBatch();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        font.setScale(3, 3);
        font.setColor(0, 0, 0, 0.4F);
        font.draw(batch, "Retry", 1485, 3800 - (7150 * (1 - faderShaderTimer)));
        font.draw(batch, "Back", 1560, 2540 - (7150 * (1 - faderShaderTimer)));
        font.setScale(1F, 1F);
        font.setColor(1, 1, 1, 1);
        if (score <= highscore){
            font.draw(batch, "Highscore: " + highscore, 1360, 4605 - (7150 * (1 - faderShaderTimer)));
        } else {
            font.draw(batch, "NEW HIGHSCORE!", 1360, 4605 - (7150 * (1 - faderShaderTimer)));
        }

        batch.draw(square, 1065, 4485 - (7150 * (1 - faderShaderTimer)), 2, 2, 4, 4, 70, 70, rotator, 0, 0, 4, 4,
                   false, false);

        if (faderShaderTimer > 0) {
            font.setScale(6, 6);
            font.setColor(1, 1, 1, 1);
            font.drawMultiLine(batch, "" + score, 2250,6260-(7150*(1-faderShaderTimer)), 0, BitmapFont.HAlignment
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
