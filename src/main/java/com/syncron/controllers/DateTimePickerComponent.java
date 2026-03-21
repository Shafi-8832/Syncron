package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateTimePickerComponent {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private DatePicker datePicker;

    @FXML
    private ComboBox<String> hourComboBox;

    @FXML
    private ComboBox<String> minuteComboBox;

    @FXML
    private ComboBox<String> amPmComboBox;

    @FXML
    public void initialize() {
        for (int hour = 1; hour <= 12; hour++) {
            hourComboBox.getItems().add(String.format("%02d", hour));
        }

        for (int minute = 0; minute <= 59; minute++) {
            minuteComboBox.getItems().add(String.format("%02d", minute));
        }

        amPmComboBox.getItems().addAll("AM", "PM");

        LocalTime now = LocalTime.now();
        int hour24 = now.getHour();
        int hour12 = hour24 % 12;

        if (hour12 == 0) {
            hour12 = 12;
        }

        datePicker.setValue(LocalDate.now());
        hourComboBox.setValue(String.format("%02d", hour12));
        minuteComboBox.setValue(String.format("%02d", now.getMinute()));
        amPmComboBox.setValue(hour24 >= 12 ? "PM" : "AM");
    }

    /**
     * Returns selected date-time in {@code yyyy-MM-dd hh:mm AM/PM} format.
     */
    public String getDateTimeString() {
        LocalDate selectedDate = datePicker.getValue();
        String selectedHour = hourComboBox.getValue();
        String selectedMinute = minuteComboBox.getValue();
        String selectedAmPm = amPmComboBox.getValue();

        if (selectedDate == null || selectedHour == null || selectedMinute == null || selectedAmPm == null) {
            return "";
        }

        return String.format("%s %s:%s %s", selectedDate.format(DATE_FORMATTER), selectedHour, selectedMinute, selectedAmPm);
    }
}
