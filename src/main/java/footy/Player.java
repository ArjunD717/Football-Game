package footy;

import java.util.Objects;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.ArcType;

/**
 * A human- or AI-controlled player: acceleration movement, kicking and rendering.
 *
 * <p>Input is sampled as four direction flags each frame (see
 * {@link #setInput}); the AI driver writes the same flags, so both control
 * schemes share one movement path.
 */
public class Player extends GameObject {

    private static final double ACCELERATION = 1800;
    private static final double DRAG = 7.0;
    private static final double MAX_SPEED = 320;
    private static final double KICK_COOLDOWN = 1.00;
    private static final double KICK_IMPULSE = 360;
    private static final double KICK_REACH_PADDING = 6;
    private static final double KICK_MOMENTUM_TRANSFER = 0.35;
    private static final double KICK_RECOIL = 20;
    private static final double IMPACT_DURATION = 0.25;

    private final String name;
    private final String badge;
    private final Color detailColor;
    private final Vector2 facing = new Vector2();
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private int score;
    private double kickCooldown;
    private double kickImpactTimer;
    public Player(String name, String badge, Color fillColor, Color detailColor,
                  double x, double y, Vector2 facing) {
        super(x, y, 24, 6.0, fillColor);
        this.name = Objects.requireNonNull(name, "name");
        this.badge = Objects.requireNonNull(badge, "badge");
        this.detailColor = Objects.requireNonNull(detailColor, "detailColor");
        Vector2 initialFacing = Objects.requireNonNull(facing, "facing").copy().normalize();
        this.facing.set(initialFacing.getX(), initialFacing.getY());
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void incrementScore() {
        score++;
    }

    public void resetScore() {
        score = 0;
    }

    public void setInput(boolean up, boolean down, boolean left, boolean right) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
    }

    public void reset(double x, double y, Vector2 newFacing) {
        setPosition(x, y);
        setVelocity(0, 0);
        Vector2 normalized = Objects.requireNonNull(newFacing, "newFacing").copy().normalize();
        facing.set(normalized.getX(), normalized.getY());
        kickCooldown = 0;
        kickImpactTimer = 0;
    }

    /** Kicks toward the ball's current position relative to the player. */
    public boolean tryKick(Ball ball) {
        Objects.requireNonNull(ball, "ball");
        if (kickCooldown > 0) {
            return false;
        }

        Vector2 toBall = ball.getPosition().copy().subtract(getPosition());
        // Keeps kicks close enough to feel like real contact.
        double reach = getRadius() + ball.getRadius() + KICK_REACH_PADDING;

        if (toBall.lengthSquared() > (reach * reach)) {
            return false;
        }

        if (toBall.lengthSquared() == 0) {
            toBall = facing.copy();
        } else {
            toBall.normalize();
        }

        return finishKick(ball, toBall);
    }

    /** Kicks in an explicit direction (used by the AI); falls back to facing. */
    public boolean tryKickToward(Ball ball, Vector2 direction) {
        Objects.requireNonNull(ball, "ball");
        Objects.requireNonNull(direction, "direction");
        if (kickCooldown > 0) {
            return false;
        }

        Vector2 toBall = ball.getPosition().copy().subtract(getPosition());
        double reach = getRadius() + ball.getRadius() + KICK_REACH_PADDING;

        if (toBall.lengthSquared() > (reach * reach)) {
            return false;
        }

        Vector2 kickDirection = direction.copy();
        if (kickDirection.lengthSquared() == 0) {
            kickDirection = facing.copy();
        } else {
            kickDirection.normalize();
        }

        return finishKick(ball, kickDirection);
    }

    private boolean finishKick(Ball ball, Vector2 direction) {
        Vector2 impulse = direction.copy().scale(KICK_IMPULSE);
        // Some player momentum carries into the kick.
        impulse.addScaled(getVelocity(), KICK_MOMENTUM_TRANSFER);
        ball.getVelocity().add(impulse);
        ball.addSpin(90);

        getVelocity().addScaled(direction, -KICK_RECOIL);
        kickImpactTimer = IMPACT_DURATION;
        kickCooldown = KICK_COOLDOWN;
        return true;
    }

    /**
     * Ticks the kick cooldown and impact timers. Called every frame from
     * {@link #update(double)} and additionally during the kickoff pause so a
     * kick fired just before a goal does not stay locked through the pause.
     */
    void tickTimers(double dt) {
        if (kickImpactTimer > 0) {
            kickImpactTimer = Math.max(0, kickImpactTimer - dt);
        }
        if (kickCooldown > 0) {
            kickCooldown = Math.max(0, kickCooldown - dt);
        }
    }

