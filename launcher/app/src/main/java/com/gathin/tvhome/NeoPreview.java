package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * The Wallpapers screen's preview: Home in miniature on the picked colour, drawn with a few rounded rectangles (the
 * outlined frame, the card with its lines and button, the row of coloured tiles), so no screenshot is kept.
 */
public final class NeoPreview extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float d;
    private int background = 0xFFEEE8DC;
    private int[] tiles = {Neo.BLUE, Neo.PINK, Neo.YELLOW, Neo.LAVENDER, Neo.TEAL, Neo.ORANGE, Neo.CREAM};

    public NeoPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        d = getResources().getDisplayMetrics().density;
        ink.setColor(Neo.INK);
        stroke.setColor(Neo.INK);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f * d);
    }

    /** A picture wallpaper, drawn into the frame instead of the colour; null for a colour one. */
    private BitmapShader picture;
    private final Matrix pictureMatrix = new Matrix();
    private Bitmap pictureBitmap;

    void set(int background, int[] tiles, Bitmap picture) {
        this.background = background;
        if (tiles.length > 0) this.tiles = tiles;
        pictureBitmap = picture;
        this.picture = picture == null ? null : new BitmapShader(picture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = 8 * d, w = getWidth() - s, h = getHeight() - s;
        // The frame: the wallpaper's colour, an outline and a hard shadow.
        box(c, 0, 0, w, h, 20 * d, background, s);
        if (picture != null) {
            // The picture cropped to the frame like Home's centerCrop, inside the outline.
            float scale = Math.max(w / pictureBitmap.getWidth(), h / pictureBitmap.getHeight());
            pictureMatrix.setScale(scale, scale);
            pictureMatrix.postTranslate((w - pictureBitmap.getWidth() * scale) / 2f, (h - pictureBitmap.getHeight() * scale) / 2f);
            picture.setLocalMatrix(pictureMatrix);
            fill.setShader(picture);
            float in = stroke.getStrokeWidth();
            r.set(in, in, w - in, h - in);
            c.drawRoundRect(r, 18 * d, 18 * d, fill);
            fill.setShader(null);
        }
        // The tab-like pill at the top middle.
        box(c, w * 0.36f, 14 * d, w * 0.64f, 30 * d, -1, Neo.CREAM, 2 * d);
        r.set(w * 0.36f + 4 * d, 18 * d, w * 0.44f, 26 * d);
        fill.setColor(Neo.PINK);
        c.drawRoundRect(r, 4 * d, 4 * d, fill);
        // The card with a kicker, a title, two text lines and the yellow button.
        float cl = 30 * d, ct = 50 * d, cr = w * 0.62f, cb = h * 0.66f;
        box(c, cl, ct, cr, cb, 10 * d, Neo.CREAM, 4 * d);
        bar(c, cl + 14 * d, ct + 14 * d, cl + 70 * d, 5 * d, Neo.INK);
        bar(c, cl + 14 * d, ct + 26 * d, cl + (cr - cl) * 0.66f, 12 * d, Neo.INK);
        bar(c, cl + 14 * d, ct + 46 * d, cr - 22 * d, 4 * d, 0xFFC9BFAE);
        bar(c, cl + 14 * d, ct + 56 * d, cl + (cr - cl) * 0.6f, 4 * d, 0xFFC9BFAE);
        box(c, cl + 14 * d, ct + 70 * d, cl + 64 * d, ct + 84 * d, -1, Neo.YELLOW, 0);
        // The dock: seven tiles across the bottom.
        int n = 7;
        float gap = 8 * d, left = 26 * d, right = w - 26 * d, tw = (right - left - gap * (n - 1)) / n;
        float tt = h * 0.74f, tb = h - 18 * d;
        for (int i = 0; i < n; i++) {
            float x = left + i * (tw + gap);
            box(c, x, tt, x + tw, tb, 6 * d, tiles[i % tiles.length], i == 0 ? 4 * d : 2 * d);
        }
    }

    private void box(Canvas c, float l, float t, float rt, float b, float radius, int color, float shadow) {
        float rad = radius < 0 ? (b - t) / 2f : radius;
        if (shadow > 0) {
            r.set(l + shadow, t + shadow, rt + shadow, b + shadow);
            c.drawRoundRect(r, rad, rad, ink);
        }
        r.set(l, t, rt, b);
        fill.setColor(color);
        c.drawRoundRect(r, rad, rad, fill);
        float half = stroke.getStrokeWidth() / 2f;
        r.inset(half, half);
        c.drawRoundRect(r, rad, rad, stroke);
    }

    private void bar(Canvas c, float l, float t, float rt, float height, int color) {
        r.set(l, t, rt, t + height);
        fill.setColor(color);
        c.drawRoundRect(r, height / 2f, height / 2f, fill);
    }
}
