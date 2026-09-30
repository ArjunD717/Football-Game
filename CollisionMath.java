// handles the circle collision maths in one place
public class CollisionMath {

    private CollisionMath() {
    }


    public static boolean resolveCircleCollision(GameObject first, GameObject second, double restitution) {
        Vector2 offset = second.getPosition().copy().subtract(first.getPosition());
        double minDistance = first.getRadius() + second.getRadius();
        double distanceSquared = offset.lengthSquared();

        if (distanceSquared == 0) {
            // gives the collision a direction when two circles stack exactly
            offset.set(1, 0);
            distanceSquared = 1;
        }

        if (distanceSquared >= (minDistance * minDistance)) {
            return false;
        }

        double distance = Math.sqrt(distanceSquared);
        Vector2 normal = offset.scale(1.0 / distance);

        double inverseMassFirst = 1.0 / first.getMass();
        double inverseMassSecond = 1.0 / second.getMass();
        double totalInverseMass = inverseMassFirst + inverseMassSecond;
        double overlap = minDistance - distance;

        // move both circles apart before changing their speed
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
