package com.tealcube.java.games;

import com.badlogic.gdx.graphics.Color;

import java.util.Random;

public class RgbColor {
    private static final Random RANDOM = new Random();

    private int red;
    private int green;
    private int blue;
    private boolean redFlip;
    private boolean greenFlip;
    private boolean blueFlip;

    public RgbColor(int red, int green, int blue) {
        this.red = Math.min(255, Math.max(0, red));
        this.green = Math.min(255, Math.max(0, green));
        this.blue = Math.min(255, Math.max(0, blue));

        this.redFlip = RANDOM.nextBoolean();
        this.greenFlip = RANDOM.nextBoolean();
        this.blueFlip = RANDOM.nextBoolean();

        if (this.red > 255) {
            redFlip = true;
        }
        if (this.red < 0) {
            redFlip = false;
        }
        if (this.green > 255) {
            greenFlip = true;
        }
        if (this.green < 0) {
            greenFlip = false;
        }
        if (this.blue > 255) {
            blueFlip = true;
        }
        if (this.blue < 0) {
            blueFlip = false;
        }
    }

    public Color toColor() {
        return new Color(this.red / 255f, this.green / 255f, this.blue / 255f, 1f);
    }

    public RgbColor change(int maxAmount) {
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

        if (this.red > 220) {
            redFlip = true;
        }
        if (this.red < 70) {
            redFlip = false;
        }
        if (this.green > 220) {
            greenFlip = true;
        }
        if (this.green < 70) {
            greenFlip = false;
        }
        if (this.blue > 220) {
            blueFlip = true;
        }
        if (this.blue < 70) {
            blueFlip = false;
        }

        return this;
    }

    public RgbColor invertFlip() {
        redFlip = !redFlip;
        greenFlip = !greenFlip;
        blueFlip = !blueFlip;
        return this;
    }

    public RgbColor towards(RgbColor color, int maxAmount) {
        int theirRed = color.red;
        int theirGreen = color.green;
        int theirBlue = color.blue;

        int redDiff = Math.abs(red - theirRed);
        int blueDiff = Math.abs(blue - theirBlue);
        int greenDiff = Math.abs(green - theirGreen);

        if (redDiff <= maxAmount) {
            red = color.red;
        }
        if (greenDiff <= maxAmount) {
            green = color.green;
        }
        if (blueDiff <= maxAmount) {
            blue = color.blue;
        }

        if (red < theirRed) {
            red += Math.floor(RANDOM.nextDouble() * maxAmount);
        } else if (red > theirRed) {
            red -= Math.floor(RANDOM.nextDouble() * maxAmount);
        }
        if (green < theirGreen) {
            green += Math.floor(RANDOM.nextDouble() * maxAmount);
        } else if (green > theirGreen) {
            green -= Math.floor(RANDOM.nextDouble() * maxAmount);
        }
        if (blue < theirBlue) {
            blue += Math.floor(RANDOM.nextDouble() * maxAmount);
        } else if (blue > theirBlue) {
            blue -= Math.floor(RANDOM.nextDouble() * maxAmount);
        }

        return this;
    }

    @Override
    public int hashCode() {
        int result = getRed();
        result = 31 * result + getGreen();
        result = 31 * result + getBlue();
        result = 31 * result + (isRedFlip() ? 1 : 0);
        result = 31 * result + (isGreenFlip() ? 1 : 0);
        result = 31 * result + (isBlueFlip() ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RgbColor rgbColor = (RgbColor) o;

        if (getRed() != rgbColor.getRed()) {
            return false;
        }
        if (getGreen() != rgbColor.getGreen()) {
            return false;
        }
        if (getBlue() != rgbColor.getBlue()) {
            return false;
        }
        if (isRedFlip() != rgbColor.isRedFlip()) {
            return false;
        }
        if (isGreenFlip() != rgbColor.isGreenFlip()) {
            return false;
        }
        return isBlueFlip() == rgbColor.isBlueFlip();
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public boolean isRedFlip() {
        return redFlip;
    }

    public boolean isGreenFlip() {
        return greenFlip;
    }

    public boolean isBlueFlip() {
        return blueFlip;
    }
}
