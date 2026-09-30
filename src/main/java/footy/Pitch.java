package footy;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/**
 * Pitch geometry, goal detection, boundary physics and rendering.
 *
 * <p>Players are confined to the pitch; the ball may enter the goal mouths
 * but bounces off every other wall.
 */
public class Pitch {

    public static final int NO_GOAL = 0;
    public static final int LEFT_GOAL = 1;
    public static final int RIGHT_GOAL = 2;

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final double goalDepth;
    private final double goalHeight;

    public Pitch(double x, double y, double width, double height,
                 double goalDepth, double goalHeight) {
        if (!(width > 0) || !Double.isFinite(width)) {
            throw new IllegalArgumentException("width must be positive and finite: " + width);
        }
        if (!(height > 0) || !Double.isFinite(height)) {
            throw new IllegalArgumentException("height must be positive and finite: " + height);
        }
        if (!(goalDepth >= 0) || !Double.isFinite(goalDepth)) {
            throw new IllegalArgumentException("goalDepth must be non-negative and finite: " + goalDepth);
        }
        if (!(goalHeight > 0) || !Double.isFinite(goalHeight) || goalHeight >= height) {
            throw new IllegalArgumentException("goalHeight must be positive and smaller than height: " + goalHeight);
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.goalDepth = goalDepth;
        this.goalHeight = goalHeight;
    }

    public double getCenterX() {
        return x + (width / 2.0);
    }

    public double getLeft() {
        return x;
    }

    public double getRight() {
        return x + width;
    }

    public double getTop() {
        return y;
    }

    public double getBottom() {
        return y + height;
    }

    public double getCenterY() {
        return y + (height / 2.0);
    }

    public double getLeftKickoffX() {
        return x + (width * 0.25);
    }

    public double getRightKickoffX() {
        return x + (width * 0.75);
    }

    public double getGoalTop() {
        return y + ((height - goalHeight) / 2.0);
    }

    public double getGoalBottom() {
        return getGoalTop() + goalHeight;
    }

    private boolean insideGoalMouth(double ballY, double radius) {
        // Only balls through the open part of the goal should score.
        // Inclusive bounds so a ball exactly on the edge is handled consistently
        // by both goal detection and wall bounces.
        return ballY >= (getGoalTop() + (radius * 0.30))
            && ballY <= (getGoalBottom() - (radius * 0.30));
    }

    /** Returns {@link #LEFT_GOAL}, {@link #RIGHT_GOAL} or {@link #NO_GOAL}. */
    public int checkGoal(Ball ball) {
        if (!insideGoalMouth(ball.getY(), ball.getRadius())) {
            return NO_GOAL;
        }

        // The whole ball must cross the line before it counts.
        if ((ball.getX() + ball.getRadius()) < x) {
            return LEFT_GOAL;
        }

        if ((ball.getX() - ball.getRadius()) > (x + width)) {
            return RIGHT_GOAL;
        }

        return NO_GOAL;
    }

    /** Players stay inside the pitch while the ball can enter goals. */
    public void confinePlayer(Player player) {
        double clampedX = Math.max(x + player.getRadius(),
            Math.min(x + width - player.getRadius(), player.getX()));
        double clampedY = Math.max(y + player.getRadius(),
            Math.min(y + height - player.getRadius(), player.getY()));
        player.setPosition(clampedX, clampedY);
    }

    public void bounceBall(Ball ball) {
        double radius = ball.getRadius();
        Vector2 velocity = ball.getVelocity();
        boolean inGoalMouth = insideGoalMouth(ball.getY(), radius);

        // Top and bottom walls always bounce.
        if ((ball.getY() - radius) <= y) {
            ball.setPosition(ball.getX(), y + radius);
            velocity.set(velocity.getX(), Math.abs(velocity.getY()) * Ball.WALL_RESTITUTION);
        }

        if ((ball.getY() + radius) >= (y + height)) {
            ball.setPosition(ball.getX(), (y + height) - radius);
            velocity.set(velocity.getX(), -Math.abs(velocity.getY()) * Ball.WALL_RESTITUTION);
        }

        // Side walls only bounce when the ball is not in the goal opening.
        if (!inGoalMouth && ((ball.getX() - radius) <= x)) {
            ball.setPosition(x + radius, ball.getY());
            velocity.set(Math.abs(velocity.getX()) * Ball.WALL_RESTITUTION, velocity.getY());
        }

        if (!inGoalMouth && ((ball.getX() + radius) >= (x + width))) {
            ball.setPosition((x + width) - radius, ball.getY());
            velocity.set(-Math.abs(velocity.getX()) * Ball.WALL_RESTITUTION, velocity.getY());
        }
    }

    /**
     * Renders the pitch onto a canvas of the given size.
     *
     * @param backgroundWidth width of the full canvas, including the surround
     * @param backgroundHeight height of the full canvas, including the surround
     */
    public void draw(GraphicsContext gc, double backgroundWidth, double backgroundHeight) {
        gc.setFill(Color.rgb(0, 184, 61));
        gc.fillRect(0, 0, backgroundWidth, backgroundHeight);

        gc.setFill(Color.rgb(0, 200, 70));
        gc.fillRect(x, y, width, height);

        gc.setLineWidth(4);
        gc.setStroke(Color.WHITE);
        gc.strokeRect(x, y, width, height);

        gc.strokeLine(getCenterX(), y, getCenterX(), y + height);
        gc.strokeOval(getCenterX() - 75, getCenterY() - 75, 150, 150);
        gc.setFill(Color.WHITE);
        gc.fillOval(getCenterX() - 5, getCenterY() - 5, 10, 10);

        double boxHeight = 220;
        double boxTop = y + ((height - boxHeight) / 2.0);
        gc.strokeRect(x, boxTop, 120, boxHeight);
        gc.strokeRect(x + width - 120, boxTop, 120, boxHeight);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeRect(x - goalDepth, getGoalTop(), goalDepth, goalHeight);
        gc.strokeRect(x + width, getGoalTop(), goalDepth, goalHeight);

        // Corner arcs.
        double cornerRadius = 16;
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);

        // Top-left.
        gc.strokeArc(x - cornerRadius, y - cornerRadius,
            cornerRadius * 2, cornerRadius * 2, 270, 90, ArcType.OPEN);

        // Top-right.
        gc.strokeArc(x + width - cornerRadius, y - cornerRadius,
            cornerRadius * 2, cornerRadius * 2, 180, 90, ArcType.OPEN);

        // Bottom-left.
        gc.strokeArc(x - cornerRadius, y + height - cornerRadius,
            cornerRadius * 2, cornerRadius * 2, 0, 90, ArcType.OPEN);

        // Bottom-right.
        gc.strokeArc(x + width - cornerRadius, y + height - cornerRadius,
            cornerRadius * 2, cornerRadius * 2, 90, 90, ArcType.OPEN);

        // Penalty arcs.
        double penaltyArcRadius = 60;
        double leftPenaltySpotX = x + 80;
        double rightPenaltySpotX = x + width - 80;

        // Left arc (bulges right into the pitch).
        gc.strokeArc(
            leftPenaltySpotX - penaltyArcRadius, getCenterY() - penaltyArcRadius,
            penaltyArcRadius * 2, penaltyArcRadius * 2,
            312, 96, ArcType.OPEN);

        // Right arc (bulges left into the pitch).
        gc.strokeArc(
            rightPenaltySpotX - penaltyArcRadius, getCenterY() - penaltyArcRadius,
            penaltyArcRadius * 2, penaltyArcRadius * 2,
            132, 96, ArcType.OPEN);
    }
}
