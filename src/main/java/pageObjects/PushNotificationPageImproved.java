package pageObjects;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import utils.ConfigReader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PushNotificationPageImproved {
	
	private Page page;

    // LOCATORS
    private Locator communicationTab;
    private Locator notifications;
    private Locator notificationsprod;
    private Locator pageHeading;
    private Locator actionsButton;
    private Locator createAppNotification;

    private Locator notificationNameField;
    private Locator notificationMessageField;

    private Locator partnerListRadioButton;
    private Locator partnerCategoryRadioButton;

    private Locator categoryDropdown;
    private Locator searchTextField;
    private Locator selectAllButton;

    private Locator imageUpload;

    private Locator customLinkButton;
    private Locator customLinkTextField;

    private Locator schedulingDateTime;

    private Locator submitButton;

    private Locator profileIcon;
    private Locator logoutOption;
    private Locator logoutButton;

    private Locator pushNotificationRadioButton;
    private Locator whatsAppRadioButton;

    private Locator uploadListRadioButton;

    private Locator toastMessage;
    private Locator closeToastButton;

    private Locator contentLinkButton;
    private Locator contentLinkDropdown;
    private Locator contentSelection;

    private Locator uploadCsvButton;

    // CONSTRUCTOR
    public PushNotificationPageImproved(Page page) {

        this.page = page;

        communicationTab = page.locator("//span[text()='Communication']");
        notifications = page.locator("//a[normalize-space()='New Push Notification']");
        notificationsprod = page.locator("//a[normalize-space()='Push Notification']");
        pageHeading = page.locator("//span[@class='fs-2 fw-bolder']");

        actionsButton = page.locator("(//*[name()='svg'])[1]");
        createAppNotification = page.locator("(//a/span[text()='Create App Notification'])[1]");

        notificationNameField = page.locator("#pushnotify_name");
        notificationMessageField = page.locator("#pushnotify_msg");

        partnerListRadioButton = page.locator("#upload_list");
        partnerCategoryRadioButton = page.locator("#partner_category");

        categoryDropdown = page.locator("#btn_ptr_category");
        searchTextField = page.locator("//input[@placeholder='Search']");
        selectAllButton = page.locator("//a[@class='ms-selectall global']");

        imageUpload = page.locator("//input[@name='image_url']");

        customLinkButton = page.locator("//label[@for='custom-link']");
        customLinkTextField = page.locator("//input[@placeholder='Enter link']");

        schedulingDateTime = page.locator("//input[@name='pushnotify_time']");

        submitButton = page.locator("//button[@type='submit']");

        profileIcon = page.locator("//span/i[@class='text-primary fas fa-user-circle']");
        logoutOption = page.locator("(//span[@class='ms-2'])[3]");
        logoutButton = page.locator("//button[@class='btn btn-primary']");

        pushNotificationRadioButton = page.locator("//input[@name='channel'][@value='1']");
        whatsAppRadioButton = page.locator("//input[@name='channel'][@value='2']");

        uploadListRadioButton = page.locator("//input[@name='send_to'][@value='upload_list']");

        toastMessage = page.locator("//span[@class='mssg_content']");
        closeToastButton = page.locator("//span[@onclick='close_success_mssg()']");

        contentLinkButton = page.locator("//label[@for='content-link']");
        contentLinkDropdown = page.locator("#select2-contentLinkDropdown-container");
        contentSelection = page.locator("(//li[contains(@class,'select2-results__option')])[1]");

        uploadCsvButton = page.locator("//input[@id='upload_csv']");
    }

    // NAVIGATION

    public void openPushNotificationPage() {
        communicationTab.click();

        // Pick the correct menu link locator based on the active environment.
        // The link text differs between prod and dev/preprod because the feature
        // is at different release stages across servers.
        // env=prod  → "Push Notification"     (app.technochimes.com)
        // env=dev / env=preprod → "New Push Notification" (app.spdevmfp.com / app.sppreprod.in)
        if (ConfigReader.get("env").equals("prod")) {
            notificationsprod.click();
        } else {
            notifications.click();
        }
    }

    // public void openCreateNotification() {
    //    openPushNotificationPage();
    //    actionsButton.click();
        // createAppNotification.click();
    //    createAppNotification.first().click();
    //    }
    
    public void openCreateNotification() {
        openPushNotificationPage();
        actionsButton.click();
        createAppNotification.first().waitFor(
            new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10000)   // wait up to 10 seconds for the menu to appear
        );
        createAppNotification.first().click();
    }

    // FORM ACTIONS

    public void enterNotificationName(String name) {
    	notificationNameField.scrollIntoViewIfNeeded();
    	notificationNameField.fill(name);
    }

    public void enterNotificationMessage(String message) {
    	
    	notificationMessageField.scrollIntoViewIfNeeded();
        notificationMessageField.fill(message);
    }

    public void clickPartnerListRadio() {
        
    	partnerListRadioButton.scrollIntoViewIfNeeded();
    	partnerListRadioButton.check();
    }

    public void clickPartnerCategoryRadio() {
    	
    	partnerCategoryRadioButton.scrollIntoViewIfNeeded();
        partnerCategoryRadioButton.check();
    }

    public void openCategoryDropdown() {
    	
    	categoryDropdown.scrollIntoViewIfNeeded();
        categoryDropdown.click();
    }

    public void searchCategory(String value) {
        searchTextField.fill(value);
    }

    public void clickSelectAllCategories() {
    	
    	selectAllButton.scrollIntoViewIfNeeded();
        selectAllButton.click();
    }

    public void uploadImage(Path filePath) {
    	
    	imageUpload.scrollIntoViewIfNeeded();
        imageUpload.setInputFiles(filePath);
    }

    public void clickCustomLink() {
    	
    	customLinkButton.scrollIntoViewIfNeeded();
        customLinkButton.click();
    }

    public void enterCustomLink(String link) {
    	
    	customLinkTextField.scrollIntoViewIfNeeded();
        customLinkTextField.fill(link);
    }

    public void enterSchedulingDateTime(String date, String time) {

        String formattedDateTime = formatToDateTimeLocal(date, time);
        schedulingDateTime.scrollIntoViewIfNeeded();
        schedulingDateTime.fill(formattedDateTime);
    }

    private String formatToDateTimeLocal(String date, String time) {

        String[] parts = date.split("/");

        String day = parts[0];
        String month = parts[1];
        String year = parts[2];

        return year + "-" + month + "-" + day + "T" + time;
    }

    public void clickSubmit() {
    	
    	submitButton.scrollIntoViewIfNeeded();
        submitButton.click();
    }

    // RADIO BUTTONS

    public void clickPushNotificationRadio() {
        pushNotificationRadioButton.check();
    }

    public void clickWhatsAppRadio() {
        whatsAppRadioButton.check();
    }

    public boolean isPushNotificationSelected() {
        return pushNotificationRadioButton.isChecked();
    }

    public boolean isWhatsAppSelected() {
        return whatsAppRadioButton.isChecked();
    }

    public void clickUploadListRadio() {
        uploadListRadioButton.check();
    }

    public boolean isUploadListSelected() {
        return uploadListRadioButton.isChecked();
    }

    public boolean isPartnerCategorySelected() {
        return partnerCategoryRadioButton.isChecked();
    }

    // ACTION MENU OPTIONS

    public List<String> getActionMenuOptions() {

        List<String> options = new ArrayList<>();

        List<Locator> elements = page.locator("//a[@class='menu-link px-3']").all();

        for (Locator element : elements) {
            options.add(element.textContent().trim());
        }

        return options;
    }

    // CONTENT LINK

    public void clickContentLink() {
        contentLinkButton.click();
    }

    public void openContentDropdown() {
        contentLinkDropdown.click();
    }

    public void selectContentOption() {
        contentSelection.click();
    }

    // CSV

    public void uploadCsv(Path filePath) {
        uploadCsvButton.setInputFiles(filePath);
    }

    // VALIDATIONS

    public String getNotificationNameValidation() {
        Object validation = notificationNameField.evaluate("el => el.validationMessage");
        return validation.toString();
    }

    public String getNotificationMessageValidation() {
        Object validation = notificationMessageField.evaluate("el => el.validationMessage");
        return validation.toString();
    }

    public String getCustomLinkValidation() {
        return page.locator("#customlink_error").textContent().trim();
    }

    public String getUploadCsvValidation() {
        Object validation = uploadCsvButton.evaluate("el => el.validationMessage");
        return validation.toString();
    }

    // UTILITY ACTIONS

    public void clickOnBlankSpace() {
        page.mouse().click(50,150);
    }

    public void scrollToCategoryDropdown() {
        categoryDropdown.scrollIntoViewIfNeeded();
    }

    public void hoverOverAddPhotoButton() {
        imageUpload.hover();
    }

    public String getTotalCategoriesText() {
        return categoryDropdown.textContent().trim();
    }

    public boolean searchAndValidateOption(String searchValue) {

        searchTextField.fill(searchValue);

        List<Locator> checkboxes = page.locator("//input[@type='checkbox']").all();

        for (Locator checkbox : checkboxes) {

            String optionText = checkbox.getAttribute("title");

            if(optionText != null && optionText.equals(searchValue)) {
                return true;
            }
        }

        return false;
    }

    public void clickOnTargetCategory() {

        Locator targetCategory =
                page.locator("//label[normalize-space()='Raj2024']");

        targetCategory.click();
    }

    // TOAST

    public String getToastMessageText() {
        return toastMessage.textContent().trim();
    }

    public void closeToastMessage() {
        closeToastButton.click();
    }

    // HEADER ACTIONS

    public void logout() {
        profileIcon.click();
        logoutOption.click();
        logoutButton.click();
    }

    // PAGE HEADING

    public String getPageHeading() {
        return pageHeading.textContent().trim();
    }
    
    public boolean isImageUploaded() {
        return page.locator("#myImage").isVisible();
    }
    
    public boolean isCsvUploaded() {
        String fileName = page.locator("#file_name span").textContent();
        return fileName.contains(".csv");
    }
    
    // inputValue() is the Playwright method to read what's currently typed in a text field — different from textContent()
    // which is for display elements.
    public String getNotificationMessageText() {
        return notificationMessageField.inputValue();
    }

    
}
   
