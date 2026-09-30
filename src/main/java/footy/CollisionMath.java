package footy;

import java.util.Objects;

/**
 * Elastic collision resolution for circular bodies.
 *
 * <p>Applies positional correction split by inverse mass, then an impulse
 * along the contact normal. Stateless and thread-safe.
 */
public final class CollisionMath {

    private CollisionMath() {
    }

    /**
     * Separates two overlapping circles and exchanges momentum.
     *
     * @param restitution 0 = inelastic, 1 = perfectly elastic
     * @return {@code true} if the circles overlapped and were resolved
     */
    public static boolean resolveCircleCollision(GameObject first, GameObject second, double restitution) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (restitution < 0 || restitution > 1 || Double.isNaN(restitution)) {
            throw new IllegalArgumentException("restitution must be in [0, 1]: " + restitution);
        }

        Vector2 offset = second.getPosition().copy().subtract(first.getPosition());
        double minDistance = first.getRadius() + second.getRadius();
        double distanceSquared = offset.lengthSquared();

        final Vector2 normal;
        final double distance;
        if (distanceSquared == 0) {
            // Circles stacked exactly: true separation is zero, so pick an
            // arbitrary axis. (The old code faked a length of 1 here, which
            // left the pair 1px short of full separation, overlapping forever.)
            normal = new Vector2(1, 0);
            distance = 0;
        } else {
            if (distanceSquared >= (minDistance * minDistance)) {
                return false;
            }
            distance = Math.sqrt(distanceSquared);
            normal = offset.scale(1.0 / distance);
        }

        double inverseMassFirst = 1.0 / first.getMass();
        double inverseMassSecond = 1.0 / second.getMass();
        double totalInverseMass = inverseMassFirst + inverseMassSecond;
        double overlap = minDistance - distance;

        // Move both circles apart before changing their velocities.
        first.getPosition().subtract(normal.copy().scale(overlap * (inverseMassFirst / totalInverseMass)));
        second.getPosition().add(normal.copy().scale(overlap * (inverseMassSecond / totalInverseMass)));

        Vector2 relativeVelocity = second.getVelocity().copy().subtract(first.getVelocity());
        double velocityAlongNormal = relativeVelocity.dot(normal);

        if (velocityAlongNormal < 0) {
            double impulseMagnitude = -((1 + restitution) * velocityAlongNormal) / totalInverseMass;
            Vector2 impulse = normal.copy().scale(impulseMagnitude);

            first.getVelocity().subtract(impulse.copy().scale(inverseMassFirst));
            second.getVelocity().add(impulse.copy().scale(inverseMassSecond));
        }

        return true;
    }
}
