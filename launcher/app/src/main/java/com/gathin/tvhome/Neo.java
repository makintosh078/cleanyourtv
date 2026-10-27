package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.json.JSONObject;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The neo-brutalist look (neo_style): a warm flat background, cream cards with a thick ink outline and a hard
 * offset shadow (no blur), round pills and bright flat colours. Off unless a theme turns it on.
 *
 * Its wallpapers are flat colours (neo_wallpapers), drawn as a fill instead of a picture: a 4K image costs about
 * 33 MB of a 2 GB TV's memory, a colour nothing. Pictures in the theme's wallpapers/ folder (1920x1080 is enough,
 * named in its captions.txt) follow the colours in the same collection; the one shown reaches Home and the clock
 * through Config's wallpaper choice ({@link #wallpaperChoice}). What the owner picks on the Wallpapers screen is
 * kept in its own preferences, so a theme push does not reset it.
 */
final class Neo {

    static final int INK = 0xFF141414;
    static final int CREAM = 0xFFFFF7EA;
    static final int YELLOW = 0xFFFFD23F;
    static final int PINK = 0xFFFF5FA2;
    static final int TEAL = 0xFF2EC4B6;
    static final int BLUE = 0xFF3A5BFF;
    static final int LAVENDER = 0xFFB69CFF;
    static final int ORANGE = 0xFFFF6B35;
    /** The screensaver's own accents, a little brighter than the Home ones in the mockup. */
    static final int CLOCK_CREAM = 0xFFFFF4E0, CLOCK_PINK = 0xFFFF3EA5, CLOCK_CYAN = 0xFF29E3FF;

    /** Tile colours in dock order when the theme names none; they repeat after the last. */
    private static final int[] DEFAULT_TILES = {BLUE, PINK, YELLOW, LAVENDER, TEAL, ORANGE, CREAM};

    static final String PREFS = "neo";
    static final String KEY_WALLPAPER = "wallpaper";
    static final String KEY_DAILY = "daily";
    /** The day (epoch day) the wallpaper was picked or the daily change switched on: the daily change counts from it. */
    static final String KEY_SINCE = "since";
    static final String KEY_CLOCK_WALLPAPER = "clock_wallpaper";

    /**
     * A wallpaper of the collection: its name, a one-line description and either a flat colour or a picture. A
     * picture's colour is a placeholder until {@link #bottomColor} is asked for.
     */
    static final class Wallpaper {
        final String name;
        final int color;
        final String description;
        /** The picture, or null for a colour. */
        final File image;

        Wallpaper(String name, int color, String description) {
            this(name, color, description, null);
        }

        Wallpaper(String name, int color, String description, File image) {
            this.name = name;
            this.color = color;
            this.description = description;
            this.image = image;
        }
    }

    final boolean enabled;
    /** The letter in the round badge at the top left; empty hides the badge. */
    final String avatar;
    final int[] tileColors;
    /** "package=Category" for the label above the big name, e.g. Кино. */
    final Map<String, String> categories;
    final List<Wallpaper> wallpapers;
    /** The theme's clock_wallpaper, the default of "Show as screensaver". */
    final boolean themeClockWallpaper;

    private Neo(boolean enabled, String avatar, int[] tileColors, Map<String, String> categories,
                List<Wallpaper> wallpapers, boolean themeClockWallpaper) {
        this.enabled = enabled;
        this.avatar = avatar;
        this.tileColors = tileColors;
        this.categories = categories;
        this.wallpapers = wallpapers;
        this.themeClockWallpaper = themeClockWallpaper;
    }

    static Neo load(Context ctx) {
        boolean enabled = ctx.getResources().getBoolean(R.bool.neo_style);
        JSONObject json = Config.overlay(ctx);
        if (json == null) return new Neo(enabled, "", DEFAULT_TILES, Collections.emptyMap(), defaultWallpapers(), false);
        enabled = Config.bool(json, "neo_style", enabled);
        int[] tiles = colors(Config.stringList(json, "neo_tile_colors", Collections.emptyList()));
        List<Wallpaper> wallpapers = new ArrayList<>(wallpapers(Config.stringList(json, "neo_wallpapers", Collections.emptyList())));
        if (wallpapers.isEmpty()) wallpapers.addAll(defaultWallpapers());
        File dir = Config.dir(ctx);
        File folder = dir == null ? null : new File(dir, "wallpapers");
        Map<String, String> names = WallpaperCaptions.read(folder);
        for (File f : WallpaperRotation.images(folder)) {
            String name = names.containsKey(f.getName()) ? names.get(f.getName()) : f.getName();
            wallpapers.add(new Wallpaper(name, CREAM, ctx.getString(R.string.neo_picture), f));
        }
        return new Neo(enabled, Config.string(json, "neo_avatar", ""), tiles.length > 0 ? tiles : DEFAULT_TILES,
                Config.packageMap(Config.stringList(json, "neo_categories", Collections.emptyList())),
                wallpapers, Config.bool(json, "clock_wallpaper", false));
    }

