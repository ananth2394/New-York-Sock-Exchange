package exchange.sim;

public class Request {
    private int firstID, firstRank;
    private int secondID, secondRank;

    public Request(int firstID, int firstRank, int secondID, int secondRank) {
        this.firstID = firstID;
        this.firstRank = firstRank;
        this.secondID = secondID;
        this.secondRank = secondRank;
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

    @Override
    public String toString() {
        return "(" + firstID + ", " + firstRank + "), (" + secondID + ", " + secondRank + ")";
    }
}
