// tiny vector helper for the movement maths
public class Vector2 {

    private double x;
    private double y;


    public Vector2() {
        this(0, 0);
    }


    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }


    public double getX() {
        return x;
    }


    public double getY() {
        return y;
    }


    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }


    public Vector2 copy() {
        return new Vector2(x, y);
    }


    public Vector2 add(Vector2 other) {
        x += other.x;
        y += other.y;
        return this;
    }


    public Vector2 subtract(Vector2 other) {
        x -= other.x;
        y -= other.y;
        return this;
    }


    public Vector2 scale(double factor) {
        x *= factor;
        y *= factor;
        return this;
    }


    public double dot(Vector2 other) {
        return (x * other.x) + (y * other.y);
    }


    public double lengthSquared() {
        return (x * x) + (y * y);
    }


    public double length() {
        return Math.sqrt(lengthSquared());
    }


    public Vector2 normalize() {
        double length = length();
        if (length > 0) {
            scale(1.0 / length);
        }

        return this;
    }


    public Vector2 limit(double max) {
        double maxSquared = max * max;
        double currentSquared = lengthSquared();

        if (currentSquared > maxSquared) {
            scale(max / Math.sqrt(currentSquared));
        }

        return this;
    }
}
