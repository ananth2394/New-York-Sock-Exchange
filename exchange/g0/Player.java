package exchange.g0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id;
    private Sock[] socks;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */
        int test = random.nextInt(3);
        if (test == 0) {
            // In Offer object, null means no sock is offered
            return new Offer(null, null);
        } else if (test == 1) {
            // Making random offer
            id1 = random.nextInt(socks.length);
            return new Offer(socks[id1], null);
        } else if (test == 2) {
            // Making random offer
            id1 = random.nextInt(socks.length);
            id2 = random.nextInt(socks.length);
            while (id1 == id2)
                id2 = random.nextInt(socks.length);
            return new Offer(socks[id1], socks[id2]);
        }
        return null;
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

		List<Integer> availableOffers = new ArrayList<>();
		for (int i = 0; i < offers.size(); ++ i) {
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
        if (rank == 1) socks[id1] = newSock;
        else socks[id2] = newSock;
    }

    @Override
    public List<Sock> getSocks() {
        return Arrays.asList(socks);
    }
}
