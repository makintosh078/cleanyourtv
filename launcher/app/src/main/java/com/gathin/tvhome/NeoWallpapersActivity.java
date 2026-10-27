package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * neo_style's Wallpapers screen: Home in miniature on the picked wallpaper, the collection (the theme's colours, then
 * the pictures in its wallpapers/ folder) and two options, Change every day and Show as screensaver. Opened from
 * the gear's menu and Launcher settings.
 */
public final class NeoWallpapersActivity extends Activity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    private Neo neo;
    private SharedPreferences prefs, choices;
    /** Small decodes of the pictures for their swatches and the preview, made once per screen. */
    private final Map<File, Bitmap> thumbs = new HashMap<>();
    private float d;
    private LinearLayout list;
    private NeoPreview preview;
    private TextView infoName;
    private final List<View> rows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_neo_wallpapers);
        d = getResources().getDisplayMetrics().density;
        neo = Neo.load(this);
        prefs = Neo.prefs(this);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);

        TextView back = findViewById(R.id.neoBack);
        NeoBox.pill(back, Neo.CREAM, d, true);
        back.setOnClickListener(v -> finish());
        NeoBox.pill(findViewById(R.id.neoTime), Neo.YELLOW, d, false);
        NeoBox.applyTo(findViewById(R.id.neoTitle), new NeoBox(Neo.PINK, 14 * d, 3 * d, 6 * d));
        NeoBox.applyTo(findViewById(R.id.neoInfo), new NeoBox(Neo.CREAM, 16 * d, 3 * d, 6 * d));
        preview = findViewById(R.id.neoPreview);
        infoName = findViewById(R.id.neoInfoName);
        list = findViewById(R.id.neoList);
        build();
    }

    /** Lays the right column out again from the saved choices, keeping the focus on the same row. */
    private void build() {
        int focused = rows.indexOf(getCurrentFocus());
        list.removeAllViews();
        rows.clear();
        list.addView(chip(getString(R.string.neo_collection)));
        int current = neo.indexOf(neo.current(this).name);
        for (int i = 0; i < neo.wallpapers.size(); i++) {
            Neo.Wallpaper w = neo.wallpapers.get(i);
            final int index = i;
            row(w, i == current, v -> pick(index));
        }
        View options = chip(getString(R.string.neo_options));
        ((LinearLayout.LayoutParams) options.getLayoutParams()).topMargin = Math.round(18 * d);
        list.addView(options);
        option(getString(R.string.neo_daily), prefs.getBoolean(Neo.KEY_DAILY, false), v -> {
            boolean daily = !prefs.getBoolean(Neo.KEY_DAILY, false);
            // Count from what is on screen now, so switching it on or off never jumps to another wallpaper today.
            Neo.pick(this, neo.current(this).name);
            prefs.edit().putBoolean(Neo.KEY_DAILY, daily).apply();
            build();
        });
        option(getString(R.string.neo_as_screensaver), neo.clockWallpaper(this), v -> {
            prefs.edit().putBoolean(Neo.KEY_CLOCK_WALLPAPER, !neo.clockWallpaper(this)).apply();
            build();
        });

        Neo.Wallpaper w = neo.current(this);
        preview.set(w.color, neo.tileColors, w.image == null ? null : thumb(w.image));
        infoName.setText(w.name);
        ((TextView) findViewById(R.id.neoInfoText)).setText(w.image == null ? R.string.neo_wallpaper_about : R.string.neo_picture_about);
        int target = focused >= 0 ? focused : Math.max(0, current);
        if (!rows.isEmpty()) rows.get(Math.min(target, rows.size() - 1)).requestFocus();
    }

    /** A picture at 1/4 size (480x270 from 1080p, about 250 KB in RGB_565), enough for a swatch and the preview. */
    private Bitmap thumb(File f) {
        Bitmap b = thumbs.get(f);
        if (b != null || thumbs.containsKey(f)) return b;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 4;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            b = BitmapFactory.decodeFile(f.getPath(), o);
        } catch (RuntimeException | OutOfMemoryError e) {
            b = null;
        }
        thumbs.put(f, b);
        return b;
    }

    @Override
    protected void onDestroy() {
        for (Bitmap b : thumbs.values()) if (b != null) b.recycle();
        thumbs.clear();
        super.onDestroy();
    }

    private void pick(int index) {
        Neo.pick(this, neo.wallpapers.get(index).name);
        changed();
        build();
    }

    /** As Launcher settings does: Home rebuilds when it next resumes, so it loads or drops the picture. */
    private void changed() {
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY, choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private View chip(String text) {
        TextView v = text(text.toUpperCase(getResources().getConfiguration().getLocales().get(0)), 11, R.string.font_bold, Neo.CREAM);
        v.setLetterSpacing(0.12f);
        v.setPadding(Math.round(12 * d), Math.round(4 * d), Math.round(12 * d), Math.round(5 * d));
        v.setBackground(new NeoBox(Neo.INK, -1, 0, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Math.round(8 * d);
        v.setLayoutParams(lp);
        return v;
    }

    /** A collection row: swatch, name, description and a tick when it is the one shown; yellow when picked. */
    private void row(Neo.Wallpaper w, boolean on, View.OnClickListener click) {
        String name = w.name, about = w.description;
        LinearLayout row = focusableRow(on ? Neo.YELLOW : Neo.CREAM, click);
        ImageView swatch = new ImageView(this);
        swatch.setBackground(new NeoBox(w.color, 8 * d, 2.5f * d, 0));
        if (w.image != null) {
            // The picture inside the swatch's outline, cropped to its shape.
            swatch.setImageBitmap(thumb(w.image));
            swatch.setScaleType(ImageView.ScaleType.CENTER_CROP);
            int in = Math.round(2.5f * d);
            swatch.setPadding(in, in, in, in);
            swatch.setCropToPadding(true);
        }
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(Math.round(64 * d), Math.round(42 * d));
        sp.setMarginEnd(Math.round(14 * d));
        row.addView(swatch, sp);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(name, 17, R.string.font_display, Neo.INK));
        if (!about.isEmpty()) texts.addView(text(about, 12, R.string.font_regular, 0xFF3D3A35));
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (on) {
            TextView tick = text("✓", 15, R.string.font_bold, Neo.YELLOW);
            tick.setGravity(Gravity.CENTER);
            tick.setBackground(new NeoBox(Neo.INK, -1, 0, 0));
            row.addView(tick, new LinearLayout.LayoutParams(Math.round(28 * d), Math.round(28 * d)));
        }
    }

    /** An option row: its name and a switch, teal with the knob at the end while on. */
    private void option(String name, boolean on, View.OnClickListener click) {
        LinearLayout row = focusableRow(Neo.CREAM, click);
        row.addView(text(name, 15, R.string.font_bold, Neo.INK),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        FrameLayout track = new FrameLayout(this);
        track.setBackground(new NeoBox(on ? Neo.TEAL : 0xFFD6D3CC, -1, 2.5f * d, 0));
        View knob = new View(this);
        knob.setBackground(new NeoBox(Neo.CREAM, -1, 2.5f * d, 0));
        int k = Math.round(22 * d), m = Math.round(4 * d);
        FrameLayout.LayoutParams kp = new FrameLayout.LayoutParams(k, k, Gravity.CENTER_VERTICAL | (on ? Gravity.END : Gravity.START));
        kp.setMargins(m, 0, m, 0);
        track.addView(knob, kp);
        row.addView(track, new LinearLayout.LayoutParams(Math.round(56 * d), Math.round(30 * d)));
    }

    private LinearLayout focusableRow(int color, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setOnClickListener(click);
        int p = Math.round(10 * d);
        row.setPadding(p, p, Math.round(16 * d), p);
        NeoBox.applyTo(row, new NeoBox(color, 16 * d, 3 * d, 4 * d, 8 * d));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Math.round(6 * d);
        list.addView(row, lp);
        rows.add(row);
        return row;
    }

    private TextView text(String s, int sp, int font, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTypeface(UiFont.get(this, font));
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        v.setSingleLine(true);
        return v;
    }
}
