package exchange.sim;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class Simulator {
    private static final String root = "exchange";
    private static final String statics_root = "statics";

    private static long playerTimeout = 10000;
    private static boolean gui = false;
    private static double fps = 5;
    private static int n = 20;
    private static int p = 0;
    private static int t = 1000;
    private static List<String> playerNames = new ArrayList<String>();
    private static PlayerWrapper[] players;

    public static void main(String[] args) throws Exception {
//		args = new String[] {"-p", "g0", "g0", "g0", "g0", "-g"};
        parseArgs(args);
        players = new PlayerWrapper[p];
        for (int i = 0; i < p; ++i) {
            Log.record("Loading player " + i + ": " + playerNames.get(i));
            Player player = loadPlayer(i, playerNames.get(i));
            if (player == null) {
                Log.record("Cannot load player " + i + ": " + playerNames.get(i));
                System.exit(1);
            }
            players[i] = new PlayerWrapper(player, i, n, playerTimeout);
        }

        System.out.println("Starting game with " + p + " players");

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
                    e.printStackTrace();
                }
            }
        }

        // Simulation starts!
        for (int i = 0; i < p; ++ i)
            players[i].init(n, p, t);
        List<Transaction> lastTransactions = new ArrayList<Transaction>();
        Offer[] offers = new Offer[p];
        Request[] requests = new Request[p];
        for (int turn = 1; turn <= t; ++turn) {
            System.out.println("Round " + turn + ":");
            // Gather offers
            for (int i = 0; i < p; ++i) {
                offers[i] = players[i].makeOffer(Arrays.asList(requests), lastTransactions);
                if (offers[i].getFirst() != null && !players[i].owned(offers[i].getFirst()))
                    throw new Exception(playerNames.get(i) + "(" + i + ") making invalid offer " + offers[i]);
                if (offers[i].getSecond() != null && !players[i].owned(offers[i].getSecond()))
                    throw new Exception(playerNames.get(i) + "(" + i + ") making invalid offer " + offers[i]);
                System.out.println(playerNames.get(i) + "(" + i + ") making offer " + offers[i]);
            }
            if (gui) {
                gui(server, state(fps, turn, offers, null, null));
            }
            // Getting requests
            for (int i = 0; i < p; ++i) {
                List<Offer> toSend = new ArrayList<>();
                for (Offer offer : offers)
                    toSend.add(new Offer(offer));
                requests[i] = players[i].requestExchange(toSend);
                if (!validateRequest(offers, i, requests[i]))
                    throw new Exception(playerNames.get(i) + "(" + i + ") making invalid requests " + requests[i]);
                System.out.println(playerNames.get(i) + "(" + i + ") requesting " + requests[i]);
            }


            if (gui) {
                gui(server, state(fps, turn, offers, requests, null));
            }

            lastTransactions = ExchangeCenter.exchange(offers, requests);

            if (gui) {
                if (turn == t) fps = -1000.0;
                gui(server, state(fps, turn, offers, requests, lastTransactions));
            }

            System.out.println("Completed transactions: ");
            for (Transaction transaction : lastTransactions) {
                players[transaction.getFirstID()].removeSock(transaction.getFirstSock());
                players[transaction.getSecondID()].removeSock(transaction.getSecondSock());
                players[transaction.getFirstID()].addSock(transaction.getSecondSock());
                players[transaction.getSecondID()].addSock(transaction.getFirstSock());

                players[transaction.getFirstID()].completeTransaction(transaction);
                players[transaction.getSecondID()].completeTransaction(transaction);
                System.out.println(transaction);
            }
            System.out.println("");
        }

        for (int i = 0; i < p; ++ i) {
            System.out.println(playerNames.get(i) + " gets total embarrassment " + players[i].getTotalEmbarrassment());
        }

        for (int i = 0; i < p; ++i) {
            Log.record("Total running time for player " + i + " is " + players[i].getTotalElapsedTime() + "ms");
        }
        System.exit(0);
    }

    private static boolean validateRequest(Offer[] offers, int id, Request request) {
        if (request == null)
            return false;
        if (request.getFirstID() < -1 || request.getFirstID() >= p)
            return false;
        if (request.getSecondID() < -1 || request.getSecondID() >= p)
            return false;
        if (request.getFirstID() == id || request.getSecondID() == id)
            return false;
        if (request.getFirstID() > -1) {
            if (request.getFirstRank() < 1 || request.getFirstRank() > 2)
                return false;
            else if (request.getFirstRank() == 1) {
                if (offers[request.getFirstID()].getFirst() == null)
                    return false;
            } else if (request.getFirstRank() == 2) {
                if (offers[request.getFirstID()].getSecond() == null)
                    return false;
            }
        }
        if (request.getSecondID() > -1) {
            if (request.getSecondRank() < 1 || request.getSecondRank() > 2)
                return false;
            else if (request.getSecondRank() == 1) {
                if (offers[request.getSecondID()].getFirst() == null)
                    return false;
            } else if (request.getSecondRank() == 2) {
                if (offers[request.getSecondID()].getSecond() == null)
                    return false;
            }
        }
        return true;
    }

    private static String state(double fps, int turn, Offer[] offers, Request[] requests, List<Transaction> transactions) {
        // TODO
        DecimalFormat df = new DecimalFormat("#.00");
        double refresh = 1000.0 / fps;
        String ret = refresh + "," + turn + "," + p;// + transactions.size();
        if (transactions == null) ret += ",0";
        else ret += "," + transactions.size();
        for (int i = 0; i < p; ++i) {
            ret += "," + playerNames.get(i);
            ret += "," + df.format(players[i].getTotalEmbarrassment());
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
                            for (int k = 0; k < rep; ++ k)
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
                    }else if (args[i].equals("--fps")) {
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
        return (Player)raw_class.newInstance();
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
