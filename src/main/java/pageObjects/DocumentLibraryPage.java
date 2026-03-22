package pageObjects;

import com.microsoft.playwright.Page;

import java.nio.file.Paths;

import com.microsoft.playwright.Locator;

public class DocumentLibraryPage {
	
	private Page page;
	
	public DocumentLibraryPage(Page page) {
	    this.page = page;
	}
	
	 // LOCATORS
	
	// Selenium: driver.findElement(By.xpath("..."))
    // Playwright: page.locator("xpath=...") or page.locator("#id")
    // ================================================================

    private Locator communicationTab(){ 
    	return page.locator("//span[text()='Communication']"); 
    }
    private Locator documentLibraryLink(){ 
    	return page.locator("(//a[@class='dropdown-item'])[5]"); 
    }
    private Locator actionsButton(){ 
    	return page.locator("(//*[name()='svg'])[1]"); 
    }
    private Locator profileIcon(){ 
    	return page.locator("//i[@class='fa fa-user-circle']"); 
    }
    private Locator logOutOption(){ 
    	return page.locator("//a[normalize-space()='Log Out']"); 
    }
    private Locator logoutButtonPrimary(){ 
    	return page.locator("//a[normalize-space()='Yes' and contains(@class,'btn-primary')]"); 
    }
    private Locator logoutButtonYes(){ 
    	return page.locator("//a[normalize-space()='Yes']"); 
    }
    private Locator uploadOption(){ 
    	return page.locator("//a[@href='sp-upload-document.php']"); 
    }
    private Locator uploadButton(){ 
    	return page.locator("//input[@id='share_button']"); 
    }
    private Locator documentNameField(){ 
    	return page.locator("//input[@id='document_name']"); 
    }
    private Locator fileInput(){ 
    	return page.locator("#document_file"); 
    }
    private Locator thumbnailInput(){ 
    	return page.locator("#img_validate"); 
    }
    private Locator croppingHandle(){
    	return page.locator("//div[@class='imgareaselect-border4']"); 
    }
    private Locator applyButton(){ 
    	return page.locator("//a[@class='btn yes yellow-gold pull-right']"); 
    }
    private Locator descriptionField(){ 
    	return page.locator("//textarea[@class='form-control h150']"); 
    }
    private Locator documentOptionTwo(){ 
    	return page.locator("//input[@value='2']"); 
    }
    private Locator documentOptionThree(){ 
    	return page.locator("//input[@value='3']"); 
    }
    private Locator downloadableToggle(){ 
    	return page.locator("//span[@class='bootstrap-switch-handle-off bootstrap-switch-default']"); 
    }
    private Locator hashtagField(){ 
    	return page.locator("//input[@id='tagcsv']"); 
    }
    private Locator hashtagSuggestion(){ 
    	return page.locator("//li[@class='ui-menu-item']"); 
    }
    private Locator searchBox(){ 
    	return page.locator("//input[@type='search' and @placeholder='Search']"); 
    }
    private Locator searchResult(){ 
    	return page.locator("//td[normalize-space()='ewewew test']");
    }
    private Locator deleteOption(){ 
    	return page.locator("#Delete3"); 
    }
    private Locator okButton(){ 
    	return page.locator("//button[@type='button' and @class='btn btn-primary bootbox-accept' and text()='OK']"); 
    }
    private Locator dialogBox(){ 
    	return page.locator("//div[@class='bootbox-body']");
    }
    private Locator checkboxOption(){ 
    	return page.locator("(//input[@id='document_content'])[1]");
    }
    private Locator dynamicElement(){ 
    	return page.locator("(//td[@class='wBreak d-none d-md-table-cell' and @style='cursor: no-drop;'])[1]"); 
    }
    private Locator noRecordsElement(){ 
    	return page.locator("//td[@class='dataTables_empty' and text()='No matching records found']"); 
    }
    private Locator accessOption(){ 
    	return page.locator("//a[@id='add_synd']"); 
    }
    private Locator teamRadioButton(){ 
    	return page.locator("//input[@id='partners_option']"); 
    }
    private Locator partnerCategoryButton() { 
    	return page.locator("//button[@id='btn_ptr_category']");
    }
    private Locator categoryLabel(){ 
    	return page.locator("//label[@for='ms-opt-40']"); 
    }
    private Locator updateAccessButton(){ 
    	return page.locator("//input[@id='synd_update_id']"); 
    }
    private Locator scheduleCheckbox(){ 
    	return page.locator("//input[@id='schedule']"); 
    }
    private Locator scheduleTextbox(){ 
    	return page.locator("//input[@id='schedule_synd']");
    }
    private Locator contentUpdateDate(){ 
    	return page.locator("//input[@id='start_date' and @name='start_date' and @type='text']"); 
    }
	
	
	
	
	// ================================================================
    // FILE PATHS — replace with your actual file paths
    // In Playwright, files are set directly on the input element.
    // NO AutoIt EXE files needed at all!
    // ================================================================

