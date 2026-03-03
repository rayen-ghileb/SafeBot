package sf.project.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.List;

public class TherapeuticInsights {

    private Long id;
    private Long conversationId;
    private String summary;
    private List<String> keyThemes;
    private String emotionalTone;
    private List<String> recommendations;
    private boolean viewed;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    public TherapeuticInsights() {
        this.generatedAt = LocalDateTime.now();
        this.viewed = false;
    }

    public TherapeuticInsights(Long conversationId, String summary, List<String> keyThemes,
                                String emotionalTone, List<String> recommendations) {
        this();
        this.conversationId = conversationId;
        this.summary = summary;
        this.keyThemes = keyThemes;
        this.emotionalTone = emotionalTone;
        this.recommendations = recommendations;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getKeyThemes() { return keyThemes; }
    public void setKeyThemes(List<String> keyThemes) { this.keyThemes = keyThemes; }

    public String getEmotionalTone() { return emotionalTone; }
    public void setEmotionalTone(String emotionalTone) { this.emotionalTone = emotionalTone; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public boolean isViewed() { return viewed; }
    public void setViewed(boolean viewed) { this.viewed = viewed; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
