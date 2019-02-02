public class Mesh {

    private Surface[] surfaces;

    public Mesh(Surface... surfaces) {
        this.surfaces = surfaces;
    }

    public Surface[] getSurfaces() {
        return this.surfaces;
    }

    public void transform(Matrix4 ctm) {
        for (Surface surface : this.surfaces) surface.transform(ctm);
    }
}
