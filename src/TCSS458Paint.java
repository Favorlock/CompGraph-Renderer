import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.*;
import javax.swing.*;

import java.io.*;
import java.util.*;


public class TCSS458Paint extends JPanel implements KeyListener {

    public static final double UNICUBE_POS = Cube.UNICUBE_POS;
    public static final double UNICUBE_NEG = Cube.UNICUBE_NEG;
    public static final int DEGREES_TO_ROTATE = 3;

    public static int width;
    public static int height;

    private int imageSize;
    private FrameBuffer frameBuffer;
    private int[][] scan;
    private double[][] scanZ;
    private DepthBuffer depthBuffer;
    private Color color;
    private Matrix4 ctm;
    private Matrix4 lookAtMatrix;
    private Matrix4 projectionMatrix;
    private Vector4 lightDirection;
    private int xRotate = 0;
    private int yRotate = 0;
    private boolean drawTri = false;

    void drawPixel(int x, int y, double z, boolean force) {
        drawPixel(x, y, z, color.get(0), color.get(1), color.get(2), force);
    }

    void drawPixel(int x, int y, double z, int r, int g, int b, boolean force) {
        if (x >= width || y >= height || x < 0 || y < 0)
            return;

        if (depthBuffer.set(x, y, z) || force) {
            frameBuffer.set(x, y, r, g, b);
        }
    }

    void drawLine(Vector4 v1, Vector4 v2) {
        int x1 = worldToScreen(v1.x, width);
        int y1 = worldToScreen(v1.y, height);
        int x2 = worldToScreen(v2.x, width);
        int y2 = worldToScreen(v2.y, height);

        if (Math.abs(y2 - y1) < Math.abs(x2 - x1)) {
            if (x1 > x2) {
                drawLineLow(v2, v1);
            } else {
                drawLineLow(v1, v2);
            }
        } else {
            if (y1 > y2) {
                drawLineHigh(v2, v1);
            } else {
                drawLineHigh(v1, v2);
            }
        }
    }

    void drawLineLow(Vector4 v1, Vector4 v2) {
        int x1 = worldToScreen(v1.x, width);
        int y1 = worldToScreen(v1.y, height);
        int x2 = worldToScreen(v2.x, width);
        int y2 = worldToScreen(v2.y, height);

        double m = (y2 - y1) * 1.0 / (x2 - x1);
        double mz = (v2.z - v1.z) / (x2 - x1);
        double z = v1.z;
        double yfl = y1;
        int y;

        for (int x = x1; x <= x2; x++, z += mz, yfl += m) {
            y = (int) Math.round(yfl);
            if (y < 0 || y >= scan.length) continue;
//            drawPixel(x, y, z, false);

            if (scan != null) {
                if (scan[y][0] == Integer.MIN_VALUE) {
                    scan[y][0] = scan[y][1] = x;
                    scanZ[y][0] = scanZ[y][1] = z;
                } else {
                    if (x < scan[y][0]) {
                        scan[y][0] = x;
                        scanZ[y][0] = z;
                    }
                    if (x > scan[y][1]) {
                        scan[y][1] = x;
                        scanZ[y][1] = z;
                    }
                }
            }
        }
    }

    void drawLineHigh(Vector4 v1, Vector4 v2) {
        int x1 = worldToScreen(v1.x, width);
        int y1 = worldToScreen(v1.y, height);
        int x2 = worldToScreen(v2.x, width);
        int y2 = worldToScreen(v2.y, height);

        double m = (x2 - x1) * 1.0 / (y2 - y1);
        double mz = (v2.z - v1.z) / (y2 - y1);
        double z = v1.z;
        double xfl = x1;
        int x;

        for (int y = y1; y <= y2; y++, z += mz, xfl += m) {
            x = (int) Math.round(xfl);
            if (y < 0 || y >= scan.length) continue;
//            drawPixel(x, y, z, false);

            if (drawTri) {
                if (scan[y][0] == Integer.MIN_VALUE) {
                    scan[y][0] = scan[y][1] = x;
                    scanZ[y][0] = scanZ[y][1] = z;
                } else {
                    if (x < scan[y][0]) {
                        scan[y][0] = x;
                        scanZ[y][0] = z;
                    }
                    if (x > scan[y][1]) {
                        scan[y][1] = x;
                        scanZ[y][1] = z;
                    }
                }
            }
        }
    }

