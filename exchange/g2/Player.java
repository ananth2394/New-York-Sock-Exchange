package exchange.g2;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import java.util.*;

public class Player extends exchange.sim.Player {

    private final int K = 2;

    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id;
    private double id1DistanceToCentroid, id2DistanceToCentroid;

    private Sock[] socks;

    private int currentTurn;
    private int totalTurns;
    private int numPlayers;
    private int numSocks;

    // Declare also the centroids so that they can be reused between iterations.
    private AbstractEKmeans eKmeans;
    private Sock[] centroids;
    private SockDistanceFunction sockDistanceFunction;
    private SockCenterFunction sockCenterFunction;

    private int[][][] marketValue;
    private PriorityQueue<SockPair> rankedPairs;
    private List<Offer> lastOffers = null;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.totalTurns = t;
        this.currentTurn = t;
        this.numPlayers = p;
        this.numSocks = n * 2;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);

        // Initialize the centroids to random points (socks).
        this.centroids = new Sock[K];
        for (int i = 0; i < K; i++) {
            this.centroids[i] = new Sock(random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256));
        }

        // Initialize the functions for distance and centroid centering.
        sockDistanceFunction = new SockDistanceFunction();
        sockCenterFunction = new SockCenterFunction();

        this.marketValue = new int[8][8][8]; //Splitting into 8 equal sized rgb segments
        Comparator<SockPair> compareCentroidDistance = new Comparator<SockPair>() {
            public int compare(SockPair sp1, SockPair sp2) {
                return sp1.totalCentroidDistance - sp2.totalCentroidDistance > 0 ? 1 : -1;
            }
        };
        this.rankedPairs = new PriorityQueue<SockPair>(10, compareCentroidDistance);

