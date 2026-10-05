package com.syncron;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlControllerBindingTest {

    private static final Pattern CONTROLLER_PATTERN =
            Pattern.compile("fx:controller=\"([^\"]+)\"");

    @Test
    void allFxmlControllersResolveToClasses() throws Exception {
        Path viewsRoot = Paths.get("src/main/resources/com/syncron/views");
        assertTrue(Files.exists(viewsRoot), "Views directory not found.");

        try (Stream<Path> stream = Files.walk(viewsRoot)) {
            for (Path path : stream.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                String xml = Files.readString(path);
                Matcher matcher = CONTROLLER_PATTERN.matcher(xml);
                if (matcher.find()) {
                    String controller = matcher.group(1);
                    assertNotNull(Class.forName(controller), "Controller class missing for " + path);
                }
            }
        }
    }
}
