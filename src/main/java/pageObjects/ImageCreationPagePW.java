package pageObjects;

import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import utils.ConfigReader;

import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Page Object for the Image Creation flow:
 *   Asset Library → Add New Asset → Social Post → Asset Details → Upload → Publish Setup → Publish
 *
 * KEY PLAYWRIGHT CONCEPTS IN THIS FILE (read these — they will come up in interviews):
 *
 * 1. AUTO-WAITING
 *    In Selenium you had to write: wait.until(ExpectedConditions.elementToBeClickable(...))
 *    In Playwright, EVERY locator.click(), locator.fill(), etc. automatically waits until
 *    the element is visible, enabled, and not covered — no WebDriverWait needed at all.
 *
 * 2. FORCE CLICK  (.click(new Locator.ClickOptions().setForce(true)))
 *    When an overlay or animation is blocking a click, Playwright's solution is setForce(true).
 *    This skips the "is the element covered?" check and fires the click directly on the element.
 *    In Selenium, you did the same thing with: ((JavascriptExecutor) driver).executeScript("arguments[0].click()", el)
 *
 * 3. NATIVE FILE DIALOG  (page.waitForFileChooser())
 *    Some buttons open a native OS "Open File" dialog (not an HTML element).
 *    Selenium had no clean way to handle this — you had to use the Robot class + clipboard hack.
 *    Playwright intercepts the OS dialog BEFORE it opens, via waitForFileChooser().
 *    The lambda inside triggers the click; Playwright catches the dialog and fills it with setFiles().
 *
 * 4. HIDDEN FILE INPUT  (locator.setInputFiles())
 *    On page 2 of the wizard there is a hidden <input type="file"> element.
 *    Selenium used: fileInput.sendKeys("path/to/file")
 *    Playwright uses: locator.setInputFiles(Paths.get("path/to/file")) — same concept, cleaner API.
 *
 * 5. WAIT FOR ELEMENT TO DISAPPEAR  (page.waitForSelector(..., HIDDEN state))
 *    After selecting a dropdown option, an overlay-bg div stays visible for a moment.
 *    We must wait for it to disappear before clicking the next element.
 *    Playwright: page.waitForSelector("//div[@class='overlay-bg']", options with state=HIDDEN)
 *    Selenium:   wait.until(ExpectedConditions.invisibilityOfElementLocated(...))
 */
public class ImageCreationPagePW {

    private final Page page;

    // We read env once in the constructor so every method can build env-specific config keys
    // e.g. "image.category.option." + env → "image.category.option.prod"
    private final String env;

    public ImageCreationPagePW(Page page) {
        this.page = page;
        this.env = ConfigReader.get("env");
    }

    // ── Page 0: Asset Library ───────────────────────────────────────────────────

    /**
     * Clicks the "Add New Asset" button on the Asset Library page.
     *
     * PLAYWRIGHT CONCEPT — Auto-waiting:
     * No explicit wait is written here. Playwright's locator.click() automatically waits
     * until the button is visible and clickable before firing the click.
     * This replaces: wait.until(ExpectedConditions.elementToBeClickable(By.xpath("...")))
     */
    public void clickOnAddNewAssetButton() {
        page.locator("//button[@class='add-new-asset-btn btn btn-info']").click();
    }

    /**
     * Clicks the "Social Post" card from the asset type selection dialog.
     *
     * WHY ENV-SPECIFIC INDEX:
     * The card position in the dialog differs by environment — on dev, Social Post is card [3];
     * on prod it is card [4] because more modules are installed and the order shifts.
     * We drive the index from config so switching env=prod vs env=dev requires zero code change.
     */
    public void clickOnSocialPostButton() {
        String index = ConfigReader.get("image.social.post.index." + env);
        page.locator("(//div[@class='card-body'])[" + index + "]").click();
    }

