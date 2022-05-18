package model.utility;
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
        return (this.x < other.x + other.width &&
                this.x + this.width > other.x &&
                this.y < other.y + other.height &&
                this.y + this.height > other.y);
    };

    public void setWidth(double width) {
        this.width = width;
    };
    public void setHeight(double height)
    {
        this.height = height;
    }
    public void setX(int x)
    {
        this.x = x;
    }
    public void setY(int y)
    {
        this.y = y;
    }
    
}
