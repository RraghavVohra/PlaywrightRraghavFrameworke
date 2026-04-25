package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * ExcelReader — reads a named sheet from an .xlsx file and returns all data rows as Object[][].
 *
 * Why a dedicated utility class:
 * Multiple DataProvider methods across different test classes may need Excel data.
 * Centralising the reading logic here means a single place to fix if POI's API changes,
 * and test classes never deal with workbook/sheet/cell boilerplate directly.
 *
 * Row 0 is always treated as the header row and is skipped.
 * All cell values are returned as Strings — the test method receives typed parameters
 * from the DataProvider and is responsible for any further parsing (e.g. int, boolean).
 */
public class ExcelReader {

    /**
     * @param filePath  Path to the .xlsx file (relative to project root, e.g. "src/test/resources/testdata/...")
     * @param sheetName Name of the sheet tab inside the workbook
     * @return Object[][] where each row is one test data set, each column is one parameter
     */
    public static Object[][] getTestData(String filePath, String sheetName) throws IOException {

        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet(sheetName);

        // getLastRowNum() returns the index of the last row (0-based), which equals total data rows
        // because row 0 is the header — so this is the exact data row count.
        int rowCount = sheet.getLastRowNum();
        int colCount = sheet.getRow(0).getLastCellNum();

        Object[][] data = new Object[rowCount][colCount];

        // Start at row index 1 to skip the header row
        for (int i = 1; i <= rowCount; i++) {
            Row row = sheet.getRow(i);
            for (int j = 0; j < colCount; j++) {
                data[i - 1][j] = getCellValueAsString(row.getCell(j));
            }
        }

        workbook.close();
        fis.close();

        return data;
    }

    /**
     * Reads any cell type and returns it as a plain String.
     *
     * Why handle NUMERIC specially:
     * Excel stores integers like "287" as doubles (287.0). If we call getNumericCellValue()
     * and toString() directly we get "287.0" — which would fail a String comparison.
     * We check if the value is a whole number and cast to long to get "287" instead.
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                // Dates are stored as numbers in Excel — check before treating as plain number
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double numericValue = cell.getNumericCellValue();
                // If it's a whole number (e.g. 1.0, 287.0), return without the decimal
                if (numericValue == Math.floor(numericValue)) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                // Evaluate the formula result as a string
                return cell.getCachedFormulaResultType() == CellType.NUMERIC
                        ? String.valueOf((long) cell.getNumericCellValue())
                        : cell.getStringCellValue().trim();

            default:
                return "";
        }
    }
}
