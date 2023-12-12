package rh.db.databaseeditor;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Responses {
    public static void getFullTable(TableView table, String tableName) {
        final String URL =
                "jdbc:sqlserver://localhost;encrypt=true;trustServerCertificate=true;" +
                "databaseName=" + DBEditorController.db + ";" +
                "username=" + DBEditorController.user + ";" +
                "password=" + DBEditorController.pass + ";";;

        try (Connection connection = DriverManager.getConnection(URL);
             Statement st = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Получение всех столбцов таблицы tableName
            ResultSet colNames = metaData.getColumns(null, null, tableName, null);

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
                while (columnPriviligies.next()) {
                    // Разрешение на чтение всегда "SELECT"
                    if (!columnPriviligies.getString("PRIVILEGE").equals("SELECT"))
                        continue;
                    // Имя пользователя или роли, которым разрешено чтение
                    String grantee = columnPriviligies.getString("GRANTEE");
                    // Проверка, не относится ли текущий пользователь к роли db_owner или к роли grantee
                    ResultSet checkRoles = st.executeQuery(
                            "SELECT IS_ROLEMEMBER ('db_owner'), IS_ROLEMEMBER ('"+ grantee + "')"
                    );
                    checkRoles.next();
                    // Проврка наличия разрешения у текущег пользователя
                    if (checkRoles.getInt(1) == 1
                            || grantee.equals(metaData.getUserName())
                            || checkRoles.getInt(2) == 1) {
                        accessedColumns.add(colName);
                        colIndex++;
                        // Добавление столбца в таблицу
                        TableColumn<List<Object>, Object>  column = new TableColumn<>(colName);
                        int col = colIndex;
                        column.setCellValueFactory(cellData -> {
                            List<Object> rowData = cellData.getValue();
                            return new SimpleObjectProperty<>(rowData.get(col - 1));
                        });
                        column.setEditable(true);
                        table.getColumns().add(column);
                        break;
                    }
                }
            }

            // Заполняем список данными из результата запроса
            List<Object[]> data = new ArrayList<>();
            ResultSet resData = st.executeQuery("SELECT " + String.join(",", accessedColumns) + " FROM " + tableName);
            while (resData.next()) {
                Object[] rowData = new Object[accessedColumns.size()];
                for (int i = 0; i < accessedColumns.size(); i++) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
