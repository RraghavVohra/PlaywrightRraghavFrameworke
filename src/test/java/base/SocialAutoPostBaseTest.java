package base;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * SocialAutoPost feature base class.
 *
 * Extends BaseTest so the full browser lifecycle (@BeforeSuite, @BeforeMethod,
 * @AfterMethod, @AfterSuite) is inherited from one common place — no duplication.
 *
 * The only thing this class adds is the getScheduleDate() helper, which is specific
 * to Social Auto Post tests that use a date picker with future dates.
 */
public class SocialAutoPostBaseTest extends BaseTest {

    /**
     * Returns a future date as String[] { day, "Month YYYY" } by adding the given
     * number of days to today's date.
     *
     * Why dynamic? Hardcoded dates (e.g. "April 2026") become past dates over time
     * and cause the date picker to disable them, silently failing the test.
     * An offset (e.g. +10 days) keeps tests self-maintaining indefinitely.
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
