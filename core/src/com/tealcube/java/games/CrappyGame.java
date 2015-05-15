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

import java.util.Random;

public class CrappyGame extends ApplicationAdapter {

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private static final int BASE_PLAYER_SPEED = 21;
    private static final int BASE_BARRIER_SPEED = 24;
    private static final int WORLD_WIDTH = 4500;
    private static final int WORLD_HEIGHT = 7500;
    private static final int MAX_RIGHT_BOUNDS = 3400;
    private static final int MAX_LEFT_BOUNDS = 400;
    private static final int PLAYER_SCALE = 675;
    private static final int BACKGROUND_CHANGE_RATE = 1;
    private static Preferences preferences;

    private float RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
    private float LEFT_BOUNDS = MAX_LEFT_BOUNDS;

    private RgbColor topLeft = new RgbColor(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255));
    private RgbColor topRight = new RgbColor(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255));
    private RgbColor bottomLeft = new RgbColor(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255));
    private RgbColor bottomRight = new RgbColor(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255));
    private Texture tcLogo;
    private Texture square;
    private Texture shadow;
    private Texture effects;
    private Sound tcLoad;
    private Sound click;
    private Sound collide;
    private Music music1;
    private Music music2;
    private Music music3;
    private Music menuMusic;
    private Music gameOverMusic;

    private float shadowCreep;
    private int player_x;
    private int player_y;
    private int playerSpeed;
    private int barrierSpeed;
    private int lastRandom;
    private int lastBarrier;
    private int rotator;
    private int track;
    private float splashTimer;
    private float faderShaderTimer;
    private int highScore = 0;
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

    public CrappyGame(AdsController adsController) {
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
        parameter.characters = "aBbcCdDEefGgHhIijklmMNnOoPpqrRSsTtWuvxy1234567890:!";
        font = generator.generateFont(parameter);

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
        square = new Texture("square.png");
        shadow = new Texture("shadow.png");

        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("odyssey.mp3"));
        music1 = Gdx.audio.newMusic(Gdx.files.internal("bensound-memories.mp3"));
        music2 = Gdx.audio.newMusic(Gdx.files.internal("dream-culture.mp3"));
        music3 = Gdx.audio.newMusic(Gdx.files.internal("bensound-goinghigher.mp3"));
        gameOverMusic = Gdx.audio.newMusic(Gdx.files.internal("easy-lemon.mp3"));

        menuMusic.setVolume(0.7F);
        music1.setVolume(0.6F);
        music1.setVolume(0.6F);
        music1.setVolume(0.6F);
        gameOverMusic.setVolume(0.8F);

        menuMusic.setLooping(true);
        music1.setLooping(true);
        music2.setLooping(true);
        music3.setLooping(true);

        for (int i = 0; i < 20; i++) {
            circles.add(new Circlez(MathUtils.random(-1000, 4200), MathUtils.random(0, 9000), MathUtils.random(5, 30),
                                    MathUtils.random(500, 1300)));
        }

        preferences = Gdx.app.getPreferences("ChromaDodge");

        if (!preferences.contains("highscore")) {
            preferences.putInteger("highscore", 0);
        }
        if (!preferences.contains("music")) {
            preferences.putInteger("music", 0);
        }
        topLeft = new RgbColor(102, 205, 255);
        topRight = new RgbColor(66, 205, 255);
        bottomLeft = new RgbColor(87, 255, 190);
        bottomRight = new RgbColor(135, 255, 190);
        resetWorld();
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
        y = WORLD_HEIGHT - (WORLD_HEIGHT * (y / (float) height));
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
        highScore = getHighScore();
        score = 0;
        playerSpeed = BASE_PLAYER_SPEED;
        barrierSpeed = BASE_BARRIER_SPEED;

        player_x = WORLD_WIDTH / 2 - PLAYER_SCALE / 2;
        player_y = WORLD_HEIGHT / 6;

        shadowCreep = -50;

        RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
        LEFT_BOUNDS = MAX_LEFT_BOUNDS;
        barriers.clear();

        for (int i = 0; i < 3; i++) {
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
            playerSpeed = playerSpeed * -1;
        }
        player_x += playerSpeed;
    }

    // Moves Barriers and sets colision bounds
    private void checkBarriers() {
        for (Barrier r : barriers) {
            if (!r.counted) {
                if (!r.activated) {
                    if (r.position.y <= player_y+PLAYER_SCALE) {
                        RIGHT_BOUNDS = r.position.x - 660;
                        LEFT_BOUNDS = r.position.x - 1650;
                        r.activated = true;
                    }
                }
                if (r.position.y <= (player_y-PLAYER_SCALE)) {
                    RIGHT_BOUNDS = MAX_RIGHT_BOUNDS;
                    LEFT_BOUNDS = MAX_LEFT_BOUNDS;
                    r.counted = true;
                    score++;
                    if (score < (Math.max(highScore, 25F))) {
                        shadowCreep = -50 + (((float) score / (Math.max((float) highScore, 25F))) * 100);
                    } else {
                        shadowCreep = 50;
                    }
                }
            }
        }
    }

    private void moveBarriers() {
        lastBarrier -= barrierSpeed;
        for (Barrier r : barriers) {
            r.position.y -= barrierSpeed;
            if (r.position.y <= -PLAYER_SCALE) {
                r.position.y = lastBarrier + 2875;
                lastBarrier += 2875;
                int tempRandom = lastRandom + MathUtils.random(1, 5);
                if (tempRandom > 5) {
                    tempRandom -= 6;
                }
                lastRandom = tempRandom;
                r.position.x = 4150 + tempRandom * -425;
                r.counted = false;
                r.activated = false;
            }
        }
    }

    // Moves Barriers and sets colision bounds
    private void moveCircles() {
        for (Circlez r : circles) {
            if (gameState == GameState.START || gameState == GameState.MAIN_MENU || gameState == GameState.OPTIONS) {
                r.position.y = r.position.y - (r.speed/ 2);
            } else {
                r.position.y = r.position.y - r.speed;
            }
            if (r.position.y < -1000) {
                r.position.y = 9500;
                r.position.x = -1000 + MathUtils.random(0, 4200);
                r.scale = MathUtils.random(500,1300);
                r.speed = MathUtils.random(2,10);
            }
        }

    }

    // Handles the splash opening
    private void doSplash() {
        if (splashTimer < 90) {
            splashTimer++;
            if (splashTimer == 10) {
                tcLoad.play(0.6F);
            }
        } else {
            gameState = GameState.MAIN_MENU;
            tcLogo.dispose();
            tcLoad.dispose();
            track = getMusic();
            if (track != 3) {
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
                    music2.stop();
                    music3.stop();
                    menuMusic.stop();
                    break;
            }
            collide.play();
            if (track != 3) {
                gameOverMusic.play();
            }
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

        if (gameState == GameState.RUNNING) {
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
            if (faderShaderTimer >= 1.0F) {
                if (Gdx.input.justTouched()) {
                    float x = grabX();
                    float y = grabY();

                    // Replay Button
                    if (x > 750 && x < 3750 && y > 2900 && y < 4000) {
                        if (ads) {
                            adsController.hideBannerAd();
                        }
                        gameState = GameState.START;
                        gameOverMusic.stop();
                        resetWorld();
                        click.play();
                        return;
                    }

                    // Main Menu Button
                    if (x > 750 && x < 3750 && y > 1650 && y < 2750) {
                        if (ads) {
                            adsController.hideBannerAd();
                        }
                        gameState = GameState.MAIN_MENU;
                        menuMusic.play();
                        gameOverMusic.stop();
                        resetWorld();
                        click.play();
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

        if (gameState == GameState.MAIN_MENU) {
            shadowCreep = 50;

            colorify();

            moveCircles();

            if (Gdx.input.justTouched()) {
                float x = grabX();
                float y = grabY();
                // Play Button 1500, 5000, 6000, 2200
                if (x > 750 && x < 3750 && y > 3050 && y < 3950) {
                    gameState = GameState.START;
                    menuMusic.stop();
                    resetWorld();
                    click.play();
                    return;
                }
                // Options button 1500, 4750, 6000, 1800
                if (x > 750 && x < 3750 && y > 1975 && y < 2875) {
                    gameState = GameState.OPTIONS;
                    click.play();
                    return;
                }
                // Exit Button
                if (x > 1250 && x < 3250 && y > 900 && y < 1800) {
                    click.play();
                    Gdx.app.exit();
                    return;
                }

            }
            if (faderShaderTimer > 0.0F) {
                faderShaderTimer -= 0.1F;
                if (faderShaderTimer < 0.0F) {
                    faderShaderTimer = 0.0F;
                }
            }
        }

        if (gameState == GameState.OPTIONS) {
            shadowCreep = 50;
            moveCircles();

            colorify();
            
            if (Gdx.input.justTouched()) {
                float x = grabX();
                float y = grabY();
                // NO MUSIC *CLAP CLAPCLAPCLAPCLAP*
                if (x > 750 && x < 3750 && y > 4075 && y < 4825) {
                    track = 3;
                    setMusic(3);
                    menuMusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    click.play();
                    return;
                }
                // TRACK1
                if (x > 750 && x < 3750 && y > 3250 && y < 4000) {
                    track = 0;
                    setMusic(0);
                    menuMusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    music1.play();
                    click.play();
                    return;
                }
                // TRACK2
                if (x > 750 && x < 3750 && y > 2425 && y < 3175) {
                    track = 1;
                    setMusic(1);
                    menuMusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    music2.play();
                    click.play();
                    return;
                }
                // TRACK3
                if (x > 750 && x < 3750 && y > 1500 && y < 2350) {
                    track = 2;
                    setMusic(2);
                    menuMusic.stop();
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    music3.play();
                    click.play();
                    return;
                }
                // BACK1
                if (x > 750 && x < 7500 && y > 775 && y < 1525) {
                    gameState = GameState.MAIN_MENU;
                    music1.stop();
                    music2.stop();
                    music3.stop();
                    if (track != 3) {
                        menuMusic.play();
                    }
                    click.play();
                    return;
                }
            }
        }

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
                    case 3:
                        break;
                }
                return;
            }
        }

        if (gameState == GameState.SPLASH) {
            doSplash();
        }
    }

    private void colorify() {
        topLeft = topLeft.change(BACKGROUND_CHANGE_RATE);
        topRight = topRight.change(BACKGROUND_CHANGE_RATE);
        bottomLeft = bottomLeft.change(BACKGROUND_CHANGE_RATE);
        bottomRight = bottomRight.change(BACKGROUND_CHANGE_RATE);
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
        batch.draw(tcLogo, 1500, 3250, 1500, 1500);
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
        shapeRenderer.rect(750 + shadowCreep, 3050 - shadowCreep, 3000, 900);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(750, 3050, 3000, 900);

        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowCreep, 1975 - shadowCreep, 3000, 900);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(750, 1975, 3000, 900);

        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(1250 + shadowCreep, 900 - shadowCreep, 2000, 900);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(1250, 900, 2000, 900);

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
        font.draw(batch, "Exit", 1810, 1620);

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

        // nosound
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowCreep, 4075 - shadowCreep, 3000, 750);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 3) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(750, 4075, 3000, 750);

        // sound1
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowCreep, 3250 - shadowCreep, 3000, 750);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 0) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(750, 3250, 3000, 750);

        // sound2
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowCreep, 2425 - shadowCreep, 3000, 750);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 1) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(750, 2425, 3000, 750);

        // sound3
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowCreep, 1600 - shadowCreep, 3000, 750);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (track == 2) {
            shapeRenderer.setColor(0.8F, 1, 0.8F, 1);
        }
        shapeRenderer.rect(750, 1600, 3000, 750);

        //main menu button
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        shapeRenderer.rect(750 + shadowCreep, 775 - shadowCreep, 3000, 750);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(750, 775, 3000, 750);

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
        font.draw(batch, "Chroma", 595, 6975);
        font.draw(batch, "Dodge", 1560, 6025);
        font.setColor(1, 1, 1, 1);
        font.draw(batch, "Chroma", 545, 7025);
        font.draw(batch, "Dodge", 1510, 6100);

        font.setScale(2, 2);
        font.setColor(0, 0, 0, 0.4F);
        font.draw(batch, "No Track", 1550, 4675);
        font.draw(batch, "Track 1", 1550, 3850);
        font.draw(batch, "Track 2", 1550, 3025);
        font.draw(batch, "Track 3", 1550, 2200);
        font.draw(batch, "Back", 1750, 1375);

        batch.draw(shadow, 1020 + 50, 6975 - 1370 - 50, 2, 2, 4, 4, 150, 150, rotator, 0, 0, 4, 4, false, false);
        batch.draw(square, 1020, 6975 - 1370, 2, 2, 4, 4, 150, 150, rotator, 0, 0, 4, 4, false, false);

        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);
    }

    private void drawGameplay() {
        //Setup ShapeRenderer
        shapeRenderer.dispose();
        shapeRenderer = new ShapeRenderer();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT,
                topLeft.toColor(), topRight.toColor(), bottomLeft.toColor(), bottomRight.toColor());

        shapeRenderer.end();

        batch.dispose();
        batch = new SpriteBatch();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        // Draw background (includes text)
        for (Circlez circle : circles) {
            batch.draw(effects, circle.position.x, circle.position.y, circle.scale, circle.scale);
        }

        // Draws score text that's part of the background
        if (gameState != GameState.MAIN_MENU && gameState != GameState.OPTIONS) {
            if (faderShaderTimer < 1) {
                font.setScale(5, 5);
                font.setColor(0, 0, 0, 0.3F);
                font.drawMultiLine(batch, "" + score, 2250 + shadowCreep, 5950, 0, BitmapFont.HAlignment.CENTER);
                font.setColor(1, 1, 1, 1);
                font.drawMultiLine(batch, "" + score, 2250, 6000, 0, BitmapFont.HAlignment.CENTER);
            }
        }

        // End batch. Disable Blend.
        batch.end();
        Gdx.gl.glDisable(GL30.GL_BLEND);

        shapeRenderer.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        // Shadow of shapes
        shapeRenderer.setColor(0, 0, 0, 0.3F);
        if (gameState != GameState.MAIN_MENU && gameState != GameState.OPTIONS) {
            shapeRenderer.rect(player_x + shadowCreep, player_y - 50, PLAYER_SCALE, PLAYER_SCALE);
        }
        if (shadowCreep > 0) {
            shapeRenderer.rect(shadowCreep, 0, 450, WORLD_HEIGHT);
        } else {
            shapeRenderer.rect(4500 + shadowCreep, 0, -450, WORLD_HEIGHT);
        }
        for (Barrier barrier : barriers) {
            shapeRenderer.rect(barrier.position.x + shadowCreep - 4659, barrier.position.y - 50, 3000, PLAYER_SCALE);
            shapeRenderer.rect(barrier.position.x + shadowCreep, barrier.position.y - 50, 3000, PLAYER_SCALE);
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
        if (score <= highScore){
            font.draw(batch, "Highscore: " + highScore, 1360, 4605 - (7150 * (1 - faderShaderTimer)));
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
        tcLogo.dispose();
        effects.dispose();
        square.dispose();
        shadow.dispose();

        menuMusic.dispose();
        music1.dispose();
        music2.dispose();
        music3.dispose();
        gameOverMusic.dispose();
        collide.dispose();
        tcLoad.dispose();
        click.dispose();

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
        boolean activated;

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

    class RgbColor {
        int red;
        int green;
        int blue;
        boolean redFlip;
        boolean greenFlip;
        boolean blueFlip;

        RgbColor(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;

            if (this.red > 255) redFlip = true;
            if (this.red < 0) redFlip = false;
            if (this.green > 255) greenFlip = true;
            if (this.green < 0) greenFlip = false;
            if (this.blue > 255) blueFlip = true;
            if (this.blue < 0) blueFlip = false;
        }

        Color toColor() {
            return new Color(this.red / 255f, this.green / 255f, this.blue / 255f, 1f);
        }

        RgbColor change(int maxAmount) {
            if (redFlip) {
                red -= Math.floor(RANDOM.nextDouble() * maxAmount);
            } else {
                red += Math.floor(RANDOM.nextDouble() * maxAmount);
            }
            if (greenFlip) {
                green -= Math.floor(RANDOM.nextDouble() * maxAmount);
            } else {
                green += Math.floor(RANDOM.nextDouble() * maxAmount);
            }
            if (blueFlip) {
                blue -= Math.floor(RANDOM.nextDouble() * maxAmount);
            } else {
                blue += Math.floor(RANDOM.nextDouble() * maxAmount);
            }

            if (this.red > 220) redFlip = true;
            if (this.red < 80) redFlip = false;
            if (this.green > 220) greenFlip = true;
            if (this.green < 80) greenFlip = false;
            if (this.blue > 220) blueFlip = true;
            if (this.blue < 80) blueFlip = false;

            return this;
        }
    }
}
