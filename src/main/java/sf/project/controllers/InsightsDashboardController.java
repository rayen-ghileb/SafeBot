package sf.project.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sf.project.models.TherapeuticInsights;
import sf.project.services.MentalHealthChatService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class InsightsDashboardController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(InsightsDashboardController.class);

    @FXML private ListView<TherapeuticInsights> insightsList;
    @FXML private VBox noSelectionLabel;
    @FXML private VBox insightDetail;
    @FXML private Label summaryLabel;
    @FXML private Label emotionalToneLabel;
    @FXML private Label themesLabel;
    @FXML private Label recommendationsLabel;
    @FXML private Label dateLabel;
    @FXML private WebView insightWebView;

    private MentalHealthChatService chatService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        insightsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TherapeuticInsights item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Session - " + item.getGeneratedAt().format(FORMATTER));
                }
            }
        });

        insightsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayInsight(newVal);
            }
        });

        noSelectionLabel.setVisible(true);
        noSelectionLabel.setManaged(true);
        if (insightDetail != null) {
            insightDetail.setVisible(false);
            insightDetail.setManaged(false);
        }

        logger.info("InsightsDashboardController initialized");
    }

    public void setService(MentalHealthChatService service) {
        this.chatService = service;
        loadInsights();
    }

    private void loadInsights() {
        if (chatService == null) {
            return;
        }
        List<TherapeuticInsights> insights = chatService.getUnviewedInsights();
        insightsList.getItems().setAll(insights);
        logger.info("Loaded {} insights", insights.size());
    }

    private void displayInsight(TherapeuticInsights insight) {
        noSelectionLabel.setVisible(false);
        noSelectionLabel.setManaged(false);

        if (insightDetail != null) {
            insightDetail.setVisible(true);
            insightDetail.setManaged(true);
        }

        if (summaryLabel != null) {
            summaryLabel.setText(insight.getSummary());
        }
        if (emotionalToneLabel != null) {
            emotionalToneLabel.setText(insight.getEmotionalTone());
        }
        if (themesLabel != null && insight.getKeyThemes() != null) {
            themesLabel.setText(String.join(", ", insight.getKeyThemes()));
        }
        if (recommendationsLabel != null && insight.getRecommendations() != null) {
            recommendationsLabel.setText(String.join("\n• ", insight.getRecommendations()));
        }
        if (dateLabel != null) {
            dateLabel.setText(insight.getGeneratedAt().format(FORMATTER));
        }

        if (insightWebView != null) {
            String html = buildInsightHtml(insight);
            insightWebView.getEngine().loadContent(html);
        }

        if (chatService != null && insight.getId() != null) {
            chatService.markInsightAsViewed(insight.getId());
        }

        logger.info("Displaying insight id={}", insight.getId());
    }

    private String buildInsightHtml(TherapeuticInsights insight) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; padding: 10px;'>");
        sb.append("<h2>Session Insights</h2>");
        sb.append("<p><strong>Summary:</strong> ").append(escapeHtml(insight.getSummary())).append("</p>");
        sb.append("<p><strong>Emotional Tone:</strong> ").append(escapeHtml(insight.getEmotionalTone())).append("</p>");

        if (insight.getKeyThemes() != null && !insight.getKeyThemes().isEmpty()) {
            sb.append("<p><strong>Key Themes:</strong></p><ul>");
            for (String theme : insight.getKeyThemes()) {
                sb.append("<li>").append(escapeHtml(theme)).append("</li>");
            }
            sb.append("</ul>");
        }

        if (insight.getRecommendations() != null && !insight.getRecommendations().isEmpty()) {
            sb.append("<p><strong>Recommendations:</strong></p><ul>");
            for (String rec : insight.getRecommendations()) {
                sb.append("<li>").append(escapeHtml(rec)).append("</li>");
            }
            sb.append("</ul>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