    public static final String PDF_FILE   = "src/main/resources/testfiles/Autopost Done Notification";
    public static final String PNG_FILE   = "src/main/resources/testfiles/Amsterdam.png";
    public static final String JPG_FILE   = "src/main/resources/testfiles/budapest.jpg";
    public static final String CSV_FILE   = "src/main/resources/testfiles/pushnotificationsspuat.csv";
    public static final String XLSX_FILE  = "src/main/resources/testfiles/test.xlsx";
    public static final String MP4_FILE   = "src/main/resources/testfiles/video.mp4";
    public static final String GIF_FILE   = "src/main/resources/testfiles/Wallpaper01.gif";

    public static final String THUMBNAIL_PNG = "src/main/resources/testfiles/empirestate.png";
    public static final String THUMBNAIL_GIF = "src/main/resources/testfiles/Wallpaper01.gif";
    public static final String THUMBNAIL_JPG = "src/main/resources/testfiles/goldengate.jpg";
	
    // NAVIGATION METHODS
    // ================================================================

    public void clickOnCommunicationTab() {
        communicationTab().click();
    }

    public void clickOnDocumentLibrary() {
        documentLibraryLink().click();
    }

    public void clickOnActionsButton() {
        actionsButton().click();
    }
    
    // ACTIONS MENU OPTIONS
    // ================================================================

    public java.util.List<String> getDocumentLibraryOptions() {
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add(page.locator("//a[@href='sp-upload-document.php']").innerText().trim());
        options.add(page.locator("#add_synd").innerText().trim());
        options.add(page.locator("#add_hastag").innerText().trim());
        options.add(page.locator("#Delete3").innerText().trim());
        return options;
    }
    
    // LOGIN / LOGOUT METHODS
    // ================================================================

    public void clickOnProfileIcon() {
        profileIcon().click();
    }

    // Selenium used JavascriptExecutor to force click on this element.
    // Playwright equivalent: setForce(true)
    public void clickOnLogoutOption() {
        logOutOption().click(new Locator.ClickOptions().setForce(true));
    }

    public void clickOnLogoutButton() {
        logoutButtonPrimary().click(new Locator.ClickOptions().setForce(true));
    }

    public void clickOnLogoutButtonTwo() {
        logoutButtonYes().click(new Locator.ClickOptions().setForce(true));
    }
    
    // UPLOAD FORM METHODS
    // ================================================================

    public void clickOnUploadOption() {
        uploadOption().click();
    }

    public void clickOnUploadButton() {
        uploadButton().scrollIntoViewIfNeeded();
        uploadButton().click();
    }

    // Selenium: element.sendKeys(text)
    // Playwright: locator.fill(text)  — clears the field first, then types
    public void enterValueInDocumentNameField(String documentName) {
        documentNameField().fill(documentName);
    }
    
 // ================================================================
    // FILE UPLOAD METHODS
    //
    // KEY DIFFERENCE FROM SELENIUM:
    // Selenium needed AutoIt EXE to handle the OS file dialog:
    //   actions.moveToElement(fileInput).click().perform();
    //   Thread.sleep(3000);
    //   ProcessBuilder pb = new ProcessBuilder(autoItScript.getAbsolutePath());
    //   pb.start().waitFor();
    //
    // Playwright sets the file directly on the input — no dialog at all:
    //   page.locator("#document_file").setInputFiles(Paths.get("path/to/file"));
    // ================================================================

    public void uploadDocument(String filePath) {
        fileInput().setInputFiles(Paths.get(filePath));
    }

