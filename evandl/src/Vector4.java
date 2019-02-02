public class Vector4 {

    protected Double x, y, z, w;

    public Vector4(double x, double y, double z) {
        this.x = x;
        this.y = y ;
        this.z = z;
        this.w = 1.0D;
    }

    public Vector4(double[] data) {
        this(data[0], data[1], data[2]);
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
