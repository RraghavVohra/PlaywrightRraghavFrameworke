package dataproviders;

import org.testng.annotations.DataProvider;
import utils.ExcelReader;

import java.io.IOException;

/**
 * DataProviders — central place for all @DataProvider methods in the framework.
 *
 * Why centralised:
 * Scattering @DataProvider methods inside test classes works but clutters the test logic.
 * Keeping all providers here makes it easy to see every data source in one file, and
 * multiple test classes can share the same provider via dataProviderClass = DataProviders.class.
 *
 * How to use in a test:
 *   @Test(dataProvider = "pushNotificationData", dataProviderClass = DataProviders.class)
 *   public void myTest(String testCaseId, String name, String message, ...) { ... }
 */
public class DataProviders {

    // Path is relative to the project root — works from both Maven CLI and IDE run configurations
    private static final String EXCEL_PATH = "src/test/resources/testdata/PushNotificationData.xlsx";

    /**
     * Reads the "NotificationData" sheet and returns one row per test execution.
     * Column order: testCaseId, notificationName, notificationMessage, customLink, scheduleTime, expectedToast
     *
     * Each row becomes one call to the test method — TestNG runs the test once per row.
     * If this provider returns 3 rows, the linked @Test method runs 3 times.
     */
    @DataProvider(name = "pushNotificationData")
    public static Object[][] pushNotificationData() throws IOException {
        return ExcelReader.getTestData(EXCEL_PATH, "NotificationData");
    }
}
