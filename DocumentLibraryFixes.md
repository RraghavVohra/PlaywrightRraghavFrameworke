# Document Library — Issues & Fix Checklist

---

## CRITICAL (Won't compile or run at all)

### 1. Missing Constructor in DocumentLibraryPage.java
**What:** The class has `private Page page` but no constructor. The test calls `new DocumentLibraryPage(page)` which won't compile.
**What to do:** Add this constructor inside `DocumentLibraryPage.java` right after the `private Page page;` field:
```java
public DocumentLibraryPage(Page page) {
    this.page = page;
}
```

---

### 2. DocumentLibraryTest.java Does Not Extend BaseTest
**What:** Every other test class (`PushNotificationTest`, etc.) extends `BaseTest`. `DocumentLibraryTest` manages its own Playwright/Browser/Context/Page from scratch — creating a new browser per test, logging in manually each time, with no `auth.json` reuse.
**What to do:**
- Remove the fields: `playwright`, `browser`, `context`, `page`, `docLibPage` at the top of the class (keep `docLibPage`)
- Remove the `@BeforeMethod setUp()` and `@AfterMethod tearDown()` methods entirely
- Remove the `private void login()` method
- Change the class declaration to: `public class DocumentLibraryTest extends BaseTest`
- Add a `@BeforeMethod` that just initialises the page object and navigates, like `PushNotificationTest` does:
```java
@BeforeMethod
public void initPage() {
    docLibPage = new DocumentLibraryPage(page);
    docLibPage.clickOnCommunicationTab();
    docLibPage.clickOnDocumentLibrary();
}
```
- Remove the `docLibPage.clickOnCommunicationTab()` and `docLibPage.clickOnDocumentLibrary()` lines from the top of every test method (they will now be in `@BeforeMethod`)

---

### 3. All Config Values Are Placeholders
**What:** Lines 29–38 in `DocumentLibraryTest.java` have placeholder strings that do nothing:
```java
private static final String BASE_URL        = "https://your-app-url.com";
private static final String DOC_LIBRARY_URL = "https://your-app-url.com/document-library.php";
private static final String DOC_UPLOAD_URL  = "https://your-app-url.com/sp-upload-document.php";
private static final String USERNAME        = "your-username";
private static final String PASSWORD        = "your-password";
```
**What to do:**
- Add these entries to `src/main/resources/config.properties`:
```properties
doc.library.url=https://app.spdevmfp.com/document-library.php
doc.upload.url=https://app.spdevmfp.com/sp-upload-document.php
doc.document.name=TestDocument
doc.description.text=This is a test description.
doc.hashtag.text=testhashtag
doc.search.value=ewewew test
```
- Replace the hardcoded constants in `DocumentLibraryTest.java` with `ConfigReader.get("...")` calls:
```java
private static final String DOC_LIBRARY_URL  = ConfigReader.get("doc.library.url");
private static final String DOC_UPLOAD_URL   = ConfigReader.get("doc.upload.url");
private static final String DOCUMENT_NAME    = ConfigReader.get("doc.document.name");
private static final String DESCRIPTION_TEXT = ConfigReader.get("doc.description.text");
private static final String HASHTAG_TEXT     = ConfigReader.get("doc.hashtag.text");
private static final String SEARCH_VALUE     = ConfigReader.get("doc.search.value");
```
- Remove `BASE_URL`, `USERNAME`, and `PASSWORD` constants entirely — `BaseTest` and `auth.json` handle login already

---

### 4. Test Files Are Missing at C:/TestFiles/
**What:** `DocumentLibraryPage.java` references these hardcoded file paths:
```
C:/TestFiles/test.pdf
C:/TestFiles/test.png
C:/TestFiles/test.jpg
C:/TestFiles/test.csv
C:/TestFiles/test.xlsx
C:/TestFiles/test.mp4
C:/TestFiles/test.gif
C:/TestFiles/thumbnail.png
C:/TestFiles/thumbnail.gif
C:/TestFiles/thumbnail.jpg
```
All upload tests will crash with a file-not-found error if these don't exist.
**What to do:** Create the folder `C:/TestFiles/` and place actual test files there with exactly those names. Use small/lightweight files (a 1-page PDF, a small PNG, etc.) — they don't need to be real content.
Alternatively, if you want to keep test files inside the project, place them in `src/main/resources/testfiles/` and update the constants to use a relative path via `Paths.get("src/main/resources/testfiles/test.pdf")`.

