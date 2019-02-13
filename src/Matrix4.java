public class Matrix4 {

    public static final int DIM = 4;
    public static final int ELEMENTS = DIM * DIM;

    public static final int SCALE_SX = 0;
    public static final int SCALE_SY = 5;
    public static final int SCALE_SZ = 10;

    public static final int TRANS_TX = 3;
    public static final int TRANS_TY = 7;
    public static final int TRANS_TZ = 11;

    public static final int[] ROTATE_RX = { 5, 6, 9, 10};
    public static final int[] ROTATE_RY = { 0, 2, 8, 10};
    public static final int[] ROTATE_RZ = { 0, 1, 4, 5};

    private double[] data;

    public Matrix4(double[] data) {
        this.data = data;
    }

    private Matrix4() {
        this.data = new double[ELEMENTS];
    }

    public Matrix4 mult(Matrix4 other) {
        double[] data = new double[ELEMENTS];
        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                int index = i * DIM + j;
                for (int k = 0; k < DIM; k++) {
                    int tIndex = i * DIM + k;
                    int oIndex = k * DIM + j;
                    data[index] += this.data[tIndex] * other.data[oIndex];
                }
            }
        }

        return new Matrix4(data);
    }

    public Vector4 mult(Vector4 other) {
        double data[] = new double[4];
        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                int tIndex = i * DIM + j;
                data[i] += this.data[tIndex] * other.get(j);
            }
        }
        return new Vector4(data);
    }

    public static Matrix4 createXRotationMatrix(double degrees) {
        return createRotationMatrix(ROTATE_RX, Math.toRadians(degrees), false);
    }

    public static Matrix4 createYRotationMatrix(double degrees) {
        return createRotationMatrix(ROTATE_RY, Math.toRadians(degrees), true);
    }

    public static Matrix4 createZRotationMatrix(double degrees) {
        return createRotationMatrix(ROTATE_RZ, Math.toRadians(degrees), false);
    }

    private static Matrix4 createRotationMatrix(int[] indices, double amount, boolean swapSin) {
        Matrix4 matrix = createIdentityMatrix();
        matrix.data[indices[0]] = Math.cos(amount);
        matrix.data[indices[1]] = swapSin ? Math.sin(amount) : -Math.sin(amount);
        matrix.data[indices[2]] = swapSin ? -Math.sin(amount) : Math.sin(amount);
        matrix.data[indices[3]] = Math.cos(amount);
        return matrix;
    }

    public static Matrix4 createTranslationMatrix(double tx, double ty, double tz) {
        Matrix4 matrix = createIdentityMatrix();
        matrix.data[TRANS_TX] = tx;
        matrix.data[TRANS_TY] = ty;
        matrix.data[TRANS_TZ] = tz;
        return matrix;
    }

    public static Matrix4 createScalingMatrix(double sx, double sy, double sz) {
        Matrix4 matrix = createIdentityMatrix();
        matrix.data[SCALE_SX] = sx;
        matrix.data[SCALE_SY] = sy;
        matrix.data[SCALE_SZ] = sz;
        return matrix;
    }

    public static Matrix4 createIdentityMatrix() {
        Matrix4 matrix = new Matrix4();
        for (int i = 0; i < DIM; i++) {
            int index = i * DIM + i;
            matrix.data[index] = 1;
        }
        return matrix;
    }

}
