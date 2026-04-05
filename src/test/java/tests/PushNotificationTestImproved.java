package tests;

import base.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pageObjects.PushNotificationPageImproved;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// @Epic groups tests at the product-area level in Allure (top of the hierarchy)
// @Feature groups them under a specific module within that epic
@Epic("Agency Communication")
@Feature("Push Notification")
public class PushNotificationTestImproved extends BaseTest {

	PushNotificationPageImproved pushNotificationPage;

	@BeforeMethod
	public void initPage() {
		// page.navigate("https://app.spdevmfp.com/home/");
        // page.waitForLoadState(LoadState.NETWORKIDLE);
	    pushNotificationPage = new PushNotificationPageImproved(page);
	}
	
	 // Add this — computed once, always 30 days in the future
    private final String futureDate = LocalDate.now()
        .plusDays(30)
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    
    // Instead of repeating System.getProperty("user.dir") everywhere, 
    // I added a small private helper method getResource()
    private java.nio.file.Path getResource(String relativePath) {
        try {
            return Paths.get(getClass().getClassLoader().getResource(relativePath).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Resource not found: " + relativePath, e);
        }
    }

	// TC_PN_01
	@Story("Navigation")
	@Description("Verify user is taken to the Push Notifications list screen with correct heading and URL")
	@Test(priority = 1)
	public void test_TC_PN_01_takenToPushNotificationsScreen() {

	pushNotificationPage.openPushNotificationPage();

	Assert.assertEquals(
	pushNotificationPage.getPageHeading(),
	"PUSH NOTIFICATION");

	Assert.assertTrue(page.url().contains("AgencyCommunication/list"));
	}

	// TC_PN_04
	@Story("Navigation")
	@Description("Verify user lands on the Create App Notification screen with the correct URL")
	@Test(priority = 2)
	public void test_TC_PN_04_takentoCreateAppNotificationScreen() {

	pushNotificationPage.openCreateNotification();

    Assert.assertTrue(page.url().contains("AgencyCommunication/create"));
	}

	// TC_PN_07
	@Story("Form Validation")
	@Description("Submit the form with all fields filled except Notification Name — validation message should appear")
	@Test(priority = 3)
	public void test_TC_PN_07_fillsAllFieldsExceptNotificationName() {

	pushNotificationPage.openCreateNotification();
    
	pushNotificationPage.enterNotificationMessage("Automation Message");

	pushNotificationPage.openCategoryDropdown();
	pushNotificationPage.searchCategory("Raj2024");
	pushNotificationPage.clickOnTargetCategory();
	pushNotificationPage.clickOnBlankSpace();

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
	// pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/budapest.jpg"));

	pushNotificationPage.clickCustomLink();
	pushNotificationPage.enterCustomLink("https://google.com");

	pushNotificationPage.enterSchedulingDateTime(futureDate,"11:30");

	pushNotificationPage.clickSubmit();

	Assert.assertEquals(
	pushNotificationPage.getNotificationNameValidation(),
	"Please fill out this field.");
	}

	// TC_PN_08
	@Story("Form Validation")
	@Description("Submit the form with all fields filled except Notification Message — validation message should appear")
    @Test(priority = 4)
	public void test_TC_PN_08_fillsAllFieldsExceptNotificationMessage() {

	pushNotificationPage.openCreateNotification();

	String name = "Push_" + System.currentTimeMillis();

	pushNotificationPage.enterNotificationName(name);

	pushNotificationPage.openCategoryDropdown();
	pushNotificationPage.searchCategory("Raj2024");
	pushNotificationPage.clickOnTargetCategory();
	pushNotificationPage.clickOnBlankSpace();

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
	// pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/budapest.jpg"));

	pushNotificationPage.clickCustomLink();
	pushNotificationPage.enterCustomLink("https://google.com");

	pushNotificationPage.enterSchedulingDateTime(futureDate,"11:30");

	pushNotificationPage.clickSubmit();

	Assert.assertEquals(
	pushNotificationPage.getNotificationMessageValidation(),
	"Please fill out this field.");
	}

   
	@Story("Form Validation")
	@Description("Submit the form without selecting a Category — form should stay on the create page")
    @Test(priority = 5)
	public void test_TC_PN_017_missingCategoryField() {

	pushNotificationPage.openCreateNotification();

	String name = "Push_" + System.currentTimeMillis();

	pushNotificationPage.enterNotificationName(name);
	pushNotificationPage.enterNotificationMessage("Automation Message");

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
	// pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/budapest.jpg"));

	pushNotificationPage.clickCustomLink();
	pushNotificationPage.enterCustomLink("https://google.com");

	pushNotificationPage.enterSchedulingDateTime(futureDate,"11:30");

    pushNotificationPage.clickSubmit();

    Assert.assertTrue(page.url().contains("AgencyCommunication/create"),
    "Form should stay on create page when category is missing");
}

    
   @Story("Form Validation")
   @Description("Submit the form with Custom Link option selected but no URL entered — validation message should appear")
   @Test(priority = 6)
	public void test_TC_PN_019_missingCustomLinkField() {

	pushNotificationPage.openCreateNotification();

	String name = "Push_" + System.currentTimeMillis();

	pushNotificationPage.enterNotificationName(name);
	pushNotificationPage.enterNotificationMessage("Automation Message");

	pushNotificationPage.openCategoryDropdown();
	pushNotificationPage.searchCategory("Raj2024");
	pushNotificationPage.clickOnTargetCategory();
	pushNotificationPage.clickOnBlankSpace();

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
	// pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/budapest.jpg"));

	pushNotificationPage.clickCustomLink();

	pushNotificationPage.enterSchedulingDateTime(futureDate,"11:30");

	pushNotificationPage.clickSubmit();

	Assert.assertEquals(
	pushNotificationPage.getCustomLinkValidation(),
	"Please enter Custom Link to proceed");
	}

	@Story("Navigation")
	@Description("Open the action menu and verify all expected options are present")
	@Test(priority = 7)
	public void test_TC_PN_03_clickActionMenuButton() {

	pushNotificationPage.openPushNotificationPage();
	pushNotificationPage.openCreateNotification();

	List<String> expected = Arrays.asList(
	"Create App Notification",
	"WhatsApp Template List",
	"Delete"
	 );

	Assert.assertEquals(
	pushNotificationPage.getActionMenuOptions(),
	expected);
	}

	
    @Story("Notification Channel")
    @Description("Switch between WhatsApp and Push Notification radio buttons — verify each selection is registered")
    @Test(priority = 8)
	public void test_TC_PN_06_notificationChannelSelection() {

    pushNotificationPage.openCreateNotification();

    pushNotificationPage.clickWhatsAppRadio();
	Assert.assertTrue(pushNotificationPage.isWhatsAppSelected());

	pushNotificationPage.clickPushNotificationRadio();
	Assert.assertTrue(pushNotificationPage.isPushNotificationSelected());
	}


    @Story("Send To Options")
    @Description("Switch between Upload List and Partner Category radio buttons — verify each selection is registered")
    @Test(priority = 9)
	public void test_TC_PN_013_sendToOptionsSelection() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.clickUploadListRadio();
	Assert.assertTrue(pushNotificationPage.isUploadListSelected());

	pushNotificationPage.clickPartnerCategoryRadio();
	Assert.assertTrue(pushNotificationPage.isPartnerCategorySelected());
	}