//        System.out.println("Initial embarrassment for player " + id + ": " + getEmbarrasment());
        pairBlossom();
    }

    private void getSocksFarthestFromCentroid() {
        // Right now, we are only interested in the ids of the two socks with max distance from
        // their assigned centroids.
        computeClusters();

        double distance;
        double maxDistance = Double.NEGATIVE_INFINITY;
        int maxDistanceId = -1;
        double secondMaxDistance = Double.NEGATIVE_INFINITY;
        int secondMaxDistanceId = -1;

        int[] assignments = eKmeans.assignments;
        Sock assignedCentroidSock1 = null, assignedCentroidSock2 = null;
        for (int j = 0; j < this.socks.length; j += 2) {
            assignedCentroidSock1 = centroids[assignments[j]];
            assignedCentroidSock2 = centroids[assignments[j + 1]];
            double distance1 = assignedCentroidSock1.distance(this.socks[j]);
            double distance2 = assignedCentroidSock2.distance(this.socks[j + 1]);
            double curDistance = distance1 + distance2;
            rankedPairs.add(new SockPair(this.socks[j], this.socks[j + 1]));
            if (curDistance > maxDistance) {
                maxDistance = distance1;
                secondMaxDistance = distance2;
                secondMaxDistanceId = j + 1;
                maxDistanceId = j;
            }
        }
        this.id1 = maxDistanceId;
        this.id1DistanceToCentroid = maxDistance;
        this.id2 = secondMaxDistanceId;
        this.id2DistanceToCentroid = secondMaxDistance;
    }

    private void computeClusters() {
        this.eKmeans = new AbstractEKmeans<Sock, Sock>(
                this.centroids, this.socks, false,
                sockDistanceFunction, sockCenterFunction, null
        );
        eKmeans.run();
    }


    private double getEmbarrasment() {
        double result = 0;
        for (int i = 0; i < this.numSocks; i += 2) {
            result += this.socks[i].distance(this.socks[i + 1]);
        }
        return result;
    }

    public void pairBlossom() {
        int[] match = new Blossom(getCostMatrix(), true).maxWeightMatching();
        List<Sock> result = new ArrayList<Sock>();
        for (int i = 0; i < match.length; i++) {
            if (match[i] < i) continue;
            result.add(socks[i]);
            result.add(socks[match[i]]);
        }

        socks = (Sock[]) result.toArray(new Sock[socks.length]);
    }

    private float[][] getCostMatrix() {
        float[][] matrix = new float[numSocks * (numSocks - 1) / 2][3];
        int idx = 0;
        for (int i = 0; i < socks.length; i++) {
            for (int j = i + 1; j < socks.length; j++) {
                matrix[idx] = new float[]{i, j, (float) (-socks[i].distance(socks[j]))};
                idx++;
            }
        }
        return matrix;
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
            lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */
        for (Request request : lastRequests) {
            if (request == null) continue;
            if (request.getFirstID() >= 0 && request.getFirstRank() >= 0) {
                Sock first = lastOffers.get(request.getFirstID()).getSock(request.getFirstRank());
                marketValue[first.R / 32][first.G / 32][first.B / 32] += Math.pow(totalTurns - currentTurn, 2);
            }
            if (request.getSecondID() >= 0 && request.getSecondRank() >= 0) {
                Sock second = lastOffers.get(request.getSecondID()).getSock(request.getSecondRank());
                marketValue[second.R / 32][second.G / 32][second.B / 32] += Math.pow(totalTurns - currentTurn, 2);
            }
        }

        currentTurn--;
        pairBlossom();
        getSocksFarthestFromCentroid();

        SockPair maxMarketPair = rankedPairs.poll();
        int maxMarketValue = marketValue[maxMarketPair.s1.R / 32][maxMarketPair.s1.G / 32][maxMarketPair.s1.B / 32] +
                marketValue[maxMarketPair.s1.R / 32][maxMarketPair.s1.G / 32][maxMarketPair.s1.B / 32];

        for (int i = 0; i < 5; i++) {
            SockPair next = rankedPairs.poll();
            int nextMarketValue = marketValue[next.s1.R / 32][next.s1.G / 32][next.s1.B / 32] + marketValue[next.s2.R / 32][next.s2.G / 32][next.s2.B / 32];
            if (nextMarketValue > maxMarketValue) {
                maxMarketPair = next;
                maxMarketValue = nextMarketValue;
            }
        }
        id1 = getSocks().indexOf(maxMarketPair.s1);
        id2 = getSocks().indexOf(maxMarketPair.s2);
        rankedPairs.clear();

        return new Offer(maxMarketPair.s1, maxMarketPair.s2);
    }

    private double getMinDistanceToAnyCentroid(Sock s) {
        double minDistance = Double.POSITIVE_INFINITY;
        int minIndex = 0;
        for (int i = 0; i < this.centroids.length; ++i) {
            double distance = this.centroids[i].distance(s);
            if (distance < minDistance) {
                minDistance = distance;
                minIndex = i;
            }
        }
        return minDistance;
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
        for (Offer offer : offers) {
            Sock first = offer.getFirst();
            Sock second = offer.getSecond();
            marketValue[first.R / 32][first.G / 32][first.B / 32] -= Math.pow(totalTurns - currentTurn, 2);
            marketValue[second.R / 32][second.G / 32][second.B / 32] -= Math.pow(totalTurns - currentTurn, 2);
        }
        lastOffers = offers;

        double minDistance = this.id1DistanceToCentroid;
        int minDistanceOfferId = -1;
        int minDistanceOfferRank = -1;

        double secondMinDistance = this.id2DistanceToCentroid;
        int secondMinDistanceOfferId = -1;
        int secondMinDistanceOfferRank = -1;

        double distance;
        for (int i = 0; i < offers.size(); ++i) {
            if (i == id) continue; // Skip our own offer.
            for (int j = 1; j < 3; ++j) {
                Sock s = offers.get(i).getSock(j);
                if (s == null) continue;

                distance = getMinDistanceToAnyCentroid(s);
                if (distance < minDistance) {
                    secondMinDistance = minDistance;
                    secondMinDistanceOfferId = minDistanceOfferId;
                    secondMinDistanceOfferRank = minDistanceOfferRank;

                    minDistance = distance;
                    minDistanceOfferId = i;
                    minDistanceOfferRank = j;
                } else if (distance < secondMinDistance) {
                    secondMinDistance = distance;
                    secondMinDistanceOfferId = i;
                    secondMinDistanceOfferRank = j;
                }
            }
        }

        return new Request(minDistanceOfferId, minDistanceOfferRank, secondMinDistanceOfferId, secondMinDistanceOfferRank);
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
        Sock one = socks[id1];
        Sock two = socks[id2];
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
