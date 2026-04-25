package tests;

import base.BaseTest;
import dataproviders.DataProviders;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pageObjects.PushNotificationPageImproved;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * PushNotificationDataDrivenTest — data-driven test cases for the Push Notification feature.
 *
 * These are NEW test cases that sit alongside PushNotificationTestImproved without touching it.
 * All test data (name, message, link, time, expected result) is read from:
 *   src/test/resources/testdata/PushNotificationData.xlsx → sheet "NotificationData"
 *
 * Before running: generate the Excel file by running TestDataGenerator.main() once.
 */
@Epic("Agency Communication")
@Feature("Push Notification")
public class PushNotificationDataDrivenTest extends BaseTest {

    PushNotificationPageImproved pushNotificationPage;

    // Schedule date is always +30 days from today — same pattern as PushNotificationTestImproved.
    // Dynamic so the date never becomes a past date and the date picker never rejects it.
    private final String futureDate = LocalDate.now()
            .plusDays(30)
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    @BeforeMethod
    public void initPage() {
        pushNotificationPage = new PushNotificationPageImproved(page);
    }

    private java.nio.file.Path getResource(String relativePath) {
        try {
            return Paths.get(getClass().getClassLoader().getResource(relativePath).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Resource not found: " + relativePath, e);
        }
    }

    /**
     * TC_DD_PN — Create a push notification using data read from Excel.
     *
     * This single test method runs once per row in the Excel sheet.
     * With 3 rows in the sheet, TestNG runs it 3 times — each time with a different
     * name, message, link, and time. The testCaseId parameter appears in Allure and
     * Extent reports so you can tell which row failed at a glance.
     *
     * Why one method for multiple rows instead of separate methods:
     * The flow is identical for all rows — only the data changes. Duplicating the method
     * body per row is noise. DataProvider separates the "what to test" from the "how to test".
     *
     * @param testCaseId    identifier shown in reports (e.g. TC_DD_PN_01)
     * @param name          text to enter in the Notification Name field
     * @param message       text to enter in the Notification Message field
     * @param customLink    URL to enter in the Custom Link field
     * @param scheduleTime  time in HH:mm format (e.g. "10:00")
     * @param expectedToast the success toast message expected after submission
     */
    @Story("Create Notification")
    @Description("Create a push notification with data from Excel — verify success toast for each row")
    @Test(dataProvider = "pushNotificationData",
          dataProviderClass = DataProviders.class,
          priority = 1)
    public void test_TC_DD_PN_createNotificationFromExcel(
            String testCaseId,
            String name,
            String message,
            String customLink,
            String scheduleTime,
            String expectedToast) {

        // Log the test case ID to console — useful when watching a parallel run
        System.out.println("[DataDriven] Running: " + testCaseId
                + " | Name: " + name + " | Time: " + scheduleTime);

        pushNotificationPage.openCreateNotification();

        // Fill notification name and message — both driven from Excel
        pushNotificationPage.enterNotificationName(name);
        pushNotificationPage.enterNotificationMessage(message);

        // Select partner category — using "Raj2024" which exists on preprod
        // This is not data-driven because category selection is environment-specific,
        // not part of the variation being tested here
        pushNotificationPage.openCategoryDropdown();
        pushNotificationPage.searchCategory("Raj2024");
        pushNotificationPage.clickOnTargetCategory();
        pushNotificationPage.clickOnBlankSpace();

        // Upload the standard test image — not part of the data variation
        pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));

        // Custom link — driven from Excel
        pushNotificationPage.clickCustomLink();
        pushNotificationPage.enterCustomLink(customLink);

        // Schedule date is always +30 days (dynamic), time comes from Excel
        pushNotificationPage.enterSchedulingDateTime(futureDate, scheduleTime);

        pushNotificationPage.clickSubmit();

        // Assert the toast message matches the expected value from Excel
        Assert.assertEquals(
                pushNotificationPage.getToastMessageText(),
                expectedToast,
                "Toast message mismatch for test case: " + testCaseId
        );

        pushNotificationPage.closeToastMessage();
    }
}
