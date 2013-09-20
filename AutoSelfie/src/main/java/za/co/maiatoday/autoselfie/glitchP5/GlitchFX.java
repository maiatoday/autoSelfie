package za.co.maiatoday.autoselfie.glitchP5;

import android.graphics.Bitmap;

import java.util.Random;

import za.co.maiatoday.autoselfie.util.MathsUtils;

/**
 * Created by maia on 2013/09/20.
 */
public class GlitchFX {
    Bitmap bitmap;
    int[] area = new int[0];
    int[] area_ = new int[0];
    int lastxPos;
    int lastyPos;
    int lastw;
    int lasth;
    int lastsX;
    int lastsY;
    int[] pixels;
    int[] lastPixels;

    public Bitmap getBitmap() {
        return bitmap;
    }


    public GlitchFX(Bitmap b) {
        this.bitmap = b;
        lastPixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
    }

    public void open() {
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    public void close() {
        bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        System.arraycopy(pixels, 0, lastPixels, 0, pixels.length);
    }

    public void glitch(int xPos, int yPos, int w, int h, int sX, int sY) {
        computeArea(xPos, yPos, w, h, sX, sY);
        Random r = new Random();
        int shiftr = r.nextInt(16);
        if (area.length < area_.length) {
            for (int j = 0; j < area.length; j++) {
                pixels[area[j]] = lastPixels[area_[j]];
                // bitmap.pixels[area[j]] += bitmap.pixels[area_[j]] << shiftr;
            }
        } else {
            for (int j = 0; j < area_.length; j++) {
                pixels[area[j]] += lastPixels[area_[j]] << shiftr;
                // bitmap.pixels[area[j]] += bitmap.pixels[area_[j]] << shiftr;
            }
        }
    }

    void computeArea(int xPos, int yPos, int w, int h, int sX, int sY) {
        if (xPos != lastxPos || yPos != lastyPos || w != lastw || h != lasth || sX != lastsX || sY != lastsY) {
            int startX = MathsUtils.constrain(xPos - w / 2, 0, bitmap.getWidth() - 1);
            int startY = MathsUtils.constrain(yPos - h / 2, 0, bitmap.getHeight() - 1);
            int endX = MathsUtils.constrain(xPos + w / 2, 0, bitmap.getWidth() - 1);
            int endY = MathsUtils.constrain(yPos + h / 2, 0, bitmap.getHeight() - 1);

            int startX_ = MathsUtils.constrain(xPos - w / 2 + sX, 0, bitmap.getWidth() - 1);
            int startY_ = MathsUtils.constrain(yPos - h / 2 + sY, 0, bitmap.getHeight() - 1);
            int endX_ = MathsUtils.constrain(xPos + w / 2 + sX, 0, bitmap.getWidth() - 1);
            int endY_ = MathsUtils.constrain(yPos + h / 2 + sY, 0, bitmap.getHeight() - 1);

            w = Math.abs(startX - endX);
            h = Math.abs(startY - endY);

            area = new int[w * h];
            int i = 0;
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    area[i] = bitmap.getWidth() * y + x;
                    i++;
                }
            }

            w = Math.abs(startX_ - endX_);
            h = Math.abs(startY_ - endY_);
            area_ = new int[w * h];
            i = 0;
            for (int y = startY_; y < endY_; y++) {
                for (int x = startX_; x < endX_; x++) {
                    area_[i] = bitmap.getWidth() * y + x;
                    i++;
                }
            }
        }
        lastxPos = xPos;
        lastyPos = yPos;
        lastw = w;
        lasth = h;
        lastsX = sX;
        lastsY = sY;
    }
}
