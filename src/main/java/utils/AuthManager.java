package utils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.io.File;
import java.nio.file.Paths;


public class AuthManager {
	


	public static void ensureLogin(Browser browser) {

	File file = new File("auth.json");

	// If login already saved, skip
	if (file.exists()) {
	System.out.println("Session exists. Skipping login.");
	return;
	}

	System.out.println("No session found. Performing login...");

	BrowserContext context = browser.newContext();
	Page page = context.newPage();

	page.navigate("https://app.spdevmfp.com/");

	// YOUR LOGIN LOCATORS
	page.fill("#username", "prem.chandra@bizight.com");
	page.fill("#password", "Sbtest@123");
	page.click("xpath=(//button[@type='submit'])[1]");

	// IMPORTANT WAIT
	page.waitForURL("**/home/**");
	page.waitForLoadState(LoadState.NETWORKIDLE);

	context.storageState(
	new BrowserContext.StorageStateOptions()
	.setPath(Paths.get("auth.json")));

    context.close();

    System.out.println("Login completed & saved.");
	
	}
}
