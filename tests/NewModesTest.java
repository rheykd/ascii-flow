import ru.ascii.wallpaper.AsciiScene;
import java.util.Arrays;

/** Чёрная дыра, пульсар, звёзды и выключаемое вращение галактики. */
public final class NewModesTest {
    static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static int occupied(AsciiScene scene) { int n = 0; for (char c : scene.glyphs) if (c != ' ') n++; return n; }

    public static void main(String[] args) {
        AsciiScene scene = new AsciiScene();
        for (int[] size : new int[][]{{36, 64}, {64, 110}, {120, 60}}) {
            scene.resize(size[0], size[1]);
            for (int mode = 9; mode <= 10; mode++) {
                scene.render(mode, 2, .515); char[] first = scene.glyphs.clone();
                require(occupied(scene) > 20, "Empty mode " + mode);
                scene.render(mode, 5.3, .515);
                require(!Arrays.equals(first, scene.glyphs), "Static mode " + mode);
                for (float v : scene.light) require(v >= 0 && v <= 1, "Brightness range " + mode);
            }
            // Звёзды: без анимации неподвижны, с анимацией меняются.
            scene.options.starsAnimation = false;
            scene.render(11, 2, .515); char[] still = scene.glyphs.clone(); float[] stillLight = scene.light.clone();
            require(occupied(scene) > 10, "Empty stars");
            scene.render(11, 7, .515);
            require(Arrays.equals(still, scene.glyphs) && Arrays.equals(stillLight, scene.light), "Stars must be still");
            scene.options.starsAnimation = true;
            scene.render(11, 2, .515); float[] a = scene.light.clone();
            scene.render(11, 4, .515);
            require(!Arrays.equals(a, scene.light), "Stars must pulse");
            scene.options.starsAnimation = false;
            // Галактика: выключенное вращение замораживает картинку.
            scene.options.galaxySpin = false;
            scene.render(4, 1, .515); char[] g = scene.glyphs.clone();
            scene.render(4, 9, .515);
            require(Arrays.equals(g, scene.glyphs), "Galaxy must stop");
            scene.options.galaxySpin = true;
            scene.render(4, 9, .515);
            require(!Arrays.equals(g, scene.glyphs), "Galaxy must spin");
        }
        // Долгая работа без выхода за массивы.
        scene.resize(64, 110);
        for (int mode = 9; mode <= 11; mode++) for (double t = 0; t < 3600; t += 37.7) scene.render(mode, t, .515);
        require(AsciiScene.NAMES.length == 12, "Twelve modes");
        System.out.println("PASS: black hole, pulsar, stars animation toggle, galaxy spin toggle.");
    }
}
