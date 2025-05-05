package GUI;

import Executor.DynamicProgramming;
import Executor.ImageProcessor;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import java.net.URL;
import java.util.ResourceBundle;

public class ProcessSceneController implements Initializable {
    @FXML private ImageView imageView;
    @FXML private Button SeedPointButton;
    @FXML private Button TargetPointButton;
    @FXML private Canvas canvas;

    private Image currentImage;
    public double[][] greyarray;
    public ImageProcessor imageProcessor;
    public DynamicProgramming dynamicProgramming;
    public double[][] cost;
    private Point2D seedPoint;
    private Point2D targetPoint;
    private Point2D[] imageCorners = new Point2D[4];

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("SeedPointButton: " + SeedPointButton);
        System.out.println("imageView: " + imageView);
        System.out.println("canvas: " + canvas);
        if (canvas != null) {
            canvas.getGraphicsContext2D().setFill(Color.TRANSPARENT);
            clearCanvas();
        }
    }

    public void initData(Image image) {
        this.currentImage = image;
        imageView.setImage(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(400);
        imageView.setFitHeight(400);
        this.greyarray = imageToDoubleArray(currentImage);
        this.imageProcessor = new ImageProcessor(this.greyarray);
        this.cost = imageProcessor.cost;
        this.dynamicProgramming = new DynamicProgramming(imageProcessor);
        calculateImageCorners();
        clearCanvas();
    }

    private void calculateImageCorners() {
        double width = currentImage.getWidth();
        double height = currentImage.getHeight();
        imageCorners[0] = new Point2D(0, 0);
        imageCorners[1] = new Point2D(width - 1, 0);
        imageCorners[2] = new Point2D(width - 1, height - 1);
        imageCorners[3] = new Point2D(0, height - 1);
        System.out.println("图片四角坐标：");
        for (int i = 0; i < 4; i++) {
            System.out.printf("角%d: (%.1f, %.1f)%n", i + 1, imageCorners[i].getX(), imageCorners[i].getY());
        }
    }

    public double[][] imageToDoubleArray(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader pixelReader = image.getPixelReader();
        double[][] result = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                double grayValue = (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114);
                result[y][x] = grayValue;
            }
        }
        return result;
    }

    @FXML
    private void setSeedPoint(ActionEvent event) {
        System.out.println("jaja");
        seedPoint = null;
        clearCanvas();
        canvas.setOnMouseClicked(null);
        System.out.println("注册 Canvas 鼠标点击事件以选择种子点");
        canvas.setOnMouseClicked(mouseEvent -> {
            System.out.println("ja21");
            try {
                double mouseX = mouseEvent.getX();
                double mouseY = mouseEvent.getY();
                System.out.println("Canvas 点击坐标: (" + mouseX + ", " + mouseY + ")");
                Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
                int x = (int) Math.round(imagePoint.getX());
                int y = (int) Math.round(imagePoint.getY());
                x = Math.max(0, Math.min(x, (int) currentImage.getWidth() - 1));
                y = Math.max(0, Math.min(y, (int) currentImage.getHeight() - 1));
                seedPoint = new Point2D(x, y);
                System.out.printf("选择的种子点: (%d, %d)%n", x, y);
                drawPoint(x, y, Color.BLACK);
                canvas.setOnMouseClicked(null);
                imageProcessor.computeEdgeCost(x, y);
            } catch (Exception e) {
                System.out.println("setSeedPoint 错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void setTargetPoint(ActionEvent event) {
        targetPoint = null;
        clearCanvas();
        if (seedPoint != null) {
            drawPoint((int) seedPoint.getX(), (int) seedPoint.getY(), Color.BLACK);
        }
        canvas.setOnMouseClicked(null);
        System.out.println("注册 Canvas 鼠标点击事件以选择目标点");
        canvas.setOnMouseClicked(mouseEvent -> {
            try {
                double mouseX = mouseEvent.getX();
                double mouseY = mouseEvent.getY();
                System.out.println("Canvas 点击坐标: (" + mouseX + ", " + mouseY + ")");
                Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
                int x = (int) Math.round(imagePoint.getX());
                int y = (int) Math.round(imagePoint.getY());
                x = Math.max(0, Math.min(x, (int) currentImage.getWidth() - 1));
                y = Math.max(0, Math.min(y, (int) currentImage.getHeight() - 1));
                targetPoint = new Point2D(x, y);
                System.out.printf("选择的目标点: (%d, %d)%n", x, y);
                drawPoint(x, y, Color.BLACK);
                if (seedPoint != null) {
                    int seedX = (int) seedPoint.getX();
                    int seedY = (int) seedPoint.getY();
                    dynamicProgramming.computePaths(seedX, seedY);
                    int[][] path = dynamicProgramming.getPath(x, y);
                    drawPath(path);
                }
                canvas.setOnMouseClicked(null);
            } catch (Exception e) {
                System.out.println("setTargetPoint 错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private Point2D imageViewToImageCoordinates(double mouseX, double mouseY) {
        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();
        double viewWidth = imageView.getFitWidth();
        double viewHeight = imageView.getFitHeight();
        double scaleX = imageWidth / viewWidth;
        double scaleY = imageHeight / viewHeight;
        double imageX = mouseX * scaleX;
        double imageY = mouseY * scaleY;
        imageX = Math.max(0, Math.min(imageX, imageWidth - 1));
        imageY = Math.max(0, Math.min(imageY, imageHeight - 1));
        return new Point2D(imageX, imageY);
    }

    private Point2D canvasToImageCoordinates(double canvasX, double canvasY) {
        if (currentImage == null) {
            System.out.println("错误: currentImage 为 null");
            return new Point2D(0, 0);
        }
        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        double scaleX = imageWidth / canvasWidth;
        double scaleY = imageHeight / canvasHeight;
        double imageX = canvasX * scaleX;
        double imageY = canvasY * scaleY;
        imageX = Math.max(0, Math.min(imageX, imageWidth - 1));
        imageY = Math.max(0, Math.min(imageY, imageHeight - 1));
        return new Point2D(imageX, imageY);
    }

    private void drawPoint(int imageX, int imageY, Color color) {
        if (canvas == null || currentImage == null) {
            System.out.println("错误: Canvas 或 currentImage 为 null");
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color);
        double canvasX = imageX * (canvas.getWidth() / currentImage.getWidth());
        double canvasY = imageY * (canvas.getHeight() / currentImage.getHeight());
        gc.fillOval(canvasX - 2, canvasY - 2, 4, 4);
    }

    private void drawPath(int[][] path) {
        if (path == null || path.length == 0) {
            System.out.println("无有效路径");
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        for (int i = 0; i < path.length; i++) {
            int imageX = path[i][0];
            int imageY = path[i][1];
            double canvasX = imageX * (canvas.getWidth() / currentImage.getWidth());
            double canvasY = imageY * (canvas.getHeight() / currentImage.getHeight());
            gc.fillOval(canvasX - 2, canvasY - 2, 4, 4);
            if (i > 0) {
                int prevX = path[i - 1][0];
                int prevY = path[i - 1][1];
                double prevCanvasX = prevX * (canvas.getWidth() / currentImage.getWidth());
                double prevCanvasY = prevY * (canvas.getHeight() / currentImage.getHeight());
                gc.strokeLine(prevCanvasX, prevCanvasY, canvasX, canvasY);
            }
        }
    }

    private void clearCanvas() {
        if (canvas != null) {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
    }
}