    public void uploadDocumentUsingPDF()  { uploadDocument(PDF_FILE); }
    public void uploadDocumentUsingPNG()  { uploadDocument(PNG_FILE); }
    public void uploadDocumentUsingJPG()  { uploadDocument(JPG_FILE); }
    public void uploadDocumentUsingCSV()  { uploadDocument(CSV_FILE); }
    public void uploadDocumentUsingXLSX() { uploadDocument(XLSX_FILE); }
    public void uploadDocumentUsingMP4()  { uploadDocument(MP4_FILE); }
    public void uploadDocumentUsingGIF()  { uploadDocument(GIF_FILE); }


    // ================================================================
    // THUMBNAIL
    // Selenium: thumbnailInput.sendKeys(filePath)
    // Playwright: locator.setInputFiles(Paths.get(filePath))
    // ================================================================

    public void attachThumbnail(String thumbnailPath) {
        thumbnailInput().setInputFiles(Paths.get(thumbnailPath));
    }


    // ================================================================
    // CROP AREA RESIZE
    // Selenium: Actions.clickAndHold → moveByOffset(50, 50) → release
    // Playwright: page.mouse().move/down/up
    // ================================================================

    public void resizeCroppingArea() {
        croppingHandle().waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
        com.microsoft.playwright.BoundingBox box = croppingHandle().boundingBox();
        if (box != null) {
            page.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
            page.mouse().down();
            page.mouse().move(box.x + box.width / 2 + 50, box.y + box.height / 2 + 50);
            page.mouse().up();
        }
    }

    public void clickOnApplyButton() {
        applyButton().click();
    }

    public void enterValueInDescriptionField(String text) {
        descriptionField().scrollIntoViewIfNeeded();
        descriptionField().fill(text);
    }
    
 // ================================================================
    // VALIDATION MESSAGE METHODS
    //
    // Selenium: js.executeScript("return arguments[0].validationMessage", element)
    // Playwright: locator.evaluate("el => el.validationMessage")
    // ================================================================

    public String getValidationMessageForDocumentNameField() {
        return (String) documentNameField().evaluate("el => el.validationMessage");
    }

    public String getValidationMessageForDescriptionField() {
        return (String) descriptionField().evaluate("el => el.validationMessage");
    }

    public String getValidationMessageForDocumentAttachmentField() {
        return (String) fileInput().evaluate("el => el.validationMessage");
    }


    // ================================================================
    // DOCUMENT OPTIONS
    // ================================================================

    public void clickOnDocumentOptionTwo() {
        documentOptionTwo().click();
    }

    public void clickOnDocumentOptionThree() {
        documentOptionThree().click();
    }

    public void clickOnDownloadableOption() {
        downloadableToggle().click();
    }


    // ================================================================
    // HASHTAG METHODS
    // ================================================================

    public void enterInternalHashtag(String text) {
        hashtagField().fill(text);
    }

    public void selectInternalHashtag() {
        hashtagSuggestion().click();
    }


    // ================================================================
    // SEARCH & RESULTS
    // ================================================================

    public void enterIntoSearchBox(String text) {
        searchBox().fill(text);
    }

    public String getSearchResultText() {
        return searchResult().innerText();
    }


    // ================================================================
    // DELETE FLOW
    // ================================================================

    public void clickOnDeleteOption() {
        deleteOption().click();
    }

    public void clickOnOkButton() {
        okButton().click();
    }

    public String getDialogBoxText() {
        return dialogBox().innerText();
    }


    // ================================================================
    // CHECKBOX & DYNAMIC TEXT
    // ================================================================

    public void clickOnCheckBoxOption() {
        checkboxOption().click();
    }

    public String getDynamicText() {
        return dynamicElement().innerText();
    }

    public String noRecordsElementMethod() {
        noRecordsElement().waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
        return noRecordsElement().innerText();
    }


    // ================================================================
    // ACCESS CONTROL METHODS
    // ================================================================

    public void clickOnAccessOption() {
        accessOption().click();
    }

    // Selenium used JavascriptExecutor to click the radio button.
    // Playwright handles it with a plain click() — no JS needed.
    public void clickOnTeamRadioButton() {
        teamRadioButton().click();
    }

    public void clickOnPartnerCategoryButton() {
        partnerCategoryButton().click();
    }