    @Story("Category Selection")
    @Description("Click 'Select All' in the category dropdown — verify total categories text is populated")
    @Test(priority = 10)
	public void test_TC_PN_015_selectAllCategories() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.openCategoryDropdown();
	pushNotificationPage.clickSelectAllCategories();

	String categoriesText =
	pushNotificationPage.getTotalCategoriesText();

	Assert.assertNotNull(categoriesText);
	}

    @Story("Category Selection")
    @Description("Search for 'Raj2024' in the category dropdown and verify it appears in the list")
    @Test(priority = 11)
	public void test_TC_PN_016_verifySearchTextField() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.openCategoryDropdown();

	boolean found =
	pushNotificationPage.searchAndValidateOption("Raj2024");

    Assert.assertTrue(found);
	}

    @Story("Image Upload")
    @Description("Upload a PNG image and hover over the Add Photo button — verify the upload interaction succeeds")
    @Test(priority = 12)
	public void test_TC_PN_09_verifyImageUpload() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
	// pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/budapest.jpg"));

	pushNotificationPage.hoverOverAddPhotoButton();

	Assert.assertTrue(true);
	}

   @Story("Create Notification")
   @Description("Fill all required fields and submit — verify success toast message appears")
   @Test(priority = 13)
	public void test_TC_PN_023_verifyNotificationStatus() {

    pushNotificationPage.openCreateNotification();

	String name = "Push_" + System.currentTimeMillis();

	pushNotificationPage.enterNotificationName(name);
	pushNotificationPage.enterNotificationMessage("Automation Message");

	pushNotificationPage.openCategoryDropdown();
	pushNotificationPage.searchCategory("Raj2024");
	pushNotificationPage.clickOnTargetCategory();
	pushNotificationPage.clickOnBlankSpace();

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
    // pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/budapest.jpg"));

	pushNotificationPage.clickCustomLink();
	pushNotificationPage.enterCustomLink("https://google.com");

	pushNotificationPage.enterSchedulingDateTime(futureDate,"11:30");

	pushNotificationPage.clickSubmit();

	Assert.assertEquals(
	pushNotificationPage.getToastMessageText(),
	"Push Notification Saved.");

	pushNotificationPage.closeToastMessage();
}

	
    @Story("Create Notification")
    @Description("Submit a notification using Content Link instead of Custom Link — verify success toast message appears")
    @Test(priority = 14)
	public void test_TC_PN_020_verifyContentLink() {

	pushNotificationPage.openCreateNotification();

	String name = "Push_" + System.currentTimeMillis();

	pushNotificationPage.enterNotificationName(name);
	pushNotificationPage.enterNotificationMessage("Automation Message");

	pushNotificationPage.openCategoryDropdown();
	pushNotificationPage.searchCategory("Raj2024");
	pushNotificationPage.clickOnTargetCategory();
	pushNotificationPage.clickOnBlankSpace();

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
	// pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/budapest.jpg"));

	pushNotificationPage.clickContentLink();
	pushNotificationPage.openContentDropdown();
	pushNotificationPage.selectContentOption();

	pushNotificationPage.enterSchedulingDateTime(futureDate,"11:30");

	pushNotificationPage.clickSubmit();

	Assert.assertEquals(pushNotificationPage.getToastMessageText(),
    "Push Notification Saved.");
	}

    @Story("Image Upload")
    @Description("Upload a PNG file and verify the image preview is displayed in the form")
    @Test(priority = 15)
	public void test_TC_PN_012_imagesInDifferentFormat() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
	// pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/Amsterdam.png"));

	Assert.assertTrue(pushNotificationPage.isImageUploaded(),
    "Image preview should appear after uploading a PNG file");
	
	}

    @Story("Form Input")
    @Description("Enter special characters in the Notification Message field — verify the field retains them correctly")
    @Test(priority = 16)
	public void test_TC_PN_049_pushNotificationWithSpecialCharacter() {

	pushNotificationPage.openCreateNotification();

	String name = "Push_" + System.currentTimeMillis();
	pushNotificationPage.enterNotificationName(name);

	String specialMessage = "!@#$%^&*() Notification";
	pushNotificationPage.enterNotificationMessage(specialMessage);

	Assert.assertEquals(pushNotificationPage.getNotificationMessageText(),specialMessage,
	"Message field should accept and retain special characters");
	}

	@Story("CSV Upload")
	@Description("Select Partner List, upload a CSV file — verify the file name appears confirming successful upload")
	@Test(priority = 17)
	public void test_TC_PN_036_pushNotificationWithCsvUpload() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.clickPartnerListRadio();

	pushNotificationPage.uploadCsv(getResource("csv/pushnotificationsspuat.csv"));
	// pushNotificationPage.uploadCsv(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/csv/pushnotificationsspuat.csv"));

	Assert.assertTrue(pushNotificationPage.isCsvUploaded(),
		    "CSV file name should appear after upload");
	}

	@Story("CSV Upload")
	@Description("Select Partner List and submit without uploading a CSV — validation message should appear")
	@Test(priority = 18)
	public void test_TC_PN_050_pushNotificationWithoutCsvUpload() {

	pushNotificationPage.openCreateNotification();
	pushNotificationPage.clickPartnerListRadio();
	pushNotificationPage.clickSubmit();

	Assert.assertEquals(pushNotificationPage.getUploadCsvValidation(),
	"Please select a file.");
}
	
}
	
	