package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

public class upload extends Application {

    private Image selectedImage; // 存储用户选择的图片

    @Override
    public void start(Stage primaryStage) {
        Button selectButton = new Button("选择图片并转换为数组");

        selectButton.setOnAction(e -> {
            selectedImage = selectImage(primaryStage);
            if (selectedImage != null) {
                try {
                    // 跳转到处理界面
                    goToProcessScene(primaryStage, selectedImage);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        VBox root = new VBox(selectButton);
        Scene scene = new Scene(root, 300, 200);

        primaryStage.setTitle("图片上传");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void goToProcessScene(Stage primaryStage, Image image) throws IOException {

        // 加载处理界面的FXML文件
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/NewScene.fxml"));
        Parent root = loader.load();
        // 获取处理界面的控制器并传递数据
        ProcessSceneController controller = loader.getController();
        controller.initData(image);
        // 创建新场景
        Scene processScene = new Scene(root, 800, 600);
        Stage processStage = new Stage();
        processStage.setTitle("图片处理");
        processStage.setScene(processScene);

        // 设置父子窗口关系
        processStage.initOwner(primaryStage);

        // 关闭当前窗口（可选）
        // primaryStage.close();

        processStage.show();
    }
    private Image selectImage(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            return new Image(selectedFile.toURI().toString());
        }
        return null;
    }



    public static void main(String[] args) {
        launch(args);
    }
}