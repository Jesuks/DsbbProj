package GUI;

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
    @FXML private Canvas canvas;

    //addition
    private boolean isAddingTargets = false; // 当前是否处于目标点连续添加状态
    private List<Point2D> pathPoints = new ArrayList<>(); // 路径点列表（包含种子点和所有目标点）
    private int[][] closingPath = null; // 暂时闭合路径（用于自动闭合回 seedPoint）
    //addition

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

//    @FXML
//    private void setSeedPoint(ActionEvent event) {
//        System.out.println("jaja");
//        seedPoint = null;
//        clearCanvas();
//        canvas.setOnMouseClicked(null);
//        System.out.println("注册 Canvas 鼠标点击事件以选择种子点");
//        canvas.setOnMouseClicked(mouseEvent -> {
//            System.out.println("ja21");
//            try {
//                double mouseX = mouseEvent.getX();
//                double mouseY = mouseEvent.getY();
//                System.out.println("Canvas 点击坐标: (" + mouseX + ", " + mouseY + ")");
//                Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
//                int x = (int) Math.round(imagePoint.getX());
//                int y = (int) Math.round(imagePoint.getY());
//                x = Math.max(0, Math.min(x, (int) currentImage.getWidth() - 1));
//                y = Math.max(0, Math.min(y, (int) currentImage.getHeight() - 1));
//                seedPoint = new Point2D(x, y);
//                System.out.printf("选择的种子点: (%d, %d)%n", x, y);
//                drawPoint(x, y, Color.BLACK);
//                canvas.setOnMouseClicked(null);
//                imageProcessor.computeEdgeCost(x, y);
//            } catch (Exception e) {
//                System.out.println("setSeedPoint 错误: " + e.getMessage());
//                e.printStackTrace();
//            }
//        });
//    }

    //revision
    @FXML
    private void setSeedPoint(ActionEvent event) {
        seedPoint = null;
        isAddingTargets = false;
        pathPoints.clear();
        clearCanvas();  // 你的已有清屏逻辑
        canvas.setOnMouseClicked(null);

        canvas.setOnMouseClicked(mouseEvent -> {
            double mouseX = mouseEvent.getX();
            double mouseY = mouseEvent.getY();
            Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
            int x = (int) Math.round(imagePoint.getX());
            int y = (int) Math.round(imagePoint.getY());
            x = Math.max(0, Math.min(x, (int) currentImage.getWidth() - 1));
            y = Math.max(0, Math.min(y, (int) currentImage.getHeight() - 1));
            seedPoint = new Point2D(x, y);
            pathPoints.add(seedPoint);  // 将种子点放入路径列表
            drawPoint(x, y, Color.BLACK);
            canvas.setOnMouseClicked(null);  // 取消点击监听
        });
    }
    //revision

//    @FXML
//    private void setTargetPoint(ActionEvent event) {
//        targetPoint = null;
//        clearCanvas();
//        if (seedPoint != null) {
//            drawPoint((int) seedPoint.getX(), (int) seedPoint.getY(), Color.BLACK);
//        }
//        canvas.setOnMouseClicked(null);
//        System.out.println("注册 Canvas 鼠标点击事件以选择目标点");
//        canvas.setOnMouseClicked(mouseEvent -> {
//            try {
//                double mouseX = mouseEvent.getX();
//                double mouseY = mouseEvent.getY();
//                System.out.println("Canvas 点击坐标: (" + mouseX + ", " + mouseY + ")");
//                Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);
//                int x = (int) Math.round(imagePoint.getX());
//                int y = (int) Math.round(imagePoint.getY());
//                x = Math.max(0, Math.min(x, (int) currentImage.getWidth() - 1));
//                y = Math.max(0, Math.min(y, (int) currentImage.getHeight() - 1));
//                targetPoint = new Point2D(x, y);
//                System.out.printf("选择的目标点: (%d, %d)%n", x, y);
//                drawPoint(x, y, Color.BLACK);
//                if (seedPoint != null) {
//                    int seedX = (int) seedPoint.getX();
//                    int seedY = (int) seedPoint.getY();
//                    dynamicProgramming.computePaths(seedX, seedY);
//                    int[][] path = dynamicProgramming.getPath(x, y);
//                    drawPath(path);
//                }
//                canvas.setOnMouseClicked(null);
//            } catch (Exception e) {
//                System.out.println("setTargetPoint 错误: " + e.getMessage());
//                e.printStackTrace();
//            }
//        });
//    }

    //revision