    void drawTriangle(Triangle tri) {
        Vector4 normal = tri.normal().normalize();
        double shadeFactor = Math.max(normal.dotProduct(lightDirection), 0);
        int ar = (int) (0.5 * color.get(0));
        int ag = (int) (0.5 * color.get(1));
        int ab = (int) (0.5 * color.get(2));
        int dr = (int) (0.5 * shadeFactor * color.get(0));
        int dg = (int) (0.5 * shadeFactor * color.get(1));
        int db = (int) (0.5 * shadeFactor * color.get(2));
        int r = ar + dr;
        int g = ag + dg;
        int b = ab + db;

        tri.transform(lookAtMatrix);
        tri.transform(projectionMatrix);
        tri.divideByW();

        Vector4 v1 = tri.getV1();
        Vector4 v2 = tri.getV2();
        Vector4 v3 = tri.getV3();

        for (int i = 0; i < height; i++) {
            scan[i][0] = scan[i][1] = Integer.MIN_VALUE;
        }

        this.drawTri = true;
        drawLine(v1, v2);
        drawLine(v2, v3);
        drawLine(v3, v1);

        for (int y = 0; y < height; y++) {
            if (scan[y][0] == Integer.MIN_VALUE) continue;
            if (scan[y][0] == scan[y][1]) {
                drawPixel(scan[y][0], y, scanZ[y][0], r, g, b, false);
            } else {
                double m = (scanZ[y][1] - scanZ[y][0]) / (scan[y][1] - scan[y][0]);
                double z = (scanZ[y][0]);
                for (int x = scan[y][0]; x <= scan[y][1]; x++, z += m) {
                    drawPixel(x, y, z, r, g, b,false);
                }
            }
        }
        this.drawTri = false;
    }

    void drawSurface(Surface surface) {
        for (Triangle triangle : surface.getTris()) drawTriangle(triangle);
    }

    void drawMesh(Mesh mesh) {
        for (Surface surface : mesh.getSurfaces()) drawSurface(surface);
    }

