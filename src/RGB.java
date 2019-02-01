public class RGB {

    private static final int RGB_MAX = 255;

    private int red;
    private int green;
    private int blue;

    private RGB(int r, int g, int b) {
        red = r;
        green = g;
        blue = b;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    private static int doubleToInt(double value) {
        return Double.valueOf(value * RGB_MAX).intValue();
    }

    public static RGB of(double r, double g, double b) {
        return new RGB(doubleToInt(r), doubleToInt(g), doubleToInt(b));
    }
}
