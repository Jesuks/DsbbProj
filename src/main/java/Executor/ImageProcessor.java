package Executor;

//ImageProcessor类, 负责将图像转为double array, 计算cost的array.包含:
// static变量: graygraph记录灰度图和rgb转化后的像素, cost记录每一个像素的cost
// 内部方法: computeEdgeCost获取某位置的cost(直接cost[x][y]其实也行)

public class ImageProcessor {
    private double[][] graygraph; // 统一灰度数据

    private double[][] padding;
    public double[][] cost;
    private int height, width;


    // 灰度图构造函数
    public ImageProcessor(double[][] graygraph) {
        this.height = graygraph.length;
        this.width = graygraph[0].length;
        this.graygraph = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.graygraph[y][x] = graygraph[y][x];
            }
        }
        initialize();
    }


    public double[][] Padding(double[][] graygraph) {
        double[][] padding = new double[height + 2][width + 2];
        for (int i = 1; i < height + 1; i++) {
            for (int j = 1; j < width + 1; j++) {
                padding[i][j] = graygraph[i - 1][j - 1];
            }
        }
        return padding;
    }

    // 初始化边缘检测
    private void initialize() {
        cost = new double[height][width];
        padding = Padding(graygraph);
        computeCost();
    }

    private void computeCost() {
        double[][] sobelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        double[][] sobelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        for (int y = 1; y < height + 1; y++)
            for (int x = 1; x < width + 1; x++) {
                double gx = 0, gy = 0;
                for (int yy = -1; yy < 2; yy++) {
                    for (int xx = -1; xx < 2; xx++) {
                        gx += sobelX[yy + 1][xx + 1] * padding[y + yy][x + xx];
                        gy += sobelY[yy + 1][xx + 1] * padding[y + yy][x + xx];
                    }
                }
                cost[y - 1][x - 1] = 1 / (1 + Math.sqrt(gx * gx + gy * gy));
            }
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double computeEdgeCost(int x, int y) {
        return cost[y][x];
    } // 返回目标像素的cost

}
