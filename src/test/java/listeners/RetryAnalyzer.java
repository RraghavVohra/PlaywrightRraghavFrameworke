package listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import utils.ConfigReader;

/**
 * RetryAnalyzer — re-runs a failed test up to MAX_RETRY times before marking it as truly failed.
 *
 * Why: Flaky tests (network timeouts, animation delays, preprod instability) can cause false failures.
 * Retrying gives the test a fair chance before reporting it as a real defect.
 *
 * How it works:
 * - TestNG calls retry(result) after each failure.
 * - Returning true = "try again", returning false = "mark as failed".
 * - retryCount is per-instance, so each test method gets its own fresh counter.
 *
 * The max retry count is read from config.properties (key: retry.count).
 * Example: retry.count=2 means each test can run up to 3 times total (1 original + 2 retries).
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    // Tracks how many times this specific test has been retried so far
    private int retryCount = 0;

    // Read max retries from config — keeps it configurable without code changes
    private static final int MAX_RETRY = Integer.parseInt(ConfigReader.get("retry.count"));

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            System.out.println("[RetryAnalyzer] Retrying test: " + result.getName()
                    + " | Attempt " + retryCount + " of " + MAX_RETRY);
            return true;  // tell TestNG to re-run this test
        }
        return false;  // max retries exhausted — mark as FAILED
    }
}
