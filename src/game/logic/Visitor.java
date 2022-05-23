package game.logic;

import game.display.models.Passenger;
import game.display.models.Police;
import game.display.models.Pothole;
import game.display.models.Road;

/**
 * This is the interface for concrete visitors for the visitor design pattern.
 */
public interface Visitor {
    public void visit(Pothole pothole);
    public void visit(Passenger passenger);
    public void visit(Road road);
    public void visit(Police police);
    
}
