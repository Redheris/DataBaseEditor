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
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class EditOrAddRowWindow implements Initializable {
    // TODO:
    // - Сохранение введённых значений в переменные по нажатию кнопки "Отправить"
    // - Получение этих значений и контроллера и отправка запроса INSERT в БД

    @FXML
    private Label tableName;
    @FXML
    private VBox parametersBlock;
    @FXML
    private Button cancelButton;

    private static String tableNameValue;
    private static Map<String, String> parameters = new HashMap<>();

    private static String getURL() {
        return "jdbc:sqlserver://localhost;encrypt=true;trustServerCertificate=true;" +
                "databaseName=" + DBEditorController.db + ";" +
                "username=" + DBEditorController.user + ";" +
                "password=" + DBEditorController.pass + ";";
    }

    protected static void setTableColumns(String tableName) {
        tableNameValue = tableName;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tableName.setText(tableNameValue);
        final String URL = getURL();
        try (Connection connection = DriverManager.getConnection(URL);
             Statement st = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();

            System.out.println("Where? " + tableNameValue);

            // Получение всех столбцов таблицы tableName
            ResultSet colNames = metaData.getColumns(null, null, tableNameValue, null);
            while (colNames.next()) {
                String colName = colNames.getString("COLUMN_NAME");
                String colType = colNames.getString("TYPE_NAME");
                System.out.println(colName + " *** " + colType);

                if (colType.equals("int identity")) continue;

                HBox param = new HBox();
                param.setSpacing(20);
                // Название столбца
                Label paramName = new Label(colName);
                paramName.setPrefWidth(130);
                param.getChildren().add(paramName);

                switch (colType) {
                    case "int" -> {
                        // Ввод значения
                        TextField tf = new TextField();
                        tf.setId(colName);
                        tf.setOnKeyPressed(this::onIntegerTextfieldChanged);
                        param.getChildren().add(tf);
                    }
                    case "varchar" -> {
                        TextField tf = new TextField();
                        tf.setId(colName);
                        param.getChildren().add(tf);
                    }
                    case "date" -> {
                        DatePicker dp = new DatePicker();
                        dp.setEditable(false);
                        dp.setId(colName);
                        // Выбор будущей даты сочтён возможным,
                        // а других проверок не требуется в силу отсутствия ручного ввода
//                        dp.setOnAction(this::onDateTextfieldChanged);
                        param.getChildren().add(dp);
                    }
                    default -> {
                    }
                }
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

    }


    // Обработчик, проверяющий корректность ввода в целочисленный TextField
    private void onIntegerTextfieldChanged(KeyEvent event) {
        if (event.getText().isBlank()) return;
        Alert reportInfo = new Alert(Alert.AlertType.ERROR);
        TextField tf = (TextField) event.getSource();
        try {
            int value = Integer.parseInt(tf.getText() + event.getText());
            parameters.put(tf.getId(), String.valueOf(value));
        }
        catch (NumberFormatException e) {
            reportInfo.setTitle("Ошибка");
            reportInfo.setHeaderText(String.format("Параметр %s должен быть целочисленным", tf.getId()));
            reportInfo.show();
        }
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
