package exchange.g3;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import java.util.ArrayList;
import java.util.List;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id;
    private Sock[] socks;
    private SockCollection socksCollection;
    private int turn = -1;

    private RoundCollection rounds;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.socks = socks.toArray(new Sock[2 * n]);
        this.socksCollection = new SockCollection(this.socks);
        this.rounds = new RoundCollection();
    }


    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
            lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */

        // Tracks the current turn number.
        this.turn += 1;

        rounds.putTransactionInfo(lastRequests, lastTransactions);

        int[] worstPairIds = this.socksCollection.getWorstPairIds();
        this.id1 = worstPairIds[0];
        this.id2 = worstPairIds[1];
        return new Offer(
                this.socksCollection.getSock(this.id1),
                this.socksCollection.getSock(this.id2));
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
        /*
			offers.get(i)			-		Player i's offer
			For each offer:
			offer.getSock(rank = 1, 2)		-		get rank's offer
			offer.getFirst()				-		equivalent to offer.getSock(1)
			offer.getSecond()				-		equivalent to offer.getSock(2)

			Remark: For Request object, rank ranges between 1 and 2
		 */

        rounds.putOfferInfo(offers);

        List<Integer> availableOffers = new ArrayList<>();
        for (int i = 0; i < offers.size(); ++i) {
            if (i == id) continue;

            // Encoding the offer information into integer: id * 2 + rank - 1
            if (offers.get(i).getFirst() != null)
                availableOffers.add(i * 2);
            if (offers.get(i).getSecond() != null)
                availableOffers.add(i * 2 + 1);
        }

        int test = random.nextInt(3);
        if (test == 0 || availableOffers.size() == 0) {
            // In Request object, id == -1 means no request.
            return new Request(-1, -1, -1, -1);
        } else if (test == 1 || availableOffers.size() == 1) {
            // Making random requests
            int k = availableOffers.get(random.nextInt(availableOffers.size()));
            return new Request(k / 2, k % 2 + 1, -1, -1);
        } else {
            int k1 = availableOffers.get(random.nextInt(availableOffers.size()));
            int k2 = availableOffers.get(random.nextInt(availableOffers.size()));
            while (k1 == k2)
                k2 = availableOffers.get(random.nextInt(availableOffers.size()));
            return new Request(k1 / 2, k1 % 2 + 1, k2 / 2, k2 % 2 + 1);
        }
    }

    @Override
    public void completeTransaction(Transaction transaction) {
        /*
            transaction.getFirstID()        -       first player ID of the transaction
            transaction.getSecondID()       -       Similar as above
            transaction.getFirstRank()      -       Rank of the socks for first player
            transaction.getSecondRank()     -       Similar as above
            transaction.getFirstSock()      -       Sock offered by the first player
            transaction.getSecondSock()     -       Similar as above

            Remark: rank ranges between 1 and 2
         */
        int rank;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
        } else {
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        if (rank == 1) socksCollection.putSock(id1, newSock);
        else socksCollection.putSock(id2, newSock);
    }

    @Override
    public List<Sock> getSocks() {
        return socksCollection.getCollection();
    }
}

// Maintains information of all the rounds.
// - Need to implement a KB for gathering info.
class RoundCollection {
    private List<Round> roundsInfo;
    private int turn = -1;

    public RoundCollection() {
        roundsInfo = new ArrayList<>();
    }

    public void putOfferInfo(List<Offer> offers) {
        turn += 1;

        roundsInfo.add(new Round(offers));
    }

    public void putTransactionInfo(
            List<Request> requests,
            List<Transaction> transactions) {

        // For all turn# >= 0 we have request information.
        if (turn >= 0) {
            roundsInfo.get(turn).requests = requests;
            roundsInfo.get(turn).transactions = transactions;
        }
    }

    public Round getRoundInfo(int turn) {
        return roundsInfo.get(turn);
    }
}

// Stores offers, requests, and transactions for a round.
class Round {
    public List<Offer> offers;
    public List<Request> requests;
    public List<Transaction> transactions;

    public Round() {
    }

    public Round(List<Offer> offers) {
        this.offers = offers;
    }
}
