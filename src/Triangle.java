public class Triangle {

    private Vector4 p1;
    private Vector4 p2;
    private Vector4 p3;

    public Triangle(Vector4 p1, Vector4 p2, Vector4 p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    public Vector4 getP1() {
        return p1;
    }

    public Vector4 getP2() {
        return p2;
    }

    public Vector4 getP3() {
        return p3;
    }

    public void transform(Matrix4 ctm) {
        this.p1 = ctm.mult(this.p1);
        this.p2 = ctm.mult(this.p2);
        this.p3 = ctm.mult(this.p3);
    }

}
