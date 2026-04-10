package com.syncron.controllers;

import com.syncron.models.Course;
import com.syncron.utils.NavigationManager;
import com.syncron.utils.ServerConfig;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarController {

    @FXML private Label monthYearLabel;
    @FXML private GridPane dayHeaderGrid;
    @FXML private GridPane calendarGrid;
    @FXML private HBox legendBox;
    @FXML private VBox dayDetailPanel;

    private YearMonth currentMonth;
    private List<Map<String, String>> allEvents = new ArrayList<>();

    // Color scheme for assessment types
    private static final Map<String, String[]> TYPE_COLORS = new LinkedHashMap<>();
    static {
        TYPE_COLORS.put("CT", new String[]{"#FDEBD0", "#D35400", "#E67E22"});           // bg, text, border
        TYPE_COLORS.put("ASSIGNMENT", new String[]{"#D5F5E3", "#1E8449", "#27AE60"});
        TYPE_COLORS.put("OFFLINE", new String[]{"#E8DAEF", "#6C3483", "#8E44AD"});
        TYPE_COLORS.put("ONLINE", new String[]{"#D6EAF8", "#1A5276", "#2980B9"});
    }

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        buildLegend();
        buildDayHeaders();
        fetchEventsAndRender();
    }

    // DATA FETCH

    private void fetchEventsAndRender() {
        allEvents.clear();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String role = SessionManager.getCurrentUser().getRole();
            String name = SessionManager.getCurrentUser().getName().replace(" ", "%20");

            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/calendar/evaluations?role=" + role + "&name=" + name))
                    .GET().build();

            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
                allEvents = gson.fromJson(res.body(), listType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        renderMonth();
    }

    // LEGEND

    private void buildLegend() {
        legendBox.getChildren().clear();
        for (Map.Entry<String, String[]> entry : TYPE_COLORS.entrySet()) {
            HBox item = new HBox(6);
            item.setAlignment(Pos.CENTER_LEFT);

            Region dot = new Region();
            dot.setMinSize(10, 10);
            dot.setMaxSize(10, 10);
            dot.setStyle("-fx-background-color: " + entry.getValue()[2] + "; -fx-background-radius: 50;");

            Label lbl = new Label(entry.getKey().charAt(0) + entry.getKey().substring(1).toLowerCase());
            lbl.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px; -fx-font-weight: bold;");

            item.getChildren().addAll(dot, lbl);
            legendBox.getChildren().add(item);
        }
    }

    // DAY-OF-WEEK HEADERS

    private void buildDayHeaders() {
        dayHeaderGrid.getChildren().clear();
        dayHeaderGrid.getColumnConstraints().clear();

        String[] days = {"Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"};
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            col.setHgrow(Priority.ALWAYS);
            dayHeaderGrid.getColumnConstraints().add(col);

            Label dayLabel = new Label(days[i]);
            String color = (i == 0) ? "#E74C3C" : "#7F8C8D"; // Saturday = red
            dayLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setPadding(new Insets(8, 0, 8, 0));

            dayHeaderGrid.add(dayLabel, i, 0);
        }
    }

    // MONTH RENDERING

    private void renderMonth() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        // Hide detail panel
        dayDetailPanel.setVisible(false);
        dayDetailPanel.setManaged(false);

        monthYearLabel.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentMonth.getYear());

        // Column constraints
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            col.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(col);
        }

        // Group events by deadline date
        Map<String, List<Map<String, String>>> eventsByDate = new LinkedHashMap<>();
        for (Map<String, String> ev : allEvents) {
            String dDate = ev.get("deadlineDate");
            if (dDate != null && !dDate.equals("null")) {
                eventsByDate.computeIfAbsent(dDate, k -> new ArrayList<>()).add(ev);
            }
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int daysInMonth = currentMonth.lengthOfMonth();

        // Calculate starting column (Saturday = 0)
        DayOfWeek firstDow = firstOfMonth.getDayOfWeek();
        int startCol = (firstDow.getValue() + 1) % 7; // Saturday=0, Sunday=1, ... Friday=6

        LocalDate today = LocalDate.now();
        int row = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            int col = (startCol + day - 1) % 7;
            if (day > 1 && col == 0) row++;

            String dateStr = date.toString(); // yyyy-MM-dd
            List<Map<String, String>> dayEvents = eventsByDate.getOrDefault(dateStr, Collections.emptyList());
            boolean isToday = date.equals(today);
            boolean isPast = date.isBefore(today);
            boolean isSaturday = col == 0;

            VBox cell = buildDayCell(day, dayEvents, isToday, isPast, isSaturday, dateStr);
            calendarGrid.add(cell, col, row);
        }
    }

    private VBox buildDayCell(int day, List<Map<String, String>> events, boolean isToday, boolean isPast, boolean isSaturday, String dateStr) {
        VBox cell = new VBox(3);
        cell.setMinHeight(90);
        cell.setPadding(new Insets(6, 6, 6, 6));
        cell.setAlignment(Pos.TOP_LEFT);

        // Cell styling
        String baseBg = isToday ? "#FFF8F0" : "#FFFFFF";
        String borderColor = isToday ? "#D35400" : "#F0F0F0";
        String borderWidth = isToday ? "2" : "1";
        double opacity = isPast ? 0.6 : 1.0;

        String baseStyle = "-fx-background-color: " + baseBg + "; -fx-background-radius: 10; -fx-border-color: " + borderColor + "; -fx-border-radius: 10; -fx-border-width: " + borderWidth + "; -fx-opacity: " + opacity + ";";
        String hoverStyle = "-fx-background-color: " + (isToday ? "#FEF0E0" : "#F8FAFB") + "; -fx-background-radius: 10; -fx-border-color: " + (events.isEmpty() ? "#D5DBDB" : "#3498DB") + "; -fx-border-radius: 10; -fx-border-width: " + borderWidth + "; -fx-cursor: hand; -fx-opacity: 1.0;";

        cell.setStyle(baseStyle);
        if (!events.isEmpty()) {
            cell.setOnMouseEntered(e -> cell.setStyle(hoverStyle));
            cell.setOnMouseExited(e -> cell.setStyle(baseStyle));
            cell.setOnMouseClicked(e -> showDayDetail(dateStr, events));
        }

        // Day number
        Label dayNum = new Label(String.valueOf(day));
        String dayColor = isSaturday ? "#E74C3C" : (isToday ? "#D35400" : "#2C3E50");
        String dayWeight = isToday ? "bold" : "normal";
        dayNum.setStyle("-fx-font-size: 14px; -fx-font-weight: " + dayWeight + "; -fx-text-fill: " + dayColor + ";");

        // Today indicator
        if (isToday) {
            HBox todayRow = new HBox(5);
            todayRow.setAlignment(Pos.CENTER_LEFT);
            Label todayBadge = new Label("today");
            todayBadge.setStyle("-fx-background-color: #D35400; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 1 6; -fx-background-radius: 8;");
            todayRow.getChildren().addAll(dayNum, todayBadge);
            cell.getChildren().add(todayRow);
        } else {
            cell.getChildren().add(dayNum);
        }

        // Event pills (max 3 visible, then "+N more")
        int maxVisible = 3;
        int count = 0;
        for (Map<String, String> ev : events) {
            if (count >= maxVisible) {
                Label more = new Label("+" + (events.size() - maxVisible) + " more");
                more.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 9px; -fx-font-weight: bold;");
                cell.getChildren().add(more);
                break;
            }

            String type = ev.get("type") != null ? ev.get("type").toUpperCase() : "CT";
            String[] colors = TYPE_COLORS.getOrDefault(type, new String[]{"#ECF0F1", "#7F8C8D", "#BDC3C7"});

            Label pill = new Label(truncate(ev.get("title"), 14));
            pill.setMaxWidth(Double.MAX_VALUE);
            pill.setStyle("-fx-background-color: " + colors[0] + "; -fx-text-fill: " + colors[1] + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 5; -fx-background-radius: 4; -fx-border-color: " + colors[2] + "40; -fx-border-radius: 4; -fx-border-width: 0.5;");

            cell.getChildren().add(pill);
            count++;
        }

        return cell;
    }

    // DAY DETAIL PANEL

    private void showDayDetail(String dateStr, List<Map<String, String>> events) {
        dayDetailPanel.getChildren().clear();
        dayDetailPanel.setVisible(true);
        dayDetailPanel.setManaged(true);

        // Date header
        LocalDate date = LocalDate.parse(dateStr);
        String niceDate = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"));

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label dateHeader = new Label(niceDate);
        dateHeader.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label countBadge = new Label(events.size() + " deadline" + (events.size() != 1 ? "s" : ""));
        countBadge.setStyle("-fx-background-color: #FDEBD0; -fx-text-fill: #D35400; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #BDC3C7; -fx-cursor: hand; -fx-font-size: 16px;");
        closeBtn.setOnAction(e -> { dayDetailPanel.setVisible(false); dayDetailPanel.setManaged(false); });

        headerRow.getChildren().addAll(dateHeader, countBadge, spacer, closeBtn);
        dayDetailPanel.getChildren().add(headerRow);

        // Event cards
        for (Map<String, String> ev : events) {
            dayDetailPanel.getChildren().add(buildEventDetailCard(ev));
        }
    }

    private VBox buildEventDetailCard(Map<String, String> ev) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15, 18, 15, 18));

        String type = ev.get("type") != null ? ev.get("type").toUpperCase() : "CT";
        String[] colors = TYPE_COLORS.getOrDefault(type, new String[]{"#ECF0F1", "#7F8C8D", "#BDC3C7"});

        String baseStyle = "-fx-background-color: " + colors[0] + "; -fx-background-radius: 10; -fx-border-color: " + colors[2] + "60; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
        String hoverStyle = baseStyle + " -fx-effect: dropshadow(three-pass-box, " + colors[2] + "60, 10, 0, 0, 3);";
        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        // Top row: type badge + course code
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(type);
        typeBadge.setStyle("-fx-background-color: " + colors[2] + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 10;");

        String courseCode = ev.get("courseCode") != null ? ev.get("courseCode") : "";
        String courseTitle = ev.get("courseTitle") != null ? ev.get("courseTitle") : "";
        Label courseLabel = new Label(courseCode + (courseTitle.isEmpty() ? "" : " — " + courseTitle));
        courseLabel.setStyle("-fx-text-fill: " + colors[1] + "; -fx-font-size: 12px;");

        topRow.getChildren().addAll(typeBadge, courseLabel);

        // Title
        Label titleLabel = new Label(ev.get("title"));
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + colors[1] + ";");
        titleLabel.setWrapText(true);

        // Time row
        String deadlineTime = ev.get("deadlineTime") != null ? ev.get("deadlineTime") : "";
        String startTime = ev.get("startTime") != null ? ev.get("startTime") : "";

        String timeText;
        if ("CT".equals(type) || "ONLINE".equals(type)) {
            timeText = "Starts: " + formatTime(startTime) + "  •  Ends: " + formatTime(deadlineTime);
        } else {
            timeText = "Due by " + formatTime(deadlineTime);
        }

        Label timeLabel = new Label(timeText);
        timeLabel.setStyle("-fx-text-fill: " + colors[1] + "; -fx-font-size: 12px; -fx-opacity: 0.8;");

        // Countdown
        try {
            LocalDate deadlineDate = LocalDate.parse(ev.get("deadlineDate"));
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate);
            String countdown;
            if (daysLeft < 0) countdown = "Deadline passed";
            else if (daysLeft == 0) countdown = "Due today!";
            else if (daysLeft == 1) countdown = "Due tomorrow";
            else countdown = daysLeft + " days left";

            Label countdownLabel = new Label(countdown);
            String countdownColor = daysLeft < 0 ? "#95A5A6" : (daysLeft <= 1 ? "#E74C3C" : colors[1]);
            countdownLabel.setStyle("-fx-text-fill: " + countdownColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

            card.getChildren().addAll(topRow, titleLabel, timeLabel, countdownLabel);
        } catch (Exception e) {
            card.getChildren().addAll(topRow, titleLabel, timeLabel);
        }

        // Click to navigate to evaluation details
        card.setOnMouseClicked(e -> navigateToEvaluation(ev));

        return card;
    }

    // NAVIGATION

    private void navigateToEvaluation(Map<String, String> ev) {
        String courseCode = ev.get("courseCode");
        String courseTitle = ev.get("courseTitle");
        String evalId = ev.get("id");
        String type = ev.get("type");

        if (courseCode == null || evalId == null) return;

        try {
            boolean isSessional = (courseTitle != null && courseTitle.toLowerCase().contains("sessional"))
                    || (courseCode.matches(".*[02468]$"));
            String courseType = isSessional ? "sessional" : "theory";
            String credits = isSessional ? "1.5" : "3.0";

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/syncron/views/main_layout.fxml"));
            javafx.scene.Parent root = loader.load();

            SessionManager.setCurrentCourseCode(courseCode);
            SessionManager.setCurrentEvaluationId(evalId);

            MainController controller = loader.getController();
            controller.setCourseContext(courseCode, courseTitle != null ? courseTitle : "Course", courseType, credits);

            // Navigate to evaluation details
            NavigationManager.switchScreen("evaluation_details.fxml");

            String parentTab = "Common";
            if ("CT".equalsIgnoreCase(type) || "ASSIGNMENT".equalsIgnoreCase(type)) parentTab = "CT and Assignments";
            else if ("ONLINE".equalsIgnoreCase(type)) parentTab = "Onlines";
            else if ("OFFLINE".equalsIgnoreCase(type)) parentTab = "Offlines";

            controller.forceSidebarSelection(parentTab);
            controller.updateBreadcrumb(parentTab + " / " + ev.get("title"));

            Stage stage = (Stage) calendarGrid.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MONTH CONTROLS


    @FXML
    private void handlePrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        renderMonth();
    }

    @FXML
    private void handleNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        renderMonth();
    }

    @FXML
    private void handleGoToToday() {
        currentMonth = YearMonth.now();
        renderMonth();
    }

    @FXML
    private void goBackToDashboard() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/syncron/views/home.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) calendarGrid.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // utilities

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 1) + "…" : text;
    }

    private String formatTime(String time24) {
        if (time24 == null || time24.isEmpty() || time24.equals("null")) return "";
        try {
            java.time.LocalTime t = java.time.LocalTime.parse(time24);
            return t.format(DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) {
            return time24;
        }
    }
}