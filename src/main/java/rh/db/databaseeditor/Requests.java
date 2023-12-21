package rh.db.databaseeditor;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Requests {
    private static String getURL() {
        return "jdbc:sqlserver://localhost;encrypt=true;trustServerCertificate=true;" +
                "databaseName=" + DBEditorController.db + ";" +
                "username=" + DBEditorController.user + ";" +
                "password=" + DBEditorController.pass + ";";
    }
    private static void fillTableViewWithSelect (Connection connection, Statement st, TableView table,
                                                 ArrayList<String> colNames, String sql) throws SQLException {
        table.getColumns().clear();
        table.getItems().clear();

        // Заполняем список данными из результата запроса
        List<Object[]> data = new ArrayList<>();

        ResultSet resData = st.executeQuery(sql);

        int col = 0;
        for (String colName : colNames) {
            TableColumn<List<Object>, Object> column = new TableColumn<>(colName);
            int colIndex = col++;
            column.setCellValueFactory(cellData -> {
                List<Object> rowData = cellData.getValue();
                return new SimpleObjectProperty<>(rowData.get(colIndex));
            });
            table.getColumns().add(column);
        }

        int coluumnsCount = resData.getMetaData().getColumnCount();
        while (resData.next()) {
            Object[] rowData = new Object[coluumnsCount];
            for (int i = 0; i < coluumnsCount; i++) {
                if (resData.getObject(i + 1) == null)
                    rowData[i] = "NULL";
                else
                    rowData[i] = resData.getObject(i + 1);
            }
            data.add(rowData);
        }


        // Добавляем данные в таблицу
        for (Object[] row : data) {
            table.getItems().add(List.of(row));
        }
    }

    public static void getSelectAll (TableView table, String tableName){
        final String URL = getURL();
        try (Connection connection = DriverManager.getConnection(URL);
             Statement st = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();
            // Получение данных о каждом столбце таблицы tableName
            ResultSet colNames = metaData.getColumns(null, null, tableName, null);
            // Получение списка столбцов таблицы tableName
            ArrayList<String> colNamesList = new ArrayList<>();
            while (colNames.next()){
                String colName = colNames.getString("COLUMN_NAME");
                colNamesList.add(colName);
            }
            fillTableViewWithSelect(connection, st, table,
                    colNamesList,
                    "SELECT * FROM " + tableName
            );
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void getFullTable(TableView table, String tableName) {
        final String URL = getURL();

        try (Connection connection = DriverManager.getConnection(URL);
             Statement st = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Получение всех столбцов таблицы tableName
            ResultSet colNames = connection.getMetaData().getColumns(null, null, tableName, null);

            // Список для названий стобцов, на чтение которых есть разрешение
            ArrayList<String> accessedColumns = new ArrayList<>();
            int colIndex = 0;
            // Перебор всех столбцов
            while (colNames.next()) {
                String colName = colNames.getString("COLUMN_NAME");
                ResultSet columnPriviligies = st.executeQuery(
                        "EXEC sp_column_privileges @table_name = '" + tableName + "',"
                                + "@column_name = '" + colName + "'"
                );
                // Добавление в таблицу JavaFX столбцов, на чтение которых есть разрешение
                // Проход по таблице всех разрешений для текущего столбца
                while (columnPriviligies.next()) {
                    // Разрешение на чтение должно быть "SELECT"
                    if (!columnPriviligies.getString("PRIVILEGE").equals("SELECT"))
                        continue;
                    // Имя пользователя или роли, которым разрешено чтение
                    String grantee = columnPriviligies.getString("GRANTEE");
                    // Проверка, не относится ли текущий пользователь к роли db_owner или к роли grantee
                    ResultSet checkRoles = st.executeQuery(
                            "SELECT IS_ROLEMEMBER ('db_owner'), IS_ROLEMEMBER ('"+ grantee + "')"
                    );
                    checkRoles.next();
                    // Проврка наличия разрешения у текущего пользователя
                    if (checkRoles.getInt(1) == 1
                            || grantee.equals(metaData.getUserName())
                            || checkRoles.getInt(2) == 1) {
                        accessedColumns.add(colName);
                        colIndex++;

                        TableColumn<List<Object>, Object> column = new TableColumn<>(colName);
                        int col = colIndex;
                        column.setCellValueFactory(cellData -> {
                            List<Object> rowData = cellData.getValue();
                            return new SimpleObjectProperty<>(rowData.get(col - 1));
                        });
                        break;
                    }
                }
            }

            fillTableViewWithSelect(connection, st, table,
                    accessedColumns,
                    "SELECT [" + String.join("],[", accessedColumns) + "] FROM " + tableName
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void reportTopGenres (TableView table, String dateFrom, String dateTo) {
        String URL = getURL();
        try (Connection connection = DriverManager.getConnection(URL);
             Statement st = connection.createStatement()) {
            ArrayList<String> colNames = new ArrayList<>(Arrays.asList(
                    "ID жанра",
                    "Название жанра",
                    "Количество проданных книг"
            ));
            fillTableViewWithSelect(connection, st, table,
                    colNames,
                    String.format("EXEC proc_GenresTop '%s', '%s'", dateFrom, dateTo)
            );
        }
        catch (SQLException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Ошибка при обработке запроса");
            errorAlert.setHeaderText("Произошла ошибка при генерации отчёта \"" + ReportModalWindow.reportTitle + "\"");
            errorAlert.setContentText("Текст ошибки: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
    public static void reportOrderSum (String orderId) {
        String URL = getURL();
        String sql = "{CALL proc_totalSum(?, ?)}";
        try (Connection connection = DriverManager.getConnection(URL);
             CallableStatement cs = connection.prepareCall(sql)) {

            cs.setString(1, orderId);
            cs.registerOutParameter(2, Types.INTEGER); // Регистрируем выходной параметр
            cs.execute();

            int totalSum = cs.getInt(2); // Получаем значение выходного параметра

            Alert result = new Alert(Alert.AlertType.INFORMATION);
            result.setTitle("Итоговая стоимость заказа");
            result.setHeaderText("Итоговая стоимость заказа с id_order = " + orderId + ":\n" + totalSum);
            result.showAndWait();
        }
        catch (SQLException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Ошибка при обработке запроса");
            errorAlert.setHeaderText("Произошла ошибка при генерации отчёта \"" + ReportModalWindow.reportTitle + "\"");
            errorAlert.setContentText("Текст ошибки: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
    public static void reportBookPeriodProceeds (String bookId, String dateFrom, String dateTo) {
        String URL = getURL();
        String sql = "{CALL proc_BooksProceeds(?, ?, ?, ?)}";
        try (Connection connection = DriverManager.getConnection(URL);
             CallableStatement cs = connection.prepareCall(sql)) {

            cs.setString(1, bookId);
            cs.setString(2, dateFrom);
            cs.setString(3, dateTo);
            cs.registerOutParameter(4, Types.INTEGER); // Регистрируем выходной параметр
            cs.execute();
            // FIXME Ошибка "Слишком много аргументов"
            int totalSum = cs.getInt(4); // Получаем значение выходного параметра

            Alert result = new Alert(Alert.AlertType.INFORMATION);
            result.setTitle("Выручка с продажи книги за период");
            result.setHeaderText(String.format("Общая выручка с продаж книги с id_order = %s за период" +
                    "от %s до %s составила:\n%d", bookId, dateFrom, dateTo, totalSum));
            result.showAndWait();
        }
        catch (SQLException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Ошибка при обработке запроса");
            errorAlert.setHeaderText("Произошла ошибка при генерации отчёта \"" + ReportModalWindow.reportTitle + "\"");
            errorAlert.setContentText("Текст ошибки: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
    public static void reportBookPeriodSupplies (String bookId, String dateFrom, String dateTo) {
        String URL = getURL();
        String sql = "{CALL proc_BookSuppliedCount(?, ?, ?, ?)}";
        try (Connection connection = DriverManager.getConnection(URL);
             CallableStatement cs = connection.prepareCall(sql)) {

            cs.setString(1, bookId);
            cs.setString(2, dateFrom);
            cs.setString(3, dateTo);
            cs.registerOutParameter(4, Types.INTEGER); // Регистрируем выходной параметр
            cs.execute();

            int totalSum = cs.getInt(4); // Получаем значение выходного параметра

            Alert result = new Alert(Alert.AlertType.INFORMATION);
            result.setTitle("Количество поставленных экземлпяров книги");
            result.setHeaderText(String.format("Общая выручка с продаж книги с id_order = %s за период" +
                    "от %s до %s составила:\n%d", bookId, dateFrom, dateTo, totalSum));
            result.showAndWait();
        }
        catch (SQLException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Ошибка при обработке запроса");
            errorAlert.setHeaderText("Произошла ошибка при генерации отчёта \"" + ReportModalWindow.reportTitle + "\"");
            errorAlert.setContentText("Текст ошибки: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
    public static void reportAuthorBooks (TableView table, String authorId) {
        String URL = getURL();
        try (Connection connection = DriverManager.getConnection(URL);
             Statement st = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet colNames = metaData.getColumns(
                    null, null, "Book", null
            );
            ArrayList<String> colNamesArray = new ArrayList<>();
            while (colNames.next())
                colNamesArray.add(colNames.getString("COLUMN_NAME"));
            fillTableViewWithSelect(connection, st, table,
                    colNamesArray,
                    String.format("EXEC proc_AuthorCreations '%s'", authorId)
            );
        }
        catch (SQLException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Ошибка при обработке запроса");
            errorAlert.setHeaderText("Произошла ошибка при генерации отчёта \"" + ReportModalWindow.reportTitle + "\"");
            errorAlert.setContentText("Текст ошибки: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
}
