package GUI;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import Executor.DynamicProgramming;
import Executor.ImageProcessor;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.*;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class ProcessSceneController implements Initializable {
    @FXML private ImageView imageView;
    @FXML private Button SeedPointButton;
    @FXML private Button TargetPointButton;
    @FXML private Button ExportButton;
    @FXML private Canvas canvas;

    private Image currentImage;
    public double[][] greyarray;
    public ImageProcessor imageProcessor;
    public DynamicProgramming dynamicProgramming;
    public double[][] cost;
    private Point2D seedPoint;
    private Point2D targetPoint;
    private Point2D[] imageCorners = new Point2D[4];

    // 路径相关
    private boolean isAddingTargets = false;
    private List<Point2D> pathPoints = new ArrayList<>();
    private List<int[][]> committedPaths = new ArrayList<>();
    private int[][] closingPath = null;

    // 路径冷却机制
    private Point2D lastPreviewPoint = null;
    private static final double PREVIEW_MOVE_THRESHOLD = 5.0;

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

        // 动态设置 Canvas 和 ImageView 大小与图片一致
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);
        canvas.setWidth(imageWidth);
        canvas.setHeight(imageHeight);

        // 居中显示
        double paneWidth = 1038.0; // AnchorPane 的宽度
        double paneHeight = 675.0; // AnchorPane 的高度
        double layoutX = (paneWidth - imageWidth) / 2;
        double layoutY = (paneHeight - imageHeight) / 2;
        imageView.setLayoutX(layoutX);
        imageView.setLayoutY(layoutY);
        canvas.setLayoutX(layoutX);
        canvas.setLayoutY(layoutY);

        this.greyarray = imageToDoubleArray(currentImage);
        this.imageProcessor = new ImageProcessor(this.greyarray);
        this.cost = imageProcessor.cost;
        this.dynamicProgramming = new DynamicProgramming(imageProcessor);
        calculateImageCorners();
        clearCanvas();
        System.out.println("图片加载: " + imageWidth + "x" + imageHeight);
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
        // 重置所有状态
        seedPoint = null;
        targetPoint = null;
        isAddingTargets = false;
        pathPoints.clear();
        committedPaths.clear();
        closingPath = null;
        lastPreviewPoint = null;
        clearCanvas();
        canvas.setOnMouseClicked(null);
        canvas.setOnMouseMoved(null);

        System.out.println("jaja");
        canvas.setOnMouseClicked(mouseEvent -> {
            System.out.println("ja21");
            try {
                double mouseX = mouseEvent.getX();
                double mouseY = mouseEvent.getY();
                Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
                int x = (int) Math.round(imagePoint.getX());
                int y = (int) Math.round(imagePoint.getY());
                // 严格限制在图片范围内
                if (x < 0 || x >= currentImage.getWidth() || y < 0 || y >= currentImage.getHeight()) {
                    System.out.println("点击超出图片范围: (" + x + ", " + y + ")");
                    return;
                }
                seedPoint = new Point2D(x, y);
                pathPoints.add(seedPoint);
                drawPoint(x, y, Color.LIMEGREEN);
                dynamicProgramming.computePaths(x, y);
                canvas.setOnMouseClicked(null);
                System.out.println("种子点设置: (" + x + ", " + y + ")");
            } catch (Exception e) {
                System.out.println("setSeedPoint 错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void setTargetPoint(ActionEvent event) {
        if (seedPoint == null) {
            System.out.println("请先选择种子点");
            return;
        }

        isAddingTargets = !isAddingTargets;

        if (isAddingTargets) {
            System.out.println("进入连续目标点添加模式（支持实时预览）");
            canvas.setOnMouseClicked(null);
            canvas.setOnMouseMoved(null);

            // 注册鼠标移动事件：实时预览路径
            canvas.setOnMouseMoved(mouseEvent -> {
                if (pathPoints.isEmpty()) return;

                double mouseX = mouseEvent.getX();
                double mouseY = mouseEvent.getY();
                Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
                int x = (int) Math.round(imagePoint.getX());
                int y = (int) Math.round(imagePoint.getY());
                // 限制在图片范围内
                if (x < 0 || x >= currentImage.getWidth() || y < 0 || y >= currentImage.getHeight()) {
                    return;
                }

                Point2D currentPoint = new Point2D(x, y);
                if (lastPreviewPoint != null && currentPoint.distance(lastPreviewPoint) < PREVIEW_MOVE_THRESHOLD) {
                    return;
                }
                lastPreviewPoint = currentPoint;

                // 清屏并重绘
                clearCanvas();
                for (int[][] segment : committedPaths) {
                    drawPath(segment);
                }
                for (Point2D point : pathPoints) {
                    drawPoint((int) point.getX(), (int) point.getY(), Color.LIMEGREEN);
                }
                if (closingPath != null) {
                    drawPath(closingPath);
                }

                // 异步计算预览路径（从最后一个锚点到鼠标位置）
                Point2D lastPoint = pathPoints.get(pathPoints.size() - 1);
                int finalX = x;
                int finalY = y;
                Task<int[][]> previewTask = new Task<>() {
                    @Override
                    protected int[][] call() {
                        dynamicProgramming.computePaths((int) lastPoint.getX(), (int) lastPoint.getY());
                        return dynamicProgramming.getPath(x, y);
                    }
                };
                previewTask.setOnSucceeded(e -> {
                    clearCanvas();
                    for (int[][] segment : committedPaths) {
                        drawPath(segment);
                    }
                    for (Point2D point : pathPoints) {
                        drawPoint((int) point.getX(), (int) point.getY(), Color.LIMEGREEN);
                    }
                    if (closingPath != null) {
                        drawPath(closingPath);
                    }
                    drawPath(previewTask.getValue());
                });
                new Thread(previewTask).start();
            });

            // 注册鼠标点击事件：固定路径段
            canvas.setOnMouseClicked(mouseEvent -> {
                try {
                    double mouseX = mouseEvent.getX();
                    double mouseY = mouseEvent.getY();
                    Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
                    int x = (int) Math.round(imagePoint.getX());
                    int y = (int) Math.round(imagePoint.getY());
                    // 严格限制在图片范围内
                    if (x < 0 || x >= currentImage.getWidth() || y < 0 || y >= currentImage.getHeight()) {
                        System.out.println("点击超出图片范围: (" + x + ", " + y + ")");
                        return;
                    }

                    Point2D newTarget = new Point2D(x, y);
                    Point2D lastPoint = pathPoints.get(pathPoints.size() - 1);
                    pathPoints.add(newTarget);
                    drawPoint(x, y, Color.LIMEGREEN);

                    // 计算并固定路径段（从上一个点到当前点）
                    int finalX1 = x;
                    int finalY1 = y;
                    Task<int[][]> task = new Task<>() {
                        @Override
                        protected int[][] call() {
                            dynamicProgramming.computePaths((int) lastPoint.getX(), (int) lastPoint.getY());
                            return dynamicProgramming.getPath(x, y);
                        }
                    };
                    int finalX = x;
                    int finalY = y;
                    task.setOnSucceeded(e -> {
                        int[][] pathSegment = task.getValue();
                        committedPaths.add(pathSegment);
                        clearCanvas();
                        for (int[][] segment : committedPaths) {
                            drawPath(segment);
                        }
                        for (Point2D point : pathPoints) {
                            drawPoint((int) point.getX(), (int) point.getY(), Color.LIMEGREEN);
                        }
                        if (closingPath != null) {
                            drawPath(closingPath);
                        }
                        System.out.println("目标点添加: (" + x + ", " + y + "), 路径段数量: " + committedPaths.size());
                    });
                    new Thread(task).start();

                    // 更新种子点
                    seedPoint = newTarget;
                } catch (Exception e) {
                    System.out.println("setTargetPoint 点击错误: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("退出添加模式，生成闭合路径");
            canvas.setOnMouseMoved(null);
            canvas.setOnMouseClicked(null);
            lastPreviewPoint = null;

            if (pathPoints.size() > 1) {
                Point2D lastPoint = pathPoints.get(pathPoints.size() - 1);
                Task<int[][]> closeTask = new Task<>() {
                    @Override
                    protected int[][] call() {
                        dynamicProgramming.computePaths((int) lastPoint.getX(), (int) lastPoint.getY());
                        return dynamicProgramming.getPath((int) pathPoints.get(0).getX(), (int) pathPoints.get(0).getY());
                    }
                };
                closeTask.setOnSucceeded(e -> {
                    closingPath = closeTask.getValue();
                    committedPaths.add(closingPath);
                    clearCanvas();
                    for (int[][] segment : committedPaths) {
                        drawPath(segment);
                    }
                    for (Point2D point : pathPoints) {
                        drawPoint((int) point.getX(), (int) point.getY(), Color.LIMEGREEN);
                    }
                    System.out.println("闭合路径生成，路径段总数: " + committedPaths.size());
                });
                new Thread(closeTask).start();
            }
        }
    }

    @FXML
    private void exportCroppedRegion(ActionEvent event) {
        if (currentImage == null || pathPoints.size() < 2 || committedPaths.isEmpty()) {
            System.out.println("路径未闭合或路径点不足，无法导出抠图");
            return;
        }

        int width = (int) currentImage.getWidth();
        int height = (int) currentImage.getHeight();

        // 使用 committedPaths 构造完整路径
        List<int[]> fullPath = new ArrayList<>();
        for (int[][] segment : committedPaths) {
            fullPath.addAll(Arrays.asList(segment));
        }

        // 初始化遮罩
        boolean[][] mask = new boolean[height][width];
        for (int[] p : fullPath) {
            int x = p[0], y = p[1];
            if (x >= 0 && x < width && y >= 0 && y < height) {
                mask[y][x] = true;
            }
        }

        // 扫描线填充
        for (int y = 0; y < height; y++) {
            boolean inside = false;
            for (int x = 0; x < width; x++) {
                if (mask[y][x]) {
                    inside = !inside;
                }
                if (inside) {
                    mask[y][x] = true;
                }
            }
        }

        // 创建透明图像
        WritableImage cropped = new WritableImage(width, height);
        PixelReader reader = currentImage.getPixelReader();
        PixelWriter writer = cropped.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (mask[y][x]) {
                    writer.setColor(x, y, color);
                } else {
                    writer.setColor(x, y, new Color(0, 0, 0, 0));
                }
            }
        }

        // 保存图像
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出抠图区域");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG 图片", "*.png"));
        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(cropped, null), "png", file);
                System.out.println("图像保存成功: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Point2D canvasToImageCoordinates(double canvasX, double canvasY) {
        if (currentImage == null) {
            System.out.println("错误: currentImage 为 null");
            return new Point2D(0, 0);
        }
        // Canvas 和图片大小一致，直接映射
        double imageX = canvasX;
        double imageY = canvasY;
        imageX = Math.max(0, Math.min(imageX, currentImage.getWidth() - 1));
        imageY = Math.max(0, Math.min(imageY, currentImage.getHeight() - 1));
        return new Point2D(imageX, imageY);
    }

    private void drawPoint(int imageX, int imageY, Color color) {
        if (canvas == null || currentImage == null) {
            System.out.println("错误: Canvas 或 currentImage 为 null");
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color);
        // 直接使用图片坐标（Canvas 大小与图片一致）
        gc.fillOval(imageX - 2, imageY - 2, 4, 4);
    }

    private void drawPath(int[][] path) {
        if (path == null || path.length == 0) {
            System.out.println("无有效路径");
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.LIMEGREEN);
        gc.setStroke(Color.LIMEGREEN);
        gc.setLineWidth(0.5);
        for (int i = 0; i < path.length; i++) {
            int imageX = path[i][0];
            int imageY = path[i][1];
            // 确保路径点在图片范围内
            if (imageX < 0 || imageX >= currentImage.getWidth() || imageY < 0 || imageY >= currentImage.getHeight()) {
                continue;
            }
            gc.fillOval(imageX - 2, imageY - 2, 4, 4);
            if (i > 0) {
                int prevX = path[i - 1][0];
                int prevY = path[i - 1][1];
                if (prevX < 0 || prevX >= currentImage.getWidth() || prevY < 0 || prevY >= currentImage.getHeight()) {
                    continue;
                }
                gc.strokeLine(prevX, prevY, imageX, imageY);
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