package base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import io.qameta.allure.Attachment;
import org.testng.ITestResult;

import utils.AuthManager;
import utils.ConfigReader;

import java.nio.file.Paths;
import java.util.Arrays;

public class BaseTest {

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    // Runs once before suite
    @org.testng.annotations.BeforeSuite
    public void globalSetup() {

        playwright = Playwright.create();
        
        boolean headless = Boolean.parseBoolean(ConfigReader.get("browser.headless"));
        int slowMo = Integer.parseInt(ConfigReader.get("browser.slowmo"));

        //browser = playwright.chromium().launch(
             //   new BrowserType.LaunchOptions().
              //  setHeadless(headless)
              //  .setSlowMo(slowMo)); // helps debugging
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(slowMo)
                .setArgs(Arrays.asList(
                    "--start-maximized",          // maximizes the browser window on launch
                    "--window-position=0,0",      // anchors it to top-left so it doesn't open off-screen
                    "--window-size=1366,768"      // explicitly sets window size to match screen resolution (1366x768)
                                                  // --start-maximized alone is unreliable in Playwright — this ensures the correct size is applied
                )));

        // Ensure login exists
        AuthManager.ensureLogin(browser);
    }

    // Runs before each test
    @org.testng.annotations.BeforeMethod
    public void setUp() {

        // context = browser.newContext(
        // new Browser.NewContextOptions()
        // .setStorageStatePath(Paths.get("auth.json"))
        // .setViewportSize(1920,1080) // This line was added afterwards
        // );
        
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setStorageStatePath(Paths.get("auth.json"))
                        // setViewportSize(null) disables Playwright's default viewport (1280x720).
                        // Without this, Playwright overrides the window size even when --start-maximized is set.
                        // null tells Playwright to use the actual maximized window size instead.
                        .setViewportSize(null)
        );

        page = context.newPage();
        
        // GLOBAL TIMEOUTS - THIS WAS ALSO ADDED AFTERWARDS
        page.setDefaultTimeout(60000);
        page.setDefaultNavigationTimeout(60000);

        // IMPORTANT
        // page.navigate("https://app.spdevmfp.com/home/AssetLibrary");
        // page.navigate("https://app.spdevmfp.com/home");
      
        
        page.navigate(ConfigReader.get("base.url") + "/home");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        // page.waitForLoadState(LoadState.NETWORKIDLE);
        // Changed `NETWORKIDLE` to `DOMCONTENTLOADED` — significantly faster. 
        // If a specific test becomes flaky, add `NETWORKIDLE` only inside that test.
    }

    // Runs after each test — alwaysRun=true ensures this fires even when the test fails.
    // If the test failed, we grab a screenshot and attach it to the Allure report
    // so you can see exactly what was on screen at the point of failure.
    @org.testng.annotations.AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE && page != null) {
            attachScreenshot("Screenshot on failure - " + result.getName());
        }
        context.close();
    }

    // Allure attaches the byte[] as a PNG image embedded directly in the report
    @Attachment(value = "{screenshotName}", type = "image/png")
    private byte[] attachScreenshot(String screenshotName) {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(false));
    }

    // Runs once after suite
    @org.testng.annotations.AfterSuite
    public void closeAll() {
        browser.close();
        playwright.close();
    }
}