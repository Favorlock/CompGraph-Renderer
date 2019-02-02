import java.util.Arrays;

public class Color {

    private static final int RGB_MAX = 255;

    protected int[] values;

    public Color(int... values) {
        this.values = values;
    }

    public int get(int index) {
        return values[index];
    }

    public static RGB rgb(double r, double g, double b) {
        return new RGB(convertRgbValue(r), convertRgbValue(g), convertRgbValue(b));
    }

    public static RGB rgb(int r, int g, int b) {
        return new RGB(r, g, b);
    }

    private static int convertRgbValue(double value) {
        return Double.valueOf(value * RGB_MAX).intValue();
    }

    public static class RGB extends Color {
        private RGB(int r, int g, int b) {
            super(r, g, b);
        }

        public int getRed() {
            return values[0];
        }

        public int getGreen() {
            return values[1];
        }

        public int getBlue() {
            return values[2];
        }

        @Override
        public String toString() {
            return "RGB{" +
                    "values=" + Arrays.toString(values) +
                    '}';
        }
    }
}
