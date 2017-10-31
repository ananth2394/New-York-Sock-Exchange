package exchange.sim;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class Simulator {
    private static final String root = "exchange";
    private static final String statics_root = "statics";

    private static Random random;
    private static long seed = 20171030;
    private static long[] playerSeeds;
    private static long playerTimeout = 10000;
    private static boolean gui = false;
    private static boolean silent = false;
    private static double fps = 5;
    private static int n = 20;
    private static int p = 0;
    private static int t = 1000;
    private static List<String> playerNames = new ArrayList<String>();
    private static PlayerWrapper[] players;
    private static double[] totalEmbarrassments;

    public static void main(String[] args) throws Exception {
//		args = new String[] {"-p", "g1", "g2", "g3", "g4", "g5", "g6", "-n", "10", "-t", "100", "-s", "1411390388"};
        parseArgs(args);
        players = new PlayerWrapper[p];

        random = new Random(seed);
        playerSeeds = new long[p];
        for (int i = 0; i < p; ++i)
            playerSeeds[i] = random.nextLong();

        for (int i = 0; i < p; ++i) {
            Log.record("Loading player " + i + ": " + playerNames.get(i));
            Player player = loadPlayer(i, playerNames.get(i));
            if (player == null) {
                Log.record("Cannot load player " + i + ": " + playerNames.get(i));
                System.exit(1);
            }
            players[i] = new PlayerWrapper(player, i, playerNames.get(i), n, playerTimeout, playerSeeds[i]);
        }

        if (!silent) System.out.println("Starting game with " + p + " players");

        HTTPServer server = null;
        if (gui) {
            server = new HTTPServer();
            Log.record("Hosting HTTP Server on " + server.addr());
            if (!Desktop.isDesktopSupported())
                Log.record("Desktop operations not supported");
            else if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Log.record("Desktop browse operation not supported");
            else {
                try {
                    Desktop.getDesktop().browse(new URI("http://localhost:" + server.port()));
                } catch (URISyntaxException e) {
                    System.err.println(e.toString());
                }
            }
        }

        // Simulation starts!
        for (int i = 0; i < p; ++i)
            players[i].init(n, p, t);
        List<Transaction> lastTransactions = new ArrayList<Transaction>();
        Offer[] offers = new Offer[p];
        Request[] requests = new Request[p];
        totalEmbarrassments = new double[p];
        for (int i = 0; i < p; ++i) {
            if (players[i].isActive()) {
                try {
                    totalEmbarrassments[i] = players[i].getTotalEmbarrassment();
                } catch (Exception e) {
                    System.err.println(e.toString());
//                    e.printStackTrace(System.err);
                }
            }
        }
        for (int turn = 1; turn <= t; ++turn) {
            if (!silent) System.out.println("Round " + turn + ":");
            for (int i = 0; i < p; ++ i) {
                offers[i] = new Offer(null, null);
                requests[i] = new Request(-1, -1, -1, -1);
            }
            // Gather offers
            for (int i = 0; i < p; ++i) {
                if (!players[i].isActive()) continue;
                offers[i] = players[i].makeOffer(Arrays.asList(requests), lastTransactions);
                if (offers[i].getFirst() != null && !players[i].owned(offers[i].getFirst())) {
                    System.err.println(players[i].getName() + "(" + i + ") making invalid offer " + offers[i]);
                    players[i].setIllegal(true);
                    offers[i] = new Offer(null, null);
                }
                if (offers[i].getSecond() != null && !players[i].owned(offers[i].getSecond())) {
                    System.err.println(players[i].getName() + "(" + i + ") making invalid offer " + offers[i]);
                    players[i].setIllegal(true);
                    offers[i] = new Offer(null, null);
                }
                if (!silent) System.out.println(players[i].getName() + "(" + i + ") making offer " + offers[i]);
            }
            if (gui) {
                gui(server, state(fps, turn, offers, null, null, totalEmbarrassments));
            }
            // Getting requests
            for (int i = 0; i < p; ++i) {
                if (!players[i].isActive()) continue;
                List<Offer> toSend = new ArrayList<>();
                for (Offer offer : offers)
                    toSend.add(new Offer(offer));
                requests[i] = players[i].requestExchange(toSend);
                if (!validateRequest(offers, i, requests[i])) {
                    System.err.println(players[i].getName() + "(" + i + ") making invalid requests " + requests[i]);
                    players[i].setIllegal(true);
                    requests[i] = new Request(-1, -1, -1, -1);
                }
                if (!silent) System.out.println(players[i].getName() + "(" + i + ") requesting " + requests[i]);
            }


            if (gui) {
                gui(server, state(fps, turn, offers, requests, null, totalEmbarrassments));
            }

            lastTransactions = ExchangeCenter.exchange(offers, requests);

            if (gui) {
                if (turn == t) fps = -1000.0;
                gui(server, state(fps, turn, offers, requests, lastTransactions, totalEmbarrassments));
            }

            if (!silent) System.out.println("Completed transactions: ");
            for (Transaction transaction : lastTransactions) {
                players[transaction.getFirstID()].removeSock(transaction.getFirstSock());
                players[transaction.getSecondID()].removeSock(transaction.getSecondSock());
                players[transaction.getFirstID()].addSock(transaction.getSecondSock());
                players[transaction.getSecondID()].addSock(transaction.getFirstSock());

                players[transaction.getFirstID()].completeTransaction(transaction);
                players[transaction.getSecondID()].completeTransaction(transaction);

                if (players[transaction.getFirstID()].isActive()) {
                    try {
                        totalEmbarrassments[transaction.getFirstID()] = players[transaction.getFirstID()].getTotalEmbarrassment();
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                }
                if (players[transaction.getSecondID()].isActive()) {
                    try {
                        totalEmbarrassments[transaction.getSecondID()] = players[transaction.getSecondID()].getTotalEmbarrassment();
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                }
                if (!silent) System.out.println(transaction);
            }
            if (!silent) System.out.println("");
        }

        for (int i = 0; i < p; ++i) {
            System.out.println(players[i].getName() + " gets total embarrassment " + totalEmbarrassments[i]);
        }

        for (int i = 0; i < p; ++i) {
            Log.record("Total running time for player " + i + " is " + players[i].getTotalElapsedTime() + "ms");
        }
        System.exit(0);
    }

    private static boolean validateRequest(Offer[] offers, int id, Request request) {
        if (request == null)
            return false;
        // ID range check
        if (request.getFirstID() < -1 || request.getFirstID() >= p)
            return false;
        if (request.getSecondID() < -1 || request.getSecondID() >= p)
            return false;
        if (request.getFirstID() == id || request.getSecondID() == id)
            return false;

        if (request.getFirstID() > -1) {
            if (offers[request.getFirstID()].getSock(request.getFirstRank()) == null)
                return false;
        }
        if (request.getSecondID() > -1) {
            if (offers[request.getSecondID()].getSock(request.getSecondRank()) == null)
                return false;
        }
        return true;
    }

    private static String state(double fps, int turn, Offer[] offers, Request[] requests, List<Transaction> transactions, double[] totalEmbarrassments) throws Exception {
        // TODO
        DecimalFormat df = new DecimalFormat("#.00");
        double refresh = 1000.0 / fps;
        String ret = refresh + "," + turn + "," + p;// + transactions.size();
        if (transactions == null) ret += ",0";
        else ret += "," + transactions.size();
        for (int i = 0; i < p; ++i) {
            ret += "," + players[i].getName();
            ret += "," + df.format(totalEmbarrassments[i]);
            if (offers[i].getFirst() == null)
                ret += ",no";
            else ret += "," + offers[i].getFirst().toRGB();
            if (offers[i].getSecond() == null)
                ret += ",no";
            else ret += "," + offers[i].getSecond().toRGB();
            if (requests == null)
                ret += ",-1,-1,-1,-1";
            else {
                Request r = requests[i];
                ret += "," + r.getFirstID() + "," + r.getFirstRank() + "," + r.getSecondID() + "," + r.getSecondRank();
            }
        }
        if (transactions != null)
            for (Transaction t : transactions)
                ret += "," + t.getFirstID() + "," + t.getFirstRank() + "," + t.getSecondID() + "," + t.getSecondRank();
        return ret;
    }

    private static void gui(HTTPServer server, String content) {
        if (server == null) return;
        String path = null;
        for (; ; ) {
            for (; ; ) {
                try {
                    path = server.request();
                    break;
                } catch (IOException e) {
                    Log.record("HTTP request error " + e.getMessage());
                }
            }
            if (path.equals("data.txt")) {
                try {
                    server.reply(content);
                } catch (IOException e) {
                    Log.record("HTTP dynamic reply error " + e.getMessage());
                }
                return;
            }
            if (path.equals("")) path = "webpage.html";
            else if (!Character.isLetter(path.charAt(0))) {
                Log.record("Potentially malicious HTTP request \"" + path + "\"");
                break;
            }

            File file = new File(statics_root + File.separator + path);
            if (file == null) {
                Log.record("Unknown HTTP request \"" + path + "\"");
            } else {
                try {
                    server.reply(file);
                } catch (IOException e) {
                    Log.record("HTTP static reply error " + e.getMessage());
                }
            }
        }
    }

    private static void parseArgs(String[] args) {
        int i = 0;
        for (; i < args.length; ++i) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].startsWith("-p") || args[i].equals("--players")) {
                        int rep = 1;
                        try {
                            rep = Integer.parseInt(args[i].substring(2));
                        } catch (Exception e) {
                            rep = 1;
                        }
                        while (i + 1 < args.length && args[i + 1].charAt(0) != '-') {
                            ++i;
                            for (int k = 0; k < rep; ++k)
                                playerNames.add(args[i]);
                        }
                    } else if (args[i].equals("-t") || args[i].equals("--turns")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing turn limit");
                        }
                        t = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-n")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing number of pairs");
                        }
                        n = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-s") || args[i].equals("--seed")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing seed");
                        }
                        seed = Long.parseLong(args[i]);
                    } else if (args[i].equals("-tl") || args[i].equals("--timelimit")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing time limit");
                        }
                        playerTimeout = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-l") || args[i].equals("--logfile")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing logfile name");
                        }
                        Log.setLogFile(args[i]);
                    } else if (args[i].equals("-g") || args[i].equals("--gui")) {
                        gui = true;
                    } else if (args[i].equals("--silent")) {
                        silent = true;
                    } else if (args[i].equals("--fps")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing fps");
                        }
                        fps = Double.parseDouble(args[i]);
                    } else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                        Log.activate();
                    } else {
                        throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\"");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\"");
            }
        }

        p = playerNames.size();
        if (playerNames.size() < 2) {
            throw new IllegalArgumentException("Not enough players, you need at least 2 player to start a game");
        }
        Log.record("Number of players: " + playerNames.size());
        Log.record("Time limit for each player: " + playerTimeout + "ms");
        Log.record("GUI " + (gui ? "enabled" : "disabled"));
        if (gui)
            Log.record("FPS: " + fps);
    }

    private static Set<File> directory(String path, String extension) {
        Set<File> files = new HashSet<File>();
        Set<File> prev_dirs = new HashSet<File>();
        prev_dirs.add(new File(path));
        do {
            Set<File> next_dirs = new HashSet<File>();
            for (File dir : prev_dirs)
                for (File file : dir.listFiles())
                    if (!file.canRead()) ;
                    else if (file.isDirectory())
                        next_dirs.add(file);
                    else if (file.getPath().endsWith(extension))
                        files.add(file);
            prev_dirs = next_dirs;
        } while (!prev_dirs.isEmpty());
        return files;
    }

    public static Player loadPlayer(int id, String name) throws IOException, ClassNotFoundException, InstantiationException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String sep = File.separator;
        Set<File> player_files = directory(root + sep + name, ".java");
        File class_file = new File(root + sep + name + sep + "Player.class");
        long class_modified = class_file.exists() ? class_file.lastModified() : -1;
        if (class_modified < 0 || class_modified < last_modified(player_files) ||
                class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null)
                throw new IOException("Cannot find Java compiler");
            StandardJavaFileManager manager = compiler.
                    getStandardFileManager(null, null, null);
//            long files = player_files.size();
            Log.record("Compiling for player " + name);
            if (!compiler.getTask(null, manager, null, null, null,
                    manager.getJavaFileObjectsFromFiles(player_files)).call())
                throw new IOException("Compilation failed");
            class_file = new File(root + sep + name + sep + "Player.class");
            if (!class_file.exists())
                throw new FileNotFoundException("Missing class file");
        }
        ClassLoader loader = Simulator.class.getClassLoader();
        if (loader == null)
            throw new IOException("Cannot find Java class loader");
        @SuppressWarnings("rawtypes")
        Class<?> raw_class = loader.loadClass(root + "." + name + ".Player");
//        Player player = null;
//        Constructor<?> constructor = raw_class.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
//        player = (Player) constructor.newInstance(id, n, p, t);
        return (Player) raw_class.newInstance();
    }

    private static long last_modified(Iterable<File> files) {
        long last_date = 0;
        for (File file : files) {
            long date = file.lastModified();
            if (last_date < date)
                last_date = date;
        }
        return last_date;
    }
}
