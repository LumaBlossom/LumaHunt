package luma.hunt.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import luma.hunt.LumaHunt;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class SupabaseClient {
    private static SupabaseClient instance;
    
    private static final String PROXY_URL = "https://lumahunt-api.vercel.app";
    
    private static final Gson GSON = new GsonBuilder().create();
    private final HttpClient httpClient;

    private SupabaseClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    public CompletableFuture<Boolean> createLobby(String code, String ip, int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("code", code);
                json.addProperty("ip", ip);
                json.addProperty("port", port);
                
                String requestBody = GSON.toJson(json);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PROXY_URL + "/api/lobby"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 201) {
                    LumaHunt.LOGGER.info("Lobby created successfully: " + code);
                    return true;
                } else {
                    LumaHunt.LOGGER.error("Failed to create lobby. Status: " + response.statusCode() + " Body: " + response.body());
                    return false;
                }
            } catch (Exception e) {
                LumaHunt.LOGGER.error("Exception creating lobby", e);
                return false;
            }
        });
    }

    public CompletableFuture<JsonObject> getLobby(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PROXY_URL + "/api/lobby?code=" + code))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    return JsonParser.parseString(response.body()).getAsJsonObject();
                } else {
                    LumaHunt.LOGGER.error("Failed to get lobby. Status: " + response.statusCode());
                }
                return null;
            } catch (Exception e) {
                LumaHunt.LOGGER.error("Exception getting lobby", e);
                return null;
            }
        });
    }
    
    public CompletableFuture<Boolean> deleteLobby(String code) {
         return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PROXY_URL + "/api/lobby?code=" + code))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    LumaHunt.LOGGER.info("Lobby deleted: " + code);
                    return true;
                } else {
                    LumaHunt.LOGGER.error("Failed to delete lobby. Status: " + response.statusCode());
                    return false;
                }
            } catch (Exception e) {
                LumaHunt.LOGGER.error("Exception deleting lobby", e);
                return false;
            }
        });
    }
}
