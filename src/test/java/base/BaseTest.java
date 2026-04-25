package base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import io.qameta.allure.Attachment;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import utils.AuthManager;
import utils.ConfigReader;
import utils.TestContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class BaseTest {

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    // Runs once before the entire suite
    @BeforeSuite
    public void globalSetup() throws IOException {
        playwright = Playwright.create();

        boolean headless = Boolean.parseBoolean(ConfigReader.get("browser.headless"));
        int slowMo = Integer.parseInt(ConfigReader.get("browser.slowmo"));

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(slowMo)
                        .setArgs(Arrays.asList(
                                "--start-maximized",
                                "--window-position=0,0",
                                // --start-maximized alone is unreliable in Playwright — explicit size ensures correct rendering
                                "--window-size=1366,768"
                        )));

        // Create output directories for videos and traces upfront
        // so Playwright never fails trying to write to a path that doesn't exist
        Files.createDirectories(Paths.get("test-output/videos"));
        Files.createDirectories(Paths.get("test-output/traces"));

        AuthManager.ensureLogin(browser);
    }

    // Runs before each test method
    @BeforeMethod
    public void setUp() {
        // Build context options — storage state restores the saved login session
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("auth.json"))
                // null tells Playwright to use the actual OS window size instead of its default 1280x720 viewport
                .setViewportSize(null);

        // VIDEO RECORDING
        // When enabled, Playwright records a .webm video of every test automatically.
        // The video file is created inside test-output/videos/ and finalised when context.close() is called.
        // We delete the video on pass (in tearDown) so only failure videos are kept — saves disk space.
        if (Boolean.parseBoolean(ConfigReader.get("video.enabled"))) {
            contextOptions.setRecordVideoDir(Paths.get("test-output/videos"));
        }

        context = browser.newContext(contextOptions);
        page = context.newPage();

        page.setDefaultTimeout(60000);
        page.setDefaultNavigationTimeout(60000);

        // Make the page available to listeners (e.g. ExtentReportListener) for screenshot capture.
        // Uses ThreadLocal internally so parallel tests never cross-contaminate each other.
        TestContext.setPage(page);

        // TRACING
        // Playwright traces capture screenshots at every action, full DOM snapshots, and network requests.
        // Open a saved trace with: npx playwright show-trace test-output/traces/<file>.zip
        // We start tracing here and decide in tearDown whether to save or discard it.
        if (Boolean.parseBoolean(ConfigReader.get("tracing.enabled"))) {
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)  // screenshot at every action (click, fill, navigate, etc.)
                    .setSnapshots(true)    // full DOM snapshot — lets you inspect the page state at any step
                    .setSources(true)      // embeds Java source file lines so you can see which line triggered each action
            );
        }

        page.navigate(ConfigReader.get("base.url") + "/home");
        // DOMCONTENTLOADED is significantly faster than NETWORKIDLE on a busy SPA.
        // Add NETWORKIDLE inside a specific test only if that test needs a fully settled network.
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    // Runs after each test — alwaysRun=true ensures teardown fires even when the test itself fails
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) throws IOException {
        // Clear the ThreadLocal page reference before context is closed.
        // The listener's onTestFailure already ran before this point, so it had its chance to use it.
        TestContext.clear();

        boolean failed = result.getStatus() == ITestResult.FAILURE;

        // Screenshot on failure — attached inline in the Allure report
        if (failed && page != null) {
            attachScreenshot("Screenshot on failure - " + result.getName());
        }

        // TRACE HANDLING
        // Stop tracing and save the zip file only when the test fails.
        // On pass we stop without saving — no point keeping a trace for a green test.
        // View a saved trace: npx playwright show-trace test-output/traces/<file>.zip
        if (Boolean.parseBoolean(ConfigReader.get("tracing.enabled"))) {
            if (failed) {
                // Timestamp in filename prevents overwrite if the same test is retried
                String traceFile = "test-output/traces/" + result.getName()
                        + "-" + System.currentTimeMillis() + ".zip";
                context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(traceFile)));
                System.out.println("[Trace] Saved → " + traceFile);
            } else {
                context.tracing().stop();  // discard — no path means no file is written
            }
        }

        // context.close() MUST happen before we read page.video().path()
        // because Playwright finalises and writes the video file only when the context is closed
        context.close();

        // VIDEO HANDLING
        // On pass: delete the video to save disk space — we only need failure evidence.
        // On fail: keep the video and print the path so you can watch it immediately.
        if (Boolean.parseBoolean(ConfigReader.get("video.enabled")) && page != null && page.video() != null) {
            if (failed) {
                System.out.println("[Video] Saved → " + page.video().path());
            } else {
                Files.deleteIfExists(page.video().path());
            }
        }
    }

    // Allure attaches the returned byte[] as a PNG image embedded directly in the report
    @Attachment(value = "{screenshotName}", type = "image/png")
    private byte[] attachScreenshot(String screenshotName) {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(false));
    }

    // Runs once after the entire suite
    @AfterSuite
    public void closeAll() {
        browser.close();
        playwright.close();
    }
}