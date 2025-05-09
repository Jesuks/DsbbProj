package Executor;


import javafx.geometry.Point2D;
import utils.MinPQ;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class DynamicProgramming {
    private double[][] costs; // 存储从种子点到每个像素的最优路径累计成本
    private int[][] parentX, parentY; // 记录每个像素的父节点坐标，用于路径回溯
    private int width, height;
    private ImageProcessor processor;

    //addition
    private int lastSeedX = -1, lastSeedY = -1; // 上次路径计算的起点
    private boolean validCache = false;
    //addition


    public DynamicProgramming(ImageProcessor processor) {
        this.processor = processor;
        this.width = processor.getWidth();
        this.height = processor.getHeight();
        costs = new double[height][width];
        parentX = new int[height][width];
        parentY = new int[height][width];
    }

    public void computePaths(int seedX, int seedY) {
        if (validCache && seedX == lastSeedX && seedY == lastSeedY) {
            return;  // 使用缓存路径
        }

        this.lastSeedX = seedX;
        this.lastSeedY = seedY;
        this.validCache = true;

        // 计算seed到其他点的最优路径. 路径信息保存在Parent中, 代价保存在costs中.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                costs[y][x] = Double.POSITIVE_INFINITY;
            }
        }
        costs[seedY][seedX] = 0;
        // Dijkstra 算法.
        //pq: Priority Queue.此处MinPQ满足最小值优先出队列.
        MinPQ<Node> pq = new MinPQ<>(Comparator.comparingDouble(n -> n.cost));
        pq.insert(new Node(seedX, seedY, 0));
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        //1,取出cost最小节点;2, 3,检查8个方向, 计算当前节点到邻居的总成本 4,如果新成本更小, 则更新
        while (!pq.isEmpty()) {
            Node node = pq.delMin();
            int x = node.x, y = node.y;
            if (node.cost > costs[y][x]) continue;
            //寻路
            for (int[] dir : directions) {
                int nx = x + dir[0], ny = y + dir[1];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    double edgeCost = processor.computeEdgeCost(nx, ny);
                    double newCost = costs[y][x] + edgeCost;
                    if (newCost < costs[ny][nx]) {
                        costs[ny][nx] = newCost;
                        parentX[ny][nx] = x;
                        parentY[ny][nx] = y;
                        pq.insert(new Node(nx, ny, newCost));
                    }
                }
            }
        }
    }

    public int[][] getPath(int targetX, int targetY) {
        //返回最优路径
        ArrayList<int[]> path = new ArrayList<>();
        int x = targetX, y = targetY;
        if (costs[y][x] == Double.POSITIVE_INFINITY) return new int[0][];
        while (costs[y][x] != 0) {
            path.add(new int[]{x, y});
            int px = parentX[y][x], py = parentY[y][x];
            x = px;
            y = py;
        }
        path.add(new int[]{x, y});
        return path.toArray(new int[0][]);
    }

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
//public class DynamicProgramming {
//    private double[][] costs;
//    private int[][] parentX, parentY;
//    private int width, height;
//    private ImageProcessor processor;
//
//    private int lastSeedX = -1, lastSeedY = -1;
//    private boolean validCache = false;
//
//    public DynamicProgramming(ImageProcessor processor) {
//        this.processor = processor;
//        this.width = processor.getWidth();
//        this.height = processor.getHeight();
//        costs = new double[height][width];
//        parentX = new int[height][width];
//        parentY = new int[height][width];
//    }
//
//    public void computePaths(int seedX, int seedY) {
//        if (validCache && seedX == lastSeedX && seedY == lastSeedY) return;
//
//        this.lastSeedX = seedX;
//        this.lastSeedY = seedY;
//        this.validCache = true;
//
//        for (int y = 0; y < height; y++)
//            for (int x = 0; x < width; x++)
//                costs[y][x] = Double.POSITIVE_INFINITY;
//
//        costs[seedY][seedX] = 0;
//
//        MinPQ<Node> pq = new MinPQ<>(Comparator.comparingDouble(n -> n.cost));
//        pq.insert(new Node(seedX, seedY, 0));
//        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
//
//        while (!pq.isEmpty()) {
//            Node node = pq.delMin();
//            int x = node.x, y = node.y;
//            if (node.cost > costs[y][x]) continue;
//
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
//
//    public int[][] getPath(int targetX, int targetY) {
//        ArrayList<int[]> path = new ArrayList<>();
//        int x = targetX, y = targetY;
//        if (costs[y][x] == Double.POSITIVE_INFINITY) return new int[0][];
//        while (costs[y][x] != 0) {
//            path.add(new int[]{x, y});
//            int px = parentX[y][x], py = parentY[y][x];
//            x = px; y = py;
//        }
//        path.add(new int[]{x, y});
//        return path.toArray(new int[0][]);
//    }
//
//    private static class Node {
//        int x, y;
//        double cost;
//        Node(int x, int y, double cost) {
//            this.x = x;
//            this.y = y;
//            this.cost = cost;
//        }
//    }
//}
