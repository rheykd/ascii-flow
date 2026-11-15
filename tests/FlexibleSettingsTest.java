import ru.ascii.wallpaper.AsciiScene;
import ru.ascii.wallpaper.GridGeometry;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import java.io.File;

public class FlexibleSettingsTest {
    static int occupied(AsciiScene scene) {
        int n = 0; for (char c : scene.glyphs) if (c != ' ') n++; return n;
    }
    static void connected(AsciiScene scene) {
        boolean[] visited = new boolean[scene.glyphs.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < visited.length; i++) if (scene.glyphs[i] != ' ') { queue.add(i); visited[i] = true; break; }
        int count = 0;
        while (!queue.isEmpty()) {
            int i = queue.remove(), x = i % scene.cols, y = i / scene.cols; count++;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= scene.cols || ny >= scene.rows) continue;
                int j = ny * scene.cols + nx;
                if (!visited[j] && scene.glyphs[j] != ' ') { visited[j] = true; queue.add(j); }
            }
        }
        if (count != occupied(scene)) throw new AssertionError("Disconnected cube edge");
    }
    public static void main(String[] args) throws Exception {
        GridGeometry grid = new GridGeometry();
        AsciiScene scene = new AsciiScene();
        int cases = 0;
        int[][] resolutions = {{1080,2340},{2340,1080},{1440,3120},{360,800}};
        for (int[] resolution : resolutions) for (int density : new int[]{24,36,64,120,180})
            for (int spacing : new int[]{70,100,180}) {
                grid.configure(resolution[0], resolution[1], density, spacing);
                if (grid.cols != density || grid.rows * grid.cellH + .01 < resolution[1]) throw new AssertionError("Grid does not cover screen");
                if (Math.abs(grid.cols * grid.cellW - resolution[0]) > .01) throw new AssertionError("Wrong density");
                scene.resize(grid.cols, grid.rows);
                for (int t = 0; t < 100; t += 7) {
                    scene.render(0, t, grid.cellW / grid.cellH, true, 1);
                    if (occupied(scene) < 10) throw new AssertionError("Cube is empty");
                    connected(scene); cases++;
                }
            }
        grid.configure(1080, 2340, 120, 100); scene.resize(grid.cols, grid.rows);
        scene.render(0, 0, grid.cellW / grid.cellH, true, 1);
        int wireCount = occupied(scene); char[] wire = scene.glyphs.clone();
        scene.render(0, 0, grid.cellW / grid.cellH, false, 1);
        if (occupied(scene) < wireCount * 2 || Arrays.equals(wire, scene.glyphs)) throw new AssertionError("Cube style does not change surface");
        scene.render(0, 0, grid.cellW / grid.cellH, true, .4);
        if (occupied(scene) >= wireCount) throw new AssertionError("Object scale is ignored");
        byte[] font = Files.readAllBytes(Paths.get("app/src/main/assets/fonts/terminal8x8.bin"));
        if (font.length != 1024) throw new AssertionError("Invalid bitmap font length");
        for (int c = 33; c < 127; c++) {
            boolean ink = false;
            for (int y = 0; y < 8; y++) ink |= font[c * 8 + y] != 0;
            if (!ink) throw new AssertionError("Missing glyph " + c);
        }
        Font classic = Font.createFont(Font.TRUETYPE_FONT, new File("app/src/main/assets/fonts/Cousine-Regular.ttf"));
        if (classic.canDisplayUpTo("AaBb0123@#+-/") != -1) throw new AssertionError("Missing classic glyph");
        System.out.println("PASS: " + cases + " connected wireframe frames, density limits, landscape/portrait, filled/wire toggle, scale, both bundled fonts.");
        renderPreview(font, classic);
    }
    static void renderPreview(byte[] bitmapFont, Font classic) throws Exception {
        int w = 360, h = 600;
        BufferedImage sheet = new BufferedImage(w * 3, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics(); g.setColor(new Color(0x020704)); g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        String[] labels = {"TERMINAL / 36 COL", "TERMINAL / 64 COL", "TERMINAL / 120 COL", "CLASSIC / WIREFRAME", "CLASSIC / FILLED", "TERMINAL / WAVES"};
        GridGeometry grid = new GridGeometry(); AsciiScene scene = new AsciiScene();
        for (int panel = 0; panel < 6; panel++) {
            int ox = panel % 3 * w, oy = panel / 3 * h;
            g.setColor(Color.BLACK); g.fillRect(ox + 10, oy + 10, w - 20, h - 20);
            int columns = panel == 0 ? 36 : panel == 2 ? 120 : 64;
            grid.configure(w - 28, h - 82, columns, 100); scene.resize(grid.cols, grid.rows);
            scene.render(panel == 5 ? 1 : 0, 0, grid.cellW/grid.cellH, panel != 4, 1);
            float glyphW = grid.cellW * .9f;
            Font scaled = classic.deriveFont(100f); g.setFont(scaled);
            scaled = classic.deriveFont(100 * glyphW / g.getFontMetrics().charWidth('M'));
            g.setFont(scaled);
            for (int y = 0; y < grid.rows; y++) for (int x = 0; x < grid.cols; x++) {
                int i = y * grid.cols + x, c = scene.glyphs[i]; if (c == ' ') continue;
                float a = scene.light[i] * .9f;
                g.setColor(new Color((int)(57*a),(int)(255*a),(int)(120*a)));
                float cx = ox + 14 + (x + .5f)*grid.cellW, cy = oy+60+grid.top+(y+.5f)*grid.cellH;
                if (panel == 3 || panel == 4) {
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.drawString(String.valueOf((char)c),cx-glyphW*.5f,cy+(g.getFontMetrics().getAscent()-g.getFontMetrics().getDescent())*.5f);
                } else {
                    float px = glyphW/8, py = glyphW/4;
                    for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) if ((bitmapFont[c*8+row] & (1<<col)) != 0) {
                        g.fill(new java.awt.geom.Rectangle2D.Float(cx-glyphW*.5f+col*px,cy-glyphW+row*py,px,py));
                    }
                }
            }
            g.setColor(new Color(0x39ff78)); g.setFont(new Font("Monospaced",Font.PLAIN,13));
            g.drawString(labels[panel],ox+22,oy+36);
        }
        g.dispose(); ImageIO.write(sheet,"png",new File("dist/1.1-renderer-preview.png"));
    }
}
