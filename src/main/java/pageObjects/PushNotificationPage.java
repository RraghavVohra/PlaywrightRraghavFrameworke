package pageObjects;


import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

public class PushNotificationPage {
	
	    private Page page;

	    public PushNotificationPage(Page page) {
	        this.page = page;
	    }

	    // LOCATORS
	    private Locator communicationTab() {
	        return page.locator("xpath=//span[normalize-space()='Communication']");
	    }

	    private Locator notifications() {
	        return page.locator("xpath=//a[normalize-space()='New Push Notification']");
	    }

	    private Locator pageHeading() {
	        return page.locator("xpath=//span[@class='fs-2 fw-bolder']");
	    }

	    private Locator actionsButton() {
	        return page.locator("xpath=(//*[name()='svg'])[1]");
	    }

	    private Locator createAppNotification() {
	        return page.locator("xpath=//a/span[text()='Create App Notification'][1]");
	    }

	    private Locator notificationNameField() {
	        return page.locator("#pushnotify_name");
	    }

	    private Locator notificationMessageField() {
	        return page.locator("#pushnotify_msg");
	    }

	    private Locator categoryDropdown() {
	        return page.locator("#btn_ptr_category");
	    }

	    private Locator imageUpload() {
	        return page.locator("xpath=//input[@name='image_url']");
	    }

	    private Locator submitButton() {
	        return page.locator("xpath=//button[@type='submit']");
	    }

	    private Locator toastMessage() {
	        return page.locator("xpath=//span[@class='mssg_content']");
	    }

	    // ACTION METHODS
	    
	    public void openNotificationPage() {

	        // ✅ Wait until page is stable after login reuse
	        // page.waitForLoadState(LoadState.NETWORKIDLE);

	        // ✅ Wait for communication tab to appear first
	        communicationTab().waitFor(new Locator.WaitForOptions()
	                .setState(WaitForSelectorState.VISIBLE));

	        communicationTab().click();
	        
	        notifications().waitFor(new Locator.WaitForOptions()
	        .setState(WaitForSelectorState.VISIBLE));
	        
	        notifications().click();
	    }

	    // public void openNotificationPage() {
	    //     communicationTab().click();
	    //     notifications().click();
	    // }

	    public String getHeading() {
	        return pageHeading().textContent();
	    }

	   // public void clickActions() {
	   //     actionsButton().click();
	   // }

	    public void clickCreateAppNotification() {
	        createAppNotification().click();
	    }

	    public void enterNotificationName(String name) {
	        notificationNameField().fill(name);
	    }

	    public void enterNotificationMessage(String msg) {
	        notificationMessageField().fill(msg);
	    }

	    //public void uploadImage(String filepath) {
	    //    imageUpload().setInputFiles(Paths.get(filePath));
	    //}

	    public void clickSubmit() {
	        submitButton().click();
	    }

	    public String getToastMessage() {
	        return toastMessage().textContent();
	    }
	    
	    public void clickActions() {
	    	page.locator("(//*[name()='svg'])[1]").click();
	    }
	    
	    public boolean isMenuVisible() {
	    	return page.locator("//a[@class='menu-link px-3']").first().isVisible();
	    	
	    }
	    
	    public void clickCreate() {
	    	page.locator("text=Create App Notification").click();
	    }
	}	


