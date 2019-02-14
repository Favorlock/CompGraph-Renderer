import java.awt.image.WritableRaster;

public class FrameBuffer {

    private int width;
    private int height;
    private double[] buffer;

    public FrameBuffer(int width, int height) {
        resize(width, height);
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
                set(x, y, Color.WHITE);
            }
        }
    }

    public Color at(int x, int y) {
        int ri = (height - y - 1) * width * 3 + x * 3;
        int gi = ri + 1;
        int bi = ri + 2;

        return Color.rgb(buffer[ri], buffer[gi], buffer[bi]);
    }

    public boolean set(int x, int y, Color color) {
        int ri = (height - y - 1) * width * 3 + x * 3;
        int gi = ri + 1;
        int bi = ri + 2;

        buffer[ri] = color.get(0);
        buffer[gi] = color.get(1);
        buffer[bi] = color.get(2);

        return false;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        buffer = new double[width * height * 3];
        clear();
    }

    public void draw(WritableRaster raster, int x, int y) {
        int w = this.width / 2;
        int h = this.height / 2;
        double[] out = new double[w * h * 3];

        for (int by = 0; by < this.height; by++) {
            for (int bx = 0; bx < this.width; bx++) {
                int ox = bx / 2;
                int oy = by / 2;
                int ri = oy * w * 3 + ox * 3;
                out[ri] += buffer[by * this.width * 3 + bx * 3];
                out[ri + 1] += buffer[by * this.width * 3 + bx * 3 + 1];
                out[ri + 2] += buffer[by * this.width * 3 + bx * 3 + 2];
                if (bx % 2 == 1 && by % 2 == 1) {
                    out[ri] /= 4;
                    out[ri + 1] /= 4;
                    out[ri + 2] /= 4;
                }
            }
        }

        raster.setPixels(x, y, w, h, out);
    }
}
