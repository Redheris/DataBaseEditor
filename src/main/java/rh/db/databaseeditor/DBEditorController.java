package rh.db.databaseeditor;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.*;

public class DBEditorController {
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
    private MenuButton tablesMenu;

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
            DBEditorApplication.setConnection(connection);
            DBEditorApplication.setStatement(statement);

            // Скрываем форму авторизации
            login_box.setVisible(false);
            password.setText(null);

            resultInfo.setTitle("Авторизация прошла успешно");
            resultInfo.setHeaderText(
                    "Авторизация пользователя \"" + username.getText() +
                    "\" в базе данных \"" + dbName.getText() + "\" прошла успешно"
            );
            resultInfo.showAndWait();

            generateTablesMenu();

            logout.setDisable(false);
            usernameInfo.setText(username.getText());
            dbNameInfo.setText(dbName.getText());
            loginInfo.setPrefWidth(100);
            loginInfo.setVisible(true);
        }
        // Произошла ошибка при подключении к серверу и базе данных
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
        DBEditorApplication.logout();

        // Отображаем форму авторизации
        login_box.setVisible(true);

        // Сбрасываем данные о соединении
        DBEditorApplication.setConnection(null);
        DBEditorApplication.setStatement(null);
        usernameInfo.setText("NONE");
        dbNameInfo.setText("NONE");
        logout.setDisable(true);
        loginInfo.setVisible(false);
        loginInfo.setPrefWidth(0);
        tablesMenu.getItems().clear();
    }

    private void generateTablesMenu() {
        tablesMenu.getItems().clear();
        String sqlReq = "SELECT name " +
                "FROM SYSOBJECTS " +
                "WHERE xtype = 'U'";
        try (ResultSet resultSet = DBEditorApplication.getStatement().executeQuery(sqlReq)) {
            while (resultSet.next()) {
                MenuItem item = new MenuItem(resultSet.getString(1));

                tablesMenu.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}