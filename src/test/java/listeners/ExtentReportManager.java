package listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import utils.ConfigReader;

/**
 * ExtentReportManager — creates and configures the single ExtentReports instance for the suite.
 *
 * Why a separate manager class:
 * ExtentReports must be initialised exactly once per suite run. Putting this logic in the
 * listener itself risks double-initialisation if the listener is instantiated more than once.
 * The static getInstance() pattern (lazy singleton) guarantees one instance regardless of
 * how many times the listener is created by TestNG.
 *
 * Output: test-output/extent-report/index.html — a single self-contained HTML file.
 * No server needed; just open it in any browser. This is the key advantage over Allure,
 * which requires a CLI or server to render.
 */
public class ExtentReportManager {

    private static ExtentReports extent;

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            // SparkReporter is the modern Extent HTML reporter (replaced Classic/Bootstrap in v5)
            ExtentSparkReporter spark = new ExtentSparkReporter("test-output/extent-report/index.html");

            spark.config().setTheme(Theme.DARK);
            spark.config().setDocumentTitle("SalesPanda Automation Report");
            spark.config().setReportName("Test Execution Report");
            // Embeds screenshots as Base64 strings directly in the HTML — no separate folder needed
            spark.config().setEncoding("UTF-8");

            extent = new ExtentReports();
            extent.attachReporter(spark);

            // System info section shown at the top of the report — useful context at a glance
            extent.setSystemInfo("Application", "SalesPanda");
            extent.setSystemInfo("Environment", ConfigReader.get("env"));
            extent.setSystemInfo("Base URL", ConfigReader.get("base.url"));
            extent.setSystemInfo("Browser", "Chromium");
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        }
        return extent;
    }
}
