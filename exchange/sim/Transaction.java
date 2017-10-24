package exchange.sim;

public class Transaction {
    private int firstID, firstRank;
    private int secondID, secondRank;
    private Sock firstSock, secondSock;

    public Transaction(int firstID, int firstRank, int secondID, int secondRank, Sock firstSock, Sock secondSock) {
        this.firstID = firstID;
        this.firstRank = firstRank;
        this.secondID = secondID;
        this.secondRank = secondRank;
        this.firstSock = new Sock(firstSock);
        this.secondSock = new Sock(secondSock);
    }

    public int getFirstID() {
        return firstID;
    }

    public int getFirstRank() {
        return firstRank;
    }

    public int getSecondID() {
        return secondID;
    }

    public int getSecondRank() {
        return secondRank;
    }

    public Sock getFirstSock() {
        return new Sock(firstSock);
    }

    public Sock getSecondSock() {
        return new Sock(secondSock);
    }

    @Override
    public String toString() {
        return "Transaction[" + firstID + "(" + firstRank + "): " + firstSock + ", " + secondID + "(" + secondRank + "): " + secondSock + "]";
    }
}
