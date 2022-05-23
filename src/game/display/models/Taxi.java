package game.display.models;



import game.logic.Visitor;
import game.utility.ETaxiPositions;
import javafx.scene.image.Image;

public class Taxi extends Sprite
{

  
    private double wallet = 0;
    private int career_passengers = 0;
    private transient boolean isPrime = true;
    private transient Image primeImage, secondImage = null;
    private int punishment_clock = 0;
    private int engine_upgrade = 0;
    private int pothole_resistance = 0;
    private boolean hasNOS = false;
    private transient boolean usedNOS = false;
    private transient ETaxiPositions myPosition = null;


  /**
     * Constructor for the Taxi class
     * @param x X coordinate of the taxi
     * @param y Y coordinate of the taxi
     */
    public Taxi(int x, int y)
    {
        super(x, y);
    }

    /**
     * Sets the EngineUpgrade of the taxi
     * @param upgrade The EngineUpgrade of the taxi
     */
    public void setEngineUpgrade(int upgrade)
    {
        this.engine_upgrade = upgrade;
    }

    /**
     * Returns the state of the taxi's NOS
     * @return True if the taxi has used NOS, false otherwise
     */
    public boolean hasUsedNos()
    {
        return usedNOS;
    }

    /**
     * Sets the state of the taxi's NOS usage.
     * @param usedNOS True if the taxi has used NOS, false otherwise
     */
    public void setUsedNos(boolean usedNOS)
    {
        this.usedNOS = usedNOS;
    }
    /**
     * Checks the state of the Taxi's NOS
     * @return True if the taxi has NOS, false otherwise
     */
    public boolean hasNOS()
    {
        return this.hasNOS;
    }

    /**
     * Sets the state of the taxi's NOS
     * @param nos True if the taxi has NOS, false otherwise
     */
    public void setNOS(boolean nos)
    {
        this.hasNOS = nos;
    }
    /**
     * Returns the EngineUpgrade of the taxi.
     * @return The EngineUpgrade of the taxi
     */
    public int getEngineUpgrade()
    {
        return this.engine_upgrade;
    }
    /**
     * Sets the image set of the Taxi
     * @param prime The first image of the taxi
     * @param secondary The second image of the taxi
     */
    public void setImageSet(Image prime, Image secondary)
    {
        this.primeImage = prime;
        this.secondImage = secondary;
    }

    /**
     * Adds punishment to the taxi, this will slow it down.
     * @param punishment The punishment time to add
     */
    public void addPunishment(int punishment)
    {
        this.punishment_clock += punishment;
    }

    /**
     * Returns the time of the taxi's punishment 
     * @return The time of the taxi's punishment 
     */
    public int getPunishment()
    {
        return this.punishment_clock;
    }

    /**
     * Reduces the punishment time of the taxi.
     */
    public void minusPunishment()
    {
        this.punishment_clock = this.punishment_clock - (1 + this.engine_upgrade);
    }

    /**
     * Returns the PotHoleResistance of the Taxi
     * @return The PotHoleResistance of the Taxi
     */
    public int getPotholeResistance()
    {
        return this.pothole_resistance;
    }

    /**
     * Sets the PotHoleResistance of the Taxi
     * @param resistance
     */
    public void setPotHoleResistance(int resistance)
    {
        this.pothole_resistance = resistance;
    }

    /**
     * Returns the current {@code ETaxiPositions} of the taxi.
     */
    public ETaxiPositions getPosition()
    {
        return myPosition;
    }

    /**
     * Adds cash to the Taxi's wallet.
     * @param cash The cash to add to the wallet.
     */
    public void addCash(double cash)
    {
        wallet+= cash;
        career_passengers++;
    }

    /**
     * Returns the career total passengers
     * @return the number of total passengers collected in this save.
     */
    public int getCareerPassengers()
    {
        return this.career_passengers;
    }

    /**
     * Returns the wallet of the taxi
     * @return The wallet of the taxi.
     */
    public double getWallet()
    {
        return this.wallet;
    }
    /**
     * Acceptor for the visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        // Nothing to do here.
    }

    /**
     * Swaps the images of the taxi, used to make it look like the taxi's wheels are moving.
     */
    public void swapImage()
    {
        if (isPrime)
        {
            this.setImage(secondImage);
            isPrime = false;
        } else
        {
            this.setImage(primeImage);
            isPrime = true;
        }
    }


    /**
     * Scales the taxi based on the given {@code ETaxiPositions}
     * @param position The {@code ETaxiPositions} to scale the taxi to.
     */
    public void scale(ETaxiPositions position)
    {
        myPosition = position;
        this.setY(myPosition.getLocation());
        this.myImageView.setFitHeight(myPosition.getHeightScale());
        this.myBound.setWidth(this.myImageView.getFitHeight()*1.5);
        this.myBound.setY(this.myBound.getY() + 30);
        this.myBound.setHeight(this.myImageView.getFitHeight() * 0.5);

        // This is being done as the bound box of the taxi must be dynamic.
        // This helps eliminate invalid collisons with sprites
        if (myPosition == ETaxiPositions.FOURTH_LANE_TAXI)
        {
            this.myBound.setY(this.myBound.getY() + 60);
            this.myBound.setHeight(this.myBound.getHeight() - 40);
        };
        if (myPosition == ETaxiPositions.THIRD_LANE_TAXI)
        {
            this.myBound.setY(this.myBound.getY() + 50);
            this.myBound.setHeight(this.myBound.getHeight() - 35);
        };
        if (myPosition == ETaxiPositions.SECOND_LANE_TAXI)
        {
            this.myBound.setY(this.myBound.getY() + 40);
            this.myBound.setHeight(this.myBound.getHeight() - 30);
        };
        if (myPosition == ETaxiPositions.FIRST_LANE_TAXI)
        {
            this.myBound.setY(this.myBound.getY() - 20);
        };
        }





        /**
         * Setting the career passengers of the taxi.
         * @param career_passengers count of passengers
         */
    public void setCareerPassengers(int career_passengers) {
        this.career_passengers = career_passengers;
    }

    /**
     * Sets the wallet of the taxi
     * @param d wallet to set (amount)
     */
    public void setWallet(double d) {
        this.wallet = d;
    }

    

    
}