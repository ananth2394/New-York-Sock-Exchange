package exchange.g3;

import exchange.sim.Sock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class SockCollection {

    ArrayList<Sock> collection;
    int embarrassment;


    public SockCollection() {
        collection = new ArrayList<Sock>();
        embarrassment = 0;
    }

    public SockCollection(ArrayList<Sock> collection) {
        this.collection = collection;
        embarrassment = 0;
    }


    public SockCollection(Sock[] socks) {
        this.collection = new ArrayList<Sock>(Arrays.asList(socks));
        embarrassment = 0;
    }

    public void addSock(Sock newSock) {
        collection.add(newSock);
    }


    public void removeSock(Sock unwantedSock) {
        collection.remove(unwantedSock);
    }


    private ArrayList<ArrayList<Sock>> getSockBuckets() {
        ArrayList<ArrayList<Sock>> rgb = new ArrayList<>();

        char[] colors = new char[]{'R', 'G', 'B'};

        for (int i = 0; i < 3; ++i) {
            rgb.add(sortSockBucket(getSocksOfAColor(colors[i])));
        }

        return rgb;
    }

    private ArrayList<Sock> sortSockBucket(ArrayList<Sock> socks) {
        Collections.sort(socks, new Comparator<Sock>() {
            @Override
            public int compare(Sock sock1, Sock sock2) {
                // Weird messing sorting algorithm because I am
                //  unable to find a way to use the bucket color
                //  here directly. Performing reflection for each iteration
                //  was an over-kill.
                if (sock1.R >= sock1.G && sock1.R >= sock1.B)
                    return sock2.R - sock1.R;

                if (sock1.G > sock1.R && sock1.G >= sock1.B)
                    return sock2.G - sock1.G;

                return sock2.B - sock1.B;
            }
        });

        return socks;
    }

    private ArrayList<Sock> getSocksOfAColor(char color) {
        ArrayList<Sock> socks = new ArrayList<>();

        for (Sock s : collection) {
            switch (color) {
                case 'R':
                    if (s.R >= s.B && s.R >= s.G) {
                        socks.add(s);
                    }

                    break;

                case 'G':
                    if (s.G > s.R && s.G >= s.B) {
                        socks.add(s);
                    }

                    break;

                case 'B':
                    if (s.B > s.R && s.B > s.G) {
                        socks.add(s);
                    }

                    break;

                default:
                    System.err.println("Unknown color!");
            }
        }

        return socks;
    }

    // Change the order of socks in your collection using some algorithm.
    private void preprocessSockCollection() {
        /*ArrayList<ArrayList<Sock>> rgb = getSockBuckets();
        collection.clear();

        for (ArrayList<Sock> bucket : rgb) {
            collection.addAll(bucket);
        }*/
        ArrayList<Sock> processedSocks = new ArrayList<Sock>();
        while (!(collection.isEmpty())) {
            Sock s = collection.get(0);
            collection.remove(s);
            double smallest_dist = 442.0; //max distance socks can be
            Sock bestSock = null;
            for (Sock s2 : collection) {
                double d = s.distance(s2);
                if (d < smallest_dist) {
                    smallest_dist = d;
                    bestSock = s2;
                }
            }

            processedSocks.add(s);
            processedSocks.add(bestSock);
            collection.remove(bestSock);
        }
        collection.addAll(processedSocks);
    }

    private int[] getWorstPairingSockIds() {
        int w1 = -1;
        int w2 = -1;

        double maxDist = -1.0;

        for (int i = 0; i < collection.size(); i += 2) {
            Sock sock1 = collection.get(i);
            Sock sock2 = collection.get(i + 1);

            if (sock1.distance(sock2) > maxDist) {
                w1 = i;
                w2 = i + 1;
                maxDist = sock1.distance(sock2);
            }
        }

        return new int[]{w1, w2};
    }

    public void sortSocks() {
        Collections.sort(collection, new Comparator<Sock>() {
            @Override
            public int compare(Sock sock1, Sock sock2) {
                //Descending Order
                return sock2.hashCode() - sock1.hashCode();
            }
        });
    }


    public void computeEmbarrassment() {
        preprocessSockCollection();
        //sortSocks();

        if (collection.size() % 2 != 0) {
            embarrassment = -1;
            return;
        }

        int result = 0;
        for (int i = 0; i < collection.size(); i += 2) {
            Sock sock1 = collection.get(i);
            Sock sock2 = collection.get(i + 1);

            result += sock1.distance(sock2);
        }

        this.embarrassment = result;
    }

    public int getEmbarrassment() {
        computeEmbarrassment();
        return this.embarrassment;
    }

    public int[] getWorstPairIds() {
        computeEmbarrassment();

        if (collection.size() < 2)
            return new int[]{-1, -1};

        return getWorstPairingSockIds();
    }

    public Sock getSock(int id) {
        return collection.get(id);
    }

    public void putSock(int id, Sock s) {
        collection.remove(id);
        collection.add(s);
    }

    public ArrayList<Sock> getCollection() {
        return collection;
    }
}
