package game.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

/**
 * Central loader for bundled game assets (images, difficulty tables, stylesheets).
 *
 * <p>
 * Assets are addressed with slash-separated, platform independent paths such as
 * {@code "img/taxi_move_1.png"}. Each path is first resolved on the classpath so
 * the game runs from a packaged jar (and inside a container), and only then from
 * the working directory so it still runs straight out of a source checkout.
 * </p>
 */
public final class Assets {

    /** Images are immutable and shared, so one decode per path is enough. */
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

    private Assets() {
    }

    /**
     * Opens an asset as a stream.
     *
     * @param path slash-separated asset path, e.g. {@code "dat/easy.txt"}
     * @return an open stream that the caller must close
     * @throws IOException if the asset cannot be found on the classpath or on disk
     */
    public static InputStream open(String path) throws IOException {
        String normalised = path.replace('\\', '/');

        InputStream fromClasspath = Assets.class.getResourceAsStream("/" + normalised);
        if (fromClasspath != null) {
            return fromClasspath;
        }

        File onDisk = new File(normalised);
        if (onDisk.isFile()) {
            return new FileInputStream(onDisk);
        }

        throw new IOException("Asset not found on classpath or in "
                + new File(".").getAbsolutePath() + ": " + normalised);
    }

    /**
     * Loads an image, reusing a previously decoded copy where possible.
     *
     * @param path slash-separated asset path, e.g. {@code "img/pothole.png"}
     * @return the decoded image, never {@code null}
     * @throws IllegalStateException if the image is missing or cannot be decoded, because a
     *                               half-loaded sprite sheet produces confusing downstream
     *                               failures rather than an actionable message
     */
    public static Image image(String path) {
        Image cached = IMAGE_CACHE.get(path);
        if (cached != null) {
            return cached;
        }

        try (InputStream in = open(path)) {
            Image image = new Image(in);
            if (image.isError()) {
                throw new IllegalStateException("Could not decode image: " + path, image.getException());
            }
            IMAGE_CACHE.put(path, image);
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load image: " + path, e);
        }
    }

    /**
     * Resolves an asset to a URL suitable for JavaFX APIs that take one (stylesheets, for example).
     *
     * @param path slash-separated asset path
     * @return an external-form URL, or {@code null} when the asset is unavailable
     */
    public static String url(String path) {
        String normalised = path.replace('\\', '/');

        java.net.URL fromClasspath = Assets.class.getResource("/" + normalised);
        if (fromClasspath != null) {
            return fromClasspath.toExternalForm();
        }

        File onDisk = new File(normalised);
        if (onDisk.isFile()) {
            return onDisk.toURI().toString();
        }
        return null;
    }
}