    /**
     * Waits for the Social Post creation form to fully load.
     *
     * We wait for the "Next" button to appear — its presence confirms the form page is ready.
     * page.waitForSelector() is Playwright's equivalent of:
     *   wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("...")))
     */
    public void waitForSocialPostPageToLoad() {
        page.waitForSelector("//button[normalize-space()='Next']");
    }

    // ── Page 1: Social Post Form (Attach + Next) ────────────────────────────────

    /**
     * Clicks the "Attach" button and handles the native OS file dialog.
     *
     * PLAYWRIGHT CONCEPT — waitForFileChooser() (THE MOST IMPORTANT CONCEPT HERE):
     *
     * The "Attach" button opens a native OS "Open File" dialog — this is NOT an HTML element,
     * it is an operating system window. Selenium had no built-in way to handle this, so you
     * had to use the Robot class to simulate keyboard input (CTRL+V, then ENTER) after putting
     * the file path into the clipboard. That was fragile and platform-dependent.
     *
     * Playwright intercepts the OS file dialog BEFORE it even opens:
     *   - page.waitForFileChooser( lambda ) — registers a listener for the upcoming dialog
     *   - The lambda triggers the click that would open the dialog
     *   - Playwright catches the dialog event and calls setFiles() to fill in the file path
     *   - The dialog never visually opens on screen
     *
     * The lambda pattern (passing the click INSIDE the waitForFileChooser call) is critical —
     * if you click first and THEN call waitForFileChooser, you will miss the event.
     */
    public void attachFile() {
        // Scroll to the Attach button first so it is in the viewport
        page.locator("//button[normalize-space()='Attach']").scrollIntoViewIfNeeded();

        // Register file chooser listener AND trigger the click inside the same lambda
        // Playwright intercepts the OS dialog before it opens and fills in the file path
        FileChooser fileChooser = page.waitForFileChooser(() ->
            page.locator("//button[normalize-space()='Attach']").click()
        );
        fileChooser.setFiles(Paths.get(ConfigReader.get("image.attach.file")));

        // WHY NO NETWORKIDLE HERE:
        // NETWORKIDLE waits until there are zero network requests for 500ms.
        // SPAs like this have background polling / websocket connections that never fully stop,
        // so NETWORKIDLE never fires and the test hangs for the full 60s timeout.
        // Instead we scroll to bring the Next button into view — this also gives the app a
        // brief moment to finish processing the attached file before the Next click fires.
        page.locator("//button[normalize-space()='Attach']").scrollIntoViewIfNeeded();
    }

    /**
     * Clicks "Next" to move from the Social Post form to the Asset Details page.
     *
     * PLAYWRIGHT CONCEPT — Force click:
     * setForce(true) skips the "is the element covered by an overlay?" check and fires
     * the click directly. Used here because the button can be partially obscured after
     * a file is attached (an animation or overlay is still present).
     *
     * Equivalent Selenium code:
     *   ((JavascriptExecutor) driver).executeScript("arguments[0].click()", nextButton)
     *
     * After clicking, we wait for the Name field — its appearance confirms the Asset Details
     * page has loaded and is ready for input.
     */
    public void clickOnNextButton() {
        Locator nextBtn = page.locator("//button[@type='button' and contains(@class,'btn-info') and contains(text(),'Next')]");

        // Scroll Next button into view before clicking — mirrors what Selenium's window.scrollBy did.
        // Also ensures the button is fully in the viewport so the click registers correctly.
        nextBtn.scrollIntoViewIfNeeded();
        nextBtn.click(new Locator.ClickOptions().setForce(true));

        // WHY REGEX INSTEAD OF GLOB:
        // Glob patterns like "**/global-asset-details**" can silently fail on SPA URLs that use
        // hash routing (e.g. "/#/home/new-asset/global-asset-details") because the # is treated
        // specially. A regex pattern matches against the full raw URL string with no ambiguity.
        page.waitForURL(Pattern.compile(".*global-asset-details.*"));

        // Confirm the form is rendered and ready for input
        page.waitForSelector("//input[@placeholder='Name']");
    }

