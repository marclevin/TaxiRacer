package game.logic;

/**
 * This interface implements the visitor pattern.
 */
public interface Acceptor {
    public void accept(Visitor visitor);
}
