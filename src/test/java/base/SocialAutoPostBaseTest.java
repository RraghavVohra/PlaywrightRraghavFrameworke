package base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import utils.AuthManager;
import utils.ConfigReader;

import java.nio.file.Paths;
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
                .setArgs(Arrays.asList(
                    "--start-maximized",       // maximizes the browser window on launch
                    "--window-position=0,0",   // anchors to top-left so it doesn't open off-screen
                    "--window-size=1366,768"   // explicitly sets resolution — --start-maximized alone
                                               // is unreliable in Playwright, this ensures correct size
                ))
        );

        // Login once and save session to auth.json.
        // AuthManager checks if auth.json already exists — if so, skips login entirely.
        // This is the same pattern BaseTest uses, so all suites share one saved session.
        // WHY: Without this, every @BeforeMethod was doing a full login, costing ~5s per test
        // and making the suite 7x slower than necessary.
        AuthManager.ensureLogin(browser);
    }

    @org.testng.annotations.BeforeMethod
    public void setUp() {
        // Restore the saved auth session — no login step needed.
        // setStorageStatePath loads the cookies and localStorage from auth.json so the
        // browser is already authenticated when the new context opens.
        context = browser.newContext(
            new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("auth.json"))
                .setViewportSize(null) // let --start-maximized control the window size
        );
        page = context.newPage();
        page.setDefaultTimeout(30000);

        // Navigate to home — session is already authenticated, no login page shown.
        page.navigate(ConfigReader.get("preprod.base.url") + "/home");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        System.out.println("Session restored. Navigated to home.");
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
