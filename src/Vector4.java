public class Vector4 {

    protected Double x, y, z, w;

    public Vector4(double x, double y, double z) {
        this(x, y, z, 1.0D);
    }

    public Vector4(double x, double y, double z, double w) {
        this.x = x;
        this.y = y ;
        this.z = z;
        this.w = w;
    }

    public Vector4(double[] data) {
        this(data[0], data[1], data[2], data[3]);
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Double getZ() {
        return z;
    }

    public Double getW() {
        return w;
    }

    public Double get(int index) {
        if (index == 0)
            return x;
        else if (index == 1)
            return y;
        else if (index == 2)
            return z;
        else if (index == 3)
            return w;
        throw new IllegalArgumentException("Invalid Index");
    }

    public Double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector4 subtract(Vector4 other) {
        return new Vector4(x - other.x, y - other.y, z - other.z);
    }

    public Vector4 normalize() {
        Vector4 v = new Vector4(x, y, z, w);
        double length = v.length();
        if (length > 0) {
            double percent = 1.0 / length;
            v.x = v.x * percent;
            v.y = v.y * percent;
            v.z = v.z * percent;
        }
        return v;
    }

    public Vector4 crossProduct(Vector4 other) {
        Vector4 v = new Vector4(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x,
                0
        );

        return v;
    }

    public double dotProduct(Vector4 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector4 divideByW() {
        return new Vector4(x / w, y / w, z / w, 1);
    }

    @Override
    public String toString() {
        return "Vector4{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", w=" + w +
                '}';
    }
}
