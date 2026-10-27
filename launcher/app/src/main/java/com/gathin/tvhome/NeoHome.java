package com.gathin.tvhome;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

/**
 * Home in neo_style (Neo): the round badge and the yellow clock pill in the header, the card for the focused dock app
 * in the top shelf, the VPN pill's colours and the strip along the bottom (the wallpaper's name and a dot per row).
 * MainActivity keeps the focus, the tiles and the shelf's position; this only builds and fills the views.
 */
final class NeoHome {

    /** What the card's buttons ask of Home. */
    interface Host {
        /** Opens the dock app at this position, as OK on its tile would. */
        void openTile(int position);

        void removeFromDock(int position);

        void focusTile(int position);
    }

    private final Activity activity;
    private final Neo neo;
    private final Host host;
    private final float density;
    private final View root;
    private final View bottom;
    private final TextView category, name, caption, wallpaperPill;
    private final TextView open, dock;
    private final LinearLayout dots;
    private int position = -1;

    NeoHome(Activity activity, Neo neo, View root, LinearLayout hero, Host host) {
        this.activity = activity;
        this.neo = neo;
        this.host = host;
        this.root = root;
        density = activity.getResources().getDisplayMetrics().density;
        LayoutInflater inflater = LayoutInflater.from(activity);

        // Header: the round badge instead of the greeting and the big clock, the time as a yellow pill by the buttons.
        ViewGroup left = activity.findViewById(R.id.leftBlock);
        for (int i = 0; i < left.getChildCount(); i++) left.getChildAt(i).setVisibility(View.GONE);
        if (!neo.avatar.isEmpty()) left.addView(avatar(neo.avatar));
        LinearLayout buttons = activity.findViewById(R.id.topButtons);
        TextView settings = activity.findViewById(R.id.btnSettings);
        buttons.addView(clockPill(), buttons.indexOfChild(settings));
        settings.setTextColor(Neo.INK);
        settings.setCompoundDrawableTintList(ColorStateList.valueOf(Neo.INK));
        pill(settings, Neo.CREAM, true);
        buttons.setClipChildren(false);

        // The shelf: the card instead of the big name (which stays, hidden, as the shelf's change check).
        for (int i = 0; i < hero.getChildCount(); i++) hero.getChildAt(i).setVisibility(View.GONE);
        hero.setClipChildren(false);
        hero.setClipToPadding(false);
        ViewGroup.MarginLayoutParams heroLp = (ViewGroup.MarginLayoutParams) hero.getLayoutParams();
        heroLp.setMarginStart(Math.round(48 * density));
        hero.setLayoutParams(heroLp);
        View card = inflater.inflate(R.layout.neo_card, hero, false);
        hero.addView(card, 0);
        View body = card.findViewById(R.id.neoCardBody);
        NeoBox.applyTo(body, new NeoBox(Neo.CREAM, 22 * density, 3 * density, 10 * density));
        ((ImageView) card.findViewById(R.id.neoCorner)).setImageDrawable(new Corner(density));
        category = card.findViewById(R.id.neoCategory);
        category.setBackground(new NeoBox(Neo.INK, -1, 0, 0));
        name = card.findViewById(R.id.neoName);
        caption = card.findViewById(R.id.neoCaption);
        open = card.findViewById(R.id.neoOpen);
        dock = card.findViewById(R.id.neoDock);
        pill(open, Neo.YELLOW, true);
        pill(dock, Neo.CREAM, true);
        open.setOnClickListener(v -> host.openTile(position));
        dock.setOnClickListener(v -> host.removeFromDock(position));
        // The dock button turns yellow while focused, so the focus reads at a distance as well as by the lift.
        dock.setOnFocusChangeListener((v, focused) ->
                ((NeoBox) v.getBackground()).setColor(focused ? Neo.YELLOW : Neo.CREAM));
        View.OnKeyListener keys = (v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                host.focusTile(position);
                return true;
            }
            // Nothing past the two buttons: the default search would jump to a pill or a tile.
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return v == open;
            return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && v == dock;
        };
        open.setOnKeyListener(keys);
        dock.setOnKeyListener(keys);

