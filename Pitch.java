import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

// draws the pitch and decides when a goal counts
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
        // only balls through the open part of the goal should score
        return ballY > (getGoalTop() + (radius * 0.30))
            && ballY < (getGoalBottom() - (radius * 0.30));
    }


    public int checkGoal(Ball ball) {
        if (!insideGoalMouth(ball.getY(), ball.getRadius())) {
            return NO_GOAL;
        }

        if ((ball.getX() + ball.getRadius()) < x) {
            return LEFT_GOAL;
        }

        if ((ball.getX() - ball.getRadius()) > (x + width)) {
            return RIGHT_GOAL;
        }

        return NO_GOAL;
    }


    public void confinePlayer(Player player) {
        // players stay inside the pitch while the ball can enter goals
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

        // top and bottom walls always bounce
        if ((ball.getY() - radius) <= y) {
            ball.setPosition(ball.getX(), y + radius);
            velocity.set(velocity.getX(), Math.abs(velocity.getY()) * Ball.WALL_RESTITUTION);
        }

        if ((ball.getY() + radius) >= (y + height)) {
            ball.setPosition(ball.getX(), (y + height) - radius);
            velocity.set(velocity.getX(), -Math.abs(velocity.getY()) * Ball.WALL_RESTITUTION);
        }

        // side walls only bounce when the ball is not in the goal opening
        if (!inGoalMouth && ((ball.getX() - radius) <= x)) {
            ball.setPosition(x + radius, ball.getY());
            velocity.set(Math.abs(velocity.getX()) * Ball.WALL_RESTITUTION, velocity.getY());
        }

        if (!inGoalMouth && ((ball.getX() + radius) >= (x + width))) {
            ball.setPosition((x + width) - radius, ball.getY());
            velocity.set(-Math.abs(velocity.getX()) * Ball.WALL_RESTITUTION, velocity.getY());
        }
    }


    public void draw(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 184, 61));
        gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

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
        
        // corner arcs
        double cornerRadius = 16;
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        
        // top-left
        gc.strokeArc(x - cornerRadius, y - cornerRadius,
            cornerRadius * 2, cornerRadius * 2, 270, 90, ArcType.OPEN);
        
        // top-right
        gc.strokeArc(x + width - cornerRadius, y - cornerRadius,
            cornerRadius * 2, cornerRadius * 2, 180, 90, ArcType.OPEN);
        
        // bottom-left
        gc.strokeArc(x - cornerRadius, y + height - cornerRadius,
            cornerRadius * 2, cornerRadius * 2, 0, 90, ArcType.OPEN);
        
        // bottom-right
        gc.strokeArc(x + width - cornerRadius, y + height - cornerRadius,
        cornerRadius * 2, cornerRadius * 2, 90, 90, ArcType.OPEN);
        
        // penalty arcs
        double penaltyArcRadius = 60;
        double leftPenaltySpotX = x + 80;
        double rightPenaltySpotX = x + width - 80;
        
        // left arc (bulges right into pitch)
        gc.strokeArc(
            leftPenaltySpotX - penaltyArcRadius, getCenterY() - penaltyArcRadius,
            penaltyArcRadius * 2, penaltyArcRadius * 2,
            312, 96, ArcType.OPEN);
        
        // right arc (bulges left into pitch)
        gc.strokeArc(
            rightPenaltySpotX - penaltyArcRadius, getCenterY() - penaltyArcRadius,
            penaltyArcRadius * 2, penaltyArcRadius * 2,
            132, 96, ArcType.OPEN);
    }
}
