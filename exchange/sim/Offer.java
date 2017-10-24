package exchange.sim;

public class Offer {
    private Sock first, second;

    public Offer(Sock first, Sock second) {
        if (first != null) this.first = new Sock(first);
        if (second != null) this.second = new Sock(second);
    }

    public Offer(Offer offer) {
        if (offer.getFirst() != null) this.first = new Sock(offer.getFirst());
        if (offer.getSecond() != null) this.second = new Sock(offer.getSecond());
    }

    public Sock getSock(int rank) {
        if (rank == 1) return this.first;
        else if (rank == 2) return this.second;
        else return null;
    }

    public Sock getFirst() {
        return this.first;
    }

    public Sock getSecond() {
        return this.second;
    }

    @Override
    public String toString() {
        return "[" + first + ", " + second + "]";
    }
}
