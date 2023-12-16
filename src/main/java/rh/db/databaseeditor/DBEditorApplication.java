package rh.db.databaseeditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DBEditorApplication extends Application {

    private static Connection connection;
    private static Statement statement;

    public static void main(String[] args) {
        launch();
    }

    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(DBEditorApplication.class.getResource("dbe-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Bookshop Database Editor");
        stage.setScene(scene);
        stage.setMinWidth(770);
        stage.setMinHeight(450);
        stage.show();
    }

    // Селекторы для connection и statement
    protected static void setConnection(Connection c) {
        connection = c;
    }
    protected static Connection getConnection() {
        return connection;
    }

    protected static void setStatement(Statement st) {
        statement = st;
    }

    protected static Statement getStatement() {
        return statement;
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