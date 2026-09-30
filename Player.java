import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.ArcType;

// one human controlled player
public class Player extends GameObject {

    private static final double ACCELERATION = 1800;
    private static final double DRAG = 7.0;
    private static final double MAX_SPEED = 320;
    private static final double KICK_COOLDOWN = 1.00;
    private static final double KICK_IMPULSE = 360;
    private double kickImpactTimer = 0;
    private static final double IMPACT_DURATION = 0.25;

    private final String name;
    private final String badge;
    private final Color detailColor;
    private Vector2 facing;
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private int score;
    private double kickCooldown;


    public Player(String name, String badge, Color fillColor, Color detailColor,
                  double x, double y, Vector2 facing) {
        super(x, y, 24, 6.0, fillColor);
        this.name = name;
        this.badge = badge;
        this.detailColor = detailColor;
        this.facing = facing.copy().normalize();
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
        facing = newFacing.copy().normalize();
        kickCooldown = 0;
    }


    public boolean tryKick(Ball ball) {
        if (kickCooldown > 0) {
            return false;
        }

        Vector2 toBall = ball.getPosition().copy().subtract(getPosition());
        // keeps kicks close enough to feel like real contact
        double reach = getRadius() + ball.getRadius() + 6;

        if (toBall.lengthSquared() > (reach * reach)) {
            return false;
        }

        if (toBall.lengthSquared() == 0) {
            toBall = facing.copy();
        }
        else {
            toBall.normalize();
        }

        return finishKick(ball, toBall);
    }


    public boolean tryKickToward(Ball ball, Vector2 direction) {
        if (kickCooldown > 0) {
            return false;
        }

        Vector2 toBall = ball.getPosition().copy().subtract(getPosition());
        double reach = getRadius() + ball.getRadius() + 6;

        if (toBall.lengthSquared() > (reach * reach)) {
            return false;
        }

        Vector2 kickDirection = direction.copy();
        if (kickDirection.lengthSquared() == 0) {
            kickDirection = facing.copy();
        }
        else {
            kickDirection.normalize();
        }

        return finishKick(ball, kickDirection);
    }


    private boolean finishKick(Ball ball, Vector2 direction) {
        Vector2 impulse = direction.copy().scale(KICK_IMPULSE);
        impulse.add(getVelocity().copy().scale(0.35));
        // some player momentum carries into the kick
        ball.getVelocity().add(impulse);
        ball.addSpin(90);

        getVelocity().subtract(direction.copy().scale(20));
        // Trigger the visual effect
        this.kickImpactTimer = IMPACT_DURATION;
        
        kickCooldown = KICK_COOLDOWN;
        return true;
    }


    @Override
    public void update(double dt) {
        Vector2 movement = new Vector2();

        if (up) {
            movement.add(new Vector2(0, -1));
        }
        if (down) {
            movement.add(new Vector2(0, 1));
        }
        if (left) {
            movement.add(new Vector2(-1, 0));
        }
        if (right) {
            movement.add(new Vector2(1, 0));
        }

        if (movement.lengthSquared() > 0) {
            movement.normalize();
            facing = movement.copy();
            // facing follows the last movement direction
            getVelocity().add(movement.scale(ACCELERATION * dt));
        }

        getVelocity().scale(1.0 / (1.0 + (DRAG * dt)));
        getVelocity().limit(MAX_SPEED);
        integrate(dt);
        
        if (kickImpactTimer > 0) {
            kickImpactTimer = Math.max(0, kickImpactTimer - dt); // Clamp the impact timer
        }

        if (kickCooldown > 0) {
            kickCooldown = Math.max(0, kickCooldown - dt); // Clamp the cooldown
        }
    }


    @Override
    public void draw(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double radius = getRadius();
        // Ball Shadow
        gc.setFill(Color.color(0, 0, 0, 0.22));
        gc.fillOval(x - radius + 3, y - radius + 5, radius * 2, radius * 2);
        // Ball
        gc.setFill(getFillColor());
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        // Outline
        gc.setLineWidth(3);
        gc.setStroke(detailColor);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
        // Facing direction (arrow)
        gc.setLineWidth(3);
        gc.setStroke(detailColor);
        
        double arrowLength = 16;
        double wingLength = 6;
        double wingAngle = Math.toRadians(30); // Angle of the arrow wings
        // Calculate the tip of the arrow
        double tipX = x + (facing.getX() * arrowLength);
        double tipY = y + (facing.getY() * arrowLength);
        // Main stem of the arrow
        gc.strokeLine(x, y, tipX, tipY);
        // Calculate angles for the arrow wings
        double baseAngle = Math.atan2(facing.getY(), facing.getX());
        // Left arrow wing
        double wing1X = tipX - wingLength * Math.cos(baseAngle - wingAngle);
        double wing1Y = tipY - wingLength * Math.sin(baseAngle - wingAngle);
        gc.strokeLine(tipX, tipY, wing1X, wing1Y);
        // Right arrow wing
        double wing2X = tipX - wingLength * Math.cos(baseAngle + wingAngle);
        double wing2Y = tipY - wingLength * Math.sin(baseAngle + wingAngle);
        gc.strokeLine(tipX, tipY, wing2X, wing2Y);

        double progress = 1.0 - (kickCooldown / KICK_COOLDOWN);
        progress = Math.max(0, Math.min(1, progress)); // clamp between 0 and 1
        
        double ringX = x - radius - 6;
        double ringY = y - radius - 6;
        double ringSize = (radius * 2) + 12;
        
        if (kickImpactTimer > 0) {
            double kickEffectProgress = 1.0 - (kickImpactTimer / IMPACT_DURATION);
            // Kick effect expands as time passes
            double effectRadius = getRadius() + (kickEffectProgress * 30);
            // Kick effect fades out as it expands
            double alpha = 1.0 - kickEffectProgress;
            
            gc.save();
            gc.setStroke(Color.ORANGE);
            gc.setGlobalAlpha(alpha);
            // Effect gets thinner as it expands
            gc.setLineWidth(2 + (1.0 - kickEffectProgress) * 3);
            
            gc.strokeOval(getX() - effectRadius, getY() - effectRadius, 
            effectRadius * 2, effectRadius * 2);
            gc.restore();
        }
        
        // Background faint ring (always visible)
        gc.save();
        gc.setGlobalAlpha(0.2);
        gc.setLineWidth(2);
        gc.setStroke(detailColor);
        gc.strokeOval(ringX, ringY, ringSize, ringSize);
        gc.restore();
        
        if (kickCooldown > 0) {
            // Progress arc with fade
            gc.save();
            // eased progress
            double eased = 1 - Math.pow(1 - progress, 2);
            // fade in as it charges
            double alpha = 0.3 + (0.7 * eased);
            gc.setGlobalAlpha(alpha);
            
            gc.setLineWidth(3);
            gc.setStroke(detailColor);
            
            gc.strokeArc(ringX, ringY, ringSize, ringSize, 90, -360 * progress, ArcType.OPEN);
            
            gc.restore();
        }
        else {
            // Full ring when kick is ready
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
