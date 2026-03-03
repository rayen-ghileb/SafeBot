package sf.project;

import org.junit.jupiter.api.Test;
import sf.project.utils.CrisisDetector;

import static org.junit.jupiter.api.Assertions.*;

class CrisisDetectorTest {

    @Test
    void testSingletonThreadSafety() throws InterruptedException {
        CrisisDetector[] instances = new CrisisDetector[10];
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> instances[index] = CrisisDetector.getInstance());
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        for (CrisisDetector inst : instances) {
            assertSame(instances[0], inst, "All threads should get the same singleton instance");
        }
    }

    @Test
    void testDetectCrisisKeyword() {
        CrisisDetector detector = CrisisDetector.getInstance();
        assertTrue(detector.detectCrisis("I want to kill myself"));
        assertTrue(detector.detectCrisis("I have suicidal thoughts"));
        assertFalse(detector.detectCrisis("I am feeling a bit sad today"));
        assertFalse(detector.detectCrisis(""));
        assertFalse(detector.detectCrisis(null));
    }

    @Test
    void testCrisisResponse() {
        CrisisDetector detector = CrisisDetector.getInstance();
        String response = detector.getCrisisResponse();
        assertNotNull(response);
        assertFalse(response.isBlank());
    }
}
