package exchange.sim;

public class Sock {
    public int R, G, B;

    public Sock(int R, int G, int B) {
        this.R = R;
        this.G = G;
        this.B = B;
    }

    public Sock(Sock sock) {
        this.R = sock.R;
        this.G = sock.G;
        this.B = sock.B;
    }

    @Override
    public int hashCode() {
        return R * 256 * 256 + G * 256 + B;
    }

    @Override
    public boolean equals(Object obj) {
        Sock s = (Sock) obj;
        return R == s.R && G == s.G && B == s.B;
    }

    public double distance(Sock s) {
        return Math.sqrt(Math.pow(this.R - s.R, 2) + Math.pow(this.G - s.G, 2) + Math.pow(this.B - s.B, 2));
    }

    @Override
    public String toString() {
        return "Sock(" + R + ", " + G + ", " + B + ")";
    }

    public String toRGB() {
        return String.format("%02X%02X%02X", R, G, B);
    }
}
