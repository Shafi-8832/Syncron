package com.syncron.utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TimeEngine {

    /**
     * Attaches a live, ticking countdown to any JavaFX Label.
     * @param targetLabel The Label to update every second.
     * @param type "ONLINE" or "OFFLINE".
     * @param startDateTime The exact time it opens (e.g., "2026-03-30 14:00").
     * @param endDateTime The exact time it closes.
     */
    public static void startLiveCountdown(Label targetLabel, String type, LocalDateTime startDateTime, LocalDateTime endDateTime) {

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            // SCENARIO 1: The Evaluation is CLOSED
            if (now.isAfter(endDateTime) || now.isEqual(endDateTime)) {
                targetLabel.setText("Status: Closed. No more submissions accepted.");
                targetLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-font-size: 13px;"); // Red
                return;
            }

            // SCENARIO 2: It is an ONLINE test that hasn't started yet!
            if (type.equals("ONLINE") && now.isBefore(startDateTime)) {
                String timeLeft = formatTimeDifference(now, startDateTime);
                targetLabel.setText("Starts in: " + timeLeft);
                targetLabel.setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold; -fx-font-size: 13px;"); // Orange
                return;
            }

            // SCENARIO 3: It is currently OPEN and ticking down to the deadline!
            String timeLeft = formatTimeDifference(now, endDateTime);
            targetLabel.setText(type.equals("ONLINE") ? "Time Remaining: " + timeLeft : "Ends in: " + timeLeft);

            // Turn red if less than 1 hour remains!
            if (ChronoUnit.HOURS.between(now, endDateTime) < 1) {
                targetLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-font-size: 13px;");
            } else {
                targetLabel.setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold; -fx-font-size: 13px;"); // Green
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE); // Run forever
        timeline.play();
    }

    // Helper method to turn a time gap into beautiful text like "1 day 14 hours" or "00:45:12"
    private static String formatTimeDifference(LocalDateTime from, LocalDateTime to) {
        long days = ChronoUnit.DAYS.between(from, to);
        LocalDateTime temp = from.plusDays(days);

        long hours = ChronoUnit.HOURS.between(temp, to);
        temp = temp.plusHours(hours);

        long minutes = ChronoUnit.MINUTES.between(temp, to);
        temp = temp.plusMinutes(minutes);

        long seconds = ChronoUnit.SECONDS.between(temp, to);

        if (days > 0) {
            return days + " day" + (days > 1 ? "s " : " ") + hours + " hr" + (hours != 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hr " + minutes + " min";
        } else {
            // If less than an hour, show a digital ticking clock format: 45:12
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}