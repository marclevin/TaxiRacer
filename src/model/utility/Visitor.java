package model.utility;

import model.logic.Passenger;
import model.logic.Taxi;

public interface Visitor {
    public void visit(Taxi taxi);
    public void visit(Passenger passenger);
    
}
