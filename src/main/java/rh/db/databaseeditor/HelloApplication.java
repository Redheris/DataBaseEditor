package rh.db.databaseeditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class HelloApplication extends Application {

    private static Connection connection;
    private static Statement statement;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        ResultSet resultSet = null;

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Bookshop Database Editor");
        stage.setScene(scene);
        stage.setMinWidth(666);
        stage.setMinHeight(450);
        stage.show();
    }

    // Сеттеры для connection и statement
    protected static void setConnection(Connection c) {
        connection = c;
    }

    protected static void setStatement(Statement st) {
        statement = st;
    }

    protected static void logout() {
        try {
            // Закрытие соединения
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}