package sf.project.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Detects crisis-related content in user messages.
 * Thread-safe singleton using double-checked locking.
 */
public class CrisisDetector {

    private static final Logger logger = LoggerFactory.getLogger(CrisisDetector.class);

    private static volatile CrisisDetector instance;

    private final List<String> crisisKeywords = List.of(
            "suicide", "suicidal", "kill myself", "end my life", "want to die",
            "self-harm", "self harm", "cutting myself", "hurt myself",
            "no reason to live", "give up on life", "hopeless", "worthless",
            "crisis", "emergency", "help me now"
    );

    private static final String CRISIS_RESOURCES =
            "If you are in immediate danger, please call emergency services (911) or a crisis hotline:\n" +
            "- National Suicide Prevention Lifeline: 988 (call or text)\n" +
            "- Crisis Text Line: Text HOME to 741741\n" +
            "- International Association for Suicide Prevention: https://www.iasp.info/resources/Crisis_Centres/";

    private CrisisDetector() {
    }

    public static CrisisDetector getInstance() {
        if (instance == null) {
            synchronized (CrisisDetector.class) {
                if (instance == null) {
                    instance = new CrisisDetector();
                }
            }
        }
        return instance;
    }

    /**
     * Checks if the given message contains crisis-related content.
     *
     * @param message the user message to analyze
     * @return true if crisis content is detected
     */
    public boolean detectCrisis(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        for (String keyword : crisisKeywords) {
            if (lowerMessage.contains(keyword)) {
                logger.warn("Crisis keyword detected in message: '{}'", keyword);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns crisis resources message.
     *
     * @return crisis resource information
     */
    public String getCrisisResponse() {
        logger.info("Providing crisis resources to user");
        return CRISIS_RESOURCES;
    }
}
