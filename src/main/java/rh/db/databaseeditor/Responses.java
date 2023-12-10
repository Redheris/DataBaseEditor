package rh.db.databaseeditor;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Responses {
    public static void getResponse(TableView table, String tableName) {
        DBEditorController dbe = new DBEditorController();
        final String URL =
                "jdbc:sqlserver://localhost;encrypt=true;trustServerCertificate=true;" +
                "databaseName=" + DBEditorController.db + ";" +
                "username=" + DBEditorController.user + ";" +
                "password=" + DBEditorController.pass + ";";;

        try (Connection connection = DriverManager.getConnection(URL);
             Statement st = connection.createStatement();
             ResultSet res = st.executeQuery("SELECT * FROM " + tableName)) {
            // Получение метаданных базы данных
            DatabaseMetaData metaData = connection.getMetaData();

            int columnCount = res.getMetaData().getColumnCount();
            ResultSet colNames = metaData.getColumns(null, null, tableName, null);

            // Очистка таблицы перед добавлением новых данных
            // Добавление столбцов из запроса
            for (int i = 0; i < columnCount; i++) {
                String colName = res.getMetaData().getColumnName(i + 1);
                TableColumn<List<Object>, Object>  column = new TableColumn<>(colName);

                int columnIndex = i;
                column.setCellValueFactory(cellData -> {
                    List<Object> rowData = cellData.getValue();
                    return new ReadOnlyObjectWrapper<>(rowData.get(columnIndex));
                });
                table.getColumns().add(column);
            }

            // Создаем список для хранения данных
            List<Object[]> data = new ArrayList<>();

            // Заполняем список данными из результата запроса
            while (res.next()) {
                Object[] rowData = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    if (res.getObject(i) == null)
                        rowData[i - 1] = "NULL";
                    else
                        rowData[i - 1] = res.getObject(i);
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
