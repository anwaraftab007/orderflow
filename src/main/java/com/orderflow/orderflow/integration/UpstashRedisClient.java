package com.orderflow.orderflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;

@Component
public class UpstashRedisClient {
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String token;

    public UpstashRedisClient(ObjectMapper objectMapper, @Value("${redis.url}") String url,
                              @Value("${redis.token}") String token) {
        this.objectMapper = objectMapper;
        this.endpoint = URI.create(url.endsWith("/") ? url.substring(0, url.length() - 1) : url);
        this.token = token;
    }

    public String get(String key) { return resultText(command(List.of("GET", key))); }
    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        return "OK".equals(resultText(command(List.of("SET", key, value, "NX", "EX", ttlSeconds))));
    }
    public void set(String key, String value, long ttlSeconds) {
        command(List.of("SET", key, value, "EX", ttlSeconds));
    }
    public void delete(String key) { command(List.of("DEL", key)); }

    public boolean compareAndSet(String key, String expected, String replacement, long ttlSeconds) {
        String script = "if redis.call('GET',KEYS[1])==ARGV[1] then redis.call('SET',KEYS[1],ARGV[2],'EX',ARGV[3]); return 1 else return 0 end";
        return command(List.of("EVAL", script, 1, key, expected, replacement, ttlSeconds)).path("result").asInt() == 1;
    }
    public void compareAndDelete(String key, String expected) {
        String script = "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) else return 0 end";
        command(List.of("EVAL", script, 1, key, expected));
    }

    private JsonNode command(List<?> command) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String body = objectMapper.writeValueAsString(command);
                HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5))
                        .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 500) throw new RedisOperationException("Redis temporarily unavailable");
                JsonNode json = objectMapper.readTree(response.body());
                if (response.statusCode() >= 400 || json.has("error")) throw new RedisOperationException("Redis command failed");
                return json;
            } catch (RedisOperationException exception) {
                last = exception;
                if (attempt == 3) break;
            } catch (Exception exception) {
                last = new RedisOperationException("Redis temporarily unavailable", exception);
                if (attempt == 3) break;
            }
            try { Thread.sleep(100L * attempt); } catch (InterruptedException exception) {
                Thread.currentThread().interrupt(); throw new RedisOperationException("Redis operation interrupted", exception);
            }
        }
        throw last;
    }

    private String resultText(JsonNode response) {
        JsonNode result = response.get("result");
        return result == null || result.isNull() ? null : result.asText();
    }
}