        bottom = inflater.inflate(R.layout.neo_bottom, (ViewGroup) root, false);
        ((ViewGroup) root).addView(bottom);
        wallpaperPill = bottom.findViewById(R.id.neoWallpaperPill);
        pill(wallpaperPill, Neo.CREAM, false);
        dots = bottom.findViewById(R.id.neoDots);
        refresh();
    }

    /** The wallpaper Home was built with; a picture comes and goes only with a rebuild. */
    private String shown;

    /**
     * The wallpaper may have changed with the day: a new colour is just painted, a picture coming or going needs
     * Home rebuilt (its WallpaperPlayer is set up from Config). The Wallpapers screen's own changes rebuild anyway.
     */
    void refresh() {
        Neo.Wallpaper w = neo.current(activity);
        if (shown != null && !shown.equals(w.name)) {
            activity.recreate();
            return;
        }
        shown = w.name;
        root.setBackgroundColor(w.color);
        bottom.setBackgroundColor(Neo.bottomColor(w));
        wallpaperPill.setText(activity.getString(R.string.neo_wallpaper_pill, w.name));
    }

    /** Fills the card for the dock app at this position. */
    void show(int position, String pkg, String label) {
        this.position = position;
        String cat = pkg == null ? null : neo.categories.get(pkg);
        category.setText(cat != null ? cat : activity.getString(R.string.neo_app));
        name.setText(label);
        long when = pkg == null ? 0 : LastOpened.get(activity, pkg);
        caption.setText(when > 0 ? activity.getString(R.string.last_opened, LastOpened.describe(activity, when))
                : activity.getString(R.string.neo_press_ok));
    }

    /** How much higher the dock sits than in the usual look, for its name pill above the bottom strip. */
    int dockLift() {
        return Math.round(44 * density);
    }

    /** Up from the dock: onto the card's Open button. */
    void focusCard() {
        open.requestFocus();
    }

    /**
     * One dot per row of Home, the focused row's filled. The strip shows on the dock's row only: further down the
     * rows scroll to the bottom of the screen, where it would cover them.
     */
    void onRow(int row, int rows) {
        bottom.animate().alpha(row == 0 ? 1f : 0f).setDuration(150).start();
        if (dots.getChildCount() != rows) {
            dots.removeAllViews();
            int size = Math.round(12 * density);
            for (int i = 0; i < rows; i++) {
                View dot = new View(activity);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                if (i > 0) lp.setMarginStart(Math.round(8 * density));
                dots.addView(dot, lp);
            }
        }
        for (int i = 0; i < rows; i++) {
            dots.getChildAt(i).setBackground(new NeoBox(i == row ? Neo.INK : Neo.CREAM, -1, 2 * density, 0));
        }
        dots.setVisibility(rows > 1 ? View.VISIBLE : View.INVISIBLE);
    }

    /** The VPN pill: teal with a shield and "Connected" while a VPN is up, pink "Off" while it is not. */
    void styleVpn(TextView pill, boolean up) {
        SpannableStringBuilder text = new SpannableStringBuilder("VPN\n");
        text.setSpan(new RelativeSizeSpan(1.25f), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.append(activity.getString(up ? R.string.neo_vpn_connected : R.string.neo_vpn_off));
        pill.setSingleLine(false);
        pill.setMaxLines(2);
        pill.setLineSpacing(0, 0.9f);
        pill.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        pill.setText(text);
        pill.setTextColor(Neo.INK);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        pill.setTypeface(UiFont.get(activity, R.string.font_bold));
        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(up ? R.drawable.ic_shield : R.drawable.ic_lock, 0, 0, 0);
        pill.setCompoundDrawableTintList(ColorStateList.valueOf(Neo.INK));
        if (pill.getBackground() instanceof NeoBox) {
            ((NeoBox) pill.getBackground()).setColor(up ? Neo.TEAL : Neo.PINK);
            return;
        }
        // Two lines need more than the header's 42dp: 50dp, plus the shadow's room that pill() adds.
        ViewGroup.LayoutParams lp = pill.getLayoutParams();
        lp.height = Math.round(50 * density);
        pill.setLayoutParams(lp);
        pill.setPaddingRelative(Math.round(14 * density), 0, Math.round(18 * density), 0);
        pill.setCompoundDrawablePadding(Math.round(8 * density));
        pill(pill, up ? Neo.TEAL : Neo.PINK, true);
    }

    private void pill(TextView v, int color, boolean focusable) {
        NeoBox.pill(v, color, density, focusable);
    }

    private TextView avatar(String letter) {
        TextView v = new TextView(activity);
        int size = Math.round(46 * density);
        v.setText(letter);
        v.setGravity(Gravity.CENTER);
        v.setTextColor(Neo.INK);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        v.setTypeface(UiFont.get(activity, R.string.font_display));
        v.setIncludeFontPadding(false);
        NeoBox box = NeoBox.applyTo(v, new NeoBox(Neo.YELLOW, -1, 2.5f * density, 3 * density));
        v.setLayoutParams(new LinearLayout.LayoutParams(size + Math.round(box.reserve()), size + Math.round(box.reserve())));
        return v;
    }

    private TextClock clockPill() {
        TextClock clock = new TextClock(activity);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(Neo.INK);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        clock.setTypeface(UiFont.get(activity, R.string.font_display_bold));
        clock.setGravity(Gravity.CENTER);
        clock.setIncludeFontPadding(false);
        int pad = Math.round(16 * density);
        clock.setPaddingRelative(pad, 0, pad, 0);
        clock.setFocusable(false);
        NeoBox.applyTo(clock, new NeoBox(Neo.YELLOW, -1, 2.5f * density, 3 * density));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                Math.round(42 * density + 3 * density));
        lp.setMarginStart(Math.round(10 * density));
        clock.setLayoutParams(lp);
        return clock;
    }

    /** The card's yellow corner: a triangle with an ink outline and a hard shadow, pointing up and to the right. */
    static final class Corner extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float shadow;

        Corner(float density) {
            shadow = 4 * density;
            fill.setColor(Neo.YELLOW);
            ink.setColor(Neo.INK);
            outline.setColor(Neo.INK);
            outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeWidth(3 * density);
            outline.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width() - shadow, h = b.height() - shadow;
            path.reset();
            path.moveTo(b.left + w * 0.05f, b.top + h * 0.62f);
            path.lineTo(b.left + w * 0.82f, b.top + h * 0.05f);
            path.lineTo(b.left + w * 0.95f, b.top + h * 0.95f);
            path.close();
            path.offset(shadow, shadow);
            canvas.drawPath(path, ink);
            path.offset(-shadow, -shadow);
            canvas.drawPath(path, fill);
            canvas.drawPath(path, outline);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