    @Override
    public void update(double dt) {
        double moveX = 0;
        double moveY = 0;
        if (up) {
            moveY -= 1;
        }
        if (down) {
            moveY += 1;
        }
        if (left) {
            moveX -= 1;
        }
        if (right) {
            moveX += 1;
        }

        if (moveX != 0 || moveY != 0) {
            double length = Math.hypot(moveX, moveY);
            double dirX = moveX / length;
            double dirY = moveY / length;
            // Facing follows the last movement direction.
            facing.set(dirX, dirY);
            Vector2 velocity = getVelocity();
            velocity.set(
                velocity.getX() + dirX * ACCELERATION * dt,
                velocity.getY() + dirY * ACCELERATION * dt);
        }

        getVelocity().scale(1.0 / (1.0 + (DRAG * dt)));
        getVelocity().limit(MAX_SPEED);
        integrate(dt);
        tickTimers(dt);
    }

    @Override
    public void draw(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double radius = getRadius();
        // Shadow.
        gc.setFill(Color.color(0, 0, 0, 0.22));
        gc.fillOval(x - radius + 3, y - radius + 5, radius * 2, radius * 2);
        // Body.
        gc.setFill(getFillColor());
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        // Outline.
        gc.setLineWidth(3);
        gc.setStroke(detailColor);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
        // Facing direction (arrow).
        gc.setLineWidth(3);
        gc.setStroke(detailColor);

        double arrowLength = 16;
        double wingLength = 6;
        double wingAngle = Math.toRadians(30); // Angle of the arrow wings.
        // Tip of the arrow.
        double tipX = x + (facing.getX() * arrowLength);
        double tipY = y + (facing.getY() * arrowLength);
        // Main stem of the arrow.
        gc.strokeLine(x, y, tipX, tipY);
        // Arrow wings.
        double baseAngle = Math.atan2(facing.getY(), facing.getX());
        double wing1X = tipX - wingLength * Math.cos(baseAngle - wingAngle);
        double wing1Y = tipY - wingLength * Math.sin(baseAngle - wingAngle);
        gc.strokeLine(tipX, tipY, wing1X, wing1Y);
        double wing2X = tipX - wingLength * Math.cos(baseAngle + wingAngle);
        double wing2Y = tipY - wingLength * Math.sin(baseAngle + wingAngle);
        gc.strokeLine(tipX, tipY, wing2X, wing2Y);

        double progress = 1.0 - (kickCooldown / KICK_COOLDOWN);
        progress = Math.max(0, Math.min(1, progress)); // Clamp between 0 and 1.

        double ringX = x - radius - 6;
        double ringY = y - radius - 6;
        double ringSize = (radius * 2) + 12;

        if (kickImpactTimer > 0) {
            double kickEffectProgress = 1.0 - (kickImpactTimer / IMPACT_DURATION);
            // Kick effect expands as time passes.
            double effectRadius = getRadius() + (kickEffectProgress * 30);
            // Kick effect fades out as it expands.
            double alpha = 1.0 - kickEffectProgress;

            gc.save();
            gc.setStroke(Color.ORANGE);
            gc.setGlobalAlpha(alpha);
            // Effect gets thinner as it expands.
            gc.setLineWidth(2 + (1.0 - kickEffectProgress) * 3);

            gc.strokeOval(getX() - effectRadius, getY() - effectRadius,
                effectRadius * 2, effectRadius * 2);
            gc.restore();
        }

        // Background faint ring (always visible).
        gc.save();
        gc.setGlobalAlpha(0.2);
        gc.setLineWidth(2);
        gc.setStroke(detailColor);
        gc.strokeOval(ringX, ringY, ringSize, ringSize);
        gc.restore();

        if (kickCooldown > 0) {
            // Progress arc with fade.
            gc.save();
            // Eased progress.
            double eased = 1 - Math.pow(1 - progress, 2);
            // Fade in as it charges.
            double alpha = 0.3 + (0.7 * eased);
            gc.setGlobalAlpha(alpha);

            gc.setLineWidth(3);
            gc.setStroke(detailColor);

            gc.strokeArc(ringX, ringY, ringSize, ringSize, 90, -360 * progress, ArcType.OPEN);

            gc.restore();
        } else {
            // Full ring when kick is ready.
            gc.save();
            gc.setLineWidth(3);
            gc.setStroke(detailColor);
            gc.strokeOval(ringX, ringY, ringSize, ringSize);

            gc.restore();
        }

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 32));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(badge, x, y + 1);
    }
}
