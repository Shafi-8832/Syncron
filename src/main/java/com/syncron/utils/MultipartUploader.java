package com.syncron.utils;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultipartUploader {

    public static boolean uploadFileToCloud(File file, String courseCode, String uploaderId) {
        try {
            // A random string that acts as a "wall" between the text data and the binary file data
            String boundary = "---KernelBoundary" + UUID.randomUUID().toString();
            HttpClient client = HttpClient.newHttpClient();

            List<byte[]> byteArrays = new ArrayList<>();

            // 1. Pack the Course Code
            addFormField(byteArrays, boundary, "courseCode", courseCode);
            // 2. Pack the Teacher's ID
            addFormField(byteArrays, boundary, "uploaderId", uploaderId);

            // 3. Pack the actual File Bytes
            String fileHeader = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n";
            byteArrays.add(fileHeader.getBytes(StandardCharsets.UTF_8));
            byteArrays.add(Files.readAllBytes(file.toPath()));
            byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));

            // 4. Seal the package
            byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            // 5. Merge all bytes into one massive array for transit
            int totalLength = byteArrays.stream().mapToInt(b -> b.length).sum();
            byte[] requestBody = new byte[totalLength];
            int offset = 0;
            for (byte[] b : byteArrays) {
                System.arraycopy(b, 0, requestBody, offset, b.length);
                offset += b.length;
            }

            // 6. Shoot it over the Wi-Fi!
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/upload"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper method to format text fields perfectly for HTTP POST
    private static void addFormField(List<byte[]> list, String boundary, String name, String value) {
        String field = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" +
                value + "\r\n";
        list.add(field.getBytes(StandardCharsets.UTF_8));
    }
}