    // ── Page 2: Asset Details Form ──────────────────────────────────────────────

    /**
     * Types the asset name into the "Name" text field.
     *
     * PLAYWRIGHT CONCEPT — fill() vs sendKeys():
     * locator.fill() is Playwright's equivalent of sendKeys().
     * Difference: fill() clears the field first, then types. sendKeys() appends to existing text.
     * Use fill() when you want to set a value cleanly from scratch.
     */
    public void enterTextIntoNameTextField(String nameText) {
        page.locator("//input[@placeholder='Name']").fill(nameText);
    }

    /**
     * Opens the Categories dropdown by clicking the search box inside it.
     * Force click used because the field can be partially intercepted by the form layout.
     */
    public void clickOnCategoryField() {
        page.locator("(//input[contains(@class,'searchBox')])[1]")
            .click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Selects the correct category option from the dropdown.
     *
     * WHY ENV-SPECIFIC:
     * The category named "Term" exists on dev; on prod the equivalent is "Term Plan".
     * We read the option text from config using the env key so one method covers all environments.
     *
     * After selecting the option we wait for the overlay-bg div to disappear.
     * WHY: The app shows an overlay while the dropdown is open; if we try to click the next
     * element while the overlay is still visible, the click lands on the overlay instead.
     * PLAYWRIGHT CONCEPT — WaitForSelectorState.HIDDEN:
     *   Waits until the element is no longer visible — equivalent to Selenium's
     *   wait.until(ExpectedConditions.invisibilityOfElementLocated(...))
     */
    public void clickOnCategoryOption() {
        String optionText = ConfigReader.get("image.category.option." + env);
        Locator option = page.locator("//li[normalize-space()='" + optionText + "']");

        // evaluate("el => el.click()") fires a raw JS click — bypasses CSS-visibility checks.
        // Equivalent to Selenium's: ((JavascriptExecutor) driver).executeScript("arguments[0].click()", el)
        option.evaluate("el => el.click()");

        // WHY Escape after the JS click:
        // A raw JS el.click() only fires the click event — it does NOT simulate the full mouse
        // event sequence (mousedown → mouseup → click) that the dropdown's close handler listens to.
        // So the option gets selected but the dropdown stays open.
        // Pressing Escape is the most reliable way to close any dropdown regardless of implementation.
        page.keyboard().press("Escape");

        // Wait for overlay to fully disappear before the next interaction
        page.waitForSelector("//div[@class='overlay-bg']",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));
    }

