package com.flashlearn.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashlearn.dto.request.AiGenerateCardsRequest;
import com.flashlearn.dto.response.AiCardDraftResponse;
import com.flashlearn.dto.response.AiGenerateCardsResponse;
import com.flashlearn.exception.AiIntegrationException;
import com.flashlearn.service.AiAbuseProtectionService;
import com.flashlearn.service.AiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final int DEFAULT_CARD_COUNT = 10;
    private static final int MAX_SOURCE_TEXT_LENGTH = 20_000;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}]{4,}");

    private final ObjectMapper objectMapper;
    private final AiAbuseProtectionService aiAbuseProtectionService;

    @Value("${app.ai.ollama.enabled:false}")
    private boolean enabled;

    @Value("${app.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${app.ai.ollama.model:qwen2.5:7b-instruct}")
    private String model;

    @Value("${app.ai.ollama.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${app.ai.ollama.auth-token:}")
    private String authToken;

    @Override
    public AiGenerateCardsResponse generateCards(AiGenerateCardsRequest request, Long userId) {
        aiAbuseProtectionService.validateGenerationAllowed(userId);
        if (!enabled) {
            throw new AiIntegrationException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI-функция временно недоступна: интеграция с Ollama отключена"
            );
        }

        int desiredCount = request.getDesiredCount() != null ? request.getDesiredCount() : DEFAULT_CARD_COUNT;
        String sourceText = normalizeSourceText(request.getSourceText());
        String prompt = buildPrompt(sourceText, desiredCount);

        HttpResponse<String> response = callOllama(prompt);
        List<AiCardDraftResponse> cards = parseCards(response.body(), sourceText, desiredCount);

        log.info("AI сгенерировал карточки: userId={}, model={}, requested={}, returned={}",
                userId, model, desiredCount, cards.size());

        return AiGenerateCardsResponse.builder()
                .provider("ollama")
                .model(model)
                .cards(cards)
                .build();
    }

    private String normalizeSourceText(String sourceText) {
        String normalized = sourceText == null ? "" : sourceText.trim();
        if (normalized.length() > MAX_SOURCE_TEXT_LENGTH) {
            return normalized.substring(0, MAX_SOURCE_TEXT_LENGTH);
        }
        return normalized;
    }

    private HttpResponse<String> callOllama(String prompt) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("stream", false);
            payload.put("format", "json");
            payload.put("prompt", prompt);
            payload.put("options", Map.of(
                    "temperature", 0.2,
                    "top_p", 0.9
            ));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

            if (StringUtils.hasText(authToken)) {
                requestBuilder.header("Authorization", "Bearer " + authToken.trim());
            }

            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiIntegrationException(
                        HttpStatus.BAD_GATEWAY,
                        "Ollama вернула ошибку HTTP " + response.statusCode()
                );
            }
            return response;
        } catch (JsonProcessingException e) {
            throw new AiIntegrationException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сериализовать AI-запрос");
        } catch (IOException e) {
            throw new AiIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "Не удалось обратиться к Ollama. Проверьте base-url и доступность сервиса"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiIntegrationException(HttpStatus.GATEWAY_TIMEOUT, "Запрос к Ollama был прерван");
        } catch (IllegalArgumentException e) {
            throw new AiIntegrationException(HttpStatus.BAD_REQUEST, "Некорректный base-url для Ollama");
        }
    }

    private List<AiCardDraftResponse> parseCards(String ollamaBody, String sourceText, int desiredCount) {
        try {
            JsonNode root = objectMapper.readTree(ollamaBody);
            String llmJson = root.path("response").asText();
            if (!StringUtils.hasText(llmJson)) {
                throw new AiIntegrationException(HttpStatus.BAD_GATEWAY, "Ollama вернула пустой ответ");
            }

            OllamaCardsPayload payload = objectMapper.readValue(llmJson, OllamaCardsPayload.class);
            if (payload.getCards() == null || payload.getCards().isEmpty()) {
                throw new AiIntegrationException(HttpStatus.BAD_GATEWAY, "AI не вернул ни одной карточки");
            }

            Set<String> sourceTokens = extractTokens(sourceText);
            List<AiCardDraftResponse> result = new ArrayList<>();
            int skippedAsIrrelevant = 0;
            for (OllamaCard card : payload.getCards()) {
                String front = trimToNull(card.getFront());
                String back = trimToNull(card.getBack());
                if (front == null || back == null) {
                    continue;
                }
                if (!isGroundedInSource(sourceTokens, front, back, card.getHint())) {
                    skippedAsIrrelevant++;
                    continue;
                }
                result.add(AiCardDraftResponse.builder()
                        .front(front)
                        .back(back)
                        .hint(trimToNull(card.getHint()))
                        .position(result.size() + 1)
                        .build());
                if (result.size() >= desiredCount) {
                    break;
                }
            }

            if (result.isEmpty()) {
                throw new AiIntegrationException(HttpStatus.BAD_GATEWAY, "AI вернул некорректные карточки");
            }
            if (skippedAsIrrelevant > 0) {
                log.warn("AI карточки отфильтрованы как нерелевантные: skipped={}, kept={}", skippedAsIrrelevant, result.size());
            }
            return result;
        } catch (JsonProcessingException e) {
            throw new AiIntegrationException(HttpStatus.BAD_GATEWAY, "Не удалось разобрать ответ от Ollama");
        }
    }

    private boolean isGroundedInSource(Set<String> sourceTokens, String front, String back, String hint) {
        if (sourceTokens.isEmpty()) {
            return true;
        }
        String candidate = front + " " + back + " " + (hint == null ? "" : hint);
        Set<String> cardTokens = extractTokens(candidate);
        for (String token : cardTokens) {
            if (sourceTokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> extractTokens(String text) {
        Set<String> tokens = new HashSet<>();
        if (!StringUtils.hasText(text)) {
            return tokens;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildPrompt(String sourceText, int desiredCount) {
        return """
                Ты помощник для сервиса интервального повторения.
                По исходному тексту создай ровно %d учебных карточек.
                Используй язык исходного текста.

                Верни ТОЛЬКО валидный JSON-объект без markdown:
                {
                  "cards": [
                    { "front": "вопрос", "back": "ответ", "hint": "короткая подсказка или null" }
                  ]
                }

                Правила:
                - вопрос краткий и однозначный;
                - ответ проверяемый и конкретный;
                - без дубликатов;
                - если фактов не хватает, не выдумывай;
                - карточки должны быть строго по материалу исходного текста;
                - не добавляй посторонние факты и аббревиатуры;
                - язык всех полей (front, back, hint) должен точно совпадать с языком исходного текста;
                - не смешивай языки в одной карточке и не добавляй англоязычные вставки, если исходный текст не на английском.

                Исходный текст:
                %s
                """.formatted(desiredCount, sourceText);
    }

    @Data
    private static class OllamaCardsPayload {
        private List<OllamaCard> cards;
    }

    @Data
    private static class OllamaCard {
        private String front;
        private String back;
        private String hint;
    }
}
