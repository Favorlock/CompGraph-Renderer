public class Surface {

    private Triangle[] tris;

    public Surface(Triangle... tris) {
        this.tris = tris;
    }

    public Triangle[] getTris() {
        return this.tris;
    }

    public void transform(Matrix4 ctm) {
        for (Triangle tri : this.tris) tri.transform(ctm);
    }
}
