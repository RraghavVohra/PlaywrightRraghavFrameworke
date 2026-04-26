package tests;

import base.SocialAutoPostBaseTest;
import pageObjects.SocialAutoPostPagePW;
import utils.ConfigReader;

import org.testng.annotations.Test;

public class SocialAutoPostPlaywrightTest extends SocialAutoPostBaseTest {

    SocialAutoPostPagePW socialPage;

    // Common navigation to Social Auto Post page (called at the start of every test).
    // Navigation path differs by environment:
    //   dev/preprod : Communication tab → Social Auto Post
    //   prod        : Automation tab → Social → Auto Post
    private void navigateToSocialAutoPost() {
        socialPage = new SocialAutoPostPagePW(page);
        if (ConfigReader.get("env").equals("prod")) {
            socialPage.clickOnAutomationTab();
            socialPage.clickOnSocialOption();
            socialPage.clickOnAutoPostTab();
        } else {
            socialPage.clickOnCommunicationTabPreprod();
            socialPage.clickOnSocialAutoPostOptionPreprod();
        }
        socialPage.clickOnActionsButton();
        socialPage.clickOnCreatePostButton();
    }

    @Test(priority = 1)
    public void test_TC_SAP_01_whenPostedWithPngImageWithCobranding() {
        navigateToSocialAutoPost();

        socialPage.uploadFileInPNG(ConfigReader.get("social.image.png"));
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnEnableCobrandingButton();
        socialPage.enterInTitleTextfield(ConfigReader.get("social.title") + "_" + System.currentTimeMillis());
        socialPage.enterValueInDescriptionTextfield(ConfigReader.get("social.description"));
        socialPage.scrollDownByFiveHundred();
        socialPage.clickOnPartnerCategoryButton();
        socialPage.clickOnSelectPartnerCategory();
        socialPage.clickOnStaticText();
        socialPage.clickOnFacebook();
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnOpenDateTimePicker();
        // +1 day from today — always picks tomorrow, keeps test simple and future-safe
        String[] date01 = getScheduleDate(1);
        socialPage.selectFutureDate(date01[0], date01[1]);
        socialPage.selectTime("10", "30");
        socialPage.verifySelection();
        socialPage.clickOnSchedulePostButton();

        System.out.println("Test Case TC_SAP_01 is passed!");
    }

    @Test(priority = 2)
    public void test_TC_SAP_02_whenPostedWithJpgImageWithCobranding() {
        navigateToSocialAutoPost();

        socialPage.uploadFileInJPG(ConfigReader.get("social.image.jpg"));
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnEnableCobrandingButton();
        socialPage.enterInTitleTextfield(ConfigReader.get("social.title") + "_" + System.currentTimeMillis());
        socialPage.enterValueInDescriptionTextfield(ConfigReader.get("social.description"));
        socialPage.scrollDownByFiveHundred();
        socialPage.clickOnPartnerCategoryButton();
        socialPage.clickOnSelectPartnerCategory();
        socialPage.clickOnStaticText();
        socialPage.clickOnFacebook();
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnOpenDateTimePicker();
        // +1 day from today — always picks tomorrow, keeps test simple and future-safe
        String[] date02 = getScheduleDate(1);
        socialPage.selectFutureDate(date02[0], date02[1]);
        socialPage.selectTime("10", "30");
        socialPage.verifySelection();
        socialPage.clickOnSchedulePostButton();

        System.out.println("Test Case TC_SAP_02 is passed!");
    }

    @Test(priority = 3)
    public void test_TC_SAP_03_whenPostedWithVideo() {
        navigateToSocialAutoPost();

        socialPage.uploadFileInMP4(ConfigReader.get("social.video"));
        socialPage.uploadThumbnailInJPG(ConfigReader.get("social.thumbnail.jpg"));
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnEnableCobrandingButton();
        socialPage.enterInTitleTextfield(ConfigReader.get("social.title") + "_" + System.currentTimeMillis());
        socialPage.enterValueInDescriptionTextfield(ConfigReader.get("social.description"));
        socialPage.scrollDownByFiveHundred();
        socialPage.clickOnPartnerCategoryButton();
        socialPage.clickOnSelectPartnerCategory();
        socialPage.clickOnStaticText();
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnOpenDateTimePicker();
        // +1 day from today — always picks tomorrow, keeps test simple and future-safe
        String[] date03 = getScheduleDate(1);
        socialPage.selectFutureDate(date03[0], date03[1]);
        socialPage.selectTime("10", "30");
        socialPage.verifySelection();
        socialPage.clickOnSchedulePostButton();

        System.out.println("Test Case TC_SAP_03 is passed!");
    }