    void createImage() {
        ctm = null;
        color = Color.WHITE;
        Scanner input = getFile();
        while (input.hasNext()) {
            String command = input.next();
            if (command.equals("DIM")) {
                width = input.nextInt() * 2;
                height = input.nextInt() * 2;
                imageSize = width * height;

                scan = new int[height][2];
                scanZ = new double[height][2];

                if (frameBuffer == null) {
                    frameBuffer = new FrameBuffer(width, height);
                } else if (frameBuffer.width() != width || frameBuffer.height() != height) {
                    frameBuffer.resize(width, height);
                } else {
                    frameBuffer.clear();
                }

                if (depthBuffer == null) {
                    depthBuffer = new DepthBuffer(width, height);
                } else if (depthBuffer.width() != width || depthBuffer.height() != height) {
                    depthBuffer.resize(width, height);
                } else {
                    depthBuffer.clear();
                }
            } else if (command.equals("LINE")) {
                initCurrentTransformationMatrix();
                Matrix4 scene = getSceneTransformation();
                Vector4 p1 = scene.mult(this.ctm.mult(new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble())));
                Vector4 p2 = scene.mult(this.ctm.mult(new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble())));
                drawLine(p1, p2);
            } else if (command.equals("RGB")) {
                color = Color.rgb(input.nextDouble(), input.nextDouble(), input.nextDouble());
            } else if (command.equals("TRI")) {
                initCurrentTransformationMatrix();
                scan = new int[height][2];

                Triangle tri = new Triangle(
                        new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()),
                        new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()),
                        new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble())
                );

                Matrix4 scene = getSceneTransformation();
                tri.transform(ctm);
                tri.transform(scene);
                drawTriangle(tri);
                scan = null;
            } else if (command.equals("LOAD_IDENTITY_MATRIX")) {
                ctm = Matrix4.createIdentityMatrix();
            } else if (command.equals("SCALE")) {
                initCurrentTransformationMatrix();
                double sx = input.nextDouble();
                double sy = input.nextDouble();
                double sz = input.nextDouble();
                Matrix4 matrix = Matrix4.createScalingMatrix(sx, sy, sz);
                ctm = matrix.mult(ctm);
            } else if (command.equals("ROTATEX")) {
                initCurrentTransformationMatrix();
                double degrees = input.nextDouble();
                Matrix4 matrix = Matrix4.createXRotationMatrix(degrees);
                ctm = matrix.mult(ctm);
            } else if (command.equals("ROTATEY")) {
                initCurrentTransformationMatrix();
                double degrees = input.nextDouble();
                Matrix4 matrix = Matrix4.createYRotationMatrix(degrees);
                ctm = matrix.mult(ctm);
            } else if (command.equals("ROTATEZ")) {
                initCurrentTransformationMatrix();
                double degrees = input.nextDouble();
                Matrix4 matrix = Matrix4.createZRotationMatrix(degrees);
                ctm = matrix.mult(ctm);
            } else if (command.equals("TRANSLATE")) {
                initCurrentTransformationMatrix();
                double tx = input.nextDouble();
                double ty = input.nextDouble();
                double tz = input.nextDouble();
                Matrix4 matrix = Matrix4.createTranslationMatrix(tx, ty, tz);
                ctm = matrix.mult(ctm);
            } else if (command.equals("WIREFRAME_CUBE")) {
                initCurrentTransformationMatrix();
                Matrix4 scene = getSceneTransformation();

                Vector4 lbf = scene.mult(this.ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_POS)));
                Vector4 lbr = scene.mult(this.ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_NEG)));
                Vector4 rbr = scene.mult(this.ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_NEG)));
                Vector4 rbf = scene.mult(this.ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_POS)));
                Vector4 ltf = scene.mult(this.ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_POS)));
                Vector4 ltr = scene.mult(this.ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_NEG)));
                Vector4 rtr = scene.mult(this.ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_NEG)));
                Vector4 rtf = scene.mult(this.ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_POS)));

                drawLine(lbf, lbr);
                drawLine(lbr, rbr);
                drawLine(rbr, rbf);
                drawLine(rbf, lbf);

                drawLine(lbf, ltf);
                drawLine(lbr, ltr);
                drawLine(rbr, rtr);
                drawLine(rbf, rtf);

                drawLine(ltf, ltr);
                drawLine(ltr, rtr);
                drawLine(rtr, rtf);
                drawLine(rtf, ltf);
            } else if (command.equals("SOLID_CUBE")) {
                initCurrentTransformationMatrix();
                Matrix4 scene = getSceneTransformation();
                Cube cube = new Cube();
                cube.transform(this.ctm);
                cube.transform(scene);

                drawMesh(cube);
            } else if (command.equals("LIGHT_DIRECTION")) {
                lightDirection = new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble(), 0.0).normalize();
            } else if (command.equals("LOOKAT")) {
                double ex = input.nextDouble();
                double ey = input.nextDouble();
                double ez = input.nextDouble();
                double cx = input.nextDouble();
                double cy = input.nextDouble();
                double cz = input.nextDouble();
                double ux = input.nextDouble();
                double uy = input.nextDouble();
                double uz = input.nextDouble();
                lookAtMatrix = createLookAtMatrix(ex, ey, ez, cx, cy, cz, ux, uy, uz);
            } else if (command.equals("ORTHO") || command.equals("FRUSTUM")) {
                ProjectionType type = ProjectionType.valueOf(command);
                double left = input.nextDouble();
                double right = input.nextDouble();
                double top = input.nextDouble();
                double bottom = input.nextDouble();
                double near = input.nextDouble();
                double far = input.nextDouble();

                if (type == ProjectionType.ORTHO) {
                    projectionMatrix = createOrthoProjectionMatrix(left, right, top, bottom, near, far);
                } else {
                    projectionMatrix = createFrustumProjectionMatrix(left, right, top, bottom, near, far);
                }
            }
        }
    }

    private Matrix4 createOrthoProjectionMatrix(double left, double right, double top, double bottom, double near, double far) {
        Matrix4 matrix = new Matrix4(new double[]{
                2.0 / (right - left), 0, 0, -(right + left) / (right - left),
                0, 2.0 / (top - bottom), 0, -(top + bottom) / (top - bottom),
                0, 0, -2.0 / (far - near), -(far + near) / (far - near),
                0, 0, 0, 1
        });

        return matrix;
    }

    private Matrix4 createFrustumProjectionMatrix(double left, double right, double top, double bottom, double near, double far) {
        Matrix4 matrix = new Matrix4(new double[]{
                2.0 * near / (right - left), 0, 0, -near * (right + left) / (right - left),
                0, 2.0 * near / (top - bottom), 0, -near * (top + bottom) / (top - bottom),
                0, 0, -(far + near) / (far - near), 2.0 * far * near / (near - far),
                0, 0, -1, 0
        });

        return matrix;
    }

    private Matrix4 createLookAtMatrix(double ex, double ey, double ez, double cx, double cy, double cz, double ux, double uy, double uz) {
        Vector4 eye = new Vector4(ex, ey, ez, 0);
        Vector4 center = new Vector4(cx, cy, cz, 0);
        Vector4 up = new Vector4(ux, uy, uz, 0);

        Vector4 n = eye.subtract(center).normalize();
        Vector4 u = up.crossProduct(n).normalize();
        Vector4 v = n.crossProduct(u).normalize();

        double tx = u.dotProduct(eye);
        double ty = v.dotProduct(eye);
        double tz = n.dotProduct(eye);

        Matrix4 matrix = new Matrix4(new double[]{
                u.x, u.y, u.z, -tx,
                v.x, v.y, v.z, -ty,
                n.x, n.y, n.z, -tz,
                0, 0, 0, 1
        });

        return matrix;
    }

    public void paintComponent(Graphics g) {
        createImage();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = image.getRaster();
        frameBuffer.draw(raster, 0, 0);
        g.drawImage(image, 0, 0, null);
    }

    public static void main(String args[]) {
        JFrame frame = new JFrame("LINE DEMO");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        selectFile();

        JPanel rootPane = new TCSS458Paint();
        getDim(rootPane);
        rootPane.setPreferredSize(new Dimension(width, height));

        frame.addKeyListener((KeyListener) rootPane);

        frame.getContentPane().add(rootPane);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }

    static File selectedFile = null;

    static private void selectFile() {
        int approve; //return value from JFileChooser indicates if the user hit cancel

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));

        approve = chooser.showOpenDialog(null);
        if (approve != JFileChooser.APPROVE_OPTION) {
            System.exit(0);
        } else {
            selectedFile = chooser.getSelectedFile();
        }
    }

    static private Scanner getFile() {
        Scanner input = null;
        try {
            input = new Scanner(selectedFile);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "There was an error with the file you chose",
                    "File Error", JOptionPane.ERROR_MESSAGE);
        }
        return input;
    }

    static void getDim(JPanel rootPane) {
        Scanner input = getFile();

        String command = input.next();
        if (command.equals("DIM")) {
            width = input.nextInt();
            height = input.nextInt();
            rootPane.setPreferredSize(new Dimension(width, height));
        }
    }

    static int worldToScreen(double pos, int bound) {
        return (int) Math.round(((pos + 1) * (bound - 1)) / 2.0);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            yRotate = getDegreeOffset(yRotate, -DEGREES_TO_ROTATE);
            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            yRotate = getDegreeOffset(yRotate, DEGREES_TO_ROTATE);
            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            xRotate = getDegreeOffset(xRotate, -DEGREES_TO_ROTATE);
            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            xRotate = getDegreeOffset(xRotate, DEGREES_TO_ROTATE);
            repaint();
        }
    }

    private int getDegreeOffset(int current, int amount) {
        if (current + amount < 0)
            current = 360 + (current + amount);
        else if (current + amount >= 360)
            current = (current + amount) % 360;
        else
            current += amount;
        return current;
    }

    private Matrix4 getSceneTransformation() {
        Matrix4 matrix = Matrix4.createIdentityMatrix();
        if (yRotate != 0)
            matrix = Matrix4.createYRotationMatrix(yRotate).mult(matrix);
        if (xRotate != 0)
            matrix = Matrix4.createXRotationMatrix(xRotate).mult(matrix);
        return matrix;
    }

    private void initCurrentTransformationMatrix() {
        if (ctm == null)
            ctm = Matrix4.createIdentityMatrix();
    }
}
