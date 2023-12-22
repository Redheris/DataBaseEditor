package rh.db.databaseeditor;

import javafx.collections.ObservableList;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Button logout, btnAddNewRow, btnFilterJoin, btnEditRow, btnDeleteRow;
    @FXML
    private MenuButton tablesMenu, reportsMenu, viewsMenu, btnFindDescendants;
    @FXML
    private MenuItem reportOrderSum, reportBookPeriodProceeds, reportGenresTop,
            reportBookPeriodSupplies, reportAuthorBooks, joinAdresses;
    @FXML
    private TableView responseTable;

    protected static String db, user, pass, currentFullTable, searchFilterPattern = "", currentJoin;
    protected static boolean isAdmin;
    private static final List<String> tableNames = new ArrayList<>();
    private static final Map<String, MenuButton> menuButtons = new HashMap<>();
    protected static ObservableList selectedRow;

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
                btnAddNewRow.setDisable(false);
                btnEditRow.setDisable(false);
                btnDeleteRow.setDisable(false);
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
                btnAddNewRow.setDisable(false);
                btnEditRow.setDisable(false);
                btnDeleteRow.setDisable(false);
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
                btnAddNewRow.setDisable(false);
                btnEditRow.setDisable(false);
                btnDeleteRow.setDisable(false);
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
                btnAddNewRow.setDisable(false);
                btnEditRow.setDisable(false);
                btnDeleteRow.setDisable(false);
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
                btnAddNewRow.setDisable(false);
                btnEditRow.setDisable(false);
                btnDeleteRow.setDisable(false);
                getReportParams(reportBookPeriodSupplies.getText());
                if (ReportModalWindow.isPassed())
                    Requests.reportBookPeriodSupplies(
                            ReportModalWindow.idValue,
                            ReportModalWindow.dateFromValue,
                            ReportModalWindow.dateToValue
                    );
            });
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
                btnAddNewRow.setDisable(false);
                btnEditRow.setDisable(false);
                btnDeleteRow.setDisable(false);
            });

            // Сохранение информации о связях таблиц
            for (String tableName : tableNames) {
                menuButtons.put(tableName, new MenuButton());
            }

            DatabaseMetaData metaData = connection.getMetaData();
            for (String tableName : tableNames) {
                ResultSet importedTables = metaData.getImportedKeys(null, null, tableName);
                while (importedTables.next()) {
                    String pkTableName = importedTables.getString("PKTABLE_NAME");
                    String pkColumnName = importedTables.getString("PKCOLUMN_NAME");
                    String fkColumnName = importedTables.getString("FKCOLUMN_NAME");
                    String fkTableName = importedTables.getString("FKTABLE_NAME");
                    DatabaseMetaData columnsMeta = connection.getMetaData();
                    ResultSet columns = columnsMeta.getColumns(null, null, pkTableName, null);
                    List<String> colNames = new ArrayList<>();
                    while (columns.next()) {
                        colNames.add(columns.getString("COLUMN_NAME"));
                    }
                    int colIndex = colNames.indexOf(pkColumnName);
                    MenuItem item = new MenuItem(fkTableName);
                    item.setOnAction(e -> {
                        selectedRow = responseTable.getSelectionModel().getSelectedItems();
                        if (selectedRow.isEmpty()){
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setHeaderText("Выберите строку таблицы");
                            alert.showAndWait();
                            return;
                        }
                        String value = selectedRow.get(colIndex).toString();
                        value = value.substring(1, value.length() - 1).split(", ")[colIndex];

                        responseTable.getItems().clear();
                        Requests.getDescendantSelect(
                                responseTable, fkTableName, fkColumnName, pkTableName, pkColumnName, value
                        );
                        currentFullTable = fkTableName;
                        btnFindDescendants.getItems().setAll(menuButtons.get(fkTableName).getItems());
                    });
                    menuButtons.get(pkTableName).getItems().add(item);
                }
            }
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
        btnEditRow.setDisable(true);
        btnDeleteRow.setDisable(true);
        searchValue.setVisible(false);
        searchValue.setManaged(false);
        btnFilterJoin.setVisible(false);
        btnFilterJoin.setManaged(false);
        btnFindDescendants.getItems().clear();
        tableNames.clear();
        menuButtons.clear();
    }

    private void generateTablesMenu() {
        tablesMenu.getItems().clear();
        String sqlReq = "SELECT * FROM sys.objects WHERE [type]='U' AND is_ms_shipped=0";
        try (ResultSet resultSet = DBEditorApplication.getStatement().executeQuery(sqlReq)) {
            while (resultSet.next()) {
                tableNames.add(resultSet.getString(1));
                MenuItem item = new MenuItem(resultSet.getString(1));
                item.setOnAction(event -> {
                    responseTable.getColumns().clear();
                    responseTable.getItems().clear();
                    btnAddNewRow.setDisable(false);
                    btnEditRow.setDisable(false);
                    btnDeleteRow.setDisable(false);
                    searchValue.setVisible(false);
                    searchValue.setManaged(false);
                    btnFilterJoin.setVisible(false);
                    btnFilterJoin.setManaged(false);
//                    btnFindDescendants = menuButtons.get(item.getText());
                    btnFindDescendants.getItems().setAll(menuButtons.get(item.getText()).getItems());
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
                    btnEditRow.setDisable(false);
                    btnDeleteRow.setDisable(false);
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

        AddRowWindow.setTableColumns(responseTable, currentFullTable);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("addRowWindow.fxml"));
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

    public void onEditRowButtonClick(ActionEvent actionEvent) {
        selectedRow = responseTable.getSelectionModel().getSelectedItems();
        if (selectedRow == null || selectedRow.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("Выберите строку таблицы");
            alert.showAndWait();
            return;
        }
        AddRowWindow.setTableColumns(responseTable, currentFullTable, true);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("addRowWindow.fxml"));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Создаем новое окно
        Stage stage = new Stage();
        stage.setTitle("Редактирование строки");

        stage.setScene(new Scene(root));
        // Указываем, что оно модальное
        stage.initModality(Modality.WINDOW_MODAL);
        // Указываем, что оно должно блокировать главное окно
        stage.initOwner(btnAddNewRow.getScene().getWindow());

        // Открываем окно и ждем пока его закроют
        stage.showAndWait();
    }

    public void onDeleteRowButtonClick(ActionEvent actionEvent) {
        selectedRow = responseTable.getSelectionModel().getSelectedItems();
        if (selectedRow == null || selectedRow.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("Выберите строку таблицы");
            alert.showAndWait();
            return;
        }
        Requests.deleteRow(responseTable, currentFullTable);
    }
}