package ru.ascii.wallpaper;

/** Grid spacing does not depend on font metrics or the size of a glyph. */
public final class GridGeometry {
    public int cols, rows;
    public float cellW, cellH, left, top;
    public void configure(int width, int height, int columns, int rowSpacing) {
        cols = Math.max(24, Math.min(180, columns));
        cellW = Math.max(1, width) / (float)cols;
        cellH = cellW * 1.95f * Math.max(70, Math.min(180, rowSpacing)) / 100f;
        rows = Math.max(8, (int)Math.ceil(Math.max(1, height) / cellH));
        left = 0;
        top = (height - rows * cellH) * .5f;
    }
}