    @Test(priority = 4)
    public void test_TC_SAP_04_whenPostedWithSpecialCharacterInTitleAndDescription() {
        navigateToSocialAutoPost();

        socialPage.uploadFileInJPG(ConfigReader.get("social.image.jpg"));
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnEnableCobrandingButton();
        socialPage.enterInTitleTextfield(ConfigReader.get("social.title") + "_" + System.currentTimeMillis());
        socialPage.enterValueInDescriptionTextfield(ConfigReader.get("social.description"));
        socialPage.scrollDownByFiveHundred();
        socialPage.clickOnPartnerCategoryButton();
        socialPage.clickOnSelectPartnerCategory();
        socialPage.clickOnStaticText();
        socialPage.clickOnFacebook();
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnOpenDateTimePicker();
        // +1 day from today — always picks tomorrow, keeps test simple and future-safe
        String[] date04 = getScheduleDate(1);
        socialPage.selectFutureDate(date04[0], date04[1]);
        socialPage.selectTime("10", "30");
        socialPage.verifySelection();
        socialPage.clickOnSchedulePostButton();

        System.out.println("Test Case TC_SAP_04 is passed!");
    }

    @Test(priority = 5)
    public void test_TC_SAP_05_whenPostedOnAllSocialMediaWithCustomUrl() {
        navigateToSocialAutoPost();

        socialPage.uploadFileInJPG(ConfigReader.get("social.image.jpg"));
        socialPage.scrollDownByTwoHundred();
        socialPage.enterInTitleTextfield(ConfigReader.get("social.title") + "_" + System.currentTimeMillis());
        socialPage.enterValueInDescriptionTextfield(ConfigReader.get("social.description"));
        socialPage.scrollDownByFiveHundred();
        socialPage.clickOnPartnerCategoryButton();
        socialPage.clickOnSelectPartnerCategory();
        socialPage.clickOnStaticText();
        socialPage.clickOnCustomURLRadioButton();
        socialPage.enterCustomURL();
        socialPage.clickOnTwitter();
        socialPage.clickOnLinkedIn();
        socialPage.clickOnFacebook();
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnOpenDateTimePicker();
        // +1 day from today — always picks tomorrow, keeps test simple and future-safe
        String[] date05 = getScheduleDate(1);
        socialPage.selectFutureDate(date05[0], date05[1]);
        socialPage.selectTime("10", "30");
        socialPage.verifySelection();
        socialPage.clickOnSchedulePostButton();

        System.out.println("Test Case TC_SAP_05 is passed!");
    }

    @Test(priority = 6)
    public void test_TC_SAP_06_whenPostedOnAllSocialMediaWithNoneOption() {
        navigateToSocialAutoPost();

        socialPage.uploadFileInJPG(ConfigReader.get("social.image.jpg"));
        socialPage.scrollDownByTwoHundred();
        socialPage.enterInTitleTextfield(ConfigReader.get("social.title") + "_" + System.currentTimeMillis());
        socialPage.enterValueInDescriptionTextfield(ConfigReader.get("social.description"));
        socialPage.scrollDownByFiveHundred();
        socialPage.clickOnPartnerCategoryButton();
        socialPage.clickOnSelectPartnerCategory();
        socialPage.clickOnStaticText();
        socialPage.clickOnNoneRadioButton();
        socialPage.clickOnTwitter();
        socialPage.clickOnLinkedIn();
        socialPage.clickOnFacebook();
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnOpenDateTimePicker();
        // +1 day from today — always picks tomorrow, keeps test simple and future-safe
        String[] date06 = getScheduleDate(1);
        socialPage.selectFutureDate(date06[0], date06[1]);
        socialPage.selectTime("10", "30");
        socialPage.verifySelection();
        socialPage.clickOnSchedulePostButton();

        System.out.println("Test Case TC_SAP_06 is passed!");
    }

    @Test(priority = 7)
    public void test_TC_SAP_07_whenPostedOnAllSocialMediaWithMicrositeURL() {
        navigateToSocialAutoPost();

        socialPage.uploadFileInJPG(ConfigReader.get("social.image.jpg"));
        socialPage.scrollDownByTwoHundred();
        socialPage.enterInTitleTextfield(ConfigReader.get("social.title") + "_" + System.currentTimeMillis());
        socialPage.enterValueInDescriptionTextfield(ConfigReader.get("social.description"));
        socialPage.scrollDownByFiveHundred();
        socialPage.clickOnPartnerCategoryButton();
        socialPage.clickOnSelectPartnerCategory();
        socialPage.clickOnStaticText();
        socialPage.clickOnTwitter();
        socialPage.clickOnLinkedIn();
        socialPage.clickOnFacebook();
        socialPage.scrollDownByTwoHundred();
        socialPage.clickOnOpenDateTimePicker();
        // +1 day from today — always picks tomorrow, keeps test simple and future-safe
        String[] date07 = getScheduleDate(1);
        socialPage.selectFutureDate(date07[0], date07[1]);
        socialPage.selectTime("17", "30");
        socialPage.verifySelection();
        socialPage.clickOnSchedulePostButton();

        System.out.println("Test Case TC_SAP_07 is passed!");
    }
}
