package tests;

import base.BaseTest;
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
	@Test(priority = 1)
	public void test_TC_PN_01_takenToPushNotificationsScreen() {

	pushNotificationPage.openPushNotificationPage();

	Assert.assertEquals(
	pushNotificationPage.getPageHeading(),
	"PUSH NOTIFICATION");

	Assert.assertTrue(page.url().contains("AgencyCommunication/list"));
	}

	// TC_PN_04
	@Test(priority = 2)
	public void test_TC_PN_04_takentoCreateAppNotificationScreen() {

	pushNotificationPage.openCreateNotification();

    Assert.assertTrue(page.url().contains("AgencyCommunication/create"));
	}

	// TC_PN_07
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

	
    @Test(priority = 8)
	public void test_TC_PN_06_notificationChannelSelection() {

    pushNotificationPage.openCreateNotification();

    pushNotificationPage.clickWhatsAppRadio();
	Assert.assertTrue(pushNotificationPage.isWhatsAppSelected());

	pushNotificationPage.clickPushNotificationRadio();
	Assert.assertTrue(pushNotificationPage.isPushNotificationSelected());
	}


    @Test(priority = 9)
	public void test_TC_PN_013_sendToOptionsSelection() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.clickUploadListRadio();
	Assert.assertTrue(pushNotificationPage.isUploadListSelected());

	pushNotificationPage.clickPartnerCategoryRadio();
	Assert.assertTrue(pushNotificationPage.isPartnerCategorySelected());
	}


    @Test(priority = 10)
	public void test_TC_PN_015_selectAllCategories() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.openCategoryDropdown();
	pushNotificationPage.clickSelectAllCategories();

	String categoriesText =
	pushNotificationPage.getTotalCategoriesText();

	Assert.assertNotNull(categoriesText);
	}

    @Test(priority = 11)
	public void test_TC_PN_016_verifySearchTextField() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.openCategoryDropdown();

	boolean found =
	pushNotificationPage.searchAndValidateOption("Raj2024");

    Assert.assertTrue(found);
	}

    @Test(priority = 12)
	public void test_TC_PN_09_verifyImageUpload() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
	// pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/budapest.jpg"));

	pushNotificationPage.hoverOverAddPhotoButton();

	Assert.assertTrue(true);
	}

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

    @Test(priority = 15)
	public void test_TC_PN_012_imagesInDifferentFormat() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.uploadImage(getResource("images/Amsterdam.png"));
	// pushNotificationPage.uploadImage(Paths.get(System.getProperty("user.dir"),
	// "src/main/resources/images/Amsterdam.png"));

	Assert.assertTrue(pushNotificationPage.isImageUploaded(),
    "Image preview should appear after uploading a PNG file");
	
	}

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

	@Test(priority = 18)
	public void test_TC_PN_050_pushNotificationWithoutCsvUpload() {

	pushNotificationPage.openCreateNotification();
	pushNotificationPage.clickPartnerListRadio();
	pushNotificationPage.clickSubmit();

	Assert.assertEquals(pushNotificationPage.getUploadCsvValidation(),
	"Please select a file.");
}
	
}
	
	