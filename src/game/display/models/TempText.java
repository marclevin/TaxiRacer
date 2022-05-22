package game.display.models;

import game.utility.EPassenger;
import javafx.scene.text.Text;

public class TempText extends Text {

    private int hold = 120;
    private boolean runnable = true;

    TempText(String text) {
        super(text);
    };


    public TempText(double x, double y, String text, EPassenger passenger) {
        super(x,y,text);
        if (passenger == EPassenger.PASSENGER_BOTTOM) {
            this.setY(passenger.getLocation() + 125);
        } else {
            this.setY(passenger.getLocation() - 25);
        }
    }

    public void tickDown()
    {
        this.hold--;
        if (runnable) {
        if (this.hold == 0)
        {
            runnable = false;
            
        }
    }
    }

    public int getHold()
    {
        return hold;
    };

    
}