---

## STRUCTURAL (Tests will run but in the wrong way)

### 5. Repeated Navigation in Every Test Method
**What:** After fixing issue 2 and moving navigation to `@BeforeMethod`, every test still has:
```java
docLibPage.clickOnCommunicationTab();
docLibPage.clickOnDocumentLibrary();
```
at the top. These will now be duplicated.
**What to do:** Delete those two lines from the beginning of each test method after the `@BeforeMethod` handles it. Go through all 21 test methods and remove those two lines.

---

### 6. logout() Is Called at the End of Tests — Not Needed With BaseTest
**What:** `BaseTest.tearDown()` calls `context.close()` after each test, which destroys the session cleanly. Explicitly calling `logout()` or `logoutPrimary()` at the end of every test is redundant and wastes time.
**What to do:** Remove `logout()` and `logoutPrimary()` calls from the end of all test methods. The context close in `BaseTest` handles cleanup.

---

## LOGIC / RELIABILITY

### 7. TC_DL_17 Is Missing a Post-Upload Assertion
**What:** `test_TC_DL_17_allMandatoryFields` (priority 4) uploads successfully but never asserts the result. All other upload tests (TC_DL_22 through TC_DL_32) end with:
```java
assertThat(page).hasURL(DOC_LIBRARY_URL);
```
TC_DL_17 skips this entirely.
**What to do:** Add `assertThat(page).hasURL(DOC_LIBRARY_URL);` after `page.waitForTimeout(3000)` in TC_DL_17.

---

### 8. searchResult() Locator Has Text Hardcoded Inside It
**What:** `DocumentLibraryPage.java` line 83:
```java
return page.locator("//td[normalize-space()='ewewew test']");
```
The text `ewewew test` is baked into the locator. If the document name ever changes, this silently fails.
**What to do:** Change the method to accept a parameter:
```java
private Locator searchResult(String text) {
    return page.locator("//td[normalize-space()='" + text + "']");
}
public String getSearchResultText(String text) {
    return searchResult(text).innerText();
}
```
Then update the test call to pass `SEARCH_VALUE`.

---

### 9. #Delete3 Is a Fragile Locator
**What:** `DocumentLibraryPage.java` line 86:
```java
return page.locator("#Delete3");
```
The `3` in `Delete3` appears to be a row index or a record ID. If the data changes, this locator will point to nothing.
**What to do:** Inspect the actual HTML of the page. If the ID is truly dynamic, use a more robust locator such as finding the Delete anchor inside the actions dropdown without relying on the index number.

---

### 10. categoryLabel() Uses a Hardcoded Database ID
**What:** `DocumentLibraryPage.java` line 113:
```java
return page.locator("//label[@for='ms-opt-40']");
```
The `40` is likely a specific category record ID in this environment. It will break in any other environment.
**What to do:** Add a config property for the category label `for` attribute value, or use a more robust locator that selects by visible label text instead of the ID.

---

### 11. #add_hastag Is Likely a Typo
**What:** `DocumentLibraryPage.java` line 171:
```java
page.locator("#add_hastag").innerText().trim()
```
`hastag` is missing the `h` — it should probably be `hashtag`.
**What to do:** Inspect the actual element ID in the browser. If it is `add_hashtag`, fix the locator. If the page genuinely has the typo as `add_hastag`, leave it as-is.

---

## Fix Order Recommendation

| Step | Issue | Why First |
|------|-------|-----------|
| 1 | Add constructor to DocumentLibraryPage | Won't compile without it |
| 2 | Extend BaseTest, remove standalone lifecycle | Foundation for everything else |
| 3 | Replace placeholder config values with ConfigReader | Tests navigate nowhere without real URLs |
| 4 | Create C:/TestFiles/ with actual test files | Upload tests crash without files |
| 5 | Remove duplicate navigation from each test method | Cleanup after step 2 |
| 6 | Remove redundant logout calls | Cleanup after step 2 |
| 7 | Add missing assertion in TC_DL_17 | Test correctness |
| 8 | Fix searchResult() hardcoded locator | Reliability |
| 9 | Verify/fix #Delete3 locator | Reliability |
| 10 | Fix categoryLabel() hardcoded ID | Reliability |
| 11 | Verify #add_hastag typo | Runtime failure risk |
