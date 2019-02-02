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
    private int[] pixels;
    private Vector4[][] scan;
    private DepthBuffer depthBuffer;
    private Color.RGB color;
    private Matrix4 ctm;
    private int xRotate = 0;
    private int yRotate = 0;

    void drawPixel(int x, int y, double z, int r, int g, int b, boolean force) {
        if (x >= width || y >= height || x < 0 || y < 0)
            return;

        if (depthBuffer.set(x, y, z) || force) {
            int ri = (height - y - 1) * width * 3 + x * 3;
            int gi = ri + 1;
            int bi = ri + 2;

            pixels[ri] = r;
            pixels[gi] = g;
            pixels[bi] = b;
        }
    }

    void drawLine(Vector4 v1, Vector4 v2) {
        if (Math.abs(v2.getY() - v1.getY()) < Math.abs(v2.getX() - v1.getX())) {
            if (v1.getX() > v2.getX()) {
                drawLineLow(v2, v1);
            } else {
                drawLineLow(v1, v2);
            }
        } else {
            if (v1.getY() > v2.getY()) {
                drawLineHigh(v2, v1);
            } else {
                drawLineHigh(v1, v2);
            }
        }
    }

    void drawLineLow(Vector4 v1, Vector4 v2) {
        int x1 = worldToScreen(v1.getX(), width);
        int y1 = worldToScreen(v1.getY(), height);
        int x2 = worldToScreen(v2.getX(), width);
        int y2 = worldToScreen(v2.getY(), height);

        int deltaX = x2 - x1;
        int deltaY = y2 - y1;
        double deltaZ = (v2.getZ() - v1.getZ()) / deltaX;
        int multY = 1;

        if (deltaY < 0) {
            multY = -1;
            deltaY = -deltaY;
        }

        int weight = 2 * deltaY - deltaX;
        int y = y1;

        double curZ = v1.getZ();
        for (int x = x1; x <= x2; x++) {
            if (!(x < 0 || x >= width || y < 0 || y >= height)) {
                if (scan != null) {
                    Vector4[] row = scan[y];
                    if (row[0] == null) {
                        row[0] = new Vector4(x, y, curZ);
                        row[1] = new Vector4(x, y, curZ);
                    } else {
                        if (x < row[0].getX())
                            row[0] = new Vector4(x, y, curZ);
                        else if (x > row[1].getX())
                            row[1] = new Vector4(x, y, curZ);
                    }
                } else {
                    drawPixel(x, y, curZ, color.getRed(), color.getGreen(), color.getBlue(), false);
                }
            }

            if (weight > 0) {
                y = y + multY;
                weight = weight - 2 * deltaX;
            }
            weight = weight + 2 * deltaY;
            curZ += deltaZ;
        }
    }

    void drawLineHigh(Vector4 v1, Vector4 v2) {
        int x1 = worldToScreen(v1.getX(), width);
        int y1 = worldToScreen(v1.getY(), height);
        int x2 = worldToScreen(v2.getX(), width);
        int y2 = worldToScreen(v2.getY(), height);

        int deltaX = x2 - x1;
        int deltaY = y2 - y1;
        double deltaZ = (v2.getZ() - v1.getZ()) / deltaY;
        int multX = 1;

        if (deltaX < 0) {
            multX = -1;
            deltaX = -deltaX;
        }

        int weight = 2 * deltaX - deltaY;
        int x = x1;

        double curZ = v1.getZ();
        for (int y = y1; y <= y2; y++) {
            if (!(x < 0 || x >= width || y < 0 || y >= height)) {
                if (scan != null) {
                    Vector4[] row = scan[y];
                    if (row[0] == null) {
                        row[0] = new Vector4(x, y, curZ);
                        row[1] = new Vector4(x, y, curZ);
                    } else {
                        if (x < row[0].getX())
                            row[0] = new Vector4(x, y, curZ);
                        else if (x > row[1].getX())
                            row[1] = new Vector4(x, y, curZ);
                    }
                } else {
                    drawPixel(x, y, curZ, color.getRed(), color.getGreen(), color.getBlue(), false);
                }
            }

            if (weight > 0) {
                x = x + multX;
                weight = weight - 2 * deltaY;
            }
            weight = weight + 2 * deltaX;
            curZ += deltaZ;
        }
    }

    void drawTriangle(Triangle tri) {
        Vector4 v1 = tri.getV1();
        Vector4 v2 = tri.getV2();
        Vector4 v3 = tri.getV3();

        drawLine(v1, v2);
        drawLine(v2, v3);
        drawLine(v3, v1);

        int y1 = worldToScreen(v1.getY(), height);
        int y2 = worldToScreen(v2.getY(), height);
        int y3 = worldToScreen(v3.getY(), height);

        int yMin = Math.max(0, Math.min(y1, Math.min(y2, y3)));
        int yMax = Math.min(height - 1, Math.max(y1, Math.max(y2, y3)));

        for (int y = yMin; y <= yMax; y++) {
            if (y < 0 || y >= height)
                continue;

            Vector4[] row = scan[y];

            if (row[0] == null || row[1] == null)
                continue;

            Vector4 vL = row[0];
            Vector4 vR = row[1];

            double deltaZ = (vR.z - vL.z) / (vR.x - vL.x);
            double curZ = vL.z;
            for (int x = vL.x.intValue(); x <= vR.x.intValue(); x++) {
                drawPixel(x, y, curZ, color.getRed(), color.getGreen(), color.getBlue(), false);
                curZ += deltaZ;
            }
        }
    }

    void drawSurface(Surface surface) {
        for (Triangle triangle : surface.getTris()) drawTriangle(triangle);
    }

    void drawMesh(Mesh mesh) {
        for (Surface surface : mesh.getSurfaces()) drawSurface(surface);
    }

    void createImage() {
        ctm = null;
        Scanner input = getFile();
        while (input.hasNext()) {
            String command = input.next();
            if (command.equals("DIM")) {
                width = input.nextInt();
                height = input.nextInt();
                imageSize = width * height;
                pixels = new int[imageSize * 3];

                if (depthBuffer == null || depthBuffer.width() != width || depthBuffer.height() != height) {
                    depthBuffer = new DepthBuffer(width, height);
                } else {
                    depthBuffer.clear();
                }

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        drawPixel(x, y, Double.NEGATIVE_INFINITY, 255, 255, 255, true);
                    }
                }
            } else if (command.equals("LINE")) {
                initCurrentTransformationMatrix();
                Matrix4 ctm = applySceneRotations();
                Vector4 p1 = ctm.mult(new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()));
                Vector4 p2 = ctm.mult(new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()));
                drawLine(p1, p2);
            } else if (command.equals("RGB")) {
                color = Color.rgb(input.nextDouble(), input.nextDouble(), input.nextDouble());
            } else if (command.equals("TRI")) {
                initCurrentTransformationMatrix();
                scan = new Vector4[height][2];

                Triangle tri = new Triangle(
                        new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()),
                        new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()),
                        new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble())
                );

                Matrix4 ctm = applySceneRotations();
                tri.transform(ctm);
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
                Matrix4 ctm = applySceneRotations();

                Vector4 lbf = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_POS));
                Vector4 lbr = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_NEG));
                Vector4 rbr = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_NEG));
                Vector4 rbf = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_POS));
                Vector4 ltf = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_POS));
                Vector4 ltr = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_NEG));
                Vector4 rtr = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_NEG));
                Vector4 rtf = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_POS));

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
                Matrix4 ctm = applySceneRotations();

                Cube cube = new Cube();
                cube.transform(ctm);

                scan = new Vector4[height][2];
                drawMesh(cube);
                scan = null;
            }
        }
    }

    public void paintComponent(Graphics g) {
        createImage();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = image.getRaster();
        raster.setPixels(0, 0, width, height, pixels);
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

    static Integer worldToScreen(double pos, int bound) {
        return Double.valueOf((bound - 1) * (pos + 1) / 2).intValue();
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

    private Matrix4 applySceneRotations() {
        Matrix4 matrix = ctm;
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
