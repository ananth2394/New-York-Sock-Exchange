package exchange.g2;

import exchange.sim.Sock;

public class SockCenterFunction implements AbstractEKmeans.CenterFunction<Sock, Sock> {

    public void center(boolean[] changed, int[] assignments, Sock[] centroids, Sock[] points) {
        for (int i = 0; i < centroids.length; ++i) {
            if (!changed[i]) {
                continue;
            }
            Sock centroid = new Sock(0, 0, 0);

            int numPointsInCluster = 0;
            for (int j = 0; j < points.length; ++j) {
                if (assignments[j] != i) {
                    // Skip point/sock since it does not belong to the cluster.
                    continue;
                }
                Sock point = points[j];
                numPointsInCluster++;
                centroid.R += point.R;
                centroid.G += point.G;
                centroid.B += point.B;
            }
            if (numPointsInCluster > 0) {
                centroid.R /= numPointsInCluster;
                centroid.G /= numPointsInCluster;
                centroid.B /= numPointsInCluster;
            }
        }
    }
}