//    @FXML
//    private void setTargetPoint(ActionEvent event) {
//        targetPoint = null;
//        clearCanvas();  // 清空画布
//        if (seedPoint != null) {
//            drawPoint((int) seedPoint.getX(), (int) seedPoint.getY(), Color.BLACK);  // 如果种子点已选择，绘制
//        }
//        canvas.setOnMouseClicked(null);
//        System.out.println("注册 Canvas 鼠标点击事件以选择目标点");
//
//        // 注册鼠标点击事件，选择目标点
//        canvas.setOnMouseClicked(mouseEvent -> {
//            try {
//                double mouseX = mouseEvent.getX();  // 变量声明不需要final
//                double mouseY = mouseEvent.getY();  // 变量声明不需要final
//                System.out.println("Canvas 点击坐标: (" + mouseX + ", " + mouseY + ")");
//                Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);  // 获取图像坐标
//                int x = (int) Math.round(imagePoint.getX());  // 定义为局部变量
//                int y = (int) Math.round(imagePoint.getY());  // 定义为局部变量
//
//                // 限制坐标范围
//                int boundedX = Math.max(0, Math.min(x, (int) currentImage.getWidth() - 1));  // 限制x坐标范围
//                int boundedY = Math.max(0, Math.min(y, (int) currentImage.getHeight() - 1));  // 限制y坐标范围
//
//                targetPoint = new Point2D(boundedX, boundedY);  // 记录目标点
//                System.out.printf("选择的目标点: (%d, %d)%n", boundedX, boundedY);
//                drawPoint(boundedX, boundedY, Color.BLACK);  // 绘制目标点
//
//                // 如果已经选择了种子点，异步计算路径
//                if (seedPoint != null) {
//                    int seedX = (int) seedPoint.getX();
//                    int seedY = (int) seedPoint.getY();
//
//                    // 创建异步任务来计算路径
//                    Task<int[][]> pathTask = new Task<int[][]>() {
//                        @Override
//                        protected int[][] call() throws Exception {
//                            try {
//                                dynamicProgramming.computePaths(seedX, seedY);  // 计算路径
//                                return dynamicProgramming.getPath(boundedX, boundedY);  // 获取路径
//                            } catch (Exception e) {
//                                System.out.println("计算路径时发生错误: " + e.getMessage());
//                                return new int[0][];  // 如果发生异常，返回空路径
//                            }
//                        }
//                    };
//
//                    // 计算完成后，绘制路径
//                    pathTask.setOnSucceeded(e -> {
//                        int[][] path = pathTask.getValue();  // 获取计算后的路径
//                        if (path.length == 0) {
//                            System.out.println("没有找到有效路径");
//                        } else {
//                            drawPath(path);  // 绘制路径
//                        }
//                    });
//
//                    // 启动异步线程
//                    new Thread(pathTask).start();
//                }
//                canvas.setOnMouseClicked(null);  // 取消点击事件
//            } catch (Exception e) {
//                System.out.println("setTargetPoint 错误: " + e.getMessage());
//                e.printStackTrace();
//            }
//        });
//    }
    //revision

    //revision2
    @FXML
    private void setTargetPoint(ActionEvent event) {
        if (seedPoint == null) {
            System.out.println("请先选择种子点");
            return;
        }

        // 切换状态：第一次点击按钮开启连续添加，第二次点击则停止并生成闭合路径
        isAddingTargets = !isAddingTargets;

        if (isAddingTargets) {
            System.out.println("进入连续目标点添加模式");

            // ✅ 清除闭合路径状态（放在点击前，更合理）
            if (closingPath != null) {
                erasePath(closingPath);
                closingPath = null;
                System.out.println("上次闭合路径已清除，准备继续添加新目标点");
            }

            canvas.setOnMouseClicked(mouseEvent -> {
                try {
                    double mouseX = mouseEvent.getX();
                    double mouseY = mouseEvent.getY();
                    Point2D imagePoint = canvasToImageCoordinates(mouseX, mouseY);

                    int x = (int) Math.round(imagePoint.getX());
                    int y = (int) Math.round(imagePoint.getY());
                    x = Math.max(0, Math.min(x, (int) currentImage.getWidth() - 1));
                    y = Math.max(0, Math.min(y, (int) currentImage.getHeight() - 1));
                    final int fx = x, fy = y;

                    Point2D newTarget = new Point2D(fx, fy);
                    Point2D last = pathPoints.get(pathPoints.size() - 1);

                    pathPoints.add(newTarget);
                    drawPoint(fx, fy, Color.BLACK);

                    // ⚠️ 这里不再需要重复判断 closingPath != null，因为状态前置清除了

                    Task<int[][]> task = new Task<>() {
                        @Override
                        protected int[][] call() {
                            dynamicProgramming.computePaths((int) last.getX(), (int) last.getY());
                            return dynamicProgramming.getPath(fx, fy);
                        }
                    };
                    task.setOnSucceeded(e -> drawPath(task.getValue()));
                    new Thread(task).start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("退出添加模式，生成闭合路径");

            // 闭合路径（最后点 → 种子点）
            if (pathPoints.size() > 1) {
                Point2D last = pathPoints.get(pathPoints.size() - 1);

                Task<int[][]> closeTask = new Task<>() {
                    @Override
                    protected int[][] call() {
                        dynamicProgramming.computePaths((int) last.getX(), (int) last.getY());
                        return dynamicProgramming.getPath((int) seedPoint.getX(), (int) seedPoint.getY());
                    }
                };

                closeTask.setOnSucceeded(e -> {
                    closingPath = closeTask.getValue();
                    drawPath(closingPath);  // 绘制闭合路径
                });

                new Thread(closeTask).start();
            }

            canvas.setOnMouseClicked(null);  // 停止监听鼠标点击
        }
    }
    //revision2

    //addition
    private void erasePath(int[][] path) {
        if (path == null || path.length == 0) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        for (int i = 1; i < path.length; i++) {
            int x1 = path[i - 1][0], y1 = path[i - 1][1];
            int x2 = path[i][0], y2 = path[i][1];

            double cx1 = x1 * (canvas.getWidth() / currentImage.getWidth());
            double cy1 = y1 * (canvas.getHeight() / currentImage.getHeight());
            double cx2 = x2 * (canvas.getWidth() / currentImage.getWidth());
            double cy2 = y2 * (canvas.getHeight() / currentImage.getHeight());

            gc.strokeLine(cx1, cy1, cx2, cy2);
        }
    }
    //addition

    //addition
    @FXML
    private void undoLastTarget(ActionEvent event) {
        if (pathPoints.size() <= 1) {
            System.out.println("没有目标点可撤销（种子点不能撤销）");
            return;
        }

        // 删除最后一个目标点
        Point2D removed = pathPoints.remove(pathPoints.size() - 1);
        System.out.println("撤销目标点: " + removed);

        // 清空画布（背景保留），重新绘制剩余路径
        clearCanvas();

        // 重画种子点
        Point2D seed = pathPoints.get(0);
        drawPoint((int) seed.getX(), (int) seed.getY(), Color.BLACK);

        // 重画每个路径段（异步）
        for (int i = 1; i < pathPoints.size(); i++) {
            Point2D from = pathPoints.get(i - 1);
            Point2D to = pathPoints.get(i);
            final int fx = (int) to.getX(), fy = (int) to.getY();

            Task<int[][]> task = new Task<>() {
                @Override
                protected int[][] call() {
                    dynamicProgramming.computePaths((int) from.getX(), (int) from.getY());
                    return dynamicProgramming.getPath(fx, fy);
                }
            };
            task.setOnSucceeded(e -> drawPath(task.getValue()));
            new Thread(task).start();

            drawPoint(fx, fy, Color.BLACK);
        }

        // 如存在闭合路径，清除它（因为路径已改变）
        if (closingPath != null) {
            erasePath(closingPath);
            closingPath = null;
        }
    }
    //addition

    //addition
    @FXML
    private void exportCroppedRegion(ActionEvent event) {
        if (currentImage == null || pathPoints.size() < 2 || closingPath == null) {
            System.out.println("路径未闭合，无法导出抠图");
            return;
        }

        int width = (int) currentImage.getWidth();
        int height = (int) currentImage.getHeight();

        // 构造完整路径点（每段路径 + 闭合段）
        List<int[]> fullPath = new ArrayList<>();
        for (int i = 1; i < pathPoints.size(); i++) {
            Point2D from = pathPoints.get(i - 1);
            Point2D to = pathPoints.get(i);
            dynamicProgramming.computePaths((int) from.getX(), (int) from.getY());
            int[][] segment = dynamicProgramming.getPath((int) to.getX(), (int) to.getY());
            fullPath.addAll(Arrays.asList(segment));
        }
        fullPath.addAll(Arrays.asList(closingPath));

        // 初始化遮罩
        boolean[][] mask = new boolean[height][width];
        for (int[] p : fullPath) {
            int x = p[0], y = p[1];
            if (x >= 0 && x < width && y >= 0 && y < height) {
                mask[y][x] = true;
            }
        }

        // 简单“扫描线填充”：每行遇到两个边界点之间填充
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

        // 创建透明图像，拷贝区域
        WritableImage cropped = new WritableImage(width, height);
        PixelReader reader = currentImage.getPixelReader();
        PixelWriter writer = cropped.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (mask[y][x]) {
                    writer.setColor(x, y, color);
                } else {
                    writer.setColor(x, y, new Color(0, 0, 0, 0)); // 设置透明
                }
            }
        }

        // 弹出保存窗口
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
    //addition

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