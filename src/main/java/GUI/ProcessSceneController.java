package GUI;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import Executor.DynamicProgramming;
import Executor.ImageProcessor;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

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
        //
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(this::handleKeyInput);
        //
        System.out.println("SeedPointButton: " + SeedPointButton);
        System.out.println("imageView: " + imageView);
        System.out.println("canvas: " + canvas);
        if (canvas != null) {
            canvas.getGraphicsContext2D().setFill(Color.TRANSPARENT);
            clearCanvas();
        }
    }
    private void handleKeyInput(KeyEvent event) {
        if (event.getCode() ==  KeyCode.PLUS || event.getText().equals("+") ){
            enterAddAnchorMode();  // 启动添加模式
        } else if (event.getCode() == KeyCode.MINUS || event.getText().equals("-")) {
            exitAddAnchorMode();   // 结束并闭合路径
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
                canvas.requestFocus();  // 🔑 获取键盘焦点
            } catch (Exception e) {
                System.out.println("setSeedPoint 错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }



//    @FXML
//    private void setTargetPoint(ActionEvent event) {
//        if (seedPoint == null) {
//            System.out.println("请先选择种子点");
//            return;
//        }
//
//        isAddingTargets = !isAddingTargets;
//
//        if (isAddingTargets) {
//            System.out.println("进入连续目标点添加模式（支持实时预览）");
//            canvas.setOnMouseClicked(null);
//            canvas.setOnMouseMoved(null);
//
//            // 鼠标移动：实时预览路径
//            canvas.setOnMouseMoved(mouseEvent -> {
//                if (pathPoints.isEmpty()) return;
//
//                double mouseX = mouseEvent.getX();
//                double mouseY = mouseEvent.getY();
//                Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
//                int x = (int) Math.round(imagePoint.getX());
//                int y = (int) Math.round(imagePoint.getY());
//                // 限制在图片范围内
//                if (x < 0 || x >= currentImage.getWidth() || y < 0 || y >= currentImage.getHeight()) {
//                    return;
//                }
//
//                Point2D currentPoint = new Point2D(x, y);
//                if (lastPreviewPoint != null && currentPoint.distance(lastPreviewPoint) < PREVIEW_MOVE_THRESHOLD) {
//                    return;
//                }
//                lastPreviewPoint = currentPoint;
//
//                clearCanvas();
//                for (int[][] segment : committedPaths) {
//                    drawPath(segment);
//                }
//                for (Point2D point : pathPoints) {
//                    drawPoint((int) point.getX(), (int) point.getY(), Color.LIMEGREEN);
//                }
//                if (closingPath != null) {
//                    drawPath(closingPath);
//                }
//
//                Point2D lastPoint = pathPoints.get(pathPoints.size() - 1);
//                Task<int[][]> previewTask = new Task<>() {
//                    @Override
//                    protected int[][] call() {
//                        dynamicProgramming.computePaths((int) lastPoint.getX(), (int) lastPoint.getY());
//                        return dynamicProgramming.getPath(x, y);
//                    }
//                };
//                previewTask.setOnSucceeded(e -> {
//                    clearCanvas();
//                    for (int[][] segment : committedPaths) {
//                        drawPath(segment);
//                    }
//                    for (Point2D point : pathPoints) {
//                        drawPoint((int) point.getX(), (int) point.getY(), Color.LIMEGREEN);
//                    }
//                    if (closingPath != null) {
//                        drawPath(closingPath);
//                    }
//                    drawPath(previewTask.getValue());
//                });
//                new Thread(previewTask).start();
//            });
//
//            // 鼠标点击：固定路径段
//            canvas.setOnMouseClicked(mouseEvent -> {
//                try {
//                    double mouseX = mouseEvent.getX();
//                    double mouseY = mouseEvent.getY();
//                    Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
//                    int x = (int) Math.round(imagePoint.getX());
//                    int y = (int) Math.round(imagePoint.getY());
//                    // 严格限制在图片范围内
//                    if (x < 0 || x >= currentImage.getWidth() || y < 0 || y >= currentImage.getHeight()) {
//                        System.out.println("点击超出图片范围: (" + x + ", " + y + ")");
//                        return;
//                    }
//
//                    Point2D newTarget = new Point2D(x, y);
//                    Point2D lastPoint = pathPoints.get(pathPoints.size() - 1);
//                    pathPoints.add(newTarget);
//                    drawPoint(x, y, Color.LIMEGREEN);
//
//                    Task<int[][]> task = new Task<>() {
//                        @Override
//                        protected int[][] call() {
//                            dynamicProgramming.computePaths((int) lastPoint.getX(), (int) lastPoint.getY());
//                            return dynamicProgramming.getPath(x, y);
//                        }
//                    };
//                    task.setOnSucceeded(e -> {
//                        int[][] pathSegment = task.getValue();
//                        committedPaths.add(pathSegment);
//                        clearCanvas();
//                        for (int[][] segment : committedPaths) {
//                            drawPath(segment);
//                        }
//                        for (Point2D point : pathPoints) {
//                            drawPoint((int) point.getX(), (int) point.getY(), Color.LIMEGREEN);
//                        }
//                        if (closingPath != null) {
//                            drawPath(closingPath);
//                        }
//                        System.out.println("目标点添加: (" + x + ", " + y + "), 路径段数量: " + committedPaths.size());
//                    });
//                    new Thread(task).start();
//
//                    // 更新种子点
//                    seedPoint = newTarget;
//                } catch (Exception e) {
//                    System.out.println("setTargetPoint 点击错误: " + e.getMessage());
//                    e.printStackTrace();
//                }
//            });
//        } else {
//            System.out.println("退出添加模式，生成闭合路径");
//            canvas.setOnMouseMoved(null);
//            canvas.setOnMouseClicked(null);
//            lastPreviewPoint = null;
//
//            if (pathPoints.size() > 1) {
//                Point2D lastPoint = pathPoints.get(pathPoints.size() - 1);
//                Task<int[][]> closeTask = new Task<>() {
//                    @Override
//                    protected int[][] call() {
//                        dynamicProgramming.computePaths((int) lastPoint.getX(), (int) lastPoint.getY());
//                        return dynamicProgramming.getPath((int) pathPoints.get(0).getX(), (int) pathPoints.get(0).getY());
//                    }
//                };
//                closeTask.setOnSucceeded(e -> {
//                    closingPath = closeTask.getValue();
//                    committedPaths.add(closingPath);
//                    clearCanvas();
//                    for (int[][] segment : committedPaths) {
//                        drawPath(segment);
//                    }
//                    for (Point2D point : pathPoints) {
//                        drawPoint((int) point.getX(), (int) point.getY(), Color.LIMEGREEN);
//                    }
//                    System.out.println("闭合路径生成，路径段总数: " + committedPaths.size());
//                });
//                new Thread(closeTask).start();
//            }
//        }
//    }

    private void enterAddAnchorMode() {
        if (seedPoint == null) {
            System.out.println("请先选择种子点");
            return;
        }

        isAddingTargets = true;
        System.out.println("进入连续目标点添加模式（支持实时预览）");

        canvas.setOnMouseClicked(null);
        canvas.setOnMouseMoved(null);

        // 鼠标移动：实时预览路径
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

                int[] snapped = snapToEdge(x, y);
                int snappX = snapped[0];
                int snappY = snapped[1];

                Point2D currentPoint = new Point2D(snappX, snappY);
                if (lastPreviewPoint != null && currentPoint.distance(lastPreviewPoint) < PREVIEW_MOVE_THRESHOLD) {
                    return;
                }
                lastPreviewPoint = currentPoint;

                Point2D lastPoint = pathPoints.get(pathPoints.size() - 1);
                dynamicProgramming.computePaths((int) lastPoint.getX(), (int) lastPoint.getY());
                int[][] path = dynamicProgramming.getPath(snappX, snappY);

                if (path.length > 120) {
                    int newSeedIndex = path.length - 70; // 固定前50个点（靠近原种子点）
                    if (newSeedIndex < 0) return;

                    // 获取新种子点并截取路径段
                    int[] newSeed = path[newSeedIndex];
                    int[][] segmentToFix = Arrays.copyOfRange(path, newSeedIndex, path.length);

                    // 反转路径段以获得正确的方向
                    List<int[]> reversedSegment = new ArrayList<>();
                    for (int i = segmentToFix.length - 1; i >= 0; i--) {
                        reversedSegment.add(segmentToFix[i]);
                    }
                    int[][] fixedPath = reversedSegment.toArray(new int[0][]);

                    // 异步更新状态
                    Task<Void> fixTask = new Task<Void>() {
                        @Override
                        protected Void call() {
                            // 固定路径段
                            committedPaths.add(fixedPath);
                            // 更新种子点
                            Platform.runLater(() -> {
                                pathPoints.add(new Point2D(newSeed[0], newSeed[1]));
                                seedPoint = new Point2D(newSeed[0], newSeed[1]);
                                drawPoint(newSeed[0], newSeed[1], Color.LIMEGREEN);
                            });
                            // 重新计算后续路径
                            dynamicProgramming.computePaths(newSeed[0], newSeed[1]);
                            return null;
                        }
                    };

                    // 更新预览路径
                    fixTask.setOnSucceeded(e -> {
                        int[][] path1 = dynamicProgramming.getPath(snappX, snappY);
                        // 清除画布后，绘制固定路径为红色
                        clearCanvas();
                        for (int[][] segment : committedPaths) {
                            drawPath(segment, Color.RED);
                        }
// 动态预览路径使用绿色
                        drawPath(path, Color.LIME);
                    });
                    new Thread(fixTask).start();
                } else {
                    // 清除画布后，绘制固定路径为红色
                    clearCanvas();
                    for (int[][] segment : committedPaths) {
                        drawPath(segment, Color.RED);
                    }
                    // 动态预览路径使用绿色
                    drawPath(path, Color.LIME);
                }
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

                    int[] snapped = snapToEdge(x, y);
                    int snappX = snapped[0];
                    int snappY = snapped[1];

                    Point2D newTarget = new Point2D(snappX, snappY);
                    Point2D lastPoint = pathPoints.get(pathPoints.size() - 1);
                    pathPoints.add(newTarget);
                    drawPoint(snappX, snappY, Color.LIMEGREEN);

                    // 计算并固定路径段（从上一个点到当前点）
                    Task<int[][]> task = new Task<>() {
                        @Override
                        protected int[][] call() {
                            dynamicProgramming.computePaths((int) lastPoint.getX(), (int) lastPoint.getY());
                            return dynamicProgramming.getPath(snappX, snappY);
                        }
                    };
                    task.setOnSucceeded(e -> {
                        int[][] pathSegment = task.getValue();
                        committedPaths.add(pathSegment);
                        // 清除画布后，绘制固定路径为红色
                        clearCanvas();
                        for (int[][] segment : committedPaths) {
                            drawPath(segment, Color.RED);
                        }
                        for (Point2D point : pathPoints) {
                            drawPoint((int) point.getX(), (int) point.getY(), Color.LIMEGREEN);
                        }
                        if (closingPath != null) {
                            drawPath(closingPath, Color.RED);
                        }
                        System.out.println("目标点添加: (" + snappX + ", " + snappY + "), 路径段数量: " + committedPaths.size());
                    });
                    new Thread(task).start();

                // 更新种子点
                seedPoint = newTarget;
            } catch (Exception e) {
                System.out.println("setTargetPoint 点击错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void exitAddAnchorMode() {
        System.out.println("退出添加模式，生成闭合路径");
        isAddingTargets = false;
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
                        drawPath(segment, Color.RED);
                    }
                    for (Point2D point : pathPoints) {
                        drawPoint((int) point.getX(), (int) point.getY(), Color.LIME);
                    }
                    System.out.println("闭合路径生成，路径段总数: " + committedPaths.size());
                });
                new Thread(closeTask).start();
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

    private int[] snapToEdge(int x, int y) {
        int r = 7; // 邻域半径，5x5区域
        int Xmin = Math.max(0, x - r);
        int Xmax = Math.min(imageProcessor.getWidth() - 1, x + r);
        int Ymin = Math.max(0, y - r);
        int Ymax = Math.min(imageProcessor.getHeight() - 1, y + r);

        double minCost = Double.MAX_VALUE;
        int Xbest = x;
        int Ybest = y;

        for (int yi = Ymin; yi <= Ymax; yi++) {
            for (int xi = Xmin; xi <= Xmax; xi++) {
                double currentCost = imageProcessor.computeEdgeCost(xi, yi);
                if (currentCost < minCost) {
                    minCost = currentCost;
                    Xbest = xi;
                    Ybest = yi;
                }
            }
        }
        return new int[]{Xbest, Ybest};
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
        gc.fillOval(imageX - 1, imageY - 1, 2, 2);
    }

    private void drawPath(int[][] path, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(color);
        gc.setLineWidth(1.5);

        if (path.length == 0) return;

        Point2D start = canvasToImageCoordinates(path[0][0], path[0][1]);
        gc.beginPath();
        gc.moveTo(start.getX(), start.getY());

        for (int[] point : path) {
            Point2D p = canvasToImageCoordinates(point[0], point[1]);
            gc.lineTo(p.getX(), p.getY());
        }
    }

    private void clearCanvas() {
        if (canvas != null) {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
    }

}