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
    private int xRotate = 0;
    private int yRotate = 0;
    private boolean drawTri = false;

    void drawPixel(int x, int y, double z, boolean force) {
        if (x >= width || y >= height || x < 0 || y < 0)
            return;

        if (depthBuffer.set(x, y, z) || force) {
            frameBuffer.set(x, y, color);
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
            drawPixel(x, y, z, false);

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
            drawPixel(x, y, z, false);

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
                drawPixel(scan[y][0], y, scanZ[y][0], false);
            } else {
                double m = (scanZ[y][1] - scanZ[y][0]) / (scan[y][1] - scan[y][0]);
                double z = (scanZ[y][0]);
                for (int x = scan[y][0]; x <= scan[y][1]; x++, z += m) {
                    drawPixel(x, y, z, false);
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
                width = input.nextInt();
                height = input.nextInt();
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
            }
        }
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
