public class Cube extends Mesh {

    public static final double UNICUBE_POS = .5;
    public static final double UNICUBE_NEG = -.5;

    private static Vector4 lbf = new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_POS);
    private static Vector4 lbr = new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_NEG);
    private static Vector4 rbr = new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_NEG);
    private static Vector4 rbf = new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_POS);
    private static Vector4 ltf = new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_POS);
    private static Vector4 ltr = new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_NEG);
    private static Vector4 rtr = new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_NEG);
    private static Vector4 rtf = new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_POS);

    public Cube() {
        super(
                new Surface(new Triangle(lbf, lbr, ltr), new Triangle(lbf, ltf, ltr)), // left
                new Surface(new Triangle(rbf, rbr, rtr), new Triangle(rbf, rtf, rtr)), // right
                new Surface(new Triangle(lbr, rbr, rtr), new Triangle(lbr, ltr, rtr)), // rear
                new Surface(new Triangle(lbf, rbf, rtf), new Triangle(lbf, ltf, rtf)), // front
                new Surface(new Triangle(lbf, lbr, rbr), new Triangle(lbf, rbf, rbr)), // bottom
                new Surface(new Triangle(ltf, ltr, rtr), new Triangle(ltf, rtf, rtr))  // top
        );
    }

    public void transform(Matrix4 ctm) {
        super.transform(ctm);
    }

}
