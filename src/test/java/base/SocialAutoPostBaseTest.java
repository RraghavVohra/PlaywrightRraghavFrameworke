package base;

import com.microsoft.playwright.*;
import utils.ConfigReader;

import java.util.Arrays;

public class SocialAutoPostBaseTest {

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    @org.testng.annotations.BeforeSuite
    public void globalSetup() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(Arrays.asList("--start-maximized", "--window-position=0,0"))
        );
    }

    @org.testng.annotations.BeforeMethod
    public void setUp() {
        context = browser.newContext();
        page = context.newPage();
        page.setDefaultTimeout(30000);

        // Navigate to preprod login page
        page.navigate(ConfigReader.get("preprod.base.url"));

        // Login
        page.locator("#username").fill(ConfigReader.get("preprod.username"));
        page.locator("#password").fill(ConfigReader.get("preprod.password"));
        page.locator("(//button[@type='submit'])[1]").evaluate("el => el.click()");
        page.waitForURL("**/AssetLibrary**");
        System.out.println("User Logged in Successfully.");
    }

    @org.testng.annotations.AfterMethod
    public void tearDown() {
        context.close();
    }

    @org.testng.annotations.AfterSuite
    public void closeAll() {
        browser.close();
        playwright.close();
    }
}
