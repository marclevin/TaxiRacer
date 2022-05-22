package game.logic;

import game.display.models.Passenger;
import game.display.models.Pothole;
import game.display.models.Road;

public interface Visitor {
    public void visit(Pothole pothole);
    public void visit(Passenger passenger);
    public void visit(Road road);
    
}
