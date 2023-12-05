package rh.db.databaseeditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class HelloApplication extends Application {

    private static final String URL =
            "jdbc:sqlserver://REDHERISPC;" +
            "databaseName=BOOKSHOP;integratedSecurity=true;" +
            "encrypt=true;trustServerCertificate=true";

    @Override
    public void start(Stage stage) throws IOException {
        ResultSet resultSet = null;

        try (Connection connection = DriverManager.getConnection(URL);
             Statement statement = connection.createStatement();) {
            // Create and execute a SELECT SQL statement.
            String selectSql = "SELECT TOP 10 id_customer, fname, lname from Customer";
            resultSet = statement.executeQuery(selectSql);

            // Print results from select statement
            while (resultSet.next()) {
                System.out.println(resultSet.getString(1) + " " + resultSet.getString(2) + " " + resultSet.getString(3));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}