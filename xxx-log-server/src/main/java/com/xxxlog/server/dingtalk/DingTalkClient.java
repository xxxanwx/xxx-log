package com.xxxlog.server.dingtalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxlog.server.config.DingTalkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "xxx-log.dingtalk", name = "enabled", havingValue = "true")
public class DingTalkClient {

    private static final Logger log = LoggerFactory.getLogger(DingTalkClient.class);

    private final DingTalkProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DingTalkClient(DingTalkProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void sendMarkdown(String title, String markdown) {
        if (!StringUtils.hasText(properties.getWebhookUrl())) {
            log.warn("DingTalk webhook-url is empty, skip send");
            return;
        }
        try {
            Map<String, Object> markdownBody = new HashMap<>();
            markdownBody.put("title", title);
            markdownBody.put("text", markdown);

            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "markdown");
            payload.put("markdown", markdownBody);

            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildSignedUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("DingTalk send failed, status={}, body={}", response.statusCode(), response.body());
            } else if (response.body() != null && !response.body().contains("\"errcode\":0")) {
                log.warn("DingTalk send rejected: {}", response.body());
            } else {
                log.info("DingTalk message sent, title={}", title);
            }
        } catch (Exception e) {
            log.warn("DingTalk send error: {}", e.getMessage());
        }
    }

    private String buildSignedUrl() throws Exception {
        String url = properties.getWebhookUrl();
        if (!StringUtils.hasText(properties.getSecret())) {
            return url;
        }
        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + properties.getSecret();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
        String connector = url.contains("?") ? "&" : "?";
        return url + connector + "timestamp=" + timestamp + "&sign=" + sign;
    }
}
