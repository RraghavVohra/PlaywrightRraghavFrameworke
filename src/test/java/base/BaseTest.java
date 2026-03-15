package base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import utils.AuthManager;

import java.nio.file.Paths;

public class BaseTest {

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    // Runs once before suite
    @org.testng.annotations.BeforeSuite
    public void globalSetup() {

        playwright = Playwright.create();

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().
                setHeadless(false)
                .setSlowMo(50)); // helps debugging

        // Ensure login exists
        AuthManager.ensureLogin(browser);
    }

    // Runs before each test
    @org.testng.annotations.BeforeMethod
    public void setUp() {

        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setStorageStatePath(Paths.get("auth.json"))
                        .setViewportSize(1920,1080) // This line was added afterwards
        );

        page = context.newPage();
        
        // GLOBAL TIMEOUTS - THIS WAS ALSO ADDED AFTERWARDS
        page.setDefaultTimeout(60000);
        page.setDefaultNavigationTimeout(60000);

        // IMPORTANT
        //page.navigate("https://app.spdevmfp.com/home/AssetLibrary");
        page.navigate("https://app.spdevmfp.com/home");
      
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    // Runs after each test
    @org.testng.annotations.AfterMethod
    public void tearDown() {
        context.close();
    }

    // Runs once after suite
    @org.testng.annotations.AfterSuite
    public void closeAll() {
        browser.close();
        playwright.close();
    }
}