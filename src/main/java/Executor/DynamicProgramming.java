package Executor;


import javafx.geometry.Point2D;
import utils.MinPQ;

import java.util.*;

public class DynamicProgramming {
    private double[][] costs; // 存储从种子点到每个像素的最优路径累计成本
    private int[][] parentX, parentY; // 记录每个像素的父节点坐标，用于路径回溯
    private int width, height;
    private ImageProcessor processor;


    public DynamicProgramming(ImageProcessor processor) {
        this.processor = processor;
        this.width = processor.getWidth();
        this.height = processor.getHeight();
        costs = new double[height][width];
        parentX = new int[height][width];
        parentY = new int[height][width];
    }

//    public void computePaths(int seedX, int seedY) {
//        // 计算seed到其他点的最优路径. 路径信息保存在Parent中, 代价保存在costs中.
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                costs[y][x] = Double.POSITIVE_INFINITY;
//            }
//        }
//        costs[seedY][seedX] = 0;
//        // Dijkstra 算法.
//        //pq: Priority Queue.此处MinPQ满足最小值优先出队列.
//        MinPQ<Node> pq = new MinPQ<>(Comparator.comparingDouble(n -> n.cost));
//        pq.insert(new Node(seedX, seedY, 0));
//        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
//        //1,取出cost最小节点;2, 3,检查8个方向, 计算当前节点到邻居的总成本 4,如果新成本更小, 则更新
//        while (!pq.isEmpty()) {
//            Node node = pq.delMin();
//            int x = node.x, y = node.y;
//            if (node.cost > costs[y][x]) continue;
//            //寻路
//            for (int[] dir : directions) {
//                int nx = x + dir[0], ny = y + dir[1];
//                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
//                    double edgeCost = processor.computeEdgeCost(nx, ny);
//                    double newCost = costs[y][x] + edgeCost;
//                    if (newCost < costs[ny][nx]) {
//                        costs[ny][nx] = newCost;
//                        parentX[ny][nx] = x;
//                        parentY[ny][nx] = y;
//                        pq.insert(new Node(nx, ny, newCost));
//                    }
//                }
//            }
//        }
//    }

    //revision
//    public void computePaths(int seedX, int seedY) {
//        // 初始化所有点的cost为无限大
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                costs[y][x] = Double.POSITIVE_INFINITY;
//            }
//        }
//        costs[seedY][seedX] = 0;
//
//        // 使用最小优先队列（MinPQ）来实现Dijkstra算法
//        MinPQ<Node> pq = new MinPQ<>(Comparator.comparingDouble(n -> n.cost));
//        pq.insert(new Node(seedX, seedY, 0));  // 将种子点加入队列
//        boolean[][] visited = new boolean[height][width];  // 标记每个节点是否已处理
//        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
//
//        // 通过Dijkstra算法计算从种子点到其他点的最短路径
//        while (!pq.isEmpty()) {
//            Node node = pq.delMin();
//            int x = node.x, y = node.y;
//
//            if (visited[y][x]) continue;  // 如果节点已经被处理，跳过
//            visited[y][x] = true;  // 标记为已处理
//
//            // 寻找邻居节点并更新路径
//            for (int[] dir : directions) {
//                int nx = x + dir[0], ny = y + dir[1];
//                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
//                    double edgeCost = processor.computeEdgeCost(nx, ny);  // 获取当前边的成本
//                    double newCost = costs[y][x] + edgeCost;
//                    if (newCost < costs[ny][nx]) {  // 如果新的路径成本更小，则更新
//                        costs[ny][nx] = newCost;
//                        parentX[ny][nx] = x;
//                        parentY[ny][nx] = y;
//                        pq.insert(new Node(nx, ny, newCost));  // 将新节点加入队列
//                    }
//                }
//            }
//        }
//    }
    //revision

    //revision2
    // 判断一个像素是否位于图像的边缘
    private boolean isEdge(int x, int y) {
        double edgeThreshold = 0.1;  // 边缘强度的阈值（可以根据需要进行调整）
        return costs[y][x] > edgeThreshold;  // 如果梯度大于阈值，认为是边缘
    }

