package sf.project.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sf.project.config.AppConfig;
import sf.project.models.Message;
import sf.project.models.TherapeuticInsights;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for chat conversations and insights.
 * All public methods are synchronized for thread safety.
 */
public class ChatDAO {

    private static final Logger logger = LoggerFactory.getLogger(ChatDAO.class);

    private static final String CREATE_CONVERSATIONS_TABLE =
            "CREATE TABLE IF NOT EXISTS conversations (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  started_at TEXT NOT NULL," +
            "  ended_at TEXT" +
            ")";

    private static final String CREATE_MESSAGES_TABLE =
            "CREATE TABLE IF NOT EXISTS messages (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  conversation_id INTEGER NOT NULL," +
            "  role TEXT NOT NULL," +
            "  content TEXT NOT NULL," +
            "  timestamp TEXT NOT NULL," +
            "  FOREIGN KEY (conversation_id) REFERENCES conversations(id)" +
            ")";

    private static final String CREATE_INSIGHTS_TABLE =
            "CREATE TABLE IF NOT EXISTS insights (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  conversation_id INTEGER NOT NULL," +
            "  data TEXT NOT NULL," +
            "  viewed INTEGER NOT NULL DEFAULT 0," +
            "  generated_at TEXT NOT NULL," +
            "  FOREIGN KEY (conversation_id) REFERENCES conversations(id)" +
            ")";

    private final String dbUrl;
    private final ObjectMapper objectMapper;

    public ChatDAO() {
        this.dbUrl = "jdbc:sqlite:" + AppConfig.getDatabaseUrl();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_CONVERSATIONS_TABLE);
            stmt.execute(CREATE_MESSAGES_TABLE);
            stmt.execute(CREATE_INSIGHTS_TABLE);
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public synchronized long startConversation() {
        String sql = "INSERT INTO conversations (started_at) VALUES (?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, LocalDateTime.now().toString());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    logger.info("Started new conversation with id={}", id);
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.error("Error starting conversation", e);
        }
        return -1;
    }

    public synchronized void endConversation(long conversationId) {
        String sql = "UPDATE conversations SET ended_at = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LocalDateTime.now().toString());
            pstmt.setLong(2, conversationId);
            pstmt.executeUpdate();
            logger.info("Ended conversation id={}", conversationId);
        } catch (SQLException e) {
            logger.error("Error ending conversation id={}", conversationId, e);
        }
    }

    public synchronized void saveMessage(Message message) {
        String sql = "INSERT INTO messages (conversation_id, role, content, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, message.getConversationId());
            pstmt.setString(2, message.getRole().name());
            pstmt.setString(3, message.getContent());
            pstmt.setString(4, message.getTimestamp().toString());
            pstmt.executeUpdate();
            logger.info("Saved message role={} for conversationId={}", message.getRole(), message.getConversationId());
        } catch (SQLException e) {
            logger.error("Error saving message", e);
        }
    }

    public synchronized List<Message> getConversationMessages(long conversationId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, conversation_id, role, content, timestamp FROM messages WHERE conversation_id = ? ORDER BY timestamp";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, conversationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message();
                    message.setId(rs.getLong("id"));
                    message.setConversationId(rs.getLong("conversation_id"));
                    message.setRole(Message.Role.valueOf(rs.getString("role")));
                    message.setContent(rs.getString("content"));
                    message.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
                    messages.add(message);
                }
            }
            logger.info("Retrieved {} messages for conversationId={}", messages.size(), conversationId);
        } catch (SQLException e) {
            logger.error("Error retrieving messages for conversationId={}", conversationId, e);
        }
        return messages;
    }

    public synchronized void saveInsights(TherapeuticInsights insights) {
        String sql = "INSERT INTO insights (conversation_id, data, viewed, generated_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String data = objectMapper.writeValueAsString(insights);
            pstmt.setLong(1, insights.getConversationId());
            pstmt.setString(2, data);
            pstmt.setInt(3, insights.isViewed() ? 1 : 0);
            pstmt.setString(4, insights.getGeneratedAt().toString());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    insights.setId(rs.getLong(1));
                }
            }
            logger.info("Saved insights for conversationId={}", insights.getConversationId());
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Error saving insights", e);
        }
    }

    public synchronized List<TherapeuticInsights> getUnviewedInsights() {
        List<TherapeuticInsights> insightsList = new ArrayList<>();
        String sql = "SELECT id, data FROM insights WHERE viewed = 0 ORDER BY generated_at DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                TherapeuticInsights insights = objectMapper.readValue(rs.getString("data"), TherapeuticInsights.class);
                insights.setId(rs.getLong("id"));
                insightsList.add(insights);
            }
            logger.info("Retrieved {} unviewed insights", insightsList.size());
        } catch (Exception e) {
            logger.error("Error retrieving unviewed insights", e);
        }
        return insightsList;
    }

    public synchronized void markInsightsAsViewed(long insightId) {
        String sql = "UPDATE insights SET viewed = 1 WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, insightId);
            pstmt.executeUpdate();
            logger.info("Marked insight id={} as viewed", insightId);
        } catch (SQLException e) {
            logger.error("Error marking insight id={} as viewed", insightId, e);
        }
    }

    public synchronized int getConversationCount() {
        String sql = "SELECT COUNT(*) FROM conversations";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                logger.info("Total conversation count: {}", count);
                return count;
            }
        } catch (SQLException e) {
            logger.error("Error getting conversation count", e);
        }
        return 0;
    }
}
