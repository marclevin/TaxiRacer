package game.logic;

import game.display.models.Passenger;
import game.display.models.Taxi;

public interface Visitor {
    public void visit(Taxi taxi);
    public void visit(Passenger passenger);
    
}
