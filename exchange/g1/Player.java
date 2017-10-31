package exchange.g1;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import java.util.*;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    public final boolean USE_ABS_THRESHOLD = false;
    public final double ABS_THRESHOLD_FRAC = 0.8;
    public double threshold;
    public ArrayList<Sock> socks;
    public ArrayList<Pair> settledPairs;
    public ArrayList<Pair> pendingPairs;
    public int offerIndex;
    public boolean tradeCompleted;
    public int timesPairOffered;
    private int myFirstOffer, mySecondOffer, id, n, t;
    private int myFirstRequestID, myFirstRequestRank, mySecondRequestID, mySecondRequestRank;
    private List<Request> lastRequests;
    private Sock lastRequestSock1, lastRequestSock2;
    private Pair pairToOffer;

    public void repair() {
        Sock[] socks = this.socks.toArray(new Sock[2 * this.n]);
        int[] match = new Blossom(getCostMatrix(socks), true).maxWeightMatching();
        ArrayList<Pair> result = new ArrayList<Pair>();
        for (int i = 0; i < match.length; i++) {
            if (match[i] < i) continue;
            Pair p = new Pair(socks[i], socks[match[i]]);
            result.add(p);
        }
        this.settledPairs = result;
        this.pendingPairs.clear();
    }

    private void adjustThreshold() {
        this.chooseNewThreshold();
        for (Pair p : this.settledPairs) {
            if (p.first.distance(p.second) >= threshold) {
                this.pendingPairs.add(p);
            } else if (!USE_ABS_THRESHOLD) {
                break;
            }
        }
        this.settledPairs.removeAll(this.pendingPairs);
    }

    private void chooseNewThreshold() {
        if (USE_ABS_THRESHOLD) {
            this.threshold = this.threshold * ABS_THRESHOLD_FRAC;
        } else {
            Comparator<Pair> comp = (Pair a, Pair b) -> {
                return (new Double(b.first.distance(b.second))).compareTo(a.first.distance(a.second));
            };
            Collections.sort(this.settledPairs, comp);
            Pair partitionPair = this.settledPairs.get(n / 5);
            this.threshold = partitionPair.first.distance(partitionPair.second);
        }
    }

    public Sock[] getSockArray() {
        ArrayList<Sock> ret = new ArrayList<Sock>(2 * this.n);
        for (Pair p : settledPairs) {
            ret.add(p.first);
            ret.add(p.second);
        }
        for (Pair p : pendingPairs) {
            ret.add(p.first);
            ret.add(p.second);
        }
        Sock[] sockArray = new Sock[ret.size()];
        return ret.toArray(sockArray);
    }

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.n = n;
        this.t = t;
        this.socks = new ArrayList<Sock>(socks);
        this.settledPairs = new ArrayList<>();
        this.pendingPairs = new ArrayList<>();
        for (int i = 0; i < socks.size() - 1; i += 2) {
            this.settledPairs.add(new Pair(socks.get(i), socks.get(i + 1)));
        }
        this.repair();
        if (USE_ABS_THRESHOLD) {
            this.threshold = 60.0;
        }
        this.adjustThreshold();
        this.myFirstOffer = 0;
        this.mySecondOffer = 0;

        this.offerIndex = 0;
        this.tradeCompleted = false;
        this.timesPairOffered = 0;
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {

        if (pendingPairs.size() == 0) {
            adjustThreshold();
            offerIndex = 0;
        }

        if (tradeCompleted == false) {
            if (timesPairOffered == 2) {
                offerIndex = (offerIndex + 1) % pendingPairs.size();
                timesPairOffered = 0;
            } else {
                this.lastRequests = lastRequests;
            }
        } else {
            timesPairOffered = 0;
            tradeCompleted = false;
        }

        pairToOffer = pendingPairs.get(offerIndex);
        if (timesPairOffered++ == 0)
            return new Offer(pairToOffer.first, pairToOffer.second);
        else
            return new Offer(pairToOffer.second, pairToOffer.first);
    }

    private Sock getMeanSock(Sock a, Sock b) {
        return new Sock((a.R + b.R) / 2, (a.G + b.G) / 2, (a.B + b.B) / 2);
    }

    private double getMinDistance(Sock s) {
        double minDistance = 1000;
        for (Pair p : pendingPairs) {
            minDistance = Math.min(minDistance, Math.min(s.distance(p.first), s.distance(p.second)));
        }
        for (Pair p : settledPairs) {
            minDistance = Math.min(minDistance, Math.min(s.distance(p.first), s.distance(p.second)));
        }
        return minDistance;
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
        /*
            offers.get(i)                   -       Player i's offer
            For each offer:
            offer.getSock(rank = 1, 2)      -       get rank's offer
            offer.getFirst()                -       equivalent to offer.getSock(1)
            offer.getSecond()               -       equivalent to offer.getSock(2)

            Remark: For Request object, rank ranges between 1 and 2
         */
        double minValSoFar = 1000;
        myFirstRequestID = -1;
        myFirstRequestRank = -1;
        mySecondRequestID = -1;
        mySecondRequestRank = -1;
        this.t--;

        if (timesPairOffered == 1) { // First time offering these socks
            for (int i = 0; i < offers.size(); ++i) {
                if (i == id) continue;
                for (int rank = 1; rank <= 2; ++rank) {
                    Sock s = offers.get(i).getSock(rank);
                    if (s != null) {
                        if (getMinDistance(s) <= minValSoFar) {
                            mySecondRequestID = myFirstRequestID;
                            mySecondRequestRank = myFirstRequestRank;
                            myFirstRequestID = i;
                            myFirstRequestRank = rank;
                            minValSoFar = getMinDistance(s);
                        }
                    }
                }
            }
            if (myFirstRequestID != -1) {
                lastRequestSock1 = offers.get(myFirstRequestID).getSock(myFirstRequestRank); // can be null
            }
            if (mySecondRequestID != -1) {
                lastRequestSock2 = offers.get(mySecondRequestID).getSock(mySecondRequestRank); // can be null
            }
            return new Request(myFirstRequestID, myFirstRequestRank, mySecondRequestID, mySecondRequestRank);
        } else { // Second time offering these socks
            List<Integer> playersInterestedInUs = new ArrayList<>();
            for (int i = 0; i < offers.size(); ++i) {
                if (lastRequests.get(i).getFirstID() == this.id || lastRequests.get(i).getSecondID() == this.id) {
                    playersInterestedInUs.add(i);
                }
            }
            for (int player : playersInterestedInUs) {
                for (int rank = 1; rank <= 2; ++rank) {
                    Sock s = offers.get(player).getSock(rank);
                    if (s != null) {
                        if (getMinDistance(s) <= minValSoFar) {
                            mySecondRequestID = myFirstRequestID;
                            mySecondRequestRank = myFirstRequestRank;
                            myFirstRequestID = player;
                            myFirstRequestRank = rank;
                            minValSoFar = getMinDistance(s);
                        }
                    }
                }
            }
            if (!(minValSoFar == this.pairToOffer.first.distance(this.pairToOffer.second) / 2)) {
                // means we have at least one request
                // Note in this version, it is possible that we only request for one sock
                // from players that are interested in us
                return new Request(myFirstRequestID, myFirstRequestRank, mySecondRequestID, mySecondRequestRank);
            } else {
                // do the same thing on all offers
                // but exclude requested ones
                int lastRequestFirstID = lastRequests.get(this.id).getFirstID();
                int lastRequestSecondID = lastRequests.get(this.id).getSecondID();
                for (int i = 0; i < offers.size(); ++i) {
                    if (i == id) continue;
                    for (int rank = 1; rank <= 2; ++rank) {
                        Sock s = offers.get(i).getSock(rank);
                        if (s != null) {
                            if (lastRequestFirstID == i && lastRequestSock1.equals(s)) {
                                continue;
                            }
                            if (lastRequestSecondID == i && lastRequestSock2.equals(s)) {
                                continue;
                            }
                            if (getMinDistance(s) <= minValSoFar) {
                                mySecondRequestID = myFirstRequestID;
                                mySecondRequestRank = myFirstRequestRank;
                                myFirstRequestID = i;
                                myFirstRequestRank = rank;
                                minValSoFar = getMinDistance(s);
                            }
                        }
                    }
                }
                return new Request(myFirstRequestID, myFirstRequestRank, mySecondRequestID, mySecondRequestRank);
            }
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
        Sock oldSock;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            oldSock = transaction.getFirstSock();
            newSock = transaction.getSecondSock();
        } else {
            oldSock = transaction.getSecondSock();
            newSock = transaction.getFirstSock();
        }
        socks.remove(oldSock);
        socks.add(newSock);
        repair();
        adjustThreshold();
        offerIndex = 0;
    }

    @Override
    public List<Sock> getSocks() {
        if (t == 0) {
            this.repair();
        }
        return Arrays.asList(this.getSockArray());
    }

    private float[][] getCostMatrix(Sock[] sockArray) {
        float[][] matrix = new float[2 * n * (2 * n - 1) / 2][3];
        int idx = 0;
        for (int i = 0; i < sockArray.length; i++) {
            for (int j = i + 1; j < sockArray.length; j++) {
                matrix[idx] = new float[]{i, j, (float) (-sockArray[i].distance(sockArray[j]))};
                idx++;
            }
        }
        return matrix;
    }

    // Embarrasment calculation for current list of sockets
    private double getTotalEmbarrassment(Sock[] list) {

        double result = 0;
        for (int i = 0; i < list.length; i += 2)
            result += list[i].distance(list[i + 1]);
        return result;
    }

    public class Pair {
        public Sock first;
        public Sock second;

        public Pair(Sock fst, Sock snd) {
            this.first = fst;
            this.second = snd;
        }
    }

    // Switch our first and second offered sock for another's player sock and returns
    // new embarrassment. If the first sock was switched for second rank offered
    // sock, the boolean hast to be true.

    // private double getBestEmbarrassment(List<Sock> originalPairing,
    //     double initialEmbarrassment, List<Offer> offers,
    //     int i, boolean isSecond)    {

    //     double bestEmbarrassmentSoFar = initialEmbarrassment;

    //     // Switch first sock with first or second on offer at player i to calculate new distance
    //     socks[myFirstOffer] = (isSecond == false) ? offers.get(i).getFirst() : offers.get(i).getSecond(); 

    //     // Get sock pairing    
    //     socks = (Sock[]) getSocks().toArray(new Sock[2 * n]);

    //     // Calculate new embarrassment
    //     double firstOfferExchEmb = getTotalEmbarrassment(socks);
    //     if(firstOfferExchEmb < bestEmbarrassmentSoFar) {
    //         myFirstRequest = i;
    //         rankFirstRequest = (isSecond == false) ? 1 : 2;
    //         bestEmbarrassmentSoFar = firstOfferExchEmb;
    //         mySecondRequest = -1;
    //     }

    //     for (int j = 0; j < offers.size(); j++) {
    //         if (j== id) continue;

    //         if ((j != i || isSecond == true) && offers.get(j).getFirst() != null)   {

    //             // Get original order
    //             socks = (Sock[]) originalPairing.toArray(new Sock[2 * n]);
    //             // Switch first sock with first on offer at player i to calculate new distance
    //             socks[myFirstOffer] = (isSecond == false) ? offers.get(i).getFirst() : offers.get(i).getSecond();   
    //             // Switch second sock to calculate new distance
    //             socks[mySecondOffer] = offers.get(j).getFirst();
    //             // Calculate new embarrassment                
    //             socks = (Sock[]) getSocks().toArray(new Sock[2 * n]);

    //             // Calculate new embarrassment
    //             double secondOfferExchEmb = getTotalEmbarrassment(socks);
    //             if(secondOfferExchEmb < bestEmbarrassmentSoFar) {
    //                 myFirstRequest = i;
    //                 rankFirstRequest = (isSecond == false) ? 1 : 2;
    //                 mySecondRequest = j;
    //                 rankSecondRequest = 1;
    //                 bestEmbarrassmentSoFar = secondOfferExchEmb;
    //             }
    //         }

    //         if((j != i || isSecond == false) && offers.get(j).getSecond() != null)   {

    //             // Get original order
    //             socks = (Sock[]) originalPairing.toArray(new Sock[2 * n]);
    //             // Switch first sock with first on offer at player i to calculate new distance
    //             socks[myFirstOffer] = (isSecond == false) ? offers.get(i).getFirst() : offers.get(i).getSecond();   
    //             // Switch second sock to calculate new distance
    //             socks[mySecondOffer] = offers.get(j).getSecond();
    //             // Calculate new embarrassment                
    //             socks = (Sock[]) getSocks().toArray(new Sock[2 * n]);

    //             // Calculate new embarrassment
    //             double secondOfferExchEmb = getTotalEmbarrassment(socks);
    //             if(secondOfferExchEmb < bestEmbarrassmentSoFar) {
    //                 myFirstRequest = i;
    //                 rankFirstRequest = (isSecond == false) ? 1 : 2;
    //                 mySecondRequest = j;
    //                 rankSecondRequest = 2;
    //                 bestEmbarrassmentSoFar = secondOfferExchEmb;
    //             }
    //         }
    //     }                    

    //     // Leave sock in the initial order
    //     socks = (Sock[]) originalPairing.toArray(new Sock[2 * n]);

    //     return bestEmbarrassmentSoFar;
    // }    

}
