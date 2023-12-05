module rh.db.databaseeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens rh.db.databaseeditor to javafx.fxml;
    exports rh.db.databaseeditor;
}