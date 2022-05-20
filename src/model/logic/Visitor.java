package model.logic;

public interface Visitor {
    public void visit(Taxi taxi);
    public void visit(Passenger passenger);
    
}
