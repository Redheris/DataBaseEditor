package rh.db.databaseeditor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class HelloController {
    @FXML
    public HBox login_box;
    @FXML
    private VBox loginInfo;
    @FXML
    private Label usernameInfo, dbNameInfo;
    @FXML
    private TextField dbName, username;
    @FXML
    private PasswordField password;
    @FXML
    private Button logout;

    @FXML
    protected void onLoginButtonClick() {
        Alert resultInfo = new Alert(Alert.AlertType.INFORMATION);
        final String URL =
                "jdbc:sqlserver://localhost;encrypt=true;trustServerCertificate=true;" +
                "databaseName=" + dbName.getText() + ";" +
                "username=" + username.getText() + ";" +
                "password=" + password.getText() + ";";
        try (Connection connection = DriverManager.getConnection(URL);
             Statement statement = connection.createStatement()) {
            HelloApplication.setConnection(connection);
            HelloApplication.setStatement(statement);

            // Скрываем форму авторизации
            login_box.setVisible(false);
            password.setText(null);

            resultInfo.setTitle("Авторизация прошла успешно");
            resultInfo.setHeaderText(
                    "Авторизация пользователя \"" + username.getText() +
                    "\" в базе данных \"" + dbName.getText() + "\" прошла успешно"
            );
            resultInfo.showAndWait();
            logout.setDisable(false);
            usernameInfo.setText(username.getText());
            dbNameInfo.setText(dbName.getText());
            loginInfo.prefWidth(100);
            loginInfo.setVisible(true);
        }
        catch (SQLException e) {
            e.printStackTrace();
            resultInfo.setTitle("Ошибка авторизации");
            resultInfo.setAlertType(Alert.AlertType.ERROR);
            resultInfo.setHeaderText(
                    "Произошла ошибка при авторизации пользователя \"" + username.getText() +
                    "\" в базе данных \"" + dbName.getText() + "\""
            );
            resultInfo.setContentText("Текст ошибки: \n" + e.getMessage());
            resultInfo.showAndWait();
        }
    }

    public void onLogoutButtonClick() {
        // Отключение от базы данных
        HelloApplication.logout();

        // Отображаем форму авторизации
        login_box.setVisible(true);

        // Сбрасываем данные о соединении
        HelloApplication.setConnection(null);
        HelloApplication.setStatement(null);
        usernameInfo.setText("NONE");
        dbNameInfo.setText("NONE");
        logout.setDisable(true);
        loginInfo.setVisible(false);
        loginInfo.prefWidth(0);
    }
}