package tests;

import java.util.Arrays;
import java.util.List;

import org.testng.Assert;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pageObjects.DocumentLibraryPage;
import utils.ConfigReader;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.options.LoadState;
import java.util.regex.Pattern;

// 'Page' import removed — this class no longer declares its own Page field.
// The 'page' instance is inherited from BaseTest (protected Page page),
// so importing it here was just leftover from when the shadowing bug existed.
import base.BaseTest;


public class DocumentLibraryTest extends BaseTest {

    // ================================================================
    // CONFIG — loaded from config.properties via ConfigReader
    // ================================================================
    private static final String DOC_LIBRARY_URL  = ConfigReader.get("doc.library.url");
    private static final String DOC_UPLOAD_URL   = ConfigReader.get("doc.upload.url");
    private static final String DOCUMENT_NAME    = ConfigReader.get("doc.document.name");
    private static final String DESCRIPTION_TEXT = ConfigReader.get("doc.description.text");
    private static final String HASHTAG_TEXT     = ConfigReader.get("doc.hashtag.text");
    // REMOVED: SEARCH_VALUE and SEARCH_EXPECTED — TC_DL_37 now dynamically reads the first
    // document name from the listing instead of relying on hardcoded config values.
    // This makes the test work on any environment without needing to know which documents exist.

    // On preprod, after a successful upload the app redirects to either:
    //   document-library.php  (dev behaviour)
    //   sp-document-list.php  (preprod behaviour)
    // This pattern accepts both so the assertion doesn't fail just because of an env difference.
    private static final Pattern POST_UPLOAD_URL_PATTERN =
        Pattern.compile(".*(document-library|sp-document-list)\\.php.*");


    // ================================================================
    // PLAYWRIGHT BROWSER SETUP
    //
    // KEY DIFFERENCE FROM SELENIUM:
    // Selenium:   WebDriver driver = new ChromeDriver();
    // Playwright: Playwright → Browser → BrowserContext → Page
    //
    // BrowserContext = isolated session (like incognito).
    // Each test gets its own fresh context — no shared cookies/state.
    // ================================================================

    // NOTE: 'page' is NOT declared here on purpose.
    // BaseTest already declares 'protected Page page' and assigns it in @BeforeMethod setUp().
    // If we declared 'private Page page' here, it would shadow the parent's field and
    // remain null forever — causing a NullPointerException on every single test.
    // So we just use the inherited 'page' from BaseTest directly.
    private DocumentLibraryPage docLibPage;


    @BeforeMethod
    public void initPage() {
        docLibPage = new DocumentLibraryPage(page);
        docLibPage.clickOnCommunicationTab();
        docLibPage.clickOnDocumentLibrary();
    }


