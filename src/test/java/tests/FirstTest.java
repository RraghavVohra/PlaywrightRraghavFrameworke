package tests;

import com.microsoft.playwright.*;

public class FirstTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try(Playwright playwright = Playwright.create()) {
			
			Browser browser = playwright.chromium().launch(
					new BrowserType.LaunchOptions().setHeadless(false));
			
			Page page = browser.newPage();
			page.navigate("https://www.google.com");
			
			System.out.println("Title is: " + page.title());
			
			browser.close();
		}
	}

}
