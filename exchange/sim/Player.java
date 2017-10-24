package exchange.sim;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public abstract class Player {
    protected Random random = new Random();

    public Player() {}

    public abstract void init(int id, int n, int p, int t, List<Sock> socks);

    public abstract Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions);

    public abstract Request requestExchange(List<Offer> offers);

    public abstract void completeTransaction(Transaction transaction);

    public abstract List<Sock> getSocks();
}
