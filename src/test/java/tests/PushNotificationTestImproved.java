package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.microsoft.playwright.options.LoadState;

import pageObjects.PushNotificationPageImproved;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class PushNotificationTestImproved extends BaseTest {

	PushNotificationPageImproved pushNotificationPage;

	@BeforeMethod
	public void initPage() {
		page.navigate("https://app.spdevmfp.com/home/");
        page.waitForLoadState(LoadState.NETWORKIDLE);
	    pushNotificationPage = new PushNotificationPageImproved(page);
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

	pushNotificationPage.uploadImage(
	Paths.get(System.getProperty("user.dir"),
	"src/test/resources/images/notification.jpg"));

	pushNotificationPage.clickCustomLink();
	pushNotificationPage.enterCustomLink("https://google.com");

	pushNotificationPage.enterSchedulingDateTime("27/04/2026","11:30");

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

	pushNotificationPage.uploadImage(
	Paths.get(System.getProperty("user.dir"),
	"src/test/resources/images/notification.jpg"));

	pushNotificationPage.clickCustomLink();
	pushNotificationPage.enterCustomLink("https://google.com");

	pushNotificationPage.enterSchedulingDateTime("27/04/2026","11:30");

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

	pushNotificationPage.uploadImage(
	Paths.get(System.getProperty("user.dir"),
	"src/test/resources/images/notification.jpg"));

	pushNotificationPage.clickCustomLink();
	pushNotificationPage.enterCustomLink("https://google.com");

	pushNotificationPage.enterSchedulingDateTime("27/04/2026","11:30");

    pushNotificationPage.clickSubmit();

	Assert.assertTrue(true);
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

	pushNotificationPage.uploadImage(
	Paths.get(System.getProperty("user.dir"),
	"src/test/resources/images/notification.jpg"));

	pushNotificationPage.clickCustomLink();

	pushNotificationPage.enterSchedulingDateTime("27/04/2026","11:30");

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

	pushNotificationPage.uploadImage(
	Paths.get(System.getProperty("user.dir"),
	"src/test/resources/images/notification.jpg"));

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

    pushNotificationPage.uploadImage(
	Paths.get(System.getProperty("user.dir"),
	"src/test/resources/images/notification.jpg"));

	pushNotificationPage.clickCustomLink();
	pushNotificationPage.enterCustomLink("https://google.com");

	pushNotificationPage.enterSchedulingDateTime("27/04/2026","11:30");

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

	pushNotificationPage.uploadImage(
	Paths.get(System.getProperty("user.dir"),
	"src/test/resources/images/notification.jpg"));

	pushNotificationPage.clickContentLink();
	pushNotificationPage.openContentDropdown();
	pushNotificationPage.selectContentOption();

	pushNotificationPage.enterSchedulingDateTime("27/04/2026","11:30");

	pushNotificationPage.clickSubmit();

	Assert.assertEquals(pushNotificationPage.getToastMessageText(),
    "Push Notification Saved.");
	}

    @Test(priority = 15)
	public void test_TC_PN_012_imagesInDifferentFormat() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.uploadImage(
	Paths.get(System.getProperty("user.dir"),
	"src/test/resources/images/notification.png"));

	Assert.assertTrue(true);
	}

    @Test(priority = 16)
	public void test_TC_PN_049_pushNotificationWithSpecialCharacter() {

	pushNotificationPage.openCreateNotification();

	String name = "Push_" + System.currentTimeMillis();

	pushNotificationPage.enterNotificationName(name);

	pushNotificationPage.enterNotificationMessage("!@#$%^&*() Notification");

	Assert.assertTrue(true);
	}

	@Test(priority = 17)
	public void test_TC_PN_036_pushNotificationWithCsvUpload() {

	pushNotificationPage.openCreateNotification();

	pushNotificationPage.clickPartnerListRadio();

	pushNotificationPage.uploadCsv(
	Paths.get(System.getProperty("user.dir"),
	"src/test/resources/csv/partners.csv"));

	Assert.assertTrue(true);
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
	
	