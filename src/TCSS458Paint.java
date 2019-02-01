import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.*;
import javax.swing.*;

import java.io.*;
import java.util.*;


public class TCSS458Paint extends JPanel implements KeyListener {

    static final double UNICUBE_POS = .5;
    static final double UNICUBE_NEG = -.5;
    static final int DEGREES_TO_ROTATE = 3;

    static int width;
    static int height;

    int imageSize;
    int[] pixels;
    Vector4[][] scan;
    Double[][] zbuffer;
    RGB color;
    Matrix4 ctm;
    int xRotate = 0;
    int yRotate = 0;

    void drawPixel(int x, int y, double z, int r, int g, int b) {
        if (x >= width || y >= height || x < 0 || y < 0)
            return;

        if (zbuffer[x][y] == null || z > zbuffer[x][y]) {
            zbuffer[x][y] = z;
            pixels[(height - y - 1) * width * 3 + x * 3] = r;
            pixels[(height - y - 1) * width * 3 + x * 3 + 1] = g;
            pixels[(height - y - 1) * width * 3 + x * 3 + 2] = b;
        }
    }

    void drawLine(Vector4 p1, Vector4 p2) {
        if (Math.abs(p2.getY() - p1.getY()) < Math.abs(p2.getX() - p1.getX())) {
            if (p1.getX() > p2.getX()) {
                drawLineLow(p2, p1);
            } else {
                drawLineLow(p1, p2);
            }
        } else {
            if (p1.getY() > p2.getY()) {
                drawLineHigh(p2, p1);
            } else {
                drawLineHigh(p1, p2);
            }
        }
    }

    void drawLineLow(Vector4 p1, Vector4 p2) {
        int x1 = worldToScreen(p1.getX(), width);
        int y1 = worldToScreen(p1.getY(), height);
        int x2 = worldToScreen(p2.getX(), width);
        int y2 = worldToScreen(p2.getY(), height);

        int deltaX = x2 - x1;
        int deltaY = y2 - y1;
        double deltaZ = (p2.getZ() - p1.getZ()) / deltaX;
        int multY = 1;

        if (deltaY < 0) {
            multY = -1;
            deltaY = -deltaY;
        }

        int weight = 2 * deltaY - deltaX;
        int y = y1;

        double curZ = p1.getZ();
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
                    drawPixel(x, y, curZ, color.getRed(), color.getGreen(), color.getBlue());
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

    void drawLineHigh(Vector4 p1, Vector4 p2) {
        int x1 = worldToScreen(p1.getX(), width);
        int y1 = worldToScreen(p1.getY(), height);
        int x2 = worldToScreen(p2.getX(), width);
        int y2 = worldToScreen(p2.getY(), height);

        int deltaX = x2 - x1;
        int deltaY = y2 - y1;
        double deltaZ = (p2.getZ() - p1.getZ()) / deltaY;
        int multX = 1;

        if (deltaX < 0) {
            multX = -1;
            deltaX = -deltaX;
        }

        int weight = 2 * deltaX - deltaY;
        int x = x1;

        double curZ = p1.getZ();
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
                    drawPixel(x, y, curZ, color.getRed(), color.getGreen(), color.getBlue());
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

    void drawTriangle(Vector4 p1, Vector4 p2, Vector4 p3) {
        drawLine(p1, p2);
        drawLine(p2, p3);
        drawLine(p3, p1);

        int y0 = worldToScreen(p1.getY(), height);
        int y1 = worldToScreen(p2.getY(), height);
        int y2 = worldToScreen(p3.getY(), height);

        int yMin = (y0 < y1 && y0 < y2) ? y0 : (y1 < y2) ? y1 : y2;
        int yMax = (y0 > y1 && y0 > y2) ? y0 : (y1 > y2) ? y1 : y2;

        for (int y = yMin; y <= yMax; y++) {
            if (y < 0 || y >= height)
                continue;

            Vector4[] row = scan[y];

            if (row[0] == null || row[1] == null)
                continue;

            double deltaZ = (row[1].getZ() - row[0].getZ()) / (row[1].getX() - row[0].getX());
            double curZ = row[0].getZ();
            for (int x = row[0].getX().intValue(); x <= row[1].getX().intValue(); x++) {
                drawPixel(x, y, curZ, color.getRed(), color.getGreen(), color.getBlue());
                curZ += deltaZ;
            }
        }
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
                zbuffer = new Double[width][height];

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        drawPixel(x, y, Double.NEGATIVE_INFINITY, 255, 255, 255);
                    }
                }
            } else if (command.equals("LINE")) {
                initCurrentTransformationMatrix();
                Matrix4 ctm = applySceneRotations();
                Vector4 p1 = ctm.mult(new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()));
                Vector4 p2 = ctm.mult(new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()));
                drawLine(p1, p2);
            } else if (command.equals("RGB")) {
                color = RGB.of(input.nextDouble(), input.nextDouble(), input.nextDouble());
            } else if (command.equals("TRI")) {
                initCurrentTransformationMatrix();
                scan = new Vector4[height][2];
                Matrix4 ctm = applySceneRotations();
                Vector4 p1 = ctm.mult(new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()));
                Vector4 p2 = ctm.mult(new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()));
                Vector4 p3 = ctm.mult(new Vector4(input.nextDouble(), input.nextDouble(), input.nextDouble()));
                drawTriangle(p1, p2, p3);
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

                Vector4 blf = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_POS));
                Vector4 blr = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_NEG));
                Vector4 brr = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_NEG));
                Vector4 brf = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_POS));
                Vector4 tlf = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_POS));
                Vector4 tlr = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_NEG));
                Vector4 trr = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_NEG));
                Vector4 trf = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_POS));

                drawLine(blf, blr);
                drawLine(blr, brr);
                drawLine(brr, brf);
                drawLine(brf, blf);

                drawLine(blf, tlf);
                drawLine(blr, tlr);
                drawLine(brr, trr);
                drawLine(brf, trf);

                drawLine(tlf, tlr);
                drawLine(tlr, trr);
                drawLine(trr, trf);
                drawLine(trf, tlf);
            } else if (command.equals("SOLID_CUBE")) {
                initCurrentTransformationMatrix();
                Matrix4 ctm = applySceneRotations();

                Vector4 blf = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_POS));
                Vector4 blr = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_NEG, UNICUBE_NEG));
                Vector4 brr = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_NEG));
                Vector4 brf = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_NEG, UNICUBE_POS));
                Vector4 tlf = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_POS));
                Vector4 tlr = ctm.mult(new Vector4(UNICUBE_NEG, UNICUBE_POS, UNICUBE_NEG));
                Vector4 trr = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_NEG));
                Vector4 trf = ctm.mult(new Vector4(UNICUBE_POS, UNICUBE_POS, UNICUBE_POS));

                scan = new Vector4[height][2];
                // Bottom
                drawTriangle(blf, blr, brr);
                drawTriangle(brr, brf, blr);

                // Rear
                drawTriangle(blr, brr, trr);
                drawTriangle(trr, tlr, blr);

                // Left
                drawTriangle(blr, blf, tlf);
                drawTriangle(tlf, tlr, blr);

                // Right
                drawTriangle(brr, brf, trf);
                drawTriangle(trf, trr, brr);

                // Front
                drawTriangle(brf, blf, tlf);
                drawTriangle(tlf, trf, brf);

                // Top
                drawTriangle(trr, trf, tlf);
                drawTriangle(tlf, tlr, trr);
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
