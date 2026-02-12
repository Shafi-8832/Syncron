module com.syncron {
    // 1. Core JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // 2. Database & SQL (IntelliJ fixed this, but we need the driver too)
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    // 3. The Libraries you added (Bootstrap, ControlsFX)
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    // 4. PERMISSIONS (Crucial for FXML to work)
    opens com.syncron to javafx.fxml;
    exports com.syncron;

    // Allow JavaFX to "see" your Controllers (Login, Dashboard)
    exports com.syncron.controllers;
    opens com.syncron.controllers to javafx.fxml;

    // Allow access to your Database Utils
    exports com.syncron.utils;
    opens com.syncron.models to javafx.base;
}