    /**
     * Clicks the "Categories" label to close the dropdown and dismiss focus.
     * Force click because residual overlay can intercept the click.
     */
    public void clickOnCategoriesStaticText() {
        page.locator("//label[normalize-space()='Categories']")
            .click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Opens the Hashtags dropdown by clicking the search box inside it.
     */
    public void clickOnHashtagField() {
        page.locator("//input[@placeholder='Select Hashtags']")
            .click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Selects the correct hashtag from the dropdown.
     * Value is env-specific: dev = "what", prod = "Insurance".
     *
     * scrollIntoViewIfNeeded() scrolls the page until the option is in the viewport before clicking.
     * Equivalent Selenium code:
     *   ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true)", element)
     */
    public void clickOnHashtag() {
        String hashtagText = ConfigReader.get("image.hashtag." + env);
        Locator hashtagOption = page.locator("//li[normalize-space()='" + hashtagText + "']");
        hashtagOption.scrollIntoViewIfNeeded();
        // Same reason as clickOnCategoryOption — CSS-hidden <li>, needs raw JS click
        hashtagOption.evaluate("el => el.click()");

        // Same reason as clickOnCategoryOption — press Escape to close the dropdown
        // after the JS click, since el.click() doesn't trigger the close event handler
        page.keyboard().press("Escape");
    }

    /**
     * Clicks the "Hashtags" label to close the hashtag dropdown and dismiss focus.
     */
    public void clickOnHashtagStaticText() {
        page.locator("//label[normalize-space()='Hashtags']").click();
    }

    /**
     * Scrolls to the very bottom of the page.
     *
     * PLAYWRIGHT CONCEPT — page.evaluate():
     * evaluate() runs JavaScript directly in the browser page, same as:
     *   ((JavascriptExecutor) driver).executeScript("window.scrollTo(...)")
     * Use it whenever Playwright doesn't have a built-in method for what you need.
     */
    public void scrollToPageBottom() {
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
    }

    /**
     * Fills the long description textarea with the text from config.
     */
    public void fillLongTextField() {
        page.locator("//textarea[@id='formTextarea']").fill(ConfigReader.get("image.long.text"));
    }

    /**
     * Clicks "Save & Proceed" to submit the Asset Details form and move to the upload page.
     *
     * After clicking, we wait for the hidden file input element to appear — its presence
     * confirms that page 2 (the image upload page) has loaded and is ready.
     */
    public void clickOnSaveAndProceed() {
        page.locator("//button[normalize-space()='Save & Proceed']")
            .click(new Locator.ClickOptions().setForce(true));

        // Wait for the Upload Thumbnail button — its appearance confirms the upload page has loaded
        page.waitForSelector("//button[normalize-space()='Upload Thumbnail']");
    }

    // ── Page 3: Image Upload ─────────────────────────────────────────────────────

    /**
     * Uploads the image using the hidden <input type="file"> element on page 2.
     *
     * PLAYWRIGHT CONCEPT — setInputFiles() (for hidden file inputs):
     * This is DIFFERENT from waitForFileChooser() above.
     * Here the upload control is a hidden HTML <input type="file"> element in the DOM —
     * not a button that opens an OS dialog. Playwright can interact with hidden inputs directly.
     *
     * Selenium equivalent: fileInput.sendKeys("C:\\path\\to\\file.jpg")
     * Playwright:          locator.setInputFiles(Paths.get("C:\\path\\to\\file.jpg"))
     *
     * KEY INTERVIEW POINT:
     * - Use waitForFileChooser() when a BUTTON click opens a native OS dialog
     * - Use setInputFiles() when there is a hidden <input type="file"> in the HTML
     */
    public void uploadImage() {
        // The "Upload Thumbnail" button opens a native OS file dialog — same pattern as attachFile().
        // We use waitForFileChooser() to intercept the dialog before it opens, then fill the path.
        // This is why setInputFiles() directly on the hidden input was not working — the button
        // does NOT just wrap a hidden input, it opens an actual OS dialog.
        page.locator("//button[normalize-space()='Upload Thumbnail']").scrollIntoViewIfNeeded();

        FileChooser fileChooser = page.waitForFileChooser(() ->
            page.locator("//button[normalize-space()='Upload Thumbnail']").click()
        );
        fileChooser.setFiles(Paths.get(ConfigReader.get("image.upload.file")));

        // Wait for Save & Proceed to confirm the upload was processed by the app
        page.waitForSelector("//button[normalize-space()='Save & Proceed']");
    }

    /**
     * Clicks "Save & Proceed" after the image upload to move to the Publish Setup page.
     *
     * After clicking we do two waits:
     * 1. Wait for the WhatsApp checkbox — confirms the Publish Setup page has loaded.
     * 2. Wait for overlay-bg to disappear — the overlay must be gone before we tick checkboxes.
     */
    public void clickOnSaveAndProceedButton() {
        page.locator("//button[normalize-space()='Save & Proceed']")
            .click(new Locator.ClickOptions().setForce(true));

        // Wait for WhatsApp checkbox to confirm Publish Setup page is loaded
        page.waitForSelector("(//input[@name='wh-platform' and @type='checkbox'])[1]");

        // Wait for overlay to fully clear so it does not block the upcoming checkbox clicks
        page.waitForSelector("//div[@class='overlay-bg']",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));
    }

