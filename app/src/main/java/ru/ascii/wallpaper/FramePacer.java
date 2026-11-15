package ru.ascii.wallpaper;

/** Carries fractional milliseconds and includes the time spent drawing. */
final class FramePacer {
    private int previousRate;
    private int remainder;

    long nextFrame(long startedAt, int rate) {
        if (rate != previousRate) {
            previousRate = rate;
            remainder = 0;
        }
        int budget = 1000 + remainder;
        remainder = budget % rate;
        return startedAt + budget / rate;
    }
}
