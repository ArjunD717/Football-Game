package footy;

import java.util.Objects;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Shared state and behaviour for every simulated body on the pitch.
 *
 * <p>Position and velocity are exposed as live references so the physics step
 * can mutate them without allocating. Callers must not replace the returned
 * objects, only mutate them via {@link Vector2} methods.
 */
public abstract class GameObject {

    private final Vector2 position;
    private final Vector2 velocity;
    private final double radius;
    private final double mass;
    private Color fillColor;

    protected GameObject(double x, double y, double radius, double mass, Color fillColor) {
        if (!(radius > 0) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("radius must be positive and finite: " + radius);
        }
        if (!(mass > 0) || !Double.isFinite(mass)) {
            throw new IllegalArgumentException("mass must be positive and finite: " + mass);
        }
        this.position = new Vector2(x, y);
        this.velocity = new Vector2();
        this.radius = radius;
        this.mass = mass;
        this.fillColor = Objects.requireNonNull(fillColor, "fillColor");
    }

    /** Live position reference; mutate, do not replace. */
    public Vector2 getPosition() {
        return position;
    }

    /** Live velocity reference; mutate, do not replace. */
    public Vector2 getVelocity() {
        return velocity;
    }

    public double getX() {
        return position.getX();
    }

    public double getY() {
        return position.getY();
    }

    public void setPosition(double x, double y) {
        position.set(x, y);
    }

    public void setVelocity(double x, double y) {
        velocity.set(x, y);
    }

    public double getRadius() {
        return radius;
    }

    public double getMass() {
        return mass;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = Objects.requireNonNull(fillColor, "fillColor");
    }

    /** Advances position from current velocity. Allocation-free. */
    protected void integrate(double dt) {
        position.set(
            position.getX() + velocity.getX() * dt,
            position.getY() + velocity.getY() * dt);
    }

    public void stop() {
        velocity.set(0, 0);
    }

    public abstract void update(double dt);

    public abstract void draw(GraphicsContext gc);
}
