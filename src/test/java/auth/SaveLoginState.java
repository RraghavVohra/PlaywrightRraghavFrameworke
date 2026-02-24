package auth;

import com.microsoft.playwright.*;
import java.nio.file.Paths;

public class SaveLoginState {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try(Playwright playwright = Playwright.create()) {
			
			Browser browser = playwright.chromium()
					.launch(new BrowserType.LaunchOptions().setHeadless(false));
			
			BrowserContext context = browser.newContext();
			Page page = context.newPage();
			
			// Open Login Page
			page.navigate("https://app.spdevmfp.com");
			// Enter Username
			page.fill("#username", "prem.chandra@bizight.com");
			// Enter Password
			page.fill("#password", "Sbtest@123");
			// Click Login
			page.click("xpath=(//button[@type='submit'])[1]");
			// Wait for Asset Library page to load
			// ** means any domain before this path.
			page.waitForURL("**/home/AssetLibrary");
			
			// SAVE SESSION STATE
			context.storageState(
					new BrowserContext.StorageStateOptions()
					    .setPath(Paths.get("auth.json")));
			
			System.out.println(" LOGIN STATE SAVED");
			
			browser.close();
			
		}
	}

}
