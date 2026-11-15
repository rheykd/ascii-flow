import ru.ascii.wallpaper.AsciiScene;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

public final class SceneTest {
    public static void main(String[] args) throws Exception {
        AsciiScene scene = new AsciiScene();
        int[][] sizes = {{30, 28}, {60, 65}, {90, 104}, {140, 230}, {104, 48}};
        long start = System.nanoTime();
        int frames = 0;
        for (int[] size : sizes) {
            scene.resize(size[0], size[1]);
            for (int mode = 0; mode < 6; mode++) {
                scene.render(mode, 2, .515);
                char[] first = scene.glyphs.clone();
                for (double t : new double[]{0, 10, 35, 1000, 100000}) {
                    scene.render(mode, t, .515); frames++;
                    int occupied = 0;
                    for (int i = 0; i < scene.glyphs.length; i++) {
                        if (scene.glyphs[i] != ' ') occupied++;
                        if (!Float.isFinite(scene.light[i]) || scene.light[i] < 0 || scene.light[i] > 1)
                            throw new AssertionError("Invalid intensity");
                    }
                    if (occupied < 10) throw new AssertionError("Empty mode " + mode);
                }
                if (Arrays.equals(first, scene.glyphs)) throw new AssertionError("Static mode " + mode);
            }
        }
        scene.resize(90, 104);
        // A rotating centered cube must remain near the center for an entire rotation.
        for (int t = 0; t < 80; t += 2) {
            scene.render(0, t, .515);
            int minX = 90, maxX = 0, minY = 104, maxY = 0;
            for (int y = 0; y < 104; y++) for (int x = 0; x < 90; x++) {
                if (scene.glyphs[y * 90 + x] != ' ') {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                }
            }
            if (Math.abs((maxX + minX) / 2.0 - 45) > 7 || Math.abs((maxY + minY) / 2.0 - 52) > 4)
                throw new AssertionError("Cube not centered");
        }
        System.out.println("PASS: " + frames + " frames, six animated modes, portrait/landscape, long uptime, centered cube.");
        System.out.printf("Renderer test runtime: %.0f ms%n", (System.nanoTime() - start) / 1e6);
        BufferedImage sheet = new BufferedImage(1080, 1320, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x080b0d)); g.fillRect(0, 0, 1080, 1320);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        String[] names = {"01 / CUBE", "02 / WAVES", "03 / TORUS", "04 / SPHERE", "05 / GALAXY", "06 / RAIN"};
        for (int mode = 0; mode < 6; mode++) {
            int ox = mode % 3 * 360, oy = mode / 3 * 660;
            g.setColor(Color.BLACK); g.fillRoundRect(ox + 12, oy + 12, 336, 636, 20, 20);
            scene.resize(60, 54); scene.render(mode, 6, .515);
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            for (int y = 0; y < 54; y++) for (int x = 0; x < 60; x++) {
                int i = y * 60 + x;
                float a = scene.light[i] * .8f;
                g.setColor(new Color((int)(145 * a), (int)(245 * a), (int)(204 * a)));
                g.drawString(String.valueOf(scene.glyphs[i]), ox + 18 + x * 5.4f, oy + 68 + y * 5.4f / .515f);
            }
            g.setFont(new Font("Monospaced", Font.PLAIN, 13));
            g.setColor(new Color(0x91f5cc)); g.drawString(names[mode], ox + 30, oy + 42);
        }
        g.dispose(); ImageIO.write(sheet, "png", new File("dist/modes-preview.png"));
    }
}
