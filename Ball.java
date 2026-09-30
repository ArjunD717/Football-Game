import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// keeps the ball movement and drawing simple
public class Ball extends GameObject {

    public static final double WALL_RESTITUTION = 0.88;
    private static final double DRAG = 0.75; // simulate friction to slow down the ball
    private static final double MAX_SPEED = 600;
    private static final double STOP_THRESHOLD = 10.0;
    private double spinAngle; // visual rotation
    
    public Ball(double x, double y) {
        super(x, y, 10, 1.0, Color.WHITE);
    }


    public void reset(double x, double y) {
        setPosition(x, y);
        setVelocity(0, 0);
        spinAngle = 0;
    }


    public void addSpin(double amount) {
        spinAngle += amount;
    }


    @Override
    public void update(double dt) {
        // Limit the ball speed to prevent glitches
        getVelocity().limit(MAX_SPEED);
        // a bit of drag stops the ball feeling too floaty
        getVelocity().scale(1.0 / (1.0 + (DRAG * dt)));
        // Clamp small velocities so the ball eventually stops
        if (getVelocity().lengthSquared() < STOP_THRESHOLD * STOP_THRESHOLD) {
            getVelocity().set(0, 0);
        }
        integrate(dt);
        spinAngle += getVelocity().length() * dt * 2.8;
    }


    @Override
    public void draw(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double radius = getRadius();

        gc.setFill(Color.color(0, 0, 0, 0.20));
        gc.fillOval(x - radius + 2, y - radius + 4, radius * 2, radius * 2);

        gc.setFill(getFillColor());
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);

        gc.save();
        // the plus sign spins just enough to show motion
        gc.translate(x, y);
        gc.rotate(spinAngle);
        gc.setLineWidth(2.5);
        gc.strokeLine(-radius * 0.48, 0, radius * 0.48, 0);
        gc.strokeLine(0, -radius * 0.48, 0, radius * 0.48);

        gc.restore();
    }
}
