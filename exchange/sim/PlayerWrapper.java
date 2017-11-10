package exchange.sim;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class PlayerWrapper {
    private Timer thread;
    private Player player;
    private String name;
    private int id, n;
    private long timeout, originalTimeout, seed;
    private Multiset<Sock> socks = new Multiset<Sock>();
    private Random random;
    private boolean illegal;

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public void setRte(boolean rte) {
        this.rte = rte;
    }

    private boolean timedOut;
    private boolean rte;

    public PlayerWrapper(Player player, int id, String name, int n, long timeout, long seed) {
        this.player = player;
        this.id = id;
        this.name = name;
        this.n = n;
        this.timeout = timeout;
        originalTimeout = timeout;
        this.illegal = false;
        this.timedOut = false;
        this.rte = false;
        this.seed = seed;
        this.random = new Random(seed);
        thread = new Timer();
    }


    public void init(int n, int p, int t) {
//        System.out.println("Generating player " + id + "'s socks");
        for (int i = 0; i < 2 * n; ++i) {
            Sock sock = new Sock(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            socks.add(sock);
//            System.out.println(sock + " " + socks.contains(sock));
        }
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> {
            player.init(id, n, p, t, socks.toList());
            return null;
        });
        try {
            thread.call_wait(timeout);
        } catch (TimeoutException e) {
            this.timedOut = true;
            System.err.println("Player " + name + "(" + id + ")" + " timed out.");
            return;
        } catch (Exception e) {
            this.rte = true;
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            System.err.println(errors.toString());
            return;
        }
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
    }

    public Offer makeOffer(List<Request> requests, List<Transaction> lastTransactions) {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> player.makeOffer(requests, lastTransactions));
        Offer ret;
        try {
            ret = thread.call_wait(timeout);
        } catch (TimeoutException e) {
            this.timedOut = true;
            System.err.println("Player " + name + "(" + id + ")" + " timed out.");
            return new Offer(null, null);
        } catch (Exception e) {
            this.rte = true;
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            System.err.println(errors.toString());
            return new Offer(null, null);
        }
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
        return ret;
    }


    public Request requestExchange(List<Offer> offers) {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> player.requestExchange(offers));
        Request ret;
        try {
            ret = thread.call_wait(timeout);
        } catch (TimeoutException e) {
            this.timedOut = true;
            System.err.println("Player " + name + "(" + id + ")" + " timed out.");
            return new Request(-1, -1, -1, -1);
        } catch (Exception e) {
            this.rte = true;
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            System.err.println(errors.toString());
            return new Request(-1, -1, -1, -1);
        }
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
        return ret;
    }


    public void completeTransaction(Transaction transaction) {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> {
            player.completeTransaction(transaction);
            return null;
        });
        try {
            thread.call_wait(timeout);
        } catch (TimeoutException e) {
            this.timedOut = true;
            System.err.println("Player " + name + "(" + id + ")" + " timed out.");
            return;
        } catch (Exception e) {
            this.rte = true;
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            System.err.println(errors.toString());
            return;
        }
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
    }

    public boolean owned(Sock sock) {
        return socks.contains(sock);
    }

    public void addSock(Sock sock) {
        socks.add(sock);
    }

    public void removeSock(Sock sock) {
        socks.remove(sock);
    }

    public long getTotalElapsedTime() {
        return originalTimeout - timeout;
    }

    public boolean isActive() {
        return !timedOut && !illegal && !rte;
    }

    public void setIllegal(boolean illegal) {
        this.illegal = illegal;
    }

    public String getName() {
        if (illegal && rte) return name + "(RTE & illegal)";
        if (!illegal && rte) return name + "(RTE)";
        if (illegal && timedOut) return name + "(timed out & illegal)";
        if (!illegal && timedOut) return name + "(timed out)";
        if (illegal && !timedOut && !rte) return name + "(illegal)";
        return name;
    }

    public double getTotalEmbarrassment() throws Exception {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> player.getSocks());
        List<Sock> list;
        try {
            list = thread.call_wait(timeout);
        } catch (TimeoutException e) {
            this.timedOut = true;
            throw new TimeoutException("Player " + name + "(" + id + ")" + " timed out.");
        } catch (Exception e) {
            this.rte = true;
            throw e;
        }
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
        //return ret;
        // List<Sock> list = player.getSocks();
        if (list.size() != 2 * n)
            return -1;
        for (Sock sock : list) {
            if (!socks.contains(sock)) {
                this.illegal = true;
                throw new IllegalArgumentException("Player " + name + "(" + id + ")" + " reports socks without ownship.");
            }
            socks.remove(sock);
        }
        for (Sock sock : list)
            socks.add(sock);

        double result = 0;
        for (int i = 0; i < list.size(); i += 2)
            result += list.get(i).distance(list.get(i + 1));
        return result;
    }

    public double finalCall() throws Exception {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> player.getSocks());
        List<Sock> list;
        if (timedOut) timeout = 0;
        try {
            list = thread.call_wait(timeout + 10000);
        } catch (Exception e) {
            throw e;
        }
        //return ret;
        // List<Sock> list = player.getSocks();
        if (list.size() != 2 * n)
            return -1;
        for (Sock sock : list) {
            if (!socks.contains(sock)) {
                this.illegal = true;
                throw new IllegalArgumentException("Player " + name + "(" + id + ")" + " reports socks without ownship.");
            }
            socks.remove(sock);
        }
        for (Sock sock : list)
            socks.add(sock);

        double result = 0;
        for (int i = 0; i < list.size(); i += 2)
            result += list.get(i).distance(list.get(i + 1));
        return result;
    }

}
