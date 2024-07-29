module com.example.comp5504_hw2_133343 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.comp5504_hw2_133343 to javafx.fxml;
    exports com.example.comp5504_hw2_133343;
}