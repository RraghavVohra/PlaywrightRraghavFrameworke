package utils;

import com.microsoft.playwright.Page;

/**
 * TestContext — a thread-safe holder for the current test's Playwright Page.
 *
 * Why this exists:
 * TestNG listeners (like ExtentReportListener) need access to the Playwright Page to capture
 * screenshots on failure. But listeners don't have a reference to BaseTest. Passing the page
 * via a public static field on BaseTest would work but pollutes the base class with listener
 * concerns. Instead, BaseTest writes to this holder and the listener reads from it — clean
 * separation with no direct coupling.
 *
 * ThreadLocal ensures that in a parallel run each test thread gets its own Page instance,
 * so threads never read each other's page by accident.
 */
public class TestContext {

    private static final ThreadLocal<Page> currentPage = new ThreadLocal<>();

    // Called from BaseTest.setUp() after the page is created
    public static void setPage(Page page) {
        currentPage.set(page);
    }

    // Called from listeners (e.g. ExtentReportListener) to grab the active page
    public static Page getPage() {
        return currentPage.get();
    }

    // Called from BaseTest.tearDown() to avoid memory leaks — ThreadLocal must be cleared
    // when the thread is returned to the pool, otherwise the old Page reference lingers
    public static void clear() {
        currentPage.remove();
    }
}
