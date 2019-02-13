public class Triangle {

    private Vector4 v1;
    private Vector4 v2;
    private Vector4 v3;

    public Triangle(Vector4 v1, Vector4 v2, Vector4 v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    public Vector4 getV1() {
        return v1;
    }

    public Vector4 getV2() {
        return v2;
    }

    public Vector4 getV3() {
        return v3;
    }

    public void transform(Matrix4 ctm) {
        this.v1 = ctm.mult(this.v1);
        this.v2 = ctm.mult(this.v2);
        this.v3 = ctm.mult(this.v3);
    }

    public void divideByW() {
        this.v1 = v1.divideByW();
        this.v2 = v2.divideByW();
        this.v3 = v3.divideByW();
    }

    @Override
    public String toString() {
        return "Triangle{" +
                "\n\tv1=" + v1 +
                ", \n\tv2=" + v2 +
                ", \n\tv3=" + v3 +
                '}';
    }
}
