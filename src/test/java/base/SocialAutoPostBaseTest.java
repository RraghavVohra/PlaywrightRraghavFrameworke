package base;

import com.microsoft.playwright.*;
import utils.ConfigReader;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;

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

    /**
     * Returns a future date as String[] { day, "Month YYYY" } by adding the given
     * number of days to today's date.
     *
     * Why dynamic? Hardcoded dates (e.g. "April 2026") become past dates over time
     * and cause the date picker to disable them, failing the test silently.
     * Using an offset (e.g. +10 days) keeps tests self-maintaining forever.
     *
     * Usage: String[] date = getScheduleDate(10);
     *        socialPage.selectFutureDate(date[0], date[1]);
     */
    protected String[] getScheduleDate(int daysFromNow) {
        LocalDate future = LocalDate.now().plusDays(daysFromNow);
        String day       = String.valueOf(future.getDayOfMonth());
        String monthYear = future.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                           + " " + future.getYear();
        return new String[]{ day, monthYear };
    }
}
