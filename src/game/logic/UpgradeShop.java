package game.logic;

import game.display.models.Taxi;

/**
 * The garage's price list and purchase rules.
 *
 * <p>
 * Prices live here rather than in the menu code so that the cost shown to the player and the
 * cost actually charged cannot drift apart.
 * </p>
 */
public final class UpgradeShop {

    /** Cost of the first, second and third engine upgrade. */
    private static final double[] ENGINE_COSTS = { 100.0, 200.0, 500.0 };
    /** Cost of the first, second and third set of wheels. */
    private static final double[] WHEEL_COSTS = { 100.0, 250.0, 700.0 };
    /**
     * Cost of the first, second and third row of extra seats.
     *
     * <p>
     * Priced above the other upgrades because capacity compounds: every extra seat is a
     * drop-off stop the player does not have to make.
     * </p>
     */
    private static final double[] CAPACITY_COSTS = { 150.0, 400.0, 900.0 };
    /** Cost of a canister of NOS, which is consumed on use and can be rebought. */
    public static final double NOS_COST = 100.0;

    /** Returned in place of a price when there is nothing left to buy. */
    public static final double UNAVAILABLE = -1.0;

    private UpgradeShop() {
    }

    /**
     * Returns the price of the taxi's next engine upgrade.
     *
     * @param taxi the taxi being upgraded
     * @return the price, or {@link #UNAVAILABLE} if the engine is already maxed out.
     */
    public static double engineCost(Taxi taxi) {
        return costAt(ENGINE_COSTS, taxi == null ? Taxi.MAX_UPGRADE : taxi.getEngineUpgrade());
    }

    /**
     * Returns the price of the taxi's next set of wheels.
     *
     * @param taxi the taxi being upgraded
     * @return the price, or {@link #UNAVAILABLE} if the wheels are already maxed out.
     */
    public static double wheelCost(Taxi taxi) {
        return costAt(WHEEL_COSTS, taxi == null ? Taxi.MAX_UPGRADE : taxi.getPotholeResistance());
    }

    /**
     * Returns the price of the taxi's next row of seats.
     *
     * @param taxi the taxi being upgraded
     * @return the price, or {@link #UNAVAILABLE} if the taxi is already at full size.
     */
    public static double capacityCost(Taxi taxi) {
        return costAt(CAPACITY_COSTS, taxi == null ? Taxi.MAX_UPGRADE : taxi.getCapacityUpgrade());
    }

    /**
     * Buys the next row of seats if the taxi can afford it.
     *
     * @param taxi the taxi being upgraded
     * @return {@code true} if the purchase went through.
     */
    public static boolean buyCapacity(Taxi taxi) {
        double cost = capacityCost(taxi);
        if (!canAfford(taxi, cost)) {
            return false;
        }
        taxi.setWallet(taxi.getWallet() - cost);
        taxi.setCapacityUpgrade(taxi.getCapacityUpgrade() + 1);
        return true;
    }

    /**
     * Buys the next engine upgrade if the taxi can afford it.
     *
     * @param taxi the taxi being upgraded
     * @return {@code true} if the purchase went through.
     */
    public static boolean buyEngine(Taxi taxi) {
        double cost = engineCost(taxi);
        if (!canAfford(taxi, cost)) {
            return false;
        }
        taxi.setWallet(taxi.getWallet() - cost);
        taxi.setEngineUpgrade(taxi.getEngineUpgrade() + 1);
        return true;
    }

    /**
     * Buys the next set of wheels if the taxi can afford it.
     *
     * @param taxi the taxi being upgraded
     * @return {@code true} if the purchase went through.
     */
    public static boolean buyWheels(Taxi taxi) {
        double cost = wheelCost(taxi);
        if (!canAfford(taxi, cost)) {
            return false;
        }
        taxi.setWallet(taxi.getWallet() - cost);
        taxi.setPotHoleResistance(taxi.getPotholeResistance() + 1);
        return true;
    }

    /**
     * Buys a canister of NOS if the taxi can afford it and is not already carrying one.
     *
     * @param taxi the taxi being upgraded
     * @return {@code true} if the purchase went through.
     */
    public static boolean buyNos(Taxi taxi) {
        if (taxi == null || taxi.hasNOS() || !canAfford(taxi, NOS_COST)) {
            return false;
        }
        taxi.setWallet(taxi.getWallet() - NOS_COST);
        taxi.setNOS(true);
        return true;
    }

    /**
     * Looks up the price of the next level in a price list.
     *
     * @param prices the price list, indexed by current level
     * @param level  the level already owned
     * @return the price of the next level, or {@link #UNAVAILABLE} when there is none.
     */
    private static double costAt(double[] prices, int level) {
        if (level < 0 || level >= prices.length) {
            return UNAVAILABLE;
        }
        return prices[level];
    }

    /**
     * Reports whether a purchase at the given price can go ahead.
     *
     * @param taxi the buyer
     * @param cost the price, possibly {@link #UNAVAILABLE}
     * @return {@code true} if the item exists and the wallet covers it.
     */
    private static boolean canAfford(Taxi taxi, double cost) {
        return taxi != null && cost != UNAVAILABLE && taxi.getWallet() >= cost;
    }
}
