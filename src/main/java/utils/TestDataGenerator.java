package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * TestDataGenerator — creates the sample Excel test data file for data-driven tests.
 *
 * Why this exists:
 * Excel (.xlsx) is a binary format — it can't be created by a text editor or committed as
 * readable source. This class generates the file programmatically using Apache POI so the
 * data structure is version-controlled as Java code. Anyone cloning the repo runs main()
 * once and the Excel file is created locally.
 *
 * How to run:
 * Right-click this file in your IDE → Run As → Java Application
 * OR from terminal: mvn exec:java -Dexec.mainClass="utils.TestDataGenerator"
 *
 * Output: src/test/resources/testdata/PushNotificationData.xlsx
 */
public class TestDataGenerator {

    public static void main(String[] args) throws IOException {

        Workbook workbook = new XSSFWorkbook();

        createPushNotificationSheet(workbook);

        String outputPath = "src/test/resources/testdata/PushNotificationData.xlsx";
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }

        workbook.close();
        System.out.println("Excel file created at: " + outputPath);
    }

    private static void createPushNotificationSheet(Workbook workbook) {

        Sheet sheet = workbook.createSheet("NotificationData");

        // ── Header row styling ──
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // ── Header row (row 0) ──
        // Column order must match the parameter order in the @DataProvider and @Test method
        String[] headers = {
            "testCaseId",       // col 0 — test identifier shown in reports
            "notificationName", // col 1 — text entered in the Notification Name field
            "notificationMessage", // col 2 — text entered in the Notification Message field
            "customLink",       // col 3 — URL entered in the Custom Link field
            "scheduleTime",     // col 4 — time in HH:mm format (date is always +30 days dynamic)
            "expectedToast"     // col 5 — expected success toast text to assert against
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // ── Data rows ──
        // Each row = one test execution. TestNG will run the @Test method once per row.
        Object[][] testData = {
            // testCaseId       | notificationName          | notificationMessage                   | customLink               | scheduleTime | expectedToast
            { "TC_DD_PN_01",   "Promo Alert",              "Summer sale starts now!",              "https://google.com",     "10:00",       "Push Notification Saved." },
            { "TC_DD_PN_02",   "Flash Weekend Offer",      "Limited time deal available for you",  "https://amazon.in",      "11:30",       "Push Notification Saved." },
            { "TC_DD_PN_03",   "New Product Launch",       "Brand new product just launched!",     "https://flipkart.com",   "09:00",       "Push Notification Saved." },
        };

        for (int i = 0; i < testData.length; i++) {
            Row row = sheet.createRow(i + 1); // +1 to start after header
            Object[] rowData = testData[i];
            for (int j = 0; j < rowData.length; j++) {
                row.createCell(j).setCellValue(rowData[j].toString());
            }
        }

        // Auto-size columns so the file is readable when opened manually
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
