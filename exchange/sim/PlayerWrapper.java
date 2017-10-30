package exchange.sim;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class PlayerWrapper {
    private Timer thread;
    private Player player;
    private int id, n;
    private long timeout, originalTimeout;
    private Multiset<Sock> socks = new Multiset<Sock>();
    private Random random = new Random();

    public PlayerWrapper(Player player, int id, int n, long timeout) {
        this.player = player;
        this.id = id;
        this.n = n;
        this.timeout = timeout;
        originalTimeout = timeout;
        thread = new Timer();
    }


    public void init(int n, int p, int t) throws Exception {
        System.out.println("Generating player " + id + "'s socks");
        for (int i = 0; i < 2 * n; ++ i) {
            Sock sock = new Sock(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            socks.add(sock);
            System.out.println(sock + " " + socks.contains(sock));
        }
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> {
            player.init(id, n, p, t, socks.toList());
            return null;
        });
        thread.call_wait(timeout);
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
    }

    public Offer makeOffer(List<Request> requests, List<Transaction> lastTransactions) throws Exception {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> player.makeOffer(requests, lastTransactions));
        Offer ret = thread.call_wait(timeout);
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
        return ret;
    }


    public Request requestExchange(List<Offer> offers) throws Exception {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> player.requestExchange(offers));
        Request ret = thread.call_wait(timeout);
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
        return ret;
    }


    public void completeTransaction(Transaction transaction) throws Exception {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> {
            player.completeTransaction(transaction);
            return null;
        });
        thread.call_wait(timeout);
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

    public double getTotalEmbarrassment() throws Exception {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> player.getSocks());
        List<Sock> list = thread.call_wait(timeout);
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
        //return ret;
        // List<Sock> list = player.getSocks();
        System.out.println("list size is "+list.size());

        if (list.size() != 2 * n){
            //System.out.println("list size failed is "+list.size()+ " " +n);
            return 1000000000;
        }

        for (Sock sock : list) {
            if (!socks.contains(sock))
                throw new Exception("Player " + id + " reports socks without ownship.");
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
