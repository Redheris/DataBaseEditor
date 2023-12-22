package rh.db.databaseeditor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AddRowWindow implements Initializable {
    @FXML
    private Label tableName;
    @FXML
    private VBox parametersBlock;
    @FXML
    private Button cancelButton;
    // Таблица с результатом из основного окна
    private static TableView table;
    private static String tableNameValue, sqlWhere = " WHERE ";
    private static final Map<String, String> parameters = new HashMap<>();
    private static boolean isEditMode = false;

    private static String getURL() {
        return "jdbc:sqlserver://localhost;encrypt=true;trustServerCertificate=true;" +
                "databaseName=" + DBEditorController.db + ";" +
                "username=" + DBEditorController.user + ";" +
                "password=" + DBEditorController.pass + ";";
    }

    protected static void setTableColumns(TableView responseTable, String tableName) {
        tableNameValue = tableName;
        table = responseTable;
        isEditMode = false;
    }

    protected static void setTableColumns(TableView responseTable, String tableName, boolean editMode) {
        tableNameValue = tableName;
        table = responseTable;
        isEditMode = editMode;
        DBEditorController.selectedRow = responseTable.getSelectionModel().getSelectedItems();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tableName.setText(tableNameValue);
        parameters.clear();
        sqlWhere = " WHERE ";
        final String URL = getURL();
        try (Connection connection = DriverManager.getConnection(URL);
             Statement st = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();
            // Получение данных о каждом столбце таблицы tableName
            ResultSet colNames = metaData.getColumns(null, null, tableNameValue, null);
            ResultSet findUnique = metaData.getIndexInfo(null, null, tableNameValue, true, true);
            ResultSet foreign = metaData.getImportedKeys(null, null, tableNameValue);

            if (isEditMode) {
                DBEditorController.selectedRow = table.getSelectionModel().getSelectedItems();
                if (DBEditorController.selectedRow.isEmpty()){
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setHeaderText("Выберите строку таблицы");
                    alert.showAndWait();
                    return;
                }
                DatabaseMetaData columnsMeta = connection.getMetaData();
                ResultSet columns = columnsMeta.getColumns(null, null, tableNameValue, null);
                List<String> colNamesList = new ArrayList<>();
                while (columns.next()) {
                    colNamesList.add(columns.getString("COLUMN_NAME"));
                }

                String value = DBEditorController.selectedRow.get(0).toString();
                value = value.substring(1, value.length() - 1);
//                value = String.join(",", value.split(", "));
                for (int i = 0; i < colNamesList.size(); ++i) {
                    parameters.put(colNamesList.get(i), value.split(", ")[i]);
                    sqlWhere += "[" + colNamesList.get(i) + "]='" + value.split(", ")[i] + "' AND ";
                }
                sqlWhere = sqlWhere.substring(0, sqlWhere.length() - 5);
            }

            while (colNames.next()) {
                String colName = colNames.getString("COLUMN_NAME");
                String colType = colNames.getString("TYPE_NAME");
                boolean isNullable = colNames.getInt("NULLABLE") == 1;
                boolean isAutoIncrement = colNames.getString("IS_AUTOINCREMENT").equals("YES");

                if (isAutoIncrement) continue;

                HBox param = new HBox();
                param.setSpacing(20);
                // Название столбца
                Label paramName = new Label(colName + (isNullable ? " (NULL)" : ""));
                paramName.setId(colName);
                paramName.setPrefWidth(130);
                param.getChildren().add(paramName);

                // Создание разных полей ввода в зависимости от типа значения столбца
                switch (colType) {
                    case "int" -> {
                        // Ввод значения
                        TextField tf = new TextField();
                        tf.setId(colName);
                        tf.setOnKeyPressed(this::onIntegerTextFieldChanged);
                        param.getChildren().add(tf);
                        if (isEditMode)
                            tf.setText(parameters.get(colName));
                    }
                    case "varchar" -> {
                        TextField tf = new TextField();
                        tf.setId(colName);
                        tf.setOnKeyTyped(this::onStringTextFieldChanged);
                        param.getChildren().add(tf);
                        if (isEditMode)
                            tf.setText(parameters.get(colName));
                    }
                    case "date" -> {
                        DatePicker dp = new DatePicker();
                        dp.setEditable(false);
                        dp.setId(colName);
                        dp.setOnAction(this::onDatePickerChanged);
                        // Выбор будущей даты сочтён возможным,
                        // а других проверок не требуется в силу отсутствия ручного ввода
//                        dp.setOnAction(this::onDateTextfieldChanged);
                        param.getChildren().add(dp);
                        if (isEditMode)
                            dp.setValue(LocalDate.parse(parameters.get(colName)));
                    }
                    default -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Ошибка");
                        alert.setHeaderText("Неизвестный тип столбца " + colType);
                        alert.showAndWait();
                    }
                }
                if (!isEditMode)
                    parameters.put(colName, "");
                parametersBlock.getChildren().add(param);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void onCancelButtonClick(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void onSendButtonClick(ActionEvent actionEvent) {
        try (Connection connection = DriverManager.getConnection(getURL())) {
            DatabaseMetaData metaData = connection.getMetaData();
            // Получение информации о каждом столбцц таблицы tableNameValue
            ResultSet colNames = metaData.getColumns(null, null, tableNameValue, null);
            // Редактируемые столбцы для запроса UPDATE
            List<String> editable = new ArrayList<>();
            // Проверка параметров для каждого столбца таблицы tableName
            while (colNames.next()) {
                // Характеристики столбца
                String colName = colNames.getString("COLUMN_NAME");
                String colType = colNames.getString("TYPE_NAME");
                boolean isNullable = colNames.getInt("NULLABLE") == 1;
                boolean isAutoIncrement = colNames.getString("IS_AUTOINCREMENT").equals("YES");

                // Игнорирование столбца с автоинкрементом (primary key столбец ID)
                if (isAutoIncrement) continue;
                // Добавление столбца в список изменяемых
                editable.add(colName);
                // Проверка значений параметров
                if (!isNullable &&
                        (parameters.get(colName) == null || parameters.get(colName).isBlank())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка");
                    alert.setHeaderText(String.format("Параметр %s не может быть пустым", colName));
                    alert.showAndWait();
                    return;
                }
                if (colType.equals("int") && (parameters.get(colName).isBlank() && !isNullable
                        && !isCorrectIntegerInput(colName, parameters.get(colName))))
                    return;
            }
            // Формирование запроса INSERT
            String colNamesString = parameters.keySet().stream().collect(Collectors.joining(","));
            StringBuilder values = new StringBuilder();
            for (String value : parameters.values()) {
                values.append(value.isBlank() ? "NULL," : "'" + value + "',");
            }
            values.deleteCharAt(values.length() - 1);
            // Отправка запроса UPDATE (если редактирование)
            if (isEditMode) {
                String sqlCols = " SET ";
                for (String col : editable) {
                    sqlCols += String.format("[%s]='%s'",col, parameters.get(col));
                    if (!col.equals(editable.get(editable.size()-1)))
                        sqlCols += ",";
                }
                connection.createStatement().execute(
                        "UPDATE " + tableNameValue + sqlCols + sqlWhere
                );
            }
            else
            // Отправка запроса INSERT
                connection.createStatement().execute(
                        "INSERT " + tableNameValue + " (" + colNamesString + ") VALUES (" + values + ")"
            );
            // Обновление таблицы
            Requests.getFullTable(table, tableNameValue);
            // Инфоомирование пользователя о созданной строке
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Успешно");
            if (isEditMode)
                success.setHeaderText("Запись успешно изменена");
            else
                success.setHeaderText("Запись успешно добавлена");
            success.showAndWait();
            // Закрытие модального окна
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean isCorrectIntegerInput(String paramName, String valueInput) {
        Alert reportInfo = new Alert(Alert.AlertType.ERROR);
        try {
            Integer.parseInt(valueInput);
            return true;
        }
        catch (NumberFormatException e) {
            reportInfo.setTitle("Ошибка");
            reportInfo.setHeaderText(String.format("Параметр %s должен быть целочисленным", paramName));
            reportInfo.show();
            return false;
        }
    }

    // Обработчик ввода в TextField для целочисленного значения, проверяющий корректность
    private void onIntegerTextFieldChanged(KeyEvent event) {
        TextField tf = (TextField) event.getSource();
        if (tf.getText().isBlank()){
            parameters.replace(tf.getId(), "");
        }
        if (event.getText().isBlank()) return;
        if (isCorrectIntegerInput(tf.getId(), tf.getText() + event.getText())) {
//            intParameters.put(tf.getId(), tf.getText() + event.getText().strip());
            parameters.replace(tf.getId(), tf.getText() + event.getText().strip());
        }
    }
    // Обработчик ввода в TextField для строчного значения
    private void onStringTextFieldChanged(KeyEvent event) {
        TextField tf = (TextField) event.getSource();
        if (tf.getText().isBlank()){
            parameters.replace(tf.getId(), "");
        }
//        otherParameters.put(tf.getId(), tf.getText());
        parameters.replace(tf.getId(), tf.getText());
    }
    // Обработчик ввода в DatePicker
    private void onDatePickerChanged(ActionEvent event) {
        DatePicker dp = (DatePicker) event.getSource();
//        otherParameters.put(dp.getId(), dp.getValue().toString());
        parameters.replace(dp.getId(), dp.getValue().toString());
    }

    // Выбор будущей даты сочтён возможным,
    // а других проверок не требуется в силу отсутствия ручного ввода
//    // Обработчик, проверяющий корректность ввода в DatePicker
//    private void onDateTextfieldChanged(ActionEvent event) {
//        Alert reportInfo = new Alert(Alert.AlertType.ERROR);
//        DatePicker tf = (DatePicker) event.getSource();
//        if (tf.getValue() == null || tf.getValue().isAfter(LocalDate.now())) {
//            reportInfo.setTitle("Ошибка");
//            reportInfo.setHeaderText(String.format("Дата в параметре %s не может быть позже", tf.getId()));
//            reportInfo.show();
//        }
//    }
}