    // ── Page 4: Publish Setup ────────────────────────────────────────────────────

    /**
     * Ticks the WhatsApp distribution checkbox.
     * Force click used — overlay-bg can still intercept checkbox clicks briefly after page load.
     */
    public void clickOnWhatsappCheckbox() {
        page.locator("(//input[@name='wh-platform' and @type='checkbox'])[1]")
            .click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Opens the "Select Partners" dropdown on the Publish Setup page.
     */
    public void selectPartnersDropdown() {
        page.locator("//span[@class='css-1v99tuv']").click();
    }

    /**
     * Selects the partner option by name (read from config).
     */
    public void selectPartnerOption() {
        String partnerName = ConfigReader.get("image.partner.option");
        page.locator("//div[contains(text(),'" + partnerName + "')]").click();
    }

    /**
     * Closes the partner dropdown by clicking the dropdown toggle again.
     *
     * In Selenium this used Actions.moveToElement().click() — a workaround because a plain click
     * on the toggle while the dropdown was open sometimes scrolled instead of closing.
     * In Playwright a regular click on the same toggle reliably closes it.
     *
     * After closing we wait until the partner option disappears from the DOM — that confirms
     * the dropdown is fully closed before we interact with the toggles below it.
     */
    public void closePartnerOptionDialogBox() {
        // Same pattern as category and hashtag dropdowns — press Escape to close.
        // Clicking the toggle span again was unreliable because the dropdown's close handler
        // expects the full mouse event sequence, not just a click event.
        // Escape closes any open dropdown definitively regardless of its implementation.
        page.keyboard().press("Escape");

        String partnerName = ConfigReader.get("image.partner.option");
        page.waitForSelector("//div[contains(text(),'" + partnerName + "')]",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));
    }

    /**
     * Enables the Cobranding toggle (index 1 among the three custom-switch inputs).
     * Force click used — toggles are often intercepted by their own label element.
     */
    public void clickOnCobrandingToggle() {
        page.locator("(//input[@id='custom-switch'])[1]")
            .click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Enables the Push Notification toggle (index 2).
     */
    public void clickOnPushNotificationToggle() {
        page.locator("(//input[@id='custom-switch'])[2]")
            .click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Enables the Email Notification toggle (index 3).
     */
    public void clickOnEmailNotificationToggle() {
        page.locator("(//input[@id='custom-switch'])[3]")
            .click(new Locator.ClickOptions().setForce(true));
    }

    // ── Publish + Logout ─────────────────────────────────────────────────────────

    /**
     * Clicks the "Publish" button and waits for the Asset Library to reload.
     *
     * After publishing, the app redirects back to the Asset Library page.
     * We wait for the profile dropdown button (#dropdown-basic) to confirm the redirect is complete
     * and the page is fully loaded before trying to interact with it.
     */
    public void clickOnPublishButton() {
        page.locator("//button[normalize-space()='Publish']")
            .click(new Locator.ClickOptions().setForce(true));

        // Wait for the profile dropdown button — confirms we are back on Asset Library
        page.waitForSelector("//button[@id='dropdown-basic']");
    }

    /**
     * Opens the profile/account dropdown after publishing (needed to access Logout).
     * Force click used — the dropdown button can overlap with page content after redirect.
     */
    public void clickOnProfileIconAfterPublishing() {
        page.locator("//button[@id='dropdown-basic']")
            .click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Clicks "Log Out" from the profile dropdown menu.
     */
    public void clickOnLogoutOption() {
        page.locator("//a[normalize-space()='Log Out']").click();
    }

    /**
     * Confirms the logout by clicking the "Logout" button on the confirmation dialog.
     */
    public void clickOnLogoutButton() {
        page.locator("//button[text()='Logout']").click();
    }
}
