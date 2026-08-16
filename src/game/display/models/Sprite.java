package game.display.models;

import game.logic.Acceptor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * This super class represents a sprite, which constitutes most of the game objects.
 *
 * <p>
 * A sprite carries an image, a position and a {@link BoundBox} used for collision
 * detection. Sprites are drawn straight onto the canvas at their requested render
 * size; they are deliberately not JavaFX scene-graph nodes, so drawing one costs a
 * single {@code drawImage} call.
 * </p>
 */
public abstract class Sprite implements Acceptor {

    protected int x;
    protected int y;
    protected final BoundBox myBound;
    protected Image myImage = null;

    /** Requested render width in pixels; {@code <= 0} means "derive it". */
    private double fitWidth = 0;
    /** Requested render height in pixels; {@code <= 0} means "derive it". */
    private double fitHeight = 0;

    /**
     * Constructor for a sprite.
     *
     * @param x X coordinate of the sprite
     * @param y Y coordinate of the sprite
     */
    protected Sprite(int x, int y) {
        this.x = x;
        this.y = y;
        this.myBound = new BoundBox(0, 0, x, y);
    }

    /**
     * Checks for intersection with another sprite.
     *
     * @param other The other sprite to check.
     * @return {@code true} when the two bounding boxes overlap.
     */
    public boolean intersects(Sprite other) {
        return myBound.intersects(other.myBound);
    }

    /**
     * Advances any movement of this sprite's own, called once per logic step after the
     * world has scrolled.
     *
     * <p>
     * Most sprites are scenery that only slides past the taxi and need do nothing here.
     * </p>
     */
    public void tick() {
        // Scenery does not move under its own power.
    }

    /**
     * Reports whether this sprite has finished and should be taken off the street.
     *
     * @return {@code true} once the sprite can be removed.
     */
    public boolean isExpired() {
        return false;
    }

    /**
     * Reports whether this sprite is recycled off the left edge of the screen.
     *
     * <p>
     * Scenery such as potholes and waiting fares is endless, so it wraps back around to the
     * right. Sprites that represent a one-off event do not, and are removed instead.
     * </p>
     *
     * @return {@code true} if the sprite should reappear on the right when it scrolls off.
     */
    public boolean wrapsAround() {
        return true;
    }

    /**
     * Sets the image of the sprite.
     *
     * @param image The image to be set.
     */
    public void setImage(Image image) {
        myImage = image;
    }

    /**
     * Gets the image currently used to draw this sprite.
     *
     * @return the current image, possibly {@code null} before assets are attached.
     */
    public Image getImage() {
        return myImage;
    }

    /**
     * Scales the sprite to an exact width, deriving the height from the image's aspect ratio.
     *
     * @param width the render width in pixels; {@code <= 0} restores the image's natural size.
     */
    protected void fitToWidth(double width) {
        this.fitWidth = width;
        this.fitHeight = 0;
    }

    /**
     * Scales the sprite to an exact height, deriving the width from the image's aspect ratio.
     *
     * @param height the render height in pixels; {@code <= 0} restores the image's natural size.
     */
    protected void fitToHeight(double height) {
        this.fitHeight = height;
        this.fitWidth = 0;
    }

    /**
     * Returns the width this sprite is drawn at, preserving the image's aspect ratio.
     *
     * @return the render width in pixels, or {@code 0} when no image is attached.
     */
    public double renderWidth() {
        if (myImage == null) {
            return 0;
        }
        if (fitWidth > 0) {
            return fitWidth;
        }
        if (fitHeight > 0) {
            return fitHeight * (myImage.getWidth() / myImage.getHeight());
        }
        return myImage.getWidth();
    }

    /**
     * Returns the height this sprite is drawn at, preserving the image's aspect ratio.
     *
     * @return the render height in pixels, or {@code 0} when no image is attached.
     */
    public double renderHeight() {
        if (myImage == null) {
            return 0;
        }
        if (fitHeight > 0) {
            return fitHeight;
        }
        if (fitWidth > 0) {
            return fitWidth * (myImage.getHeight() / myImage.getWidth());
        }
        return myImage.getHeight();
    }

    /**
     * Draws this sprite onto the supplied graphics context at its current position and scale.
     *
     * @param gc the graphics context to draw onto.
     */
    public void draw(GraphicsContext gc) {
        if (myImage == null) {
            return;
        }
        gc.drawImage(myImage, x, y, renderWidth(), renderHeight());
    }

    /**
     * Gets the BoundBox of the sprite.
     *
     * @return The BoundBox of the sprite.
     */
    public BoundBox getBoundary() {
        return myBound;
    }

    /**
     * Gets the X coordinate of the sprite.
     *
     * @return The X coordinate of the sprite.
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the Y coordinate of the sprite.
     *
     * @return The Y coordinate of the sprite.
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the X coordinate of the sprite.
     *
     * @param x The new X coordinate of the sprite.
     */
    public void setX(int x) {
        this.x = x;
        this.myBound.setX(x);
    }

    /**
     * Sets the Y coordinate of the sprite.
     *
     * @param y The new Y coordinate of the sprite.
     */
    public void setY(int y) {
        this.y = y;
        this.myBound.setY(y);
    }
}
