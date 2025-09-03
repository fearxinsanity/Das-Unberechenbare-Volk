module Das.Unberechenbare.Volk {
    requires javafx.controls;
    requires javafx.fxml;

    // Exportiert das Hauptpaket und die Unterpakete, um sie extern zugänglich zu machen
    exports com.unberechenbarevolk;
    exports com.unberechenbarevolk.controller;
    exports com.unberechenbarevolk.model;
    exports com.unberechenbarevolk.view;

    // Öffnet das View-Paket für das FXML-Framework, damit es auf die FXML-Dateien zugreifen kann
    opens com.unberechenbarevolk.view to javafx.fxml;
}