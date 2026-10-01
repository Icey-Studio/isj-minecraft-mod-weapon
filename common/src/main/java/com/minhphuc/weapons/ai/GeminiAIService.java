package com.minhphuc.weapons.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.config.AIGeminiConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GeminiAIService {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static class AIResponse {
        private final String reply;
        private final List<String> commands;
        private final String error;

        public AIResponse(String reply, List<String> commands) {
            this.reply = reply;
            this.commands = commands;
            this.error = null;
        }

        public AIResponse(String error) {
            this.reply = null;
            this.commands = new ArrayList<>();
            this.error = error;
        }

        public String getReply() {
            return reply;
        }

        public List<String> getCommands() {
            return commands;
        }

        public String getError() {
            return error;
        }

        public boolean isSuccess() {
            return error == null;
        }
    }

    public static CompletableFuture<AIResponse> processCommandAsync(ServerLevel level, ServerPlayer player, String userPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiKey = AIGeminiConfig.getApiKey();
                if (!AIGeminiConfig.isApiKeyValid()) {
                    return new AIResponse("Chưa cấu hình API Key Gemini trong file config/weapons_ai_config.json!");
                }

                String modelName = AIGeminiConfig.getModel().trim();
                String encodedKey = java.net.URLEncoder.encode(apiKey, java.nio.charset.StandardCharsets.UTF_8);
                String encodedModel = java.net.URLEncoder.encode(modelName, java.nio.charset.StandardCharsets.UTF_8);
                String endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/" 
                        + encodedModel + ":generateContent?key=" + encodedKey;

                // Build detailed context prompt for Gemini AI
                String systemContext = String.format(
                    "Bạn là Trí Tuệ & Thực Tại của Găng Tay Vô Cực (Infinity Gauntlet AI) trong Minecraft 1.21.1.\n" +
                    "Người chơi tên '%s' (Tọa độ hiện tại: X=%.1f, Y=%.1f, Z=%.1f, Chiều không gian: %s) đưa ra mệnh lệnh:\n" +
                    "\"%s\"\n\n" +
                    "Hãy đóng vai Găng tay Vô cực và thực thi ý nguyện này của chủ nhân bằng các lệnh Minecraft 1.21.1 thích hợp.\n" +
                    "Yêu cầu BẮT BUỘC trả về định dạng JSON hợp lệ duy nhất (không bọc trong triple backticks ```json, không thêm chữ ngoài JSON):\n" +
                    "{\n" +
                    "  \"reply\": \"Lời đáp uy nghiêm, thần thái của Găng tay Vô cực bằng tiếng Việt dành cho chủ nhân\",\n" +
                    "  \"commands\": [\n" +
                    "    \"lệnh minecraft 1\",\n" +
                    "    \"lệnh minecraft 2\"\n" +
                    "  ]\n" +
                    "}\n\n" +
                    "Quy tắc khi tạo lệnh commands:\n" +
                    "- Không có dấu gạch chéo / ở đầu lệnh (ví dụ: \"kill @e[type=!player,distance=..50]\" hoặc \"weather rain\" hoặc \"summon ender_dragon ~ ~ ~\" hoặc \"fill ~-3 ~ ~-3 ~3 ~5 ~3 diamond_block\").\n" +
                    "- Suy luận lệnh Minecraft tối ưu nhất theo mệnh lệnh tự nhiên của người chơi.",
                    player.getName().getString(),
                    player.getX(), player.getY(), player.getZ(),
                    level.dimension().location().toString(),
                    userPrompt
                );

                // Build Request JSON
                JsonObject rootObj = new JsonObject();

                JsonArray contentsArray = new JsonArray();
                JsonObject contentObj = new JsonObject();
                JsonArray partsArray = new JsonArray();
                JsonObject partObj = new JsonObject();

                partObj.addProperty("text", systemContext);
                partsArray.add(partObj);
                contentObj.add("parts", partsArray);
                contentsArray.add(contentObj);

                rootObj.add("contents", contentsArray);

                // Generation config for JSON mode
                JsonObject genConfig = new JsonObject();
                genConfig.addProperty("response_mime_type", "application/json");
                rootObj.add("generationConfig", genConfig);

                String requestBody = GSON.toJson(rootObj);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpointUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    WeaponsMod.LOGGER.error("Gemini API Error status {}: {}", response.statusCode(), response.body());
                    String errDetail = "";
                    try {
                        JsonObject errJson = JsonParser.parseString(response.body()).getAsJsonObject();
                        if (errJson.has("error") && errJson.getAsJsonObject("error").has("message")) {
                            errDetail = ": " + errJson.getAsJsonObject("error").get("message").getAsString();
                        }
                    } catch (Exception ignored) {}

                    if (response.statusCode() == 401) {
                        return new AIResponse("Lỗi API Key không hợp lệ (HTTP 401" + errDetail + "). Vui lòng kiểm tra lại API Key trong config!");
                    } else if (response.statusCode() == 404) {
                        return new AIResponse("Lỗi Model/Endpoint (HTTP 404" + errDetail + "). Kiểm tra lại tên model trong config!");
                    }
                    return new AIResponse("Lỗi Gemini API (HTTP " + response.statusCode() + errDetail + ")");
                }

                JsonObject jsonResp = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray candidates = jsonResp.getAsJsonArray("candidates");
                if (candidates == null || candidates.isEmpty()) {
                    return new AIResponse("Gemini API không trả về kết quả.");
                }

                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                String aiRawText = parts.get(0).getAsJsonObject().get("text").getAsString().trim();

                // Clean potential markdown wrappers if any
                if (aiRawText.startsWith("```json")) {
                    aiRawText = aiRawText.substring(7);
                }
                if (aiRawText.startsWith("```")) {
                    aiRawText = aiRawText.substring(3);
                }
                if (aiRawText.endsWith("```")) {
                    aiRawText = aiRawText.substring(0, aiRawText.length() - 3);
                }
                aiRawText = aiRawText.trim();

                JsonObject parsedAiObj = JsonParser.parseString(aiRawText).getAsJsonObject();
                String reply = parsedAiObj.has("reply") ? parsedAiObj.get("reply").getAsString() : "Đã thực hiện nguyện vọng của chủ nhân!";

                List<String> commandsList = new ArrayList<>();
                if (parsedAiObj.has("commands")) {
                    JsonArray cmdsArray = parsedAiObj.getAsJsonArray("commands");
                    for (JsonElement elem : cmdsArray) {
                        String cmd = elem.getAsString().trim();
                        if (cmd.startsWith("/")) {
                            cmd = cmd.substring(1);
                        }
                        if (!cmd.isEmpty()) {
                            commandsList.add(cmd);
                        }
                    }
                }

                return new AIResponse(reply, commandsList);

            } catch (Exception e) {
                WeaponsMod.LOGGER.error("Error processing AI command via Gemini", e);
                return new AIResponse("Lỗi khi xử lý AI: " + e.getMessage());
            }
        });
    }
}
