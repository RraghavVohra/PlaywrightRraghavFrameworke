package tests;

import base.BaseTest;
import org.testng.annotations.Test;
import pageObjects.ImageCreationPagePW;
import utils.ConfigReader;

/**
 * Test class for Image Creation via the Asset Library.
 *
 * FLOW:
 *   Asset Library → Add New Asset → Social Post card
 *   → Page 1: Attach file, click Next
 *   → Page 2: Asset Details (Name, Category, Hashtag, Long Text), Save & Proceed
 *   → Page 3: Upload image, Save & Proceed
 *   → Page 4: Publish Setup (WhatsApp, Partners, Toggles), Publish
 *   → Logout
 *
 * WHY THIS CLASS EXTENDS BaseTest (not a custom base):
 * The Image Creation flow starts from the Asset Library page — the same page that
 * BaseTest's @BeforeMethod lands on after restoring auth.json and navigating to /home.
 * No extra base class is needed (unlike SocialAutoPostBaseTest which added a date helper).
 * One level of inheritance is always preferred over two when there is nothing extra to add.
 *
 * AUTH:
 * Login is NOT done in this test. @BeforeSuite in BaseTest runs AuthManager.ensureLogin()
 * once for the whole suite, saves the session to auth.json, and @BeforeMethod restores it
 * via setStorageStatePath — so every test starts already logged in.
 */
public class ImageCreationPlaywrightTest extends BaseTest {

    // Declared at class level so all test methods in this class can share the same page object.
    // In this case there is only one test method, but this is the standard POM pattern.
    ImageCreationPagePW imageCreationPage;

    /**
     * TC_IC_01 — Full image creation flow: attach → fill form → upload → publish → logout.
     *
     * Each step is called on a separate line with a comment — this makes it easy to read
     * and easy to pinpoint which step failed when the test reports an error.
     */
    @Test(priority = 1)
    public void TC_IC_01_imageCreationThroughAssetLibrary() {

        // Initialise the page object, injecting the Page instance that BaseTest created.
        // This is the Dependency Injection pattern — ImageCreationPagePW does not create
        // its own Page; it receives it from outside (from BaseTest via the constructor).
        imageCreationPage = new ImageCreationPagePW(page);

        // ── Step 1-2: Open the asset creation wizard ──────────────────────────────
        // @BeforeMethod navigated to base.url/home — the Asset Library page.
        // We click "Add New Asset", then select the "Social Post" card from the dialog.
        // The card index is env-specific (driven from config) — no code change needed to switch envs.
        imageCreationPage.clickOnAddNewAssetButton();
        imageCreationPage.clickOnSocialPostButton();

        // ── Step 3: Wait for the Social Post form to appear ───────────────────────
        // Playwright auto-waits on every action, but an explicit waitForSelector here
        // documents the intent: "we are waiting for this specific page to load before continuing".
        imageCreationPage.waitForSocialPostPageToLoad();

        // ── Step 4: Attach the social post preview image ──────────────────────────
        // The Attach button opens a NATIVE OS file dialog (not an HTML element).
        // Playwright intercepts it with waitForFileChooser() — no Robot class needed.
        // See ImageCreationPagePW.attachFile() for the full explanation.
        imageCreationPage.attachFile();

        // ── Step 5: Click Next to go to Asset Details page ────────────────────────
        // Force click used — an overlay from the file attach step may still be present.
        // After clicking, the method waits for the Name field to confirm the page is ready.
        imageCreationPage.clickOnNextButton();

        // ── Step 6-14: Fill the Asset Details form ────────────────────────────────
        // Name field
        imageCreationPage.enterTextIntoNameTextField(ConfigReader.get("image.name.text"));

        // Category dropdown — open, select env-specific option, close by clicking label
        imageCreationPage.clickOnCategoryField();
        imageCreationPage.clickOnCategoryOption();
        imageCreationPage.clickOnCategoriesStaticText();

        // Hashtag dropdown — open, scroll to env-specific option, click, close by clicking label
        imageCreationPage.clickOnHashtagField();
        imageCreationPage.clickOnHashtag();
        imageCreationPage.clickOnHashtagStaticText();

        // Scroll down to reveal the long text area and fill it
        imageCreationPage.scrollToPageBottom();
        imageCreationPage.fillLongTextField();

        // ── Step 15: Save & Proceed → goes to image upload page ──────────────────
        imageCreationPage.clickOnSaveAndProceed();

        // ── Step 16: Upload the actual asset image ────────────────────────────────
        // This uses a hidden <input type="file"> — Playwright fills it with setInputFiles().
        // Different from attachFile() above which handled a native OS dialog.
        imageCreationPage.uploadImage();

        // Scroll all the way down to make the Save & Proceed button visible
        page.evaluate("window.scrollBy(0, 20000)");

        // ── Step 17: Save & Proceed → goes to Publish Setup page ─────────────────
        imageCreationPage.clickOnSaveAndProceedButton();

        // ── Step 18-23: Fill the Publish Setup form ───────────────────────────────
        // Tick the WhatsApp distribution checkbox
        imageCreationPage.clickOnWhatsappCheckbox();

        // Scroll down so the Partners dropdown is in view, then select a partner
        page.evaluate("window.scrollBy(0, 400)");
        imageCreationPage.selectPartnersDropdown();
        imageCreationPage.selectPartnerOption();

        // Close the dropdown, then enable the three distribution toggles
        imageCreationPage.closePartnerOptionDialogBox();
        imageCreationPage.clickOnCobrandingToggle();
        imageCreationPage.clickOnPushNotificationToggle();
        imageCreationPage.clickOnEmailNotificationToggle();

        // ── Step 24: Publish ──────────────────────────────────────────────────────
        // After publishing, the app redirects back to the Asset Library page.
        // clickOnPublishButton() waits for the profile dropdown to confirm the redirect is done.
        imageCreationPage.clickOnPublishButton();

        // ── Step 25-26: Logout ────────────────────────────────────────────────────
        // Open profile dropdown → click Log Out → confirm on the logout dialog
        imageCreationPage.clickOnProfileIconAfterPublishing();
        imageCreationPage.clickOnLogoutOption();
        imageCreationPage.clickOnLogoutButton();
    }
}
