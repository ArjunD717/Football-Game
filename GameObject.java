import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// shared bits for players and the ball
public abstract class GameObject {

    private Vector2 position;
    private Vector2 velocity;
    private final double radius;
    private final double mass;
    private Color fillColor;


    protected GameObject(double x, double y, double radius, double mass, Color fillColor) {
        position = new Vector2(x, y);
        velocity = new Vector2();
        this.radius = radius;
        this.mass = mass;
        this.fillColor = fillColor;
    }


    public Vector2 getPosition() {
        return position;
    }


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
        this.fillColor = fillColor;
    }


    protected void integrate(double dt) {
        // basic position update from current velocity
        position.add(velocity.copy().scale(dt));
    }


    public void stop() {
        velocity.set(0, 0);
    }


    public abstract void update(double dt);


    public abstract void draw(GraphicsContext gc);
}
