package rh.db.databaseeditor;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class DBEditorController {
    @FXML
    private HBox login_box;
    @FXML
    private VBox loginInfo;
    @FXML
    private VBox responsesMenu;
    @FXML
    private VBox addNewRowBlock;
    @FXML
    private Label usernameInfo, dbNameInfo;
    @FXML
    protected TextField dbName, username;
    @FXML
    protected PasswordField password;
    @FXML
    private Button logout, btnAddRow;
    @FXML
    private MenuButton tablesMenu, reportsMenu;
    @FXML
    private MenuItem reportOrderSum, reportBookPeriodProceeds, reportGenresTop, reportBookPeriodSupplies, reportAuthorBooks;
    @FXML
    private TableView responseTable, newRowTable;

    protected static String db, user, pass;
    protected static boolean isAdmin;

    public void enableAddNewRowBlock() {
        addNewRowBlock.setDisable(false);
    }

    protected String getURLAuthPart(){
        db = dbName.getText();
        user = username.getText();
        pass = password.getText();
        return "databaseName=" + db + ";" +
               "username=" + user + ";" +
               "password=" + pass + ";";
    }

    @FXML
    protected void onLoginButtonClick() {
        Alert resultInfo = new Alert(Alert.AlertType.INFORMATION);
        final String URL = "jdbc:sqlserver://localhost;encrypt=true;trustServerCertificate=true;" + getURLAuthPart();
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
            responsesMenu.setDisable(false);
            // Проверяем, является ли пользователь админ
            ResultSet checkIsAdmin = statement.executeQuery("SELECT IS_ROLEMEMBER ('db_owner')");
            checkIsAdmin.next();
            isAdmin = checkIsAdmin.getInt(1) == 1;

            // Создаём обработчики для отчётов
            reportOrderSum.setOnAction(e -> {
                ReportModalWindow.setReportParams(
                        reportOrderSum.getText(),
                        true,
                        false,
                        "ID заказа"
                );
                getReportParams(reportOrderSum.getText());
                if (ReportModalWindow.isPassed())
                    Requests.reportOrderSum(ReportModalWindow.idValue);
            });
            reportBookPeriodProceeds.setOnAction(e -> {
                ReportModalWindow.setReportParams(
                        reportBookPeriodProceeds.getText(),
                        true,
                        true,
                        "ID книги"
                );
                getReportParams(reportBookPeriodProceeds.getText());
                if (ReportModalWindow.isPassed())
                    Requests.reportBookPeriodProceeds(
                            ReportModalWindow.idValue,
                            ReportModalWindow.dateFromValue,
                            ReportModalWindow.dateToValue
                    );
            });
            reportGenresTop.setOnAction(e -> {
                ReportModalWindow.setReportParams(
                        reportGenresTop.getText(),
                        false,
                        true
                );
                getReportParams(reportGenresTop.getText());
                if (ReportModalWindow.isPassed())
                    Requests.reportTopGenres(
                            responseTable,
                            ReportModalWindow.dateFromValue,
                            ReportModalWindow.dateToValue
                    );
            });
            reportAuthorBooks.setOnAction(e -> {
                ReportModalWindow.setReportParams(
                        reportAuthorBooks.getText(),
                        true,
                        false,
                        "ID автора"
                );
                getReportParams(reportAuthorBooks.getText());
                if (ReportModalWindow.isPassed())
                    Requests.reportAuthorBooks(responseTable, ReportModalWindow.idValue);
            });
            reportBookPeriodSupplies.setOnAction(e -> {
                ReportModalWindow.setReportParams(
                        reportBookPeriodSupplies.getText(),
                        true,
                        true,
                        "ID книги"
                );
                getReportParams(reportBookPeriodSupplies.getText());
                if (ReportModalWindow.isPassed())
                    Requests.reportBookPeriodSupplies(
                            ReportModalWindow.idValue,
                            ReportModalWindow.dateFromValue,
                            ReportModalWindow.dateToValue
                    );
            });
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
    @FXML
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
        responseTable.getColumns().clear();
        responseTable.getItems().clear();
        newRowTable.getColumns().clear();
        newRowTable.getItems().clear();
        addNewRowBlock.setDisable(true);
        responsesMenu.setDisable(true);
    }

    private void generateTablesMenu() {
        tablesMenu.getItems().clear();
        newRowTable.getItems().clear();
        String sqlReq = "SELECT * " +
                "FROM SYSOBJECTS " +
                "WHERE xtype = 'U'";
        try (ResultSet resultSet = DBEditorApplication.getStatement().executeQuery(sqlReq)) {
            while (resultSet.next()) {
                MenuItem item = new MenuItem(resultSet.getString(1));
                item.setOnAction(event -> {
                    responseTable.getColumns().clear();
                    responseTable.getItems().clear();
                    newRowTable.getColumns().clear();
                    newRowTable.getItems().clear();
                    Requests.getFullTable(responseTable, item.getText(), newRowTable);
                });
                tablesMenu.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getReportParams(String reportName) {
        responseTable.getColumns().clear();
        responseTable.getItems().clear();
        addNewRowBlock.setDisable(true);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("reportModalWindow.fxml"));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Создаем новое окно
        Stage stage = new Stage();
        stage.setTitle("Параметры отчёта \"" + reportName  + "\"");

        stage.setScene(new Scene(root));
        // Указываем, что оно модальное
        stage.initModality(Modality.WINDOW_MODAL);
        // Указываем, что оно должно блокировать главное окно
        stage.initOwner(reportsMenu.getScene().getWindow());

        // Открываем окно и ждем пока его закроют
        stage.showAndWait();
    }

}