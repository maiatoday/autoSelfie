package za.co.maiatoday.autoselfie.glitchP5;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Random;

/**
 * Ported from processing glitchP5 by maia on 2013/09/20.
 */
public class GlitchP5 {
    GlitchFX glfx;
    ArrayList<TimedGlitcher> timedGlitchers = new ArrayList<TimedGlitcher>();
    Bitmap bitmap;

    public GlitchP5(Bitmap p) {
        glfx = new GlitchFX(p.getWidth(), p.getHeight());
        bitmap = p.copy(p.getConfig(), true);
    }

    public Bitmap getGlitchedBitmap() {
        return glfx.getBitmap();
    }

    public void run() {
        glfx.open(bitmap);
        for (int i = timedGlitchers.size() - 1; i >= 0; i--) {
            TimedGlitcher tg = timedGlitchers.get(i);
            tg.run();
            if (tg.done())
                timedGlitchers.remove(tg);
        }
        glfx.close();
    }


    public void glitch(int x, int y, int spreadX, int spreadY, int diaX, int diaY, int amount, float randomness, int attack, int sustain) {
        for (int i = 0; i < amount; i++) {
            Random r = new Random();
            int att = r.nextInt(attack);
            timedGlitchers.add(new TimedGlitcher((int) (x + (r.nextInt(spreadX) - spreadX / 2)),
                (int) (y + (r.nextInt(spreadY / 2) - spreadY / 2)),
                (int) (diaX * randomness), (int) (diaY * randomness),
                randomness, att, r.nextInt(sustain))
            );
        }
    }

    private class TimedGlitcher {
        int x, y, diaX, diaY, on;
        int timer;
        float randomness;

        int sX, sY;

        int onset = 0;

        TimedGlitcher(int x, int y, int diaX, int diaY, float randomness, int on, int time) {
            this.x = x;
            this.y = y;
            this.diaX = diaX;
            this.diaY = diaY;
            this.randomness = randomness;
            this.on = on;
            this.timer = time;

            Random r = new Random();
            sX = (r.nextInt(20) - 10);
            sY = (r.nextInt(20) - 10);
        }

        void run() {
            if (onset >= on) {
                glfx.glitch(x, y, diaX, diaY, sX, sY);
                timer--;
            }
            onset++;
        }

        boolean done() {
            if (timer <= 0)
                return true;
            else
                return false;
        }
    }
}
