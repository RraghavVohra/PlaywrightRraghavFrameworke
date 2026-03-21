package tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.microsoft.playwright.options.LoadState;

import base.BaseTest;
import pageObjects.PushNotificationPage;

public class PushNotificationTest extends BaseTest {

	    PushNotificationPage pushPage;

	    @BeforeMethod
	    public void initPage() {
	        // page.navigate("https://app.spdevmfp.com/home/");
	        // page.waitForLoadState(LoadState.NETWORKIDLE);
	    	// We removed above code because : 
	    	// `BaseTest.setUp()` already navigates to the home URL and waits for page load. Both test classes repeat this in their own 
	    	// `@BeforeMethod`, which doubles the page load time for every single test.
	    	// - Removed the duplicate `page.navigate()` and `waitForLoadState()` calls from test `@BeforeMethod`s.
	    	// - `BaseTest.setUp()` handles this once — test classes just initialize their page objects.
	        pushPage = new PushNotificationPage(page);
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
	        pushPage.clickCreateAppNotification();
	        Assert.assertTrue(page.url().contains("create"));
	    }
	}