    /**
     * Config's wallpaper choice in neo_style: the picture shown today by file name, or "none" while a colour is.
     * Null when the style is off, so the usual on-TV choice stands.
     */
    static String wallpaperChoice(Context ctx) {
        Neo neo = load(ctx);
        if (!neo.enabled) return null;
        Wallpaper w = neo.current(ctx);
        return w.image != null ? w.image.getName() : "none";
    }

    /**
     * The colour along the bottom of a wallpaper: its own colour, or a picture's averaged from a tiny decode (a
     * 1/32 sample, a few KB). The bottom strip on Home is filled with it, and the clock's ink decides by it.
     */
    static int bottomColor(Wallpaper w) {
        if (w.image == null) return w.color;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 32;
            Bitmap b = BitmapFactory.decodeFile(w.image.getPath(), o);
            if (b == null) return w.color;
            long r = 0, g = 0, bl = 0;
            int n = 0, from = b.getHeight() * 9 / 10;
            for (int y = from; y < b.getHeight(); y++) {
                for (int x = 0; x < b.getWidth(); x++) {
                    int c = b.getPixel(x, y);
                    r += Color.red(c);
                    g += Color.green(c);
                    bl += Color.blue(c);
                    n++;
                }
            }
            b.recycle();
            return n == 0 ? w.color : Color.rgb((int) (r / n), (int) (g / n), (int) (bl / n));
        } catch (RuntimeException | OutOfMemoryError e) {
            return w.color;
        }
    }

    /** The collection when the theme names none: the sand of the mockup alone. */
    private static List<Wallpaper> defaultWallpapers() {
        return Collections.singletonList(new Wallpaper("Песок", 0xFFEEE8DC, ""));
    }

    /** "#RRGGBB" entries; one that does not parse is left out. */
    static int[] colors(List<String> entries) {
        List<Integer> out = new ArrayList<>();
        for (String e : entries) {
            Integer c = parse(e);
            if (c != null) out.add(c);
        }
        int[] a = new int[out.size()];
        for (int i = 0; i < a.length; i++) a[i] = out.get(i);
        return a;
    }

    /** "Name|#RRGGBB|Description" entries; one without a name or a readable colour is left out. */
    static List<Wallpaper> wallpapers(List<String> entries) {
        List<Wallpaper> out = new ArrayList<>();
        for (String e : entries) {
            String[] parts = e.split("\\|", 3);
            Integer c = parts.length >= 2 ? parse(parts[1]) : null;
            String name = parts[0].trim();
            if (c == null || name.isEmpty()) continue;
            out.add(new Wallpaper(name, c, parts.length == 3 ? parts[2].trim() : ""));
        }
        return out;
    }

    private static Integer parse(String s) {
        try {
            return Color.parseColor(s.trim()) | 0xFF000000;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    int tileColor(int position) {
        return tileColors[Math.floorMod(position, tileColors.length)];
    }

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The wallpaper on screen today: with "Change every day" one place further each day, else the picked one. */
    Wallpaper current(Context ctx) {
        SharedPreferences p = prefs(ctx);
        long today = LocalDate.now().toEpochDay();
        return wallpapers.get(index(wallpapers.size(), indexOf(p.getString(KEY_WALLPAPER, null)),
                p.getBoolean(KEY_DAILY, false), today - p.getLong(KEY_SINCE, today)));
    }

    /** The picked index, moved on by the days since it was picked while the daily change is on. */
    static int index(int count, int picked, boolean daily, long days) {
        if (count <= 0) return 0;
        int base = Math.min(Math.max(0, picked), count - 1);
        return daily ? (int) Math.floorMod(base + Math.max(0, days), (long) count) : base;
    }

    /** Shows this wallpaper from today; the daily change, when on, moves on from it tomorrow. */
    static void pick(Context ctx, String name) {
        prefs(ctx).edit().putString(KEY_WALLPAPER, name).putLong(KEY_SINCE, LocalDate.now().toEpochDay()).apply();
    }

    int indexOf(String name) {
        if (name == null) return -1;
        for (int i = 0; i < wallpapers.size(); i++) if (wallpapers.get(i).name.equals(name)) return i;
        return -1;
    }

    /** "Show as screensaver": the clock over this wallpaper, as chosen on the TV or by the theme. */
    boolean clockWallpaper(Context ctx) {
        return prefs(ctx).getBoolean(KEY_CLOCK_WALLPAPER, themeClockWallpaper);
    }

    /** Dark enough that text on it must be light (Graphite). */
    static boolean isDark(int color) {
        double l = 0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color);
        return l < 110;
    }
}
