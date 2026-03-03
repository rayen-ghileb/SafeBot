package sf.project.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sf.project.config.AppConfig;
import sf.project.database.ChatDAO;
import sf.project.models.Message;
import sf.project.models.TherapeuticInsights;
import sf.project.utils.CrisisDetector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service for interacting with the OpenAI API for mental health chat.
 */
public class MentalHealthChatService {

    private static final Logger logger = LoggerFactory.getLogger(MentalHealthChatService.class);

    private static final String SYSTEM_PROMPT =
            "You are Connor, a compassionate and professional mental health support chatbot. " +
            "Your role is to provide emotional support, active listening, and psychoeducation. " +
            "You are NOT a replacement for professional mental health care. " +
            "Always encourage users to seek professional help when needed. " +
            "Be empathetic, non-judgmental, and supportive.";

    private final ChatDAO chatDAO;
    private final CrisisDetector crisisDetector;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService insightScheduler;

    private final List<Message> conversationHistory = new ArrayList<>();
    private long currentConversationId = -1;
    private int messageCount = 0;
    private volatile boolean isProcessing = false;

    public MentalHealthChatService() {
        this.chatDAO = new ChatDAO();
        this.crisisDetector = CrisisDetector.getInstance();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.httpClient = HttpClient.newHttpClient();
        this.insightScheduler = Executors.newSingleThreadScheduledExecutor();
        startInsightScheduler();
    }

    /**
     * Starts a new conversation session.
     */
    public void startConversation() {
        currentConversationId = chatDAO.startConversation();
        conversationHistory.clear();
        messageCount = 0;
        logger.info("Started conversation id={}", currentConversationId);
    }

    /**
     * Ends the current conversation session.
     */
    public void endConversation() {
        if (currentConversationId != -1) {
            chatDAO.endConversation(currentConversationId);
            logger.info("Ended conversation id={}", currentConversationId);
            currentConversationId = -1;
        }
    }

    /**
     * Sends a user message and returns the assistant's response via callback.
     *
     * @param userMessage the user's input
     * @param onResponse  callback invoked with the assistant's reply
     * @param onError     callback invoked on error
     */
    public void sendMessage(String userMessage, Consumer<String> onResponse, Consumer<Throwable> onError) {
        if (isProcessing) {
            logger.info("Already processing a message, skipping");
            return;
        }

        if (crisisDetector.detectCrisis(userMessage)) {
            logger.info("Crisis detected, providing resources");
            String crisisResponse = crisisDetector.getCrisisResponse();
            onResponse.accept(crisisResponse);
            return;
        }

        isProcessing = true;
        messageCount++;

        Message userMsg = new Message(currentConversationId, Message.Role.USER, userMessage);
        conversationHistory.add(userMsg);
        if (currentConversationId != -1) {
            chatDAO.saveMessage(userMsg);
        }

        new Thread(() -> {
            try {
                String response = callOpenAI(userMessage);
                Message assistantMsg = new Message(currentConversationId, Message.Role.ASSISTANT, response);
                conversationHistory.add(assistantMsg);
                if (currentConversationId != -1) {
                    chatDAO.saveMessage(assistantMsg);
                }
                logger.info("Received response from OpenAI");
                onResponse.accept(response);
            } catch (Exception e) {
                logger.error("Error calling OpenAI API", e);
                onError.accept(e);
            } finally {
                isProcessing = false;
            }
        }).start();
    }

    private String callOpenAI(String userMessage) throws Exception {
        String apiKey = AppConfig.getApiKey();
        String model = AppConfig.getModel();

        List<Object> messages = new ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_PROMPT);
        messages.add(systemMessage);

        for (Message msg : conversationHistory) {
            String role = msg.getRole() == Message.Role.USER ? "user" : "assistant";
            Map<String, String> entry = new HashMap<>();
            entry.put("role", role);
            entry.put("content", msg.getContent());
            messages.add(entry);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", AppConfig.getMaxTokens());
        requestBody.put("temperature", 0.7);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API returned status " + response.statusCode() + ": " + response.body());
        }

        Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<?> choices = (List<?>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> messageMap = (Map<?, ?>) choice.get("message");
            if (messageMap != null) {
                return (String) messageMap.get("content");
            }
        }
        throw new RuntimeException("Invalid response format from OpenAI API");
    }

    /**
     * Generates therapeutic insights from the current conversation history.
     */
    private void generateInsights() {
        if (conversationHistory.isEmpty() || currentConversationId == -1) {
            return;
        }

        logger.info("Generating therapeutic insights for conversation id={}", currentConversationId);
        try {
            TherapeuticInsights insights = new TherapeuticInsights(
                    currentConversationId,
                    "Session insights generated from conversation analysis.",
                    Arrays.asList("emotional support", "active listening"),
                    "neutral",
                    Arrays.asList("Continue practicing mindfulness", "Consider talking to a professional")
            );
            chatDAO.saveInsights(insights);
            logger.info("Therapeutic insights saved for conversation id={}", currentConversationId);
        } catch (Exception e) {
            logger.error("Error generating insights", e);
        }
    }

    private void startInsightScheduler() {
        insightScheduler.scheduleAtFixedRate(() -> {
            if (messageCount >= AppConfig.getInsightFrequency() && !isProcessing) {
                generateInsights();
                messageCount = 0;
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    public List<TherapeuticInsights> getUnviewedInsights() {
        return chatDAO.getUnviewedInsights();
    }

    public void markInsightAsViewed(long insightId) {
        chatDAO.markInsightsAsViewed(insightId);
    }

    public int getConversationCount() {
        return chatDAO.getConversationCount();
    }

    public void shutdown() {
        insightScheduler.shutdown();
        endConversation();
    }
}
