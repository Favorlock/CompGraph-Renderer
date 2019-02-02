public class DepthBuffer {

    private int width;
    private int height;
    private double[][] buffer;

    public DepthBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        buffer = new double[width][height];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void clear() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                buffer[x][y] = Double.NEGATIVE_INFINITY;
            }
        }
    }

    public double at(int x, int y) {
        return buffer[x][y];
    }

    public boolean set(int x, int y, double depth) {
        double depthInBuffer = at(x, y );
        if (depth > depthInBuffer) {
            buffer[x][y] = depth;
            return true;
        }
        return false;
    }
}
