package pageObjects;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import utils.ConfigReader;
import java.nio.file.Paths;

public class SocialAutoPostPagePW {

    private Page page;

    public SocialAutoPostPagePW(Page page) {
        this.page = page;
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    public void clickOnCommunicationTabPreprod() {
        Locator tab = page.locator("//span[contains(normalize-space(), 'Communication')]");
        try {
            tab.click();
        } catch (Exception e) {
            page.locator("//a[.//span[contains(normalize-space(), 'Communication')]]").click();
        }
    }

    public void clickOnAutomationTab() {
        page.locator("//span[normalize-space()='Automation']").click();
    }

    public void clickOnSocialOption() {
        page.locator("//a[normalize-space()='Social']").click();
    }

    public void clickOnSocialAutoPostOptionPreprod() {
        page.locator("//a[normalize-space()='Social Auto Post']").click();
    }

    public void clickOnAutoPostTab() {
        page.locator("//a[normalize-space()='Auto Post']").click();
    }

    // ── Create Post ────────────────────────────────────────────────────────────

    public void clickOnActionsButton() {
        Locator btn = page.locator("//div[contains(@class, 'btn-group')]//a[@data-bs-toggle='dropdown']");
        btn.hover();
        btn.click();
    }

    public void clickOnCreatePostButton() {
        page.locator("//a[normalize-space()='Create Post']").click();
    }

    // ── File Upload ────────────────────────────────────────────────────────────

    public void uploadFileInPNG(String filePath) {
        page.locator("#file-upload").setInputFiles(Paths.get(filePath));
    }

    public void uploadFileInJPG(String filePath) {
        page.locator("#file-upload").setInputFiles(Paths.get(filePath));
    }

    public void uploadFileInMP4(String filePath) {
        page.locator("#file-upload").setInputFiles(Paths.get(filePath));
    }

    public void uploadThumbnailInJPG(String filePath) {
        page.locator("//input[@id='social_tumbnail']").setInputFiles(Paths.get(filePath));
    }

    // ── Form Fields ────────────────────────────────────────────────────────────

    public void clickOnEnableCobrandingButton() {
        page.locator("#videobrand").evaluate("el => el.click()");
    }

    public void enterInTitleTextfield(String titleName) {
        page.locator("//input[@name='title' and @id='title']").fill(titleName);
    }

    public void enterValueInDescriptionTextfield(String description) {
        page.locator("//textarea[@id='description_link']").fill(description);
    }

    // ── Partner Category ───────────────────────────────────────────────────────

    public void clickOnPartnerCategoryButton() {
        page.locator("//div[@id='multiSelectDisplay']").click();
        // Wait for dropdown to open
        page.locator("#searchBox").waitFor();
    }

    public void clickOnSelectPartnerCategory() {
        // Search term and partner value are driven from config so this works on both
        // preprod (value=287) and prod (value=478) without any code change.
        page.locator("#searchBox").fill(ConfigReader.get("social.partner.search"));
        page.locator("//input[@value='" + ConfigReader.get("social.partner.value") + "']").click();
    }

    public void clickOnStaticText() {
        Locator searchBox = page.locator("#searchBox");
        if (searchBox.isVisible()) {
            page.locator("//div[@id='multiSelectDisplay']").click();
            searchBox.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        }
        // Post-wait — confirms dropdown is closed and social checkboxes are accessible
        page.locator("//label[normalize-space()='Facebook']").waitFor();
    }

    // ── Social Media Checkboxes ────────────────────────────────────────────────

    public void clickOnTwitter() {
        page.locator("//label[normalize-space()='Twitter']").click();
    }

    public void clickOnLinkedIn() {
        page.locator("//label[normalize-space()='LinkedIn']").click();
    }

    public void clickOnFacebook() {
        Locator fb = page.locator("//label[normalize-space()='Facebook']");
        fb.scrollIntoViewIfNeeded();
        fb.click();
    }

    // ── URL Options ────────────────────────────────────────────────────────────

    public void clickOnCustomURLRadioButton() {
        page.locator("//input[@type='radio' and @id='C']").evaluate("el => el.click()");
    }

    public void enterCustomURL() {
        // URL is read from config so it can be changed per environment without touching test code.
        page.locator("//input[@name='custom_url']").fill(ConfigReader.get("social.custom.url"));
    }

    public void clickOnNoneRadioButton() {
        page.locator("//input[@type='radio' and @id='N']").evaluate("el => el.click()");
    }

    // ── Date / Time Picker ─────────────────────────────────────────────────────

    public void clickOnOpenDateTimePicker() {
        Locator dateTimeButton = page.locator("//input[contains(@class, 'form_datetime')]");
        dateTimeButton.click();
        dateTimeButton.evaluate("el => el.scrollIntoView(true)");
        dateTimeButton.evaluate("el => el.focus()");
        page.locator("div.xdsoft_datepicker").waitFor();
    }

    public void selectFutureDate(String expectedDay, String expectedMonthYear) {
        int maxAttempts = 24;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String displayedMonth = page.locator("//div[@class='xdsoft_label xdsoft_month']/span").textContent().trim();
            String displayedYear  = page.locator("//div[@class='xdsoft_label xdsoft_year']/span").textContent().trim();
            String displayedMonthYear = displayedMonth + " " + displayedYear;
            System.out.println("Currently displayed month & year: " + displayedMonthYear);

            if (displayedMonthYear.equals(expectedMonthYear)) {
                Locator dateElement = page.locator(
                    "//td[contains(@class,'xdsoft_date') and @data-date='" + expectedDay +
                    "' and not(contains(@class,'xdsoft_disabled'))]"
                );
                dateElement.waitFor();
                dateElement.scrollIntoViewIfNeeded();
                dateElement.click();
                // Wait for datetime input to be populated after date click
                Locator dateInput = page.locator("//input[contains(@class, 'form_datetime')]");
                page.waitForCondition(() -> !dateInput.inputValue().isEmpty());
                return;
            }

            Locator nextButton = page.locator("//button[contains(@class,'xdsoft_next')]");
            nextButton.scrollIntoViewIfNeeded();
            nextButton.click();
            page.waitForTimeout(500); // xdsoft month-change animation — no deterministic signal
        }
        throw new RuntimeException("Exceeded max attempts. Could not find the correct month: " + expectedMonthYear);
    }

    public void selectTime(String hour, String minute) {
        page.locator("div.xdsoft_time").first().waitFor();
        Locator timeElement = page.locator(
            "//div[contains(@class,'xdsoft_time') and @data-hour='" + hour + "' and @data-minute='" + minute + "']"
        );
        timeElement.scrollIntoViewIfNeeded();
        // xdsoft clips time items inside overflow container — JS click bypasses that
        timeElement.evaluate("el => el.click()");
    }

    public void verifySelection() {
        String value = page.locator("//input[contains(@class, 'form_datetime')]").inputValue();
        System.out.println("Selected Date & Time: " + value);
    }

    // ── Submit ─────────────────────────────────────────────────────────────────

    public void clickOnSchedulePostButton() {
        Locator btn = page.locator("//button[@id='share_button']");
        btn.scrollIntoViewIfNeeded();
        btn.click();
    }

    // ── Scroll Helpers ─────────────────────────────────────────────────────────

    public void scrollDownByTwoHundred() {
        page.evaluate("window.scrollBy(0, 200)");
    }

    public void scrollDownByFiveHundred() {
        page.evaluate("window.scrollBy(0, 500)");
    }
}
