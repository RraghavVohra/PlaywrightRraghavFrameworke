package tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import base.BaseTest;
import pageObjects.PushNotificationPage;

public class PushNotificationTest extends BaseTest {

	    PushNotificationPage pushPage;

	    @BeforeMethod
	    public void initPage() {
	        page.navigate("https://app.spdevmfp.com/home/");
	        page.waitForLoadState();
	        pushPage = new PushNotificationPage(page);
	        
	        // page.waitForURL("**/home/**");
	        pushPage.openNotificationPage();
	    }

	    @Test(groups="smoke")
	    public void test_TC_PN_01_takenToPushNotificationsScreen() {
	        Assert.assertEquals(pushPage.getHeading(), "PUSH NOTIFICATION");
	    }

	    @Test(groups="smoke")
	    public void test_TC_PN_03_clicksOnActionMenuButton() {
	        pushPage.clickActions();
	        Assert.assertTrue(pushPage.isMenuVisible());
	    }

	    @Test(groups="smoke")
	    public void test_TC_PN_04_createNotification() {
	        pushPage.clickActions();
	        pushPage.clickCreate();
	        Assert.assertTrue(page.url().contains("create"));
	    }
	}

