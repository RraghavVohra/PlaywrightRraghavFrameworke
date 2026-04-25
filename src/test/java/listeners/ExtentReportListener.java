package listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.microsoft.playwright.Page;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.TestContext;

import java.util.Base64;

/**
 * ExtentReportListener — wires TestNG test events into the Extent HTML report.
 *
 * Implements ITestListener so TestNG calls the appropriate method at each test lifecycle point.
 * Registered in testng.xml as a <listener> at the suite level.
 *
 * ThreadLocal<ExtentTest>:
 * Each test method gets its own ExtentTest node in the report. In a parallel run, multiple
 * threads will be creating/updating ExtentTest objects simultaneously. ThreadLocal ensures
 * each thread only touches its own node — threads never overwrite each other's results.
 *
 * Screenshot on failure:
 * Captured via TestContext.getPage() (set by BaseTest.setUp) and embedded as a Base64 string
 * directly inside the HTML report — no separate screenshot folder needed, report stays portable.
 */
public class ExtentReportListener implements ITestListener {

    private static final ExtentReports extent = ExtentReportManager.getInstance();

    // Each thread gets its own ExtentTest — required for correct parallel test reporting
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    @Override
    public void onTestStart(ITestResult result) {
        // createTest registers a new row in the report for this method
        // getDescription() picks up the @Test(description="...") value if set, else empty
        ExtentTest test = extent.createTest(
                result.getMethod().getMethodName(),
                result.getMethod().getDescription()
        );
        extentTest.set(test);
        test.log(Status.INFO, "Test started");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        extentTest.get().pass(MarkupHelper.createLabel("PASSED", ExtentColor.GREEN));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = extentTest.get();

        // Log the failure label and the full exception stack trace
        test.fail(MarkupHelper.createLabel("FAILED", ExtentColor.RED));
        test.fail(result.getThrowable());

        // Embed a screenshot inline in the report as a Base64 image.
        // TestContext.getPage() gives us the Playwright Page for the current thread.
        // This works because BaseTest.setUp() calls TestContext.setPage(page) after creating it.
        Page page = TestContext.getPage();
        if (page != null) {
            try {
                byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));
                String base64 = Base64.getEncoder().encodeToString(screenshotBytes);
                // Embed as an HTML <img> tag — Extent renders raw HTML in log entries
                test.fail("<br><b>Screenshot at failure:</b><br>"
                        + "<img src='data:image/png;base64," + base64
                        + "' style='max-width:100%;border:1px solid #ccc;'/>");
            } catch (Exception e) {
                test.warning("Could not capture screenshot: " + e.getMessage());
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = extentTest.get();

        // A skipped result during retry means the test failed but will be retried.
        // We log it as a warning rather than a hard failure to distinguish it from a
        // dependency-skipped test. The retry that eventually passes will appear as PASS.
        if (result.getThrowable() != null) {
            test.skip(MarkupHelper.createLabel("SKIPPED / RETRYING", ExtentColor.AMBER));
            test.skip(result.getThrowable());
        } else {
            test.skip(MarkupHelper.createLabel("SKIPPED", ExtentColor.YELLOW));
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        // flush() writes all buffered test results to the HTML file.
        // Without this call the HTML file is created but stays empty.
        extent.flush();
    }
}
