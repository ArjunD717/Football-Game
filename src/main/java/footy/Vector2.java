package footy;

/**
 * Mutable 2D vector used for positions, velocities and directions.
 *
 * <p>Instances are intentionally mutable so the game loop can update them
 * without allocating. Methods that mutate return {@code this} to allow
 * chaining; use {@link #copy()} when an independent snapshot is needed.
 */
public final class Vector2 {

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

    /** Returns an independent copy of this vector. */
    public Vector2 copy() {
        return new Vector2(x, y);
    }

    public Vector2 add(Vector2 other) {
        x += other.x;
        y += other.y;
        return this;
    }

    /**
     * Adds {@code other} scaled by {@code factor} without allocating,
     * e.g. {@code velocity.addScaled(direction, ACCELERATION * dt)}.
     */
    public Vector2 addScaled(Vector2 other, double factor) {
        x += other.x * factor;
        y += other.y * factor;
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

    /** Normalizes in place; a zero vector is left unchanged. */
    public Vector2 normalize() {
        double length = length();
        if (length > 0) {
            scale(1.0 / length);
        }
        return this;
    }

    /** Scales down to {@code max} length if longer; never scales up. */
    public Vector2 limit(double max) {
        double maxSquared = max * max;
        double currentSquared = lengthSquared();
        if (currentSquared > maxSquared) {
            scale(max / Math.sqrt(currentSquared));
        }
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Vector2 other)) {
            return false;
        }
        return Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(x) + Double.hashCode(y);
    }

    @Override
    public String toString() {
        return "Vector2(" + x + ", " + y + ")";
    }
}
