package sf.project.models;

import java.time.LocalDateTime;

public class Message {

    public enum Role {
        USER, ASSISTANT, SYSTEM
    }

    private Long id;
    private Long conversationId;
    private Role role;
    private String content;
    private LocalDateTime timestamp;

    public Message() {
        this.timestamp = LocalDateTime.now();
    }

    public Message(Role role, String content) {
        this();
        this.role = role;
        this.content = content;
    }

    public Message(Long conversationId, Role role, String content) {
        this();
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
