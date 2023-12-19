package rh.db.databaseeditor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ReportModalWindow implements Initializable {
    @FXML
    private Label reportName, idParamName;
    @FXML
    private VBox idBlock, datePickerBlock;
    @FXML
    private Button cancelButton;
    @FXML
    private TextField id;
    @FXML
    private DatePicker dateFrom, dateTo;

    // Проверка на
    private static boolean isPassed = false;

    protected static String reportTitle, idValue, dateToValue, dateFromValue;
    private static String nameOfIdParam = "Идентификатор ";
    private static boolean idIsNeeded = false;
    private static boolean dateIsNeeded = false;

    public void onSendButtonClick(ActionEvent actionEvent) {
        Alert reportInfo = new Alert(Alert.AlertType.ERROR);
        if (idIsNeeded && id.getText().isEmpty() ||
                (dateIsNeeded && (dateFrom.getValue() == null || dateTo.getValue() == null))) {
            reportInfo.setTitle("Ошибка");
            reportInfo.setHeaderText("Введены не все необходимые данные");
            reportInfo.showAndWait();
            return;
        }
        if (dateIsNeeded && dateFrom.getValue() != null && dateTo.getValue() != null &&
                (dateFrom.getValue().isAfter(LocalDate.now()) || dateTo.getValue().isAfter(LocalDate.now()))) {
            reportInfo.setTitle("Ошибка");
            reportInfo.setHeaderText("Временной период не может быть позже текущей даты");
            reportInfo.showAndWait();
            return;
        }
        if (dateIsNeeded && dateFrom.getValue() != null && dateTo.getValue() != null &&
                (dateFrom.getValue().isAfter(dateTo.getValue()))){
            reportInfo.setTitle("Ошибка");
            reportInfo.setHeaderText("Начальная дата не может быть позже конечной");
            reportInfo.showAndWait();
            return;
        }
        try {
            if (idIsNeeded) {
                int id = Integer.parseInt(this.id.getText());
                idValue = String.valueOf(id);
            }
        }
        catch (NumberFormatException e){
            reportInfo.setTitle("Ошибка");
            reportInfo.setHeaderText("Основной параметр должен быть целочисленным");
            reportInfo.show();
            return;
        }
        if (dateIsNeeded) {
            dateFromValue = dateFrom.getValue().toString();
            dateToValue = dateTo.getValue().toString();
        }
        // Указываем, что параметры отчёта получены успешно
        isPassed = true;
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void onCancelButtonClick(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    protected static void setReportParams(String reportName, boolean needId, boolean needDate, String idParamName) {
        reportTitle = reportName;
        nameOfIdParam = idParamName;
        idIsNeeded = needId;
        dateIsNeeded = needDate;
    }
    protected static void setReportParams(String reportName, boolean needId, boolean needDate) {
        setReportParams(reportName, needId, needDate, "none");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isPassed = false;
        idBlock.setVisible(idIsNeeded);
        idBlock.setManaged(idIsNeeded);
        datePickerBlock.setVisible(dateIsNeeded);
        datePickerBlock.setManaged(dateIsNeeded);
        idParamName.setText(nameOfIdParam);
        reportName.setText(reportTitle);
        // Сброс введённых ранее данных
        id.setText("");
        dateFrom.setValue(null);
        dateTo.setValue(null);
    }

    public static boolean isPassed() {
        return isPassed;
    }
}
