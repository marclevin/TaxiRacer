package game.display.models;

public class BoundBox {
    private double width, height;
    private int x, y;

    public BoundBox(double width, double height, int x, int y) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    };

    public boolean intersects(BoundBox other) {
        return (this.x < other.getX() + other.getWidth() &&
                this.x + this.width > other.getX() &&
                this.y < other.getY() + other.getHeight() &&
                this.y + this.height > other.getY());
    };

    public void setWidth(double width) {
        this.width = width;
    };

    public void setHeight(double height) {
        this.height = height;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setX(double x)
    {
       this.x = (int)x;
    }

    public void setY(double y)
    {
        this.y = (int)y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

}
