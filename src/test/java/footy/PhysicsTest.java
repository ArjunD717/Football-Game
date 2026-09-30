package footy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/** Headless physics and geometry tests — no JavaFX toolkit required. */
class PhysicsTest {

    private static Ball ballAt(double x, double y) {
        return new Ball(x, y);
    }

    private static Player playerAt(double x, double y) {
        return new Player("Test", "T", Color.RED, Color.WHITE, x, y, new Vector2(1, 0));
    }

    private static Pitch pitch() {
        return new Pitch(70, 50, 820, 500, 32, 190);
    }

    @Test
    void ballEventuallyStops() {
        Ball ball = ballAt(100, 100);
        ball.setVelocity(600, 0);
        for (int i = 0; i < 60 * 30; i++) {
            ball.update(1.0 / 60);
        }
        assertEquals(0, ball.getVelocity().length(), 1e-9);
    }

    @Test
    void playerSpeedIsCapped() {
        Player player = playerAt(100, 100);
        player.setInput(false, false, false, true);
        for (int i = 0; i < 60 * 10; i++) {
            player.update(1.0 / 60);
        }
        assertTrue(player.getVelocity().length() <= 320.0001);
    }

    @Test
    void overlappingCirclesSeparate() {
        Player first = playerAt(100, 100);
        Player second = playerAt(100, 100);
        assertTrue(CollisionMath.resolveCircleCollision(first, second, 0.8));
        double minDistance = first.getRadius() + second.getRadius();
        double actual = first.getPosition().copy().subtract(second.getPosition()).length();
        assertEquals(minDistance, actual, 1e-6);
    }

    @Test
    void nonOverlappingCirclesAreUntouched() {
        Player first = playerAt(100, 100);
        Player second = playerAt(500, 500);
        assertFalse(CollisionMath.resolveCircleCollision(first, second, 0.8));
    }

    @Test
    void wholeBallMustCrossLineInsideMouth() {
        Pitch pitch = pitch();
        Ball justShort = ballAt(pitch.getLeft() - 5, pitch.getCenterY());
        assertEquals(Pitch.NO_GOAL, pitch.checkGoal(justShort));

        Ball scored = ballAt(pitch.getLeft() - 11, pitch.getCenterY());
        assertEquals(Pitch.LEFT_GOAL, pitch.checkGoal(scored));

        Ball outsideMouth = ballAt(pitch.getLeft() - 11, pitch.getTop() - 5);
        assertEquals(Pitch.NO_GOAL, pitch.checkGoal(outsideMouth));

        Ball right = ballAt(pitch.getRight() + 11, pitch.getCenterY());
        assertEquals(Pitch.RIGHT_GOAL, pitch.checkGoal(right));
    }

    @Test
    void goalMouthEdgeIsConsistent() throws Exception {
        Pitch pitch = pitch();
        double radius = 10;
        // Ball centre exactly on the goal-mouth boundary: detection and
        // bounceBall must agree it IS in the mouth (inclusive bounds), so the
        // ball scores instead of bouncing off the side wall.
        double edgeY = pitch.getGoalTop() + radius * 0.30;
        Ball edge = ballAt(pitch.getLeft() - 11, edgeY);
        edge.setVelocity(-100, 0);
        assertEquals(Pitch.LEFT_GOAL, pitch.checkGoal(edge));
        pitch.bounceBall(edge);
        assertTrue(edge.getVelocity().getX() < 0, "in-mouth balls must not bounce off the side wall");

        // Just outside the mouth the wall must bounce instead.
        Ball outside = ballAt(pitch.getLeft() - 5, edgeY - 1);
        outside.setVelocity(-100, 0);
        assertEquals(Pitch.NO_GOAL, pitch.checkGoal(outside));
        pitch.bounceBall(outside);
        assertTrue(outside.getVelocity().getX() > 0, "side wall should bounce balls outside the mouth");
    }

    @Test
    void ballBouncesOffTopWall() {
        Pitch pitch = pitch();
        Ball ball = ballAt(pitch.getCenterX(), pitch.getTop() + 9);
        ball.setVelocity(0, -200);
        pitch.bounceBall(ball);
        assertEquals(pitch.getTop() + ball.getRadius(), ball.getY(), 1e-9);
        assertTrue(ball.getVelocity().getY() > 0);
    }

    @Test
    void playersAreConfinedToPitch() {
        Pitch pitch = pitch();
        Player player = playerAt(-1000, -1000);
        pitch.confinePlayer(player);
        assertTrue(player.getX() >= pitch.getLeft() + player.getRadius());
        assertTrue(player.getY() >= pitch.getTop() + player.getRadius());
    }

    @Test
    void vectorLimitNeverScalesUp() {
        Vector2 v = new Vector2(3, 4);
        v.limit(10);
        assertEquals(5, v.length(), 1e-9);
        v.limit(2);
        assertEquals(2, v.length(), 1e-9);
    }

    @Test
    void timersTickDuringUpdate() throws Exception {
        Player player = playerAt(100, 100);
        Ball ball = ballAt(105, 100);
        assertTrue(player.tryKick(ball));
        player.update(0.5);
        assertTrue(cooldownOf(player) > 0);
        assertTrue(impactOf(player) <= 0, "impact effect should expire after 0.5 s");
        player.update(1.0);
        assertEquals(0, cooldownOf(player), 1e-9);
    }

    @Test
    void kickCooldownTicksWithoutUpdate() throws Exception {
        Player player = playerAt(100, 100);
        Ball ball = ballAt(105, 100);
        assertTrue(player.tryKick(ball));
        player.tickTimers(1.0);
        assertEquals(0, cooldownOf(player), 1e-9);
    }

    private static double cooldownOf(Player player) throws Exception {
        Field field = Player.class.getDeclaredField("kickCooldown");
        field.setAccessible(true);
        return (double) field.get(player);
    }

    private static double impactOf(Player player) throws Exception {
        Field field = Player.class.getDeclaredField("kickImpactTimer");
        field.setAccessible(true);
        return (double) field.get(player);
    }
}
