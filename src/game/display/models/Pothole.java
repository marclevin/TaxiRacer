package game.display.models;

import game.logic.Visitor;

/**
 * This class represents a pothole.
 */
public class Pothole extends Sprite  {

    /**
     * Side of the square hit box, in pixels.
     *
     * <p>
     * Deliberately smaller than the 43x30 sprite: clipping the very edge of a pothole
     * should not cost the player a wheel.
     * </p>
     */
    private static final int BOUND_SIZE = 25;

    /**
     * Steps a pothole ignores the taxi for after being hit.
     *
     * <p>
     * Long enough to cover driving clear of one at any speed. Without it a pothole scores a
     * fresh hit on every frame of contact — roughly thirty in a row — so a single bump read
     * as an invisible, continuous drain rather than as one thump the player can learn from.
     * </p>
     */
    private static final int HIT_LOCKOUT_STEPS = 60;

    private int hitLockout = 0;

    /**
     * Constructor for the pothole.
     * @param x X coordinate of the pothole
     * @param y Y coordinate of the pothole
     */
    public Pothole(int x, int y) {
        super(x, y);
        this.myBound.setWidth(BOUND_SIZE);
        this.myBound.setHeight(BOUND_SIZE);
    }

    /**
     * Claims a hit on the taxi, if this pothole has finished with the last one.
     *
     * @return {@code true} if this contact counts as a fresh hit.
     */
    public boolean tryHit() {
        if (hitLockout > 0) {
            return false;
        }
        hitLockout = HIT_LOCKOUT_STEPS;
        return true;
    }

    @Override
    public void tick() {
        if (hitLockout > 0) {
            hitLockout--;
        }
    }


    /**
     * Acceptor for the visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        
    }
    
}