    // ================================================================
    // TC_DL_01 — Navigate to Document Library screen
    // ================================================================
    @Test(priority = 1)
    public void test_TC_DL_01_takenToDocumentLibraryScreen() {
        System.out.println("=== TEST CASE TC_DL_01 EXECUTING ===");

        // Selenium: Assert.assertEquals(driver.getCurrentUrl(), expectedURL)
        // Playwright built-in assertion (auto-waits for URL to match):
        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod)
        // because preprod redirects to a different URL after upload than dev does.
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN);

        System.out.println("Test Case TC_DL_01 Passed!");
    }


    // ================================================================
    // TC_DL_03 — Actions menu shows correct options
    // ================================================================
    @Test(priority = 2)
    public void test_TC_DL_03_actionsMenuButton() {
        System.out.println("=== TEST CASE TC_DL_03 EXECUTING ===");

        docLibPage.clickOnActionsButton();

        List<String> expectedOptions = Arrays.asList("Upload", "Access", "Update Hashtag(s)", "Delete");
        List<String> actualOptions   = docLibPage.getDocumentLibraryOptions();

        System.out.println("Actual Options: " + actualOptions);
        Assert.assertEquals(actualOptions, expectedOptions);

        System.out.println("test_TC_DL_03 Passed!");
    }


    // ================================================================
    // TC_DL_04 — Clicking Upload navigates to upload screen
    // ================================================================
    @Test(priority = 3)
    public void test_TC_DL_04_uploadDocumentScreen() {
        System.out.println("=== TEST CASE TC_DL_04 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        System.out.println("test_TC_DL_04 Passed!");
    }


    // ================================================================
    // NOTE ON waitForTimeout REMOVAL (applies to all upload tests below)
    //
    // The original code had three recurring page.waitForTimeout() calls per test:
    //
    //   1. page.waitForTimeout(3000) after attachThumbnail()
    //      → REMOVED. resizeCroppingArea() already calls croppingHandle().waitFor(VISIBLE)
    //        internally before touching the mouse. The sleep was double-waiting.
    //
    //   2. page.waitForTimeout(3000) after enterValueInDescriptionField()
    //      → REMOVED. Filling a text field is synchronous — there is no async activity
    //        to wait for after it returns.
    //
    //   3. page.waitForTimeout(3000) after clickOnUploadButton()
    //      → REMOVED. The assertThat(page).hasURL(...) that follows it auto-waits up to
    //        the configured default timeout (60 s) for the URL to match. An explicit sleep
    //        before an auto-waiting assertion adds time but no safety.
    //
    // General rule: page.waitForTimeout() is a last resort. Always prefer element-based
    // waits (waitFor, assertThat) or load-state waits (waitForLoadState) instead.
    // ================================================================


    // ================================================================
    // TC_DL_17 — Upload with all mandatory fields (PDF)
    // ================================================================
    @Test(priority = 4)
    public void test_TC_DL_17_allMandatoryFields() {
        System.out.println("=== TEST CASE TC_DL_17 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        // No AutoIt! Playwright sets the file directly on the input element
        docLibPage.uploadDocumentUsingPDF();

        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);

        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_PNG);
        // No sleep needed here — resizeCroppingArea() waits for the crop handle to be
        // visible before interacting with the mouse (see page object for details).
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        docLibPage.scrollToTop();

        // assertThat auto-waits for the URL — no sleep needed before this.
        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod)
        // because preprod redirects to a different URL after upload than dev does.
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN);

        System.out.println("test_TC_DL_17 Passed!");
    }


    // ================================================================
    // TC_DL_18 — Missing Document Name shows validation message
    // ================================================================
    @Test(priority = 5)
    public void test_TC_DL_18_allMandatoryFieldsExceptDocumentName() {
        System.out.println("=== TEST CASE TC_DL_18 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingPDF();
        // Document Name intentionally skipped
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_PNG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        docLibPage.scrollToTop();

        String validationMsg = docLibPage.getValidationMessageForDocumentNameField();
        Assert.assertEquals(validationMsg, "Please fill out this field.");
        System.out.println("Document Name validation message verified.");

        System.out.println("test_TC_DL_18 Passed!");
    }


    // ================================================================
    // TC_DL_22 — Upload document in PNG format
    // ================================================================
    @Test(priority = 6)
    public void test_TC_DL_22_UploadingDocumentInPngFormat() {
        System.out.println("=== TEST CASE TC_DL_22 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingPNG();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_PNG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();

        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod)
        // because preprod redirects to a different URL after upload than dev does.
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN);

        System.out.println("test_TC_DL_22 Passed!");
    }


    // ================================================================
    // TC_DL_22_1 — Upload document in JPG format
    // ================================================================
    @Test(priority = 7)
    public void test_TC_DL_22_1_UploadingDocumentInJpgFormat() {
        System.out.println("=== TEST CASE TC_DL_22_1 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingJPG();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_JPG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();

        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod)
        // because preprod redirects to a different URL after upload than dev does.
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN);

        System.out.println("test_TC_DL_22_1 Passed!");
    }


    // ================================================================
    // TC_DL_22_2 — Upload document in CSV format
    // ================================================================
    @Test(priority = 8)
    public void test_TC_DL_22_2_UploadingDocumentInCsvFormat() {
        System.out.println("=== TEST CASE TC_DL_22_2 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingCSV();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_PNG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();

        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod)
        // because preprod redirects to a different URL after upload than dev does.
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN);

        System.out.println("test_TC_DL_22_2 Passed!");
    }


    // ================================================================
    // TC_DL_22_3 — Upload document in XLSX format
    // ================================================================
    @Test(priority = 9)
    public void test_TC_DL_22_3_UploadingDocumentInXlsxFormat() {
        System.out.println("=== TEST CASE TC_DL_22_3 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingXLSX();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_PNG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();

        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod)
        // because preprod redirects to a different URL after upload than dev does.
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN);

        System.out.println("test_TC_DL_22_3 Passed!");
    }


    // ================================================================
    // TC_DL_22_4 — Upload document in MP4 format
    // ================================================================
    @Test(priority = 10)
    public void test_TC_DL_22_4_UploadingDocumentInMp4Format() {
        System.out.println("=== TEST CASE TC_DL_22_4 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingMP4();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_PNG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();

        // MP4 uploads are large — preprod is slow so we give it 60s instead of default 5s.
        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod).
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN,
            new com.microsoft.playwright.assertions.PageAssertions.HasURLOptions().setTimeout(60000));

        System.out.println("test_TC_DL_22_4 Passed!");
    }


    // ================================================================
    // TC_DL_22_5 — Upload with GIF thumbnail
    // ================================================================
    @Test(priority = 11)
    public void test_TC_DL_22_5_UploadingGifImageForThumbnail() {
        System.out.println("=== TEST CASE TC_DL_22_5 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingMP4();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_GIF);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();

        // MP4 uploads are large — preprod is slow so we give it 60s instead of default 5s.
        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod).
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN,
            new com.microsoft.playwright.assertions.PageAssertions.HasURLOptions().setTimeout(60000));

        System.out.println("test_TC_DL_22_5 Passed!");
    }


    // ================================================================
    // TC_DL_22_6 — Upload with JPG thumbnail
    // ================================================================
    @Test(priority = 12)
    public void test_TC_DL_22_6_UploadingJpgImageForThumbnail() {
        System.out.println("=== TEST CASE TC_DL_22_6 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingMP4();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_JPG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();

        // MP4 uploads are large — preprod is slow so we give it 60s instead of default 5s.
        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod).
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN,
            new com.microsoft.playwright.assertions.PageAssertions.HasURLOptions().setTimeout(60000));

        System.out.println("test_TC_DL_22_6 Passed!");
    }


    // ================================================================
    // TC_DL_25 — Missing Description shows validation message
    // ================================================================
    @Test(priority = 13)
    public void test_TC_DL_25_fillsAllMandatoryFieldsExceptDescription() {
        System.out.println("=== TEST CASE TC_DL_25 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingMP4();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_JPG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        // Description intentionally skipped
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        docLibPage.scrollToTop();

        String validationMsg = docLibPage.getValidationMessageForDescriptionField();
        Assert.assertEquals(validationMsg, "Please fill out this field.");
        System.out.println("Description validation message verified.");

        System.out.println("test_TC_DL_25 Passed!");
    }


    // ================================================================
    // TC_DL_28 — Missing Document Attachment shows validation message
    // ================================================================
    @Test(priority = 14)
    public void test_TC_DL_28_fillsAllTheFieldsExceptDocumentOption() {
        System.out.println("=== TEST CASE TC_DL_28 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        // File upload intentionally skipped
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_JPG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        docLibPage.scrollToTop();

        String validationMsg = docLibPage.getValidationMessageForDocumentAttachmentField();
        Assert.assertEquals(validationMsg, "Please select a file.");
        System.out.println("Document Attachment validation message verified.");

        System.out.println("test_TC_DL_28 Passed!");
    }


    // ================================================================
    // TC_DL_30 — Upload with mandatory fields + document options
    // ================================================================
    @Test(priority = 15)
    public void test_TC_DL_30_fillsAllTheMandatoryAndDocumentOptions() {
        System.out.println("=== TEST CASE TC_DL_30 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingJPG();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_PNG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        // Radio button clicks are synchronous — Playwright auto-waits for the element
        // to be actionable, so no sleep is needed between these interactions.
        docLibPage.clickOnDocumentOptionTwo();
        docLibPage.clickOnDocumentOptionThree();
        docLibPage.clickOnUploadButton();

        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod)
        // because preprod redirects to a different URL after upload than dev does.
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN);

        System.out.println("test_TC_DL_30 Passed!");
    }


    // ================================================================
    // TC_DL_32 — Upload with all fields + downloadable enabled
    // ================================================================
    @Test(priority = 16)
    public void test_TC_DL_32_fillsAllTheFieldsAndMakesDownloadable() {
        System.out.println("=== TEST CASE TC_DL_32 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingJPG();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_PNG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnDocumentOptionTwo();
        docLibPage.clickOnDocumentOptionThree();
        docLibPage.clickOnDownloadableOption();
        docLibPage.clickOnUploadButton();

        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod)
        // because preprod redirects to a different URL after upload than dev does.
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN);

        System.out.println("test_TC_DL_32 Passed!");
    }


    // ================================================================
    // TC_DL_34 — Upload with all fields + internal hashtag
    // ================================================================
    @Test(priority = 17)
    public void test_TC_DL_34_fillsAllTheFieldsWithHashtag() {
        System.out.println("=== TEST CASE TC_DL_34 EXECUTING ===");

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnUploadOption();

        assertThat(page).hasURL(DOC_UPLOAD_URL);

        docLibPage.uploadDocumentUsingJPG();
        String uniqueName = DOCUMENT_NAME + "_" + System.currentTimeMillis();
        docLibPage.enterValueInDocumentNameField(uniqueName);
        docLibPage.attachThumbnail(DocumentLibraryPage.THUMBNAIL_PNG);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        docLibPage.scrollToBottom();
        docLibPage.clickOnDocumentOptionTwo();
        docLibPage.clickOnDocumentOptionThree();
        docLibPage.clickOnDownloadableOption();
        docLibPage.enterInternalHashtag(HASHTAG_TEXT);
        // selectInternalHashtag() clicks the suggestion dropdown item. Playwright's
        // click() auto-waits for it to be visible, so no sleep is needed after typing.
        docLibPage.selectInternalHashtag();
        docLibPage.clickOnUploadButton();

        // Pattern accepts both document-library.php (dev) and sp-document-list.php (preprod)
        // because preprod redirects to a different URL after upload than dev does.
        assertThat(page).hasURL(POST_UPLOAD_URL_PATTERN);

        System.out.println("test_TC_DL_34 Passed!");
    }


    // ================================================================
    // TC_DL_37 — Search functionality
    // ================================================================
    @Test(priority = 18)
    public void test_TC_DL_37_searchFunctionality() {
        System.out.println("=== TEST CASE TC_DL_37 EXECUTING ===");

        // FIX: Switched from NETWORKIDLE to DOMCONTENTLOADED.
        // NETWORKIDLE waits for ALL network activity to stop — on preprod this never
        // happens within the 60s timeout due to background polling requests.
        // DOMCONTENTLOADED is enough here because the search box is part of the initial HTML,
        // not dynamically injected after an AJAX call.
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // IMPROVED: Instead of relying on a hardcoded search value from config (which breaks
        // when that document doesn't exist on the current environment), we grab the first
        // document name already visible in the listing and search for that.
        // This makes the test self-contained and works on any environment automatically.
        // getFirstDocumentName() uses a class-only locator — no checkbox needed beforehand.
        // getDynamicText() has @style='cursor: no-drop;' which only appears after a checkbox click.
        String firstDocName = docLibPage.getFirstDocumentName();
        System.out.println("First document in listing: " + firstDocName);

        docLibPage.enterIntoSearchBox(firstDocName);

        // Verify the same document appears in the filtered results.
        String searchResultText = docLibPage.getSearchResultText(firstDocName);
        System.out.println("Search Result: " + searchResultText);
        Assert.assertEquals(searchResultText, firstDocName);

        System.out.println("test_TC_DL_37 Passed!");
    }


    // ================================================================
    // TC_DL_38 — Delete without selecting content shows error dialog
    // ================================================================
    @Test(priority = 19)
    public void test_TC_DL_38_clickDeleteWithoutSelectingAnyContent() {
        System.out.println("=== TEST CASE TC_DL_38 EXECUTING ===");

        // Wait for the table to finish loading before interacting with the Actions button.
        page.waitForLoadState(LoadState.NETWORKIDLE);

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnDeleteOption();

        // No sleep needed here — getDialogBoxText() now calls waitFor(VISIBLE) internally
        // before reading the text, so it will wait for the dialog to appear on its own.
        String dialogText = docLibPage.getDialogBoxText();
        Assert.assertEquals(dialogText, "Please select at least one document creative!");

        docLibPage.clickOnOkButton();

        System.out.println("test_TC_DL_38 Passed!");
    }


    // ================================================================
    // TC_DL_39 — Delete content and verify it is gone from search
    // ================================================================
    @Test(priority = 20)
    public void test_TC_DL_39_deleteTheContentAndSearchIt() {
        System.out.println("=== TEST CASE TC_DL_39 EXECUTING ===");

        // Wait for the data table to fully load before reading or clicking anything.
        page.waitForLoadState(LoadState.NETWORKIDLE);

        docLibPage.clickOnCheckBoxOption();
        String dynamicText = docLibPage.getDynamicText();
        System.out.println("Fetched Dynamic Text: " + dynamicText);

        // No sleep after getDynamicText() — reading inner text is synchronous.
        // Playwright's click() on the actions button will auto-wait for it to be clickable.
        docLibPage.clickOnActionsButton();
        docLibPage.clickOnDeleteOption();
        // getDialogBoxText() has an internal waitFor(VISIBLE), so no sleep needed here either.
        docLibPage.clickOnOkButton();

        // After delete, wait for the page to settle before searching.
        // Using DOMCONTENTLOADED — NETWORKIDLE times out on preprod due to background requests.
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        docLibPage.enterIntoSearchBox(dynamicText);
        // noRecordsElementMethod() already calls waitFor(VISIBLE) internally before
        // returning the text, so it handles its own wait.
        docLibPage.noRecordsElementMethod();

        System.out.println("test_TC_DL_39 Passed!");
    }


    // ================================================================
    // TC_DL_40 — Update access control for a document
    // ================================================================
    @Test(priority = 21)
    public void test_TC_DL_40_updatingTheAccessOfTheContent() {
        System.out.println("=== TEST CASE TC_DL_40 EXECUTING ===");

        // FIX: Switched from NETWORKIDLE to DOMCONTENTLOADED.
        // Preprod has background polling requests that never stop, so NETWORKIDLE times out
        // at 60s every time. DOMCONTENTLOADED is enough — the data table is part of the
        // initial HTML, not injected by a late AJAX call.
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        docLibPage.clickOnCheckBoxOption();
        String dynamicText = docLibPage.getDynamicText();
        System.out.println("Fetched Dynamic Text: " + dynamicText);

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnAccessOption();

        // All the waitForTimeout(2000) calls between each interaction below were removed.
        // Playwright's click() automatically waits for each element to be visible,
        // enabled, and stable before acting — explicit sleeps between sync UI interactions
        // add wall-clock time without making the test more reliable.
        docLibPage.clickOnTeamRadioButton();
        docLibPage.clickOnPartnerCategoryButton();
        docLibPage.clickOnCategory();
        docLibPage.clickOnPartnerCategoryButton(); // close dropdown

        docLibPage.clickOnScheduleCheckbox();
        docLibPage.clickOnScheduleCheckbox();

        // selectCurrentActiveTimeThree() has its own internal waitFor(VISIBLE) on the
        // datetime picker, so no sleep is needed after opening the schedule textbox.
        docLibPage.clickOnScheduleTextbox();
        docLibPage.selectCurrentActiveTimeThree();

        // Uncomment to select a specific date:
        // docLibPage.selectTodayInCalendar();
        // docLibPage.selectDateOfYourChoice(10, 8, 2025);

        docLibPage.clickOnUpdateAccessButton();

        System.out.println("test_TC_DL_40 Passed!");
    }

}
