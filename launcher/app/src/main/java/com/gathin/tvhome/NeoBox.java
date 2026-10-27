package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * A neo-brutalist box: a flat fill with a thick ink outline and a hard ink shadow down and to the right, with no
 * blur. Three rounded rectangles per frame, no bitmap and no offscreen layer.
 *
 * The shadow is drawn inside the bounds, so nothing has to stop clipping for it: the box keeps the room of the
 * largest shadow ({@link #reserve()}) free at the right and bottom. Focused, the shadow grows into that room; the
 * box itself stays put, so whatever is centred on it stays centred (a tile lifts by its view's translation).
 */
final class NeoBox extends Drawable {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    /** Corner radius; below 0, half the height (a pill). */
    private final float radius;
    private final float stroke;
    private final float shadow;
    private final float focusedShadow;
    private boolean lifted;

    NeoBox(int color, float radius, float stroke, float shadow, float focusedShadow) {
        this.radius = radius;
        this.stroke = stroke;
        this.shadow = shadow;
        this.focusedShadow = Math.max(shadow, focusedShadow);
        fill.setColor(color);
        ink.setColor(Neo.INK);
        outline.setColor(Neo.INK);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(stroke);
    }

    /** A box whose shadow does not change with focus. */
    NeoBox(int color, float radius, float stroke, float shadow) {
        this(color, radius, stroke, shadow, shadow);
    }

    /** The room at the right and bottom the shadow takes: add it to the view's padding so content stays on the box. */
    float reserve() {
        return focusedShadow;
    }

    void setColor(int color) {
        if (fill.getColor() == color) return;
        fill.setColor(color);
        invalidateSelf();
    }

    int getColor() {
        return fill.getColor();
    }

    /** As background of v, with the shadow's room added to its right and bottom padding. */
    static NeoBox applyTo(View v, NeoBox box) {
        int r = Math.round(box.reserve());
        v.setBackground(box);
        v.setPaddingRelative(v.getPaddingStart(), v.getPaddingTop(), v.getPaddingEnd() + r, v.getPaddingBottom() + r);
        return box;
    }

    /**
     * A pill (half-height corners, 2.5dp outline, 3dp shadow) behind v; a focusable one lifts to a 7dp shadow on focus.
     * A view with a fixed size (the header's 42dp buttons) grows by the shadow's room instead of losing it inside.
     */
    static NeoBox pill(View v, int color, float density, boolean focusable) {
        float shadow = 3 * density;
        NeoBox box = applyTo(v, new NeoBox(color, -1, 2.5f * density, shadow, focusable ? 7 * density : shadow));
        android.view.ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp == null) return box;
        int room = Math.round(box.reserve());
        if (lp.width > 0) lp.width += room;
        if (lp.height > 0) lp.height += room;
        v.setLayoutParams(lp);
        return box;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float s = lifted ? focusedShadow : shadow;
        box.set(b.left, b.top, b.right - focusedShadow, b.bottom - focusedShadow);
        float r = radius < 0 ? box.height() / 2f : radius;
        if (s > 0) {
            box.offset(s, s);
            canvas.drawRoundRect(box, r, r, ink);
            box.offset(-s, -s);
        }
        canvas.drawRoundRect(box, r, r, fill);
        if (stroke > 0) {
            float half = stroke / 2f;
            box.inset(half, half);
            canvas.drawRoundRect(box, Math.max(0, r - half), Math.max(0, r - half), outline);
        }
    }

    @Override
    public boolean isStateful() {
        return focusedShadow > shadow;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean focused = false;
        for (int s : state) {
            if (s == android.R.attr.state_focused) {
                focused = true;
                break;
            }
        }
        if (focused == lifted) return false;
        lifted = focused;
        invalidateSelf();
        return true;
    }

    @Override
    public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
        ink.setAlpha(alpha);
        outline.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        fill.setColorFilter(filter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
