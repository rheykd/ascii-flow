package ru.ascii.wallpaper;

public final class MediaTone {
    public static float luminance(int argb, float contrast, boolean invert) {
        float l=(((argb>>16)&255)*.2126f+((argb>>8)&255)*.7152f+(argb&255)*.0722f)/255;
        l=(l-.5f)*contrast+.5f;
        if(invert)l=1-l;
        return Math.max(0,Math.min(1,l));
    }
}