    public void clickOnCategory() {
        categoryLabel().click();
    }

    public void clickOnUpdateAccessButton() {
        updateAccessButton().click();
    }


    // ================================================================
    // SCHEDULE METHODS
    // ================================================================

    public void clickOnScheduleCheckbox() {
        scheduleCheckbox().click();
    }

    public void clickOnScheduleTextbox() {
        scheduleTextbox().click();
    }

    public void clickOnContentUpdate() {
        contentUpdateDate().click();
    }


    // ================================================================
    // CALENDAR METHODS
    // ================================================================

    public void selectTodayInCalendar() {
        page.locator("//div[contains(@class, 'xdsoft_datetimepicker') and contains(@style, 'display: block')]")
            .waitFor(new Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));

        Locator todayElement = page.locator(
            "//td[contains(@class, 'xdsoft_date') and contains(@class, 'xdsoft_today')]");
        todayElement.waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
        todayElement.click();
    }

    public void selectDateOfYourChoice(int day, int month, int year) {
        page.locator("//div[contains(@class, 'xdsoft_datetimepicker') and contains(@style, 'display: block')]")
            .waitFor(new Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));

        // Year
        page.locator("//div[contains(@class,'xdsoft_label') and contains(@class,'xdsoft_year')]/span").click();
        page.locator("//div[contains(@class,'xdsoft_yearselect')]//div[@data-value='" + year + "']").click();

        // Month — xdsoft uses 0-based month index internally
        page.locator("//div[contains(@class,'xdsoft_label') and contains(@class,'xdsoft_month')]/span").click();
        page.locator("//div[contains(@class,'xdsoft_monthselect')]//div[@data-value='" + (month - 1) + "']").click();

        // Day
        page.locator("//td[contains(@class,'xdsoft_date') and not(contains(@class,'xdsoft_disabled')) and @data-date='" + day + "']").click();

        // Time — try active/highlighted time first, fall back to first available
        Locator activeTime = page.locator(
            "//div[contains(@class,'xdsoft_datetimepicker') and contains(@style,'display: block')]" +
            "//div[contains(@class,'xdsoft_time') and contains(@class,'xdsoft_current')]");

        if (activeTime.isVisible()) {
            activeTime.scrollIntoViewIfNeeded();
            activeTime.click();
        } else {
            Locator firstTime = page.locator(
                "(//div[contains(@class,'xdsoft_datetimepicker') and contains(@style,'display: block')]" +
                "//div[contains(@class,'xdsoft_time')])[1]");
            firstTime.scrollIntoViewIfNeeded();
            firstTime.click();
        }
    }

    public void selectCurrentActiveTimeThree() {
        page.locator("//div[contains(@class, 'xdsoft_datetimepicker') and contains(@style,'display: block')]")
            .waitFor(new Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));

        Locator activeTime = page.locator(
            "//div[contains(@class,'xdsoft_datetimepicker') and contains(@style,'display: block')]" +
            "//div[contains(@class,'xdsoft_time') and contains(@class,'xdsoft_current')]");

        if (activeTime.isVisible()) {
            activeTime.scrollIntoViewIfNeeded();
            activeTime.click();
        } else {
            Locator firstTime = page.locator(
                "(//div[contains(@class,'xdsoft_datetimepicker') and contains(@style,'display: block')]" +
                "//div[contains(@class,'xdsoft_time')])[1]");
            firstTime.scrollIntoViewIfNeeded();
            firstTime.click();
        }
    }


    // ================================================================
    // SCROLL HELPERS
    // Selenium had a Utilities class using JavascriptExecutor.
    // Playwright uses page.evaluate() to run JavaScript directly.
    // ================================================================

    public void scrollToTop() {
        page.evaluate("() => window.scrollTo(0, 0)");
    }

    public void scrollToBottom() {
        page.evaluate("() => window.scrollTo(0, document.body.scrollHeight)");
    }

    public void scrollDownByFiveHundred() {
        page.evaluate("() => window.scrollBy(0, 500)");
    }
    
    private Locator searchResult(String text) {
        return page.locator("//td[normalize-space()='" + text + "']");
    }
    public String getSearchResultText(String text) {
        return searchResult(text).innerText();
    }

	
	
	
	
	
	
	
	
	
	

}

   