    public void computePaths(int seedX, int seedY) {
        // 初始化所有点的cost为无限大
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                costs[y][x] = Double.POSITIVE_INFINITY;
            }
        }
        costs[seedY][seedX] = 0;

        // 使用最小优先队列（MinPQ）来实现Dijkstra算法
        MinPQ<Node> pq = new MinPQ<>(Comparator.comparingDouble(n -> n.cost));
        pq.insert(new Node(seedX, seedY, 0));  // 将种子点加入队列
        boolean[][] visited = new boolean[height][width];  // 标记每个节点是否已处理
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

        // 通过Dijkstra算法计算从种子点到其他点的最短路径
        while (!pq.isEmpty()) {
            Node node = pq.delMin();
            int x = node.x, y = node.y;

            if (visited[y][x]) continue;  // 如果节点已经被处理，跳过
            visited[y][x] = true;  // 标记为已处理

            // 寻找邻居节点并更新路径
            for (int[] dir : directions) {
                int nx = x + dir[0], ny = y + dir[1];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    double edgeCost = processor.computeEdgeCost(nx, ny);  // 获取当前边的成本
                    double newCost = costs[y][x] + edgeCost;

                    // 增加穿越边缘的惩罚（使路径更倾向于边缘）
                    if (isEdge(nx, ny)) {
                        newCost += 1.5;  // 增加穿越边缘的惩罚
                    }

                    if (newCost < costs[ny][nx]) {  // 如果新的路径成本更小，则更新
                        costs[ny][nx] = newCost;
                        parentX[ny][nx] = x;
                        parentY[ny][nx] = y;
                        pq.insert(new Node(nx, ny, newCost));  // 将新节点加入队列
                    }
                }
            }
        }
    }
    //revision2

//    public int[][] getPath(int targetX, int targetY) {
//        //返回最优路径
//        ArrayList<int[]> path = new ArrayList<>();
//        int x = targetX, y = targetY;
//        if (costs[y][x] == Double.POSITIVE_INFINITY) return new int[0][];
//        while (costs[y][x] != 0) {
//            path.add(new int[]{x, y});
//            int px = parentX[y][x], py = parentY[y][x];
//            x = px;
//            y = py;
//        }
//        path.add(new int[]{x, y});
//        return path.toArray(new int[0][]);
//    }

    //revision
//    private Map<String, int[][]> pathCache = new HashMap<>();  // 缓存路径
//
//    public int[][] getPath(int targetX, int targetY) {
//        // 首先检查路径缓存
//        String cacheKey = targetX + "," + targetY;
//        if (pathCache.containsKey(cacheKey)) {
//            return pathCache.get(cacheKey);
//        }
//
//        ArrayList<int[]> path = new ArrayList<>();
//        int x = targetX, y = targetY;
//        if (costs[y][x] == Double.POSITIVE_INFINITY) return new int[0][];  // 如果没有找到路径
//
//        while (costs[y][x] != 0) {  // 回溯到种子点
//            path.add(new int[]{x, y});
//            int px = parentX[y][x], py = parentY[y][x];
//            x = px;
//            y = py;
//        }
//        path.add(new int[]{x, y});  // 添加种子点
//
//        // 将路径存入缓存
//        pathCache.put(cacheKey, path.toArray(new int[0][]));
//        return path.toArray(new int[0][]);
//    }
    //revision
    //revision2
    public int[][] getPath(int targetX, int targetY) {
        ArrayList<int[]> path = new ArrayList<>();
        int x = targetX, y = targetY;

        // 检查目标点的路径有效性
        if (costs[y][x] == Double.POSITIVE_INFINITY) return new int[0][];

        while (costs[y][x] != 0) {
            path.add(new int[]{x, y});
            int px = parentX[y][x], py = parentY[y][x];
            x = px;
            y = py;
        }
        path.add(new int[]{x, y});  // 添加种子点

        // 平滑路径
        smoothPath(path);

        return path.toArray(new int[0][]);
    }

    private void smoothPath(ArrayList<int[]> path) {
        // 简单的路径平滑方法，移除掉不必要的转折
        for (int i = 1; i < path.size() - 1; i++) {
            int[] prev = path.get(i - 1);
            int[] curr = path.get(i);
            int[] next = path.get(i + 1);

            // 简单的平均平滑
            curr[0] = (prev[0] + next[0]) / 2;
            curr[1] = (prev[1] + next[1]) / 2;
        }
    }
    //revision2

    private static class Node {
        //记录位置对应的cost
        int x, y;
        double cost;
        Node(int x, int y, double cost) {
            this.x = x;
            this.y = y;
            this.cost = cost;
        }
    }



}
