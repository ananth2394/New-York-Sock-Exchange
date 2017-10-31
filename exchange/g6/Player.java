package exchange.g6;

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
    private int id1, id2, id, n;
    private Sock[] socks;
    private Sock median;
    private double max_dist1;//for stage 0 
    private double max_dist2;//for stage 0
    private int no_txn = 0; //count the number times with no txns 
    private int pair_offer = 0; //identify stages: pair_offer = 1 for stage1

    private double dist = 70;
    private ArrayList<Sock> median_list;

    private int num_cluster;
    private boolean txn = true;

    private int t;
    private int t_counter;


    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.n = n;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.median = new Sock(128, 128, 128);
        this.median_list = new ArrayList<Sock>();
        this.max_dist1 = -1;
        this.max_dist2 = -1;
        this.t = t;
        this.t_counter = 0;
        this.num_cluster = 4;
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
            lastRequests.get(i)     -       Player i's request last round
            lastTransactions        -       All completed transactions last round.
         */
        //increment no_txn
        //this.t -= 1;
        this.t_counter += 1;
        this.no_txn += 1;
        //if stage 0
        if (this.no_txn <= 4) {
            //Use Blossom method to pair up socks
            if (this.txn == true) {
                this.median_list.clear();
                socks = pairSocks(socks);
                this.txn = false;


                ArrayList<Sock> arrayList_socks = new ArrayList<Sock>(Arrays.asList(socks));
                DBSCANClusterer clusterer = new DBSCANClusterer(arrayList_socks, 2, this.dist);
                ArrayList<ArrayList<Sock>> result = clusterer.performClustering();
                while (result.size() < this.num_cluster) {
                    this.dist = this.dist - 5;
                    clusterer = new DBSCANClusterer(arrayList_socks, 2, this.dist);
                    result = clusterer.performClustering();
                }

                //System.out.println("clusters: " + result.size());
                HashMap<Sock, Double> offerCandidate = new HashMap<Sock, Double>();
                double furthest_dist = -1;
                for (int i = 0; i < result.size(); ++i) {
                    Sock tmp_median = getMedianSock(result.get(i));
                    median_list.add(tmp_median);
                    double candidate_dist1 = -1;
                    double candidate_dist2 = -1;
                    Sock candidate1 = result.get(i).get(0);
                    Sock candidate2 = result.get(i).get(1);
                    for (int j = 0; j < result.get(i).size(); j++) {
                        if (tmp_median.distance(result.get(i).get(j)) > candidate_dist1) {
                            candidate_dist2 = candidate_dist1;
                            candidate_dist1 = tmp_median.distance(result.get(i).get(j));
                            candidate2 = candidate1;
                            candidate1 = result.get(i).get(j);
                        } else if (tmp_median.distance(result.get(i).get(j)) > candidate_dist2) {
                            candidate_dist2 = tmp_median.distance(result.get(i).get(j));
                            candidate2 = result.get(i).get(j);
                        }
                    }
                    offerCandidate.put(candidate1, candidate_dist1);
                    offerCandidate.put(candidate2, candidate_dist2);
                    // for(int p = 0; p < result.get(i).size()-1; p++){
                    //     for (int q = p; q < result.get(i).size(); q++){
                    //         if(result.get(i).get(p).distance(result.get(i).get(q)) > furthest_dist){
                    //             furthest_dist = result.get(i).get(p).distance(result.get(i).get(q));
                    //         }
                    //     }
                    // }

                }

                this.max_dist1 = -1;
                this.max_dist2 = -1;
                int maxid1 = -1;
                int maxid2 = -1;
                //System.out.println(offerCandidate);
                for (Sock s : offerCandidate.keySet()) {
                    if (offerCandidate.get(s) > this.max_dist1) {
                        this.max_dist2 = this.max_dist1;
                        this.max_dist1 = offerCandidate.get(s);
                        maxid2 = maxid1;
                        maxid1 = arrayList_socks.indexOf(s);
                    } else if (offerCandidate.get(s) > this.max_dist2) {
                        this.max_dist2 = offerCandidate.get(s);
                        maxid2 = arrayList_socks.indexOf(s);
                    }
                }
                this.dist = this.max_dist1 * 2;

                this.id1 = maxid1;
                this.id2 = maxid2;
            }//if txn == true
            this.pair_offer = 0; //this is stage 0, so pair_offer == 0;

            //return the offer with two farthest socks
            return new Offer(socks[this.id1], socks[this.id2]);
        } else {//this is stage 1
            //get the pairs of socks backwards from the socks list.
            //when no_txn = 6,7,8,9 we have the last pair
            //when no_txn = 10,11,12,13,14 we have the second last pair.
            this.id1 = socks.length - ((this.no_txn - 3) / 3) * 2 - 1;
            this.id2 = socks.length - ((this.no_txn - 3) / 3) * 2 - 2;
            if (this.id1 <= 0) {
                this.id1 = socks.length - 1;
                this.id2 = socks.length - 2;
                this.no_txn = 4;
            }
            //if the pair is good enough, pick other pairs
            while (this.t_counter < 200 && socks[this.id1].distance(socks[this.id2]) < getEmbarassment(socks) / n) {
                this.no_txn += 3;
                this.id1 = socks.length - ((this.no_txn - 3) / 3) * 2 - 1;
                this.id2 = socks.length - ((this.no_txn - 3) / 3) * 2 - 2;
                if (this.id1 <= 0) {
                    this.id1 = socks.length - 1;
                    this.id2 = socks.length - 2;
                    this.no_txn = 4;
                }
            }

            this.pair_offer = 1; // this is stage 1
            return new Offer(socks[this.id1], socks[this.id2]);
        }
        //return null;
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
        /*
            offers.get(i)           -       Player i's offer
            For each offer:
            offer.getSock(rank = 1, 2)      -       get rank's offer
            offer.getFirst()                -       equivalent to offer.getSock(1)
            offer.getSecond()               -       equivalent to offer.getSock(2)

            Remark: For Request object, rank ranges between 1 and 2
         */

        int c = 0;
        double min_dist1 = 450;
        double min_dist2 = 450;
        int min1 = -1;
        int min2 = -1;
        int rank1 = -1;
        int rank2 = -1;
        if (this.pair_offer == 0) { //if stage 0
            for (int j = 0; j < this.median_list.size(); ++j) {
                for (int i = 0; i < offers.size(); i++) {
                    if (i == id) continue; //skip the current player's offer
                    if (offers.get(i).getFirst() != null) {
                        if (offers.get(i).getFirst().distance(this.median_list.get(j)) < min_dist1) {
                            min_dist2 = min_dist1;
                            min_dist1 = offers.get(i).getFirst().distance(this.median_list.get(j));
                            min2 = min1;
                            min1 = i;
                            rank2 = rank1;
                            rank1 = 1;
                        } else if (offers.get(i).getFirst().distance(this.median_list.get(j)) < min_dist2) {
                            min_dist2 = offers.get(i).getFirst().distance(this.median_list.get(j));
                            min2 = i;
                            rank2 = 1;
                        }
                    }
                    if (offers.get(i).getSecond() != null) {
                        if (offers.get(i).getSecond().distance(this.median_list.get(j)) < min_dist1) {
                            min_dist2 = min_dist1;
                            min_dist1 = offers.get(i).getSecond().distance(this.median_list.get(j));
                            min2 = min1;
                            min1 = i;
                            rank2 = rank1;
                            rank1 = 2;
                        } else if (offers.get(i).getSecond().distance(this.median_list.get(j)) < min_dist2) {
                            min_dist2 = offers.get(i).getSecond().distance(this.median_list.get(j));
                            min2 = i;
                            rank2 = 2;
                        }
                    }
                }
            }
            if (min_dist1 < this.max_dist2) {
                c += 1;
            }
            if (min_dist2 < this.max_dist2) {
                c += 1;
            }

        } else {
            for (int i = 0; i < offers.size(); i++) {
                if (i == id) continue;
                double firstEmbarassmentOne = -1;
                double secondEmbarassmentOne = -1;
                if (offers.get(i).getFirst() != null) {
                    firstEmbarassmentOne = updatedEmbarassment(offers.get(i).getFirst(), id1);
                }
                if (offers.get(i).getSecond() != null) {
                    secondEmbarassmentOne = updatedEmbarassment(offers.get(i).getSecond(), id1);
                }
                //double firstEmbarassmentTwo = updatedEmbarassment(offers.get(i).getFirst(), id2);
                //double secondEmbarassmentTwo = updatedEmbarassment(offers.get(i).getSecond(), id2);


                double curMin1 = getEmbarassment(socks);
                double curMin2 = getEmbarassment(socks);
                if (offers.get(i).getFirst() != null) {
                    if (curMin1 > firstEmbarassmentOne) {
                        curMin2 = curMin1;
                        curMin1 = firstEmbarassmentOne;
                        min2 = min1;
                        min1 = i;
                        rank2 = rank1;
                        rank1 = 1;
                        c++;
                    } else if (curMin2 > firstEmbarassmentOne) {
                        min_dist2 = firstEmbarassmentOne;
                        min2 = i;
                        rank2 = 1;
                        c++;
                    }
                }
                if (offers.get(i).getSecond() != null) {
                    if (curMin1 > secondEmbarassmentOne) {
                        curMin2 = curMin1;
                        curMin1 = secondEmbarassmentOne;
                        min2 = min1;
                        min1 = i;
                        rank2 = rank1;
                        rank1 = 1;
                        c++;
                    } else if (curMin2 > secondEmbarassmentOne) {
                        curMin2 = secondEmbarassmentOne;
                        min2 = i;
                        rank2 = 1;
                        c++;
                    }
                }
            }
        }

        //form the request
        if (c >= 2) {
            return new Request(min1, rank1, min2, rank2);
        } else if (c == 1) {
            if (min1 == -1) {
                return new Request(min2, rank2, -1, -1);
            } else {
                return new Request(min1, rank1, -1, -1);
            }
        } else {
            return new Request(-1, -1, -1, -1);
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


        //a txn happens, set no_txn to 0
        this.no_txn = 0;
        this.txn = true;
        int rank;
        Sock newSock;
        Sock oldSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
            oldSock = transaction.getFirstSock();
        } else {
            oldSock = transaction.getSecondSock();
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        if (rank == 1) socks[id1] = newSock;
        else socks[id2] = newSock;

    }

    @Override
    public List<Sock> getSocks() {
        //return result;
        // for(int i = 0; i < socks.length; i++) {
        //     System.out.println(socks[i]);
        // }
        return Arrays.asList(socks);
    }

    public Sock[] pairSocks(Sock[] s) {
        int[] match = new Blossom(getCostMatrix(s), true).maxWeightMatching();
        List<Pair> pairs = new LinkedList<Pair>();
        List<Sock> result = new ArrayList<Sock>();
        for (int i = 0; i < match.length; i++) {
            if (match[i] < i) continue;
            pairs.add(new Pair(s[i], s[match[i]]));
        }

        Pair[] pairArr = pairs.toArray(new Pair[n]);

        Arrays.sort(pairArr);
        for (int i = 0; i < n; i++) {
            result.add(pairArr[i].u);
            result.add(pairArr[i].v);
        }

        return result.toArray(new Sock[2 * n]);
    }

    public Sock getMedianSock(ArrayList<Sock> input) {
        List<Integer> sock_R = new ArrayList<Integer>();
        List<Integer> sock_G = new ArrayList<Integer>();
        List<Integer> sock_B = new ArrayList<Integer>();
        for (Sock s : input) {
            sock_R.add(s.R);
            sock_G.add(s.G);
            sock_B.add(s.B);
        }

        Collections.sort(sock_R);
        Collections.sort(sock_G);
        Collections.sort(sock_B);

        int mid_R = sock_R.get(input.size() / 2);
        int mid_G = sock_G.get(input.size() / 2);
        int mid_B = sock_B.get(input.size() / 2);

        return new Sock(mid_R, mid_G, mid_B);
    }

    private float[][] getCostMatrix(Sock[] s) {
        float[][] matrix = new float[s.length * (s.length - 1) / 2][3];
        int idx = 0;
        for (int i = 0; i < s.length; i++) {
            for (int j = i + 1; j < s.length; j++) {
                matrix[idx] = new float[]{i, j, (float) (-s[i].distance(s[j]))};
                idx++;
            }
        }
        return matrix;
    }

    private double updatedEmbarassment(Sock newSock, int replaceIndex) {
        Sock[] copy = socks.clone();
        copy[replaceIndex] = newSock;
        copy = pairSocks(copy);
        return getEmbarassment(copy);
    }

    private double getEmbarassment(Sock[] s) {
        double result = 0;
        for (int i = 0; i < s.length; i += 2) {
            result += s[i].distance(s[i + 1]);
        }

        return result;
    }

    public void pairSocksGreedy() {
        List<Sock> next = new ArrayList<Sock>();
        for (int i = 0; i < socks.length; ++i) {
            next.add(new Sock(socks[i].R, socks[i].G, socks[i].B));
        }

        int c = 0;
        ArrayList<Point> tmp_list = new ArrayList<Point>();
        for (int i = 0; i < next.size(); ++i) {
            tmp_list.add(new Point(next.get(i).R, next.get(i).G, next.get(i).B));
        }

        while (c < socks.length - 1) {
            ClosestPair closep = new ClosestPair(tmp_list);
            closep.SolveByDivideAndConquer();
            Sock s1 = new Sock(closep.p1.x, closep.p1.y, closep.p1.z);
            // System.out.println("C is: " + c);
            socks[c] = s1;
            c += 1;
            Sock s2 = new Sock(closep.p2.x, closep.p2.y, closep.p2.z);
            socks[c] = s2;
            c += 1;
            ArrayList<Point> tmp_1 = new ArrayList<Point>();
            for (int i = 0; i < tmp_list.size(); ++i) {
                if (tmp_list.get(i).x == closep.p1.x && tmp_list.get(i).y == closep.p1.y && tmp_list.get(i).z == closep.p1.z)
                    continue;
                if (tmp_list.get(i).x == closep.p2.x && tmp_list.get(i).y == closep.p2.y && tmp_list.get(i).z == closep.p2.z)
                    continue;
                tmp_1.add(new Point(tmp_list.get(i).x, tmp_list.get(i).y, tmp_list.get(i).z));
            }
            tmp_list = tmp_1;

        }
    }

}
