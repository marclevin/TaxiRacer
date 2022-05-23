package game.display.models;

/**
 * This class represents a bounding box.
 * A bounding box surrounds a sprite and allows us to collisions easily using
 * the {@code intersect} command.
 */
public class BoundBox {
    private double width, height;
    private int x, y;

    /**
     * Constructor for the bounding box.
     * 
     * @param width  Width of the bounding box.
     * @param height Height of the bounding box.
     * @param x      X coordinate of the bounding box.
     * @param y      Y coordinate of the bounding box.
     */
    public BoundBox(double width, double height, int x, int y) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    };

    /**
     * This function detects collisions with another {@code BoundBox} object.
     * 
     * @param other The other {@code BoundBox} object.
     * @return {@code true} if the bounding boxes intersect, {@code false}
     *         otherwise.
     */
    public boolean intersects(BoundBox other) {
        return (this.x < other.getX() + other.getWidth() &&
                this.x + this.width > other.getX() &&
                this.y < other.getY() + other.getHeight() &&
                this.y + this.height > other.getY());
    };

    /**
     * Sets the width of the bounding box.
     * 
     * @param width The new width of the bounding box.
     */
    public void setWidth(double width) {
        this.width = width;
    };

    /**
     * Sets the height of the bounding box.
     * 
     * @param height The new height of the bounding box.
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * Sets the x coordinate of the bounding box.
     * 
     * @param x The new x coordinate of the bounding box.
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the x coordinate of the bounding box.
     * 
     * @param x The new x coordinate of the bounding box.
     */
    public void setX(double x) {
        this.x = (int) x;
    }

    /**
     * Sets the y coordinate of the bounding box.
     * 
     * @param y The new y coordinate of the bounding box.
     */
    public void setY(double y) {
        this.y = (int) y;
    }

    /**
     * Sets the y coordinate of the bounding box.
     * 
     * @param y The new y coordinate of the bounding box.
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Returns the X coordinate of the bounding box.
     * @return The X coordinate of the bounding box.
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the Y coordinate of the bounding box.
     * @return The Y coordinate of the bounding box.
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the width of the bounding box.
     * @return The width of the bounding box.
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the height of the bounding box.
     * @return The height of the bounding box.
     */
    public double getHeight() {
        return height;
    }

}
