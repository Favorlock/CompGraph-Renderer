public class Triangle {

    private Vector4 v1;
    private Vector4 v2;
    private Vector4 v3;

    public Triangle(Vector4 v1, Vector4 v2, Vector4 v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        arrangeByY();
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
        arrangeByY();
    }

    public void arrangeByY() {
        Vector4 tmp = null;

        if (v2.y < v1.y) {
            tmp = v1;
            v1 = v2;
            v2 = tmp;
        }

        if (v3.y < v2.y) {
            tmp = v2;
            v2 = v3;
            v3 = tmp;
        }

        if (v2.y < v1.y) {
            tmp = v1;
            v1 = v2;
            v2 = tmp;
        }

        if (v1.y == v2.y) {
            // natural flat top
            if (v2.x < v1.x) {
                tmp = v1;
                v1 = v2;
                v2 = tmp;
            }
        } else if (v2.y == v3.y) {
            // natural flat bottom
            if (v3.x < v2.x) {
                tmp = v2;
                v2 = v3;
                v3 = tmp;
            }
        }
    }
}
