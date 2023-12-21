package rh.db.databaseeditor;

import javafx.event.ActionEvent;
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
    private Label usernameInfo, dbNameInfo;
    @FXML
    protected TextField dbName, username, searchValue;
    @FXML
    protected PasswordField password;
    @FXML
    private Button logout, btnAddNewRow, btnFilterJoin;
    @FXML
    private MenuButton tablesMenu, reportsMenu, viewsMenu;
    @FXML
    private MenuItem reportOrderSum, reportBookPeriodProceeds, reportGenresTop,
            reportBookPeriodSupplies, reportAuthorBooks, joinAdresses;
    @FXML
    private TableView responseTable;

    protected static String db, user, pass, currentFullTable, searchFilterPattern = "", currentJoin;
    protected static boolean isAdmin;

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
            searchValue.setText(null);
            searchValue.setVisible(false);
            searchValue.setManaged(false);
            btnFilterJoin.setVisible(false);
            btnFilterJoin.setManaged(false);
            searchFilterPattern = "";
            currentJoin = "";
            currentFullTable = "";

            resultInfo.setTitle("Авторизация прошла успешно");
            resultInfo.setHeaderText(
                    "Авторизация пользователя \"" + username.getText() +
                    "\" в базе данных \"" + dbName.getText() + "\" прошла успешно"
            );
            resultInfo.showAndWait();

            generateTablesMenu();
            generateViewsMenu();

            logout.setDisable(false);
            usernameInfo.setText(username.getText());
            dbNameInfo.setText(dbName.getText());
            loginInfo.setPrefWidth(120);
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
                searchValue.setVisible(false);
                searchValue.setManaged(false);
                btnFilterJoin.setVisible(false);
                btnFilterJoin.setManaged(false);
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
                searchValue.setVisible(false);
                searchValue.setManaged(false);
                btnFilterJoin.setVisible(false);
                btnFilterJoin.setManaged(false);
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
                searchValue.setVisible(false);
                searchValue.setManaged(false);
                btnFilterJoin.setVisible(false);
                btnFilterJoin.setManaged(false);
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
                searchValue.setVisible(false);
                searchValue.setManaged(false);
                btnFilterJoin.setVisible(false);
                btnFilterJoin.setManaged(false);
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
                searchValue.setVisible(false);
                searchValue.setManaged(false);
                btnFilterJoin.setVisible(false);
                btnFilterJoin.setManaged(false);
                getReportParams(reportBookPeriodSupplies.getText());
                if (ReportModalWindow.isPassed())
                    Requests.reportBookPeriodSupplies(
                            ReportModalWindow.idValue,
                            ReportModalWindow.dateFromValue,
                            ReportModalWindow.dateToValue
                    );
            });
//            searchValue.setOnKeyPressed (e -> {
////                switch (currentJoin){
////                    case "adresses" ->
////                        Requests.adressesJoin(responseTable);
////                }
//                searchFilterPattern = searchValue.getText();
//            });
            btnFilterJoin.setOnAction(e -> {
                searchFilterPattern = searchValue.getText();
                Requests.adressesJoin(responseTable);
            });
            joinAdresses.setOnAction(e -> {
                Requests.adressesJoin(responseTable);
                currentJoin = "adresses";
                searchValue.setVisible(true);
                searchValue.setManaged(true);
                btnFilterJoin.setVisible(true);
                btnFilterJoin.setManaged(true);
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
        responsesMenu.setDisable(true);
        btnAddNewRow.setDisable(true);
        searchValue.setVisible(false);
        searchValue.setManaged(false);
        btnFilterJoin.setVisible(false);
        btnFilterJoin.setManaged(false);
    }

    private void generateTablesMenu() {
        tablesMenu.getItems().clear();
        String sqlReq = "SELECT * FROM sys.objects WHERE [type]='U' AND is_ms_shipped=0";
        try (ResultSet resultSet = DBEditorApplication.getStatement().executeQuery(sqlReq)) {
            while (resultSet.next()) {
                MenuItem item = new MenuItem(resultSet.getString(1));
                item.setOnAction(event -> {
                    responseTable.getColumns().clear();
                    responseTable.getItems().clear();
                    btnAddNewRow.setDisable(false);
                    searchValue.setVisible(false);
                    searchValue.setManaged(false);
                    btnFilterJoin.setVisible(false);
                    btnFilterJoin.setManaged(false);
                    Requests.getFullTable(responseTable, item.getText());
                    currentFullTable = item.getText();
                });
                tablesMenu.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void generateViewsMenu() {
        viewsMenu.getItems().clear();
        String sqlReq = "SELECT * FROM sys.objects WHERE [type]='V' AND is_ms_shipped=0";
        try (ResultSet resultSet = DBEditorApplication.getStatement().executeQuery(sqlReq)) {
            while (resultSet.next()) {
                MenuItem item = new MenuItem(resultSet.getString(1));
                item.setOnAction(event -> {
                    responseTable.getColumns().clear();
                    responseTable.getItems().clear();
                    btnAddNewRow.setDisable(false);
                    searchValue.setVisible(false);
                    searchValue.setManaged(false);
                    btnFilterJoin.setVisible(false);
                    Requests.getSelectAll(responseTable, item.getText());
                    currentFullTable = item.getText();
                });
                viewsMenu.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getReportParams(String reportName) {
        responseTable.getColumns().clear();
        responseTable.getItems().clear();
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
        stage.setTitle("Конфигурация отчёта");

        stage.setScene(new Scene(root));
        // Указываем, что оно модальное
        stage.initModality(Modality.WINDOW_MODAL);
        // Указываем, что оно должно блокировать главное окно
        stage.initOwner(reportsMenu.getScene().getWindow());

        // Открываем окно и ждем пока его закроют
        stage.showAndWait();
    }

    public void onAddNewRowButtonClick(ActionEvent actionEvent) {
        EditOrAddRowWindow.setTableColumns(responseTable, currentFullTable);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("editOrAddRowWindow.fxml"));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Создаем новое окно
        Stage stage = new Stage();
        stage.setTitle("Добавление новой строки");

        stage.setScene(new Scene(root));
        // Указываем, что оно модальное
        stage.initModality(Modality.WINDOW_MODAL);
        // Указываем, что оно должно блокировать главное окно
        stage.initOwner(btnAddNewRow.getScene().getWindow());

        // Открываем окно и ждем пока его закроют
        stage.showAndWait();
    }
}