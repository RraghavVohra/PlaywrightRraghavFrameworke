package tests;

import java.util.Arrays;
import java.util.List;

import org.testng.Assert;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pageObjects.DocumentLibraryPage;
import utils.ConfigReader;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;


import com.microsoft.playwright.Page;
import base.BaseTest;


public class DocumentLibraryTest extends BaseTest {
	
	 // ================================================================
    // CONFIG — replace with your actual values
    // (or load from a .properties file the same way your Base class does)
    // ================================================================
	private static final String DOC_LIBRARY_URL  = ConfigReader.get("doc.library.url");
	private static final String DOC_UPLOAD_URL   = ConfigReader.get("doc.upload.url");
	private static final String DOCUMENT_NAME    = ConfigReader.get("doc.document.name");
	private static final String DESCRIPTION_TEXT = ConfigReader.get("doc.description.text");
	private static final String HASHTAG_TEXT     = ConfigReader.get("doc.hashtag.text");
	private static final String SEARCH_VALUE     = ConfigReader.get("doc.search.value");


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

    
    private Page page;
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
        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);
        docLibPage.scrollToTop();
        
        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);
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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        // Description intentionally skipped
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);
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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);
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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnDocumentOptionTwo();
        page.waitForTimeout(2000);
        docLibPage.clickOnDocumentOptionThree();
        page.waitForTimeout(2000);
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnDocumentOptionTwo();
        page.waitForTimeout(2000);
        docLibPage.clickOnDocumentOptionThree();
        page.waitForTimeout(2000);
        docLibPage.clickOnDownloadableOption();
        page.waitForTimeout(2000);
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

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
        page.waitForTimeout(3000);
        docLibPage.resizeCroppingArea();
        docLibPage.clickOnApplyButton();

        docLibPage.scrollDownByFiveHundred();
        docLibPage.enterValueInDescriptionField(DESCRIPTION_TEXT);
        page.waitForTimeout(3000);
        docLibPage.scrollToBottom();
        docLibPage.clickOnDocumentOptionTwo();
        page.waitForTimeout(2000);
        docLibPage.clickOnDocumentOptionThree();
        page.waitForTimeout(2000);
        docLibPage.clickOnDownloadableOption();
        page.waitForTimeout(2000);
        docLibPage.enterInternalHashtag(HASHTAG_TEXT);
        page.waitForTimeout(2000);
        docLibPage.selectInternalHashtag();
        page.waitForTimeout(2000);
        docLibPage.clickOnUploadButton();
        page.waitForTimeout(3000);

        assertThat(page).hasURL(DOC_LIBRARY_URL);

        System.out.println("test_TC_DL_34 Passed!");
    }


    // ================================================================
    // TC_DL_37 — Search functionality
    // ================================================================
    @Test(priority = 18)
    public void test_TC_DL_37_searchFunctionality() {
        System.out.println("=== TEST CASE TC_DL_37 EXECUTING ===");

        page.waitForTimeout(3000);

        docLibPage.enterIntoSearchBox(SEARCH_VALUE);
        String searchResultText = docLibPage.getSearchResultText();
        System.out.println("Search Result: " + searchResultText);
        Assert.assertEquals(searchResultText, "ewewew test");

        System.out.println("test_TC_DL_37 Passed!");
    }


    // ================================================================
    // TC_DL_38 — Delete without selecting content shows error dialog
    // ================================================================
    @Test(priority = 19)
    public void test_TC_DL_38_clickDeleteWithoutSelectingAnyContent() {
        System.out.println("=== TEST CASE TC_DL_38 EXECUTING ===");

        page.waitForTimeout(3000);

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnDeleteOption();
        page.waitForTimeout(2000);

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

        page.waitForTimeout(1000);

        docLibPage.clickOnCheckBoxOption();
        String dynamicText = docLibPage.getDynamicText();
        System.out.println("Fetched Dynamic Text: " + dynamicText);
        page.waitForTimeout(3000);

        docLibPage.clickOnActionsButton();
        page.waitForTimeout(2000);
        docLibPage.clickOnDeleteOption();
        page.waitForTimeout(2000);
        docLibPage.clickOnOkButton();
        page.waitForTimeout(3000);

        docLibPage.enterIntoSearchBox(dynamicText);
        docLibPage.noRecordsElementMethod();

        System.out.println("test_TC_DL_39 Passed!");
    }


    // ================================================================
    // TC_DL_40 — Update access control for a document
    // ================================================================
    @Test(priority = 21)
    public void test_TC_DL_40_updatingTheAccessOfTheContent() {
        System.out.println("=== TEST CASE TC_DL_40 EXECUTING ===");

        page.waitForTimeout(3000);

        docLibPage.clickOnCheckBoxOption();
        String dynamicText = docLibPage.getDynamicText();
        System.out.println("Fetched Dynamic Text: " + dynamicText);

        docLibPage.clickOnActionsButton();
        docLibPage.clickOnAccessOption();
        page.waitForTimeout(2000);

        docLibPage.clickOnTeamRadioButton();
        page.waitForTimeout(2000);

        docLibPage.clickOnPartnerCategoryButton();
        page.waitForTimeout(2000);

        docLibPage.clickOnCategory();
        page.waitForTimeout(2000);

        docLibPage.clickOnPartnerCategoryButton(); // close dropdown
        page.waitForTimeout(2000);

        docLibPage.clickOnScheduleCheckbox();
        page.waitForTimeout(2000);
        docLibPage.clickOnScheduleCheckbox();
        page.waitForTimeout(2000);

        docLibPage.clickOnScheduleTextbox();
        page.waitForTimeout(2000);

        docLibPage.selectCurrentActiveTimeThree();

        // Uncomment to select a specific date:
        // docLibPage.selectTodayInCalendar();
        // docLibPage.selectDateOfYourChoice(10, 8, 2025);

        page.waitForTimeout(2000);
        docLibPage.clickOnUpdateAccessButton();

        System.out.println("test_TC_DL_40 Passed!");
    }

}

