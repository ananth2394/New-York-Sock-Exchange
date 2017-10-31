package exchange.g2;

import exchange.sim.Sock;

public class SockDistanceFunction implements AbstractEKmeans.DistanceFunction<Sock, Sock> {

    public void distance(boolean[] changed, double[][] distances, Sock[] centroids, Sock[] points) {
        for (int i = 0; i < centroids.length; ++i) {
            for (int j = 0; j < points.length; ++j) {
                distances[i][j] = centroids[i].distance(points[j]);
            }
        }
        ;
    }
}
