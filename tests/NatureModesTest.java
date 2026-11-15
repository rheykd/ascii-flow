import ru.ascii.wallpaper.AsciiScene;
import java.util.Arrays;

public final class NatureModesTest {
    public static void main(String[] args) {
        AsciiScene scene = new AsciiScene();
        for (int[] size : new int[][] {{24, 36}, {64, 108}, {120, 42}}) {
            scene.resize(size[0], size[1]);
            for (int mode = 12; mode <= 16; mode++) {
                scene.render(mode, .08, .52);
                float[] first = scene.light.clone();
                boolean changed = false;
                for (double time : new double[] {.08, 1.3, 6.7, 1000000.5}) {
                    scene.render(mode, time, .52);
                    int visible = 0;
                    boolean pink = false;
                    for (int i = 0; i < scene.light.length; i++) {
                        float light = scene.light[i];
                        if (!(light >= 0 && light <= 1)) throw new AssertionError("Brightness outside range");
                        if (scene.glyphs[i] != ' ') {
                            visible++;
                            pink |= scene.tones[i] == 5;
                        }
                    }
                    if (visible < 4 || pink != (mode == 14)) throw new AssertionError("Missing scene or blossom color: " + mode);
                    changed |= !Arrays.equals(first, scene.light);
                }
                if (!changed) throw new AssertionError("Static scene: " + mode);
            }
            scene.render(0, 1, .52);
            for (byte tone : scene.tones) if (tone != 0) throw new AssertionError("Palette leaked into cube");
        }
        System.out.println("PASS: five animated nature modes, brightness, palettes and long uptime.");
    }
}
