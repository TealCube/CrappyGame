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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Random;

public class CrappyGame extends ApplicationAdapter {

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private static final int BASE_PLAYER_SPEED = 4;
    private static final int BASE_BARRIER_SPEED = 5;
    private static final int WORLD_WIDTH = 900;
    private static final int WORLD_HEIGHT = 1600;
    private static final int MAX_RIGHT_BOUNDS = 680;
    private static final int MAX_LEFT_BOUNDS = 82;
    private static final int PLAYER_SCALE = 135;
    private static final int BACKGROUND_CHANGE_RATE = 2;
    private static final int BACKGROUND_CHANGE_INTERVAL = 10;
    private Preferences preferences;

    private float RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
    private float LEFT_BOUNDS = MAX_LEFT_BOUNDS;

    private RgbColor topLeft = new RgbColor(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255));
    private RgbColor topRight = new RgbColor(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255));
    private RgbColor bottomLeft = new RgbColor(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255));
    private RgbColor bottomRight = new RgbColor(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255));
    private Texture tcLogo;
    private Texture square;
    private Texture select;
    private Texture shadow;
    private Texture effects;
    private Texture gameover;
    private Sound tcLoad;
    private Sound click;
    private Sound collide;
    private Music music1;
    private Music music2;
    private Music music3;
    private Music menuMusic;
    private Music gameOverMusic;

    private String tutText;
    private int tutTextCount;
    private float tutCounter;
    private float tutAlpha;
    private boolean tutFadeIn;
    private boolean tutFinished = false;

    private int player_x;
    private int player_y;
    private int playerSpeed;
    private int barrierSpeed;
    private int lastRandom;
    private int lastBarrier;
    private int rotator;
    private int track;
    private int offset;
    private float splashTimer;
    private float faderShaderTimer;
    private float scroller;
    private int adCount = 0;
    private int highScore = 0;
    private int score = 0;
    private boolean ads = true;
    private int backgroundChangeTick = 0;
    private int moveTowardsTick = 0;

    private GameState gameState;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private Viewport viewport;
    private OrthographicCamera camera;
    private BitmapFont largeFont;

    private Array<Barrier> barriers = new Array<Barrier>();
    private Array<Circle> circles = new Array<Circle>();

    private AdsController adsController;

    public CrappyGame(AdsController adsController) {
        if (adsController != null) {
            this.adsController = adsController;
        } else {
            ads = false;
        }
    }

    // Changes the saved highscore.
    public void setHighScore(int val) {
        preferences.putInteger("highscore", val);
        preferences.flush();
    }

    // Gets the music, if you hadn't figured that out.
    public int getMusic() {
        return preferences.getInteger("music");
    }

    // Changes the saved track.
    public void setMusic(int val) {
        preferences.putInteger("music", val);
        preferences.flush();
    }

    @Override public void create() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        gameState = GameState.SPLASH;
        splashTimer = 1;
        camera = new OrthographicCamera();
        viewport = new StretchViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Fjalla.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 128;
        parameter.characters = "aBbcCdDEefGgHhIijklmMNnOoPpqrRSsTtWwuvxy1234567890:!'";
        largeFont = generator.generateFont(parameter);

        player_y = WORLD_HEIGHT / 6;

        // Splash Assets
        tcLogo = new Texture("TClogo.png");
        tcLoad = Gdx.audio.newSound(Gdx.files.internal("TCload.wav"));
        tcLoad.setVolume(0, 0.5F);

        // Gameplay Assets
        collide = Gdx.audio.newSound(Gdx.files.internal("collide.wav"));
        effects = new Texture("bkgcircle.png");

        // Menu Assets
        click = Gdx.audio.newSound(Gdx.files.internal("click.mp3"));
        gameover = new Texture("gameover.png");
        square = new Texture("square.png");
        select = new Texture("select.png");
        shadow = new Texture("shadow.png");

        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("odyssey.mp3"));
        music1 = Gdx.audio.newMusic(Gdx.files.internal("bensound-memories.mp3"));
        music2 = Gdx.audio.newMusic(Gdx.files.internal("dream-culture.mp3"));
        music3 = Gdx.audio.newMusic(Gdx.files.internal("bensound-goinghigher.mp3"));
        gameOverMusic = Gdx.audio.newMusic(Gdx.files.internal("easy-lemon.mp3"));

        menuMusic.setVolume(1.0F);
        music1.setVolume(0.7F);
        music1.setVolume(0.7F);
        music1.setVolume(0.6F);
        gameOverMusic.setVolume(0.9F);

        menuMusic.setLooping(true);
        music1.setLooping(true);
        music2.setLooping(true);
        music3.setLooping(true);

        for (int i = 0; i < 6; i++) {
            circles.add(new Circle(MathUtils.random(-150, 840), MathUtils.random(0, 1800), MathUtils.random(1, 3)
                    , MathUtils.random(300, 800)));
        }

        preferences = Gdx.app.getPreferences("ChromaDodge");

        if (!preferences.contains("highscore")) {
            preferences.putInteger("highscore", 0);
        }
        if (!preferences.contains("music")) {
            preferences.putInteger("music", 1);
        }
        topLeft = new RgbColor(102, 205, 255);
        topRight = new RgbColor(66, 205, 255);
        bottomLeft = new RgbColor(87, 255, 190);
        bottomRight = new RgbColor(135, 255, 190);
        resetWorld();
    }

    // Resets the world when you hit retry or w/e
    private void resetWorld() {
        int tempRandom;
        int barrierLoc;
        offset = 0;

        tutFinished = false;
        tutCounter = 0;
        tutFadeIn = true;
        tutAlpha = 0;
        tutTextCount = 0;
        tutText = "Tap anywhere to start!";

        track = getMusic();
        highScore = getHighScore();
        score = 0;
        playerSpeed = BASE_PLAYER_SPEED;
        barrierSpeed = BASE_BARRIER_SPEED;

        player_x = WORLD_WIDTH / 2 - PLAYER_SCALE / 2;
        player_y = WORLD_HEIGHT / 6;

        RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
        LEFT_BOUNDS = MAX_LEFT_BOUNDS;
        barriers.clear();

        // Makes it so they never see an ad until they hit highscore of 3
        if (highScore < 3) {
            adCount = 1;
        }

        for (int i = 0; i < 3; i++) {
            tempRandom = 4 - i;
            lastRandom = tempRandom;
            barrierLoc = 840 + tempRandom * -85;
            barriers.add(new Barrier(barrierLoc, 1900 + i * 630));
            lastBarrier = 1900 + i * 630;
        }
    }

    // Gets the highscore, if you hadn't figured that out.
    public int getHighScore() {
        return preferences.getInteger("highscore");
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
    }

    // World update. Makes stuff happen.
    private void updateWorld() {
        scroller = 1420 * (1 - faderShaderTimer);
        if (gameState == GameState.RUNNING) {
            if (!tutFinished) {
                checkTutorial();
            }
            movePlayer();
            moveBarriers();
            checkBarriers();
            checkGameOver();
            moveCircles();

            colorify();
            return;
        }

        if (gameState == GameState.GAME_OVER) {
            moveBarriers();
            moveCircles();

            player_y -= barrierSpeed;
            if (faderShaderTimer != 1.0F) {
                faderShaderTimer += 0.1F;
                if (faderShaderTimer > 1.0F) {
                    faderShaderTimer = 1.0F;
                }
            } else if (Gdx.input.justTouched()) {
                float x = grabX();
                float y = grabY();

                // Replay Button
                if (x > 150 && x < 750 && y > 580 && y < 800) {
                    if (ads && adCount > 2) {
                        adCount = -2;
                        gameOverMusic.stop();
                        offset = 0;
                        gameState = GameState.ADS;
                    } else {
                        adCount++;
                        gameOverMusic.stop();
                        resetWorld();
                        click.play();
                        offset = 0;
                        gameState = GameState.START;
                    }
                    return;
                }

                // Main Menu Button
                if (x > 150 && x < 750 && y > 330 && y < 550) {
                    if (track != 0) {
                        menuMusic.play();
                    }
                    gameOverMusic.stop();
                    resetWorld();
                    click.play();
                    offset = 3000;
                    player_y = -180;
                    gameState = GameState.MAIN_MENU;
                    return;
                }
            }
            return;
        }

        if (gameState == GameState.START) {
            moveCircles();
            if (!tutFinished) {
                checkTutorial();
            }

            if (faderShaderTimer != 0.0F) {
                faderShaderTimer -= 0.1F;
                if (faderShaderTimer < 0.0F) {
                    faderShaderTimer = 0.0F;
                }
            } else if (Gdx.input.justTouched()) {
                gameState = GameState.RUNNING;
                switch (track) {
                    case 0:
                        break;
                    case 1:
                        music1.play();
                        break;
                    case 2:
                        music2.play();
                        break;
                    case 3:
                        music3.play();
                        break;
                }
                topLeft.invertFlip();
                topRight.invertFlip();
                bottomLeft.invertFlip();
                bottomRight.invertFlip();
                return;
            }
            return;
        }


        if (gameState == GameState.ADS) {
            if (adsController.isWifiConnected()) {
                adsController.showInterstitialAd(new Runnable() {
                    @Override
                    public void run() {
                        resetWorld();
                        faderShaderTimer = 0;
                        gameState = GameState.START;
                    }
                });
            } else {
                resetWorld();
                faderShaderTimer = 0;
                gameState = GameState.START;
            }
        }

        if (gameState == GameState.MAIN_MENU) {
            rotator++;
            if (rotator > 359) {
                rotator = 0;
            }
            moveTowards(new RgbColor(102, 155, 245), new RgbColor(26, 145, 245), new RgbColor(87, 245, 190), new
                RgbColor(125, 255, 170));

            moveCircles();

            if (faderShaderTimer != 0.0F) {
                faderShaderTimer -= 0.1F;
                if (faderShaderTimer < 0.0F) {
                    faderShaderTimer = 0.0F;
                }
            } else if (Gdx.input.justTouched()) {
                float x = grabX();
                float y = grabY();
                // Play Button 1500, 5000, 6000, 2200
                if (x > 150 && x < 750 && y > 610 && y < 790) {
                    if (ads && adCount > 2) {
                        adCount = -2;
                        menuMusic.stop();
                        click.play();
                        gameState = GameState.ADS;
                        offset = 0;
                    } else {
                        adCount++;
                        menuMusic.stop();
                        resetWorld();
                        click.play();
                        gameState = GameState.START;
                        offset = 0;
                    }
                    return;
                }
                // Options button 1500, 4750, 6000, 1800
                if (x > 150 && x < 750 && y > 395 && y < 575) {
                    gameState = GameState.OPTIONS;
                    click.play();
                    player_y = -180;
                    offset = 3000;
                    return;
                }
                // Exit Button
                if (x > 250 && x < 650 && y > 180 && y < 360) {
                    click.play();
                    Gdx.app.exit();
                }
            }
            return;
        }

        if (gameState == GameState.OPTIONS) {
            rotator++;
            if (rotator > 359) {
                rotator = 0;
            }
            moveCircles();

            moveTowards(new RgbColor(87, 225, 190), new RgbColor(135, 225, 190), new RgbColor(240, 240, 50), new
                RgbColor(230, 230, 120));

            if (Gdx.input.justTouched()) {
                float x = grabX();
                float y = grabY();
                // NO MUSIC *CLAP CLAPCLAPCLAPCLAP*
                if (x > 150 && x < 750 && y > 815 && y < 965) {
                    track = 0;
                    setMusic(0);
                    menuMusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    click.play();
                    return;
                }
                // TRACK1
                if (x > 150 && x < 750 && y > 650 && y < 800) {
                    track = 1;
                    setMusic(1);
                    menuMusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    music1.play();
                    click.play();
                    return;
                }
                // TRACK2
                if (x > 150 && x < 750 && y > 485 && y < 635) {
                    track = 2;
                    setMusic(2);
                    menuMusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    music2.play();
                    click.play();
                    return;
                }
                // TRACK3
                if (x > 150 && x < 750 && y > 300 && y < 470) {
                    track = 3;
                    setMusic(3);
                    menuMusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    music3.play();
                    click.play();
                    return;
                }
                // BACK1
                if (x > 150 && x < 750 && y > 155 && y < 305) {
                    gameState = GameState.MAIN_MENU;
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    if (track != 0) {
                        menuMusic.play();
                    }
                    offset = 3000;
                    click.play();
                    return;
                }
            }
            return;
        }

        if (gameState == GameState.SPLASH) {
            doSplash();
        }
    }

    // Grab screen adjusted X value
    private float grabX() {
        int width = Gdx.graphics.getWidth();
        float x = Gdx.input.getX();
        x = WORLD_WIDTH * (x / (float) width);
        return x;
    }

    // Grab screen adjusted Y value
    private float grabY() {
        int height = Gdx.graphics.getHeight();
        float y = Gdx.input.getY();
        y = WORLD_HEIGHT - WORLD_HEIGHT * y / (float) height;
        return y;
    }

    // DANCE, PLAYER, DANCE!!
    private void movePlayer() {
        if (Gdx.input.justTouched()) {
            playerSpeed = playerSpeed * -1;
        }
        player_x += playerSpeed;
    }

    private void checkTutorial() {
        if (tutFadeIn) {
            tutAlpha = tutCounter;
            tutCounter = tutCounter + 0.0005F + ((1 - tutCounter) / 20);
            if (tutCounter >= 1) {
                tutCounter = 1;
                tutFadeIn = false;
            }
        } else {
            tutCounter = tutCounter + 0.25F;
            tutAlpha = 2-tutCounter;
            if (tutCounter >= 2) {
                switch (tutTextCount) {
                    case 0:
                        tutText = "Tap again to move!";
                        tutTextCount = 1;
                        tutFadeIn = true;
                        tutCounter = 0.005F;
                        break;
                    case 1:
                        tutText = "Don't hit stuff!";
                        tutTextCount = 2;
                        tutFadeIn = true;
                        tutCounter = 0.005F;
                        break;
                    case 2:
                        tutFinished = true;
                        tutFadeIn = true;
                        tutCounter = 0.005F;
                        break;
                }
            }
        }
    }

    // Moves Barriers and sets colision bounds
    private void checkBarriers() {
        for (Barrier r : barriers) {
            if (!r.counted) {
                if (!r.activated) {
                    if (r.position.y <= player_y + PLAYER_SCALE) {
                        RIGHT_BOUNDS = r.position.x - 132;
                        LEFT_BOUNDS = r.position.x - 330;
                        r.activated = true;
                    }
                } else if (r.position.y <= player_y - PLAYER_SCALE) {
                    RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
                    LEFT_BOUNDS = MAX_LEFT_BOUNDS;
                    r.counted = true;
                    score++;
                }
            }
        }
    }

    private void moveBarriers() {
        lastBarrier -= barrierSpeed;
        for (Barrier r : barriers) {
            r.position.y -= barrierSpeed;
            if (r.position.y <= -PLAYER_SCALE) {
                r.position.y = lastBarrier + 630;
                lastBarrier += 630;
                int tempRandom = lastRandom + MathUtils.random(1, 5);
                if (tempRandom > 5) {
                    tempRandom -= 6;
                }
                lastRandom = tempRandom;
                r.position.x = 840 + tempRandom * -85;
                r.counted = false;
                r.activated = false;
            }
        }
    }

    // Moves Barriers and sets colision bounds
    private void moveCircles() {
        for (Circle r : circles) {
            r.position.y = r.position.y - r.speed;
            if (r.position.y < -r.scale) {
                r.scale = MathUtils.random(300, 800);
                r.speed = MathUtils.random(1, 3);
                r.position.y = WORLD_HEIGHT;
                r.position.x = -(r.scale / 2) + MathUtils.random(0, WORLD_WIDTH - r.scale / 2);
            }
        }

    }

    // Handles the splash opening
    private void doSplash() {
        if (splashTimer < 90) {
            splashTimer++;
            if (splashTimer == 10) {
                tcLoad.play(0.4F);
            }
        } else {
            gameState = GameState.MAIN_MENU;
            tcLogo.dispose();
            tcLoad.dispose();
            track = getMusic();
            offset = 3000;
            player_y = -180;
            if (track != 0) {
                menuMusic.play();
            }
        }
    }

    // Check to see if you lose/set highscore
    private void checkGameOver() {
        if (player_x < LEFT_BOUNDS || player_x > RIGHT_BOUNDS) {
            if (score > highScore) {
                setHighScore(score);
            }
            switch (track) {
                case 0:
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    menuMusic.stop();
                    break;
                case 1:
                    music2.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    menuMusic.stop();
                    break;
                case 2:
                    music3.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    menuMusic.stop();
                    break;
                case 3:
                    music1.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    menuMusic.stop();
                    break;
            }
            collide.play();
            if (track != 0) {
                gameOverMusic.play();
            }
            gameState = GameState.GAME_OVER;
        }
    }

    private void colorify() {
        if (backgroundChangeTick++ >= BACKGROUND_CHANGE_INTERVAL) {
            topLeft = topLeft.change(BACKGROUND_CHANGE_RATE);
            topRight = topRight.change(BACKGROUND_CHANGE_RATE);
            bottomLeft = bottomLeft.change(BACKGROUND_CHANGE_RATE);
            bottomRight = bottomRight.change(BACKGROUND_CHANGE_RATE);
            backgroundChangeTick -= BACKGROUND_CHANGE_INTERVAL;
        }
    }

    private void moveTowards(RgbColor tl, RgbColor tr, RgbColor bl, RgbColor br) {
        if (moveTowardsTick++ >= BACKGROUND_CHANGE_INTERVAL / 4) {
            topLeft = topLeft.towards(tl, BACKGROUND_CHANGE_RATE);
            topRight = topRight.towards(tr, BACKGROUND_CHANGE_RATE);
            bottomLeft = bottomLeft.towards(bl, BACKGROUND_CHANGE_RATE);
            bottomRight = bottomLeft.towards(br, BACKGROUND_CHANGE_RATE);
        }
    }

    // Draw event for the renderer to use.
    private void mainDraw() {
        if (gameState == GameState.START) {
            drawGameplay();
            if (!tutFinished) {
                drawTutorial();
            }
            if (faderShaderTimer != 0) {
                drawGameOver();
            }
            return;
        }

        if (gameState == GameState.GAME_OVER) {
            drawGameplay();
            drawGameOver();
            return;
        }

        if (gameState == GameState.RUNNING) {
            drawGameplay();
            if (!tutFinished) {
                drawTutorial();
            }
            return;
        }

        if (gameState == GameState.MAIN_MENU) {
            drawGameplay();
            drawMainMenu();
            if (faderShaderTimer != 0) {
                drawGameOver();
            }
            return;
        }

        if (gameState == GameState.OPTIONS) {
            drawGameplay();
            drawOptions();
            return;
        }

        if (gameState == GameState.SPLASH) {
            drawSplash();
        }
    }

    private void drawSplash() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.enableBlending();

        Color c = batch.getColor();
        batch.setColor(c.r, c.g, c.b, 1f);
        if (splashTimer < 10) {
            batch.setColor(c.r, c.g, c.b, splashTimer / 10);
        }
        batch.draw(tcLogo, 300, 650, 300, 300);

        batch.disableBlending();
        batch.flush();
        batch.end();
    }

    private void drawTutorial() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.enableBlending();

        largeFont.setScale(0.4F, 0.4F);

        largeFont.setColor(0, 0, 0, tutAlpha / 3);
        largeFont.drawMultiLine(batch, tutText, 394 + tutCounter * 60, 500, 0, BitmapFont.HAlignment.CENTER);
        largeFont.setColor(1, 1, 1, tutAlpha);
        largeFont.drawMultiLine(batch, tutText, 390 + tutCounter * 60, 504, 0, BitmapFont.HAlignment.CENTER);

        batch.disableBlending();
        batch.flush();
        batch.end();
    }

    private void drawMainMenu() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.enableBlending();

        // shadows
        batch.draw(shadow, 160, 600, 600, 180);
        batch.draw(shadow, 160, 385, 600, 180);
        batch.draw(shadow, 260, 170, 400, 180);
        batch.draw(shadow, 216, 956, 1, 1, 2, 2, 60, 60, rotator, 0, 0, 2, 2, false, false);

        batch.disableBlending();
        // whitethings
        batch.draw(square, 150, 610, 600, 180);
        batch.draw(square, 150, 395, 600, 180);
        batch.draw(square, 250, 180, 400, 180);
        batch.draw(square, 206, 966, 1, 1, 2, 2, 60, 60, rotator, 0, 0, 2, 2, false, false);

        batch.enableBlending();
        largeFont.setScale(1.6F, 1.6F);
        largeFont.setColor(0, 0, 0, 0.3F);
        largeFont.draw(batch, "Chroma", 119, 1250);
        largeFont.draw(batch, "Dodge", 312, 1060);
        largeFont.setColor(1, 1, 1, 1);
        largeFont.draw(batch, "Chroma", 109, 1260);
        largeFont.draw(batch, "Dodge", 302, 1070);

        largeFont.setScale(1F, 1F);
        largeFont.setColor(0.6F, 0.6F, 0.6F, 1);
        largeFont.draw(batch, "Play", 353, 758);
        largeFont.draw(batch, "Options", 271, 545);
        largeFont.draw(batch, "Exit", 362, 324);

        batch.disableBlending();
        batch.flush();
        batch.end();
    }

    private void drawOptions() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        batch.enableBlending();
        // Shadows of all buttons
        batch.draw(shadow, 160, 805, 600, 150);
        batch.draw(shadow, 160, 640, 600, 150);
        batch.draw(shadow, 160, 475, 600, 150);
        batch.draw(shadow, 160, 310, 600, 150);
        batch.draw(shadow, 160, 145, 600, 150);

        batch.draw(shadow, 216, 1116, 1, 1, 2, 2, 60, 60, rotator, 0, 0, 2, 2, false, false);

        batch.disableBlending();
        // Main colors of all buttons
        batch.draw(square, 150, 815, 600, 150);
        batch.draw(square, 150, 650, 600, 150);
        batch.draw(square, 150, 485, 600, 150);
        batch.draw(square, 150, 320, 600, 150);
        batch.draw(square, 150, 155, 600, 150);
        batch.draw(select, 150, 815 - track * 165, 600, 150);

        batch.draw(square, 206, 1126, 1, 1, 2, 2, 60, 60, rotator, 0, 0, 2, 2, false, false);

        batch.enableBlending();
        // The rest of the stuff :O
        largeFont.setScale(1.6F, 1.6F);
        largeFont.setColor(0, 0, 0, 0.3F);
        largeFont.draw(batch, "Chroma", 119, 1390);
        largeFont.draw(batch, "Dodge", 312, 1200);
        largeFont.setColor(1, 1, 1, 1);
        largeFont.draw(batch, "Chroma", 109, 1400);
        largeFont.draw(batch, "Dodge", 302, 1210);

        largeFont.setScale(0.85F, 0.85F);
        largeFont.setColor(0, 0, 0, 0.4F);
        largeFont.draw(batch, "No Music", 280, 935);
        largeFont.draw(batch, "Track 1", 310, 770);
        largeFont.draw(batch, "Track 2", 310, 605);
        largeFont.draw(batch, "Track 3", 310, 440);
        largeFont.draw(batch, "Back", 350, 275);

        batch.flush();
        batch.end();
    }

    private void drawGameplay() {
        //Setup ShapeRenderer
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT, topLeft.toColor(), topRight.toColor(), bottomLeft.toColor(),
                           bottomRight.toColor());

        shapeRenderer.flush();
        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.enableBlending();

        for (Circle circle : circles) {
            batch.draw(effects, circle.position.x, circle.position.y, circle.scale, circle.scale);
        }

        // Draws score text that's part of the background
        largeFont.setScale(2, 2);
        largeFont.setColor(0, 0, 0, 0.3F);
        largeFont.drawMultiLine(batch, "" + score, 460, 1190 + offset, 0, BitmapFont.HAlignment.CENTER);
        largeFont.setColor(1, 1, 1, 1);
        largeFont.drawMultiLine(batch, "" + score, 450, 1200 + offset, 0, BitmapFont.HAlignment.CENTER);

        batch.draw(shadow, player_x + 10, player_y - 10, PLAYER_SCALE, PLAYER_SCALE);

        batch.draw(shadow, 86, 0, 10, WORLD_HEIGHT);

        for (Barrier barrier : barriers) {
            batch.draw(shadow, barrier.position.x - 922, barrier.position.y - 10, 600, PLAYER_SCALE);
            batch.draw(shadow, barrier.position.x + 10, barrier.position.y - 10, 600, PLAYER_SCALE);
        }

        //Main Shapes
        batch.disableBlending();
        batch.draw(square, player_x, player_y, PLAYER_SCALE, PLAYER_SCALE);

        batch.draw(square, 0, 0, 86, WORLD_HEIGHT);
        batch.draw(square, WORLD_WIDTH, 0, -90, WORLD_HEIGHT);

        for (Barrier barrier : barriers) {
            batch.draw(square, barrier.position.x - 932, barrier.position.y, 600, PLAYER_SCALE);
            batch.draw(square, barrier.position.x, barrier.position.y, 600, PLAYER_SCALE);
        }

        batch.flush();
        batch.end();
    }

    private void drawGameOver() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.enableBlending();

        Color c = batch.getColor();
        batch.setColor(c.r, c.g, c.b, faderShaderTimer);
        batch.draw(shadow, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.setColor(1, 1, 1, 1);

        batch.draw(gameover, 150, 330 - scroller, 600, 1000);

        largeFont.setScale(2.3F, 2.3F);
        largeFont.setColor(1, 1, 1, 1);
        largeFont.drawMultiLine(batch, "" + score, 450, 1210 - scroller, 0, BitmapFont.HAlignment.CENTER);

        largeFont.setScale(0.4F, 0.4F);
        largeFont.setColor(0.7F, 0.7F, 0.7F, 1);
        if (score <= highScore) {
            largeFont.drawMultiLine(batch, "Highscore: " + highScore, 450, 898 - scroller, 0,
                                    BitmapFont.HAlignment.CENTER);
        } else {
            largeFont.drawMultiLine(batch, "NEW HIGHSCORE!", 450, 898 - scroller, 0, BitmapFont.HAlignment.CENTER);
        }
        batch.flush();
        batch.end();
    }

    @Override
    public void render() {
        camera.update();

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        updateWorld();
        mainDraw();
    }

    @Override
    public void dispose() {
        tcLogo.dispose();
        effects.dispose();
        square.dispose();
        select.dispose();
        shadow.dispose();
        gameover.dispose();

        menuMusic.dispose();
        music1.dispose();
        music2.dispose();
        music3.dispose();
        gameOverMusic.dispose();
        collide.dispose();
        tcLoad.dispose();
        click.dispose();

        largeFont.dispose();
        batch.dispose();
        shapeRenderer.dispose();
    }

}
