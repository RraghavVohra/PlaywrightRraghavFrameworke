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

---

---

# Document Library Fixes — Session 2

## Critical Bugs

### Fix 1 — `private Page page` Shadowing `BaseTest.page`
**File:** `DocumentLibraryTest.java`

**Problem:** The test class declared its own `private Page page` field, which shadowed the `protected Page page` inherited from `BaseTest`. Since `BaseTest` assigns the page in `@BeforeMethod setUp()`, the local field was always `null`. Every test was passing `null` into `new DocumentLibraryPage(page)`, causing a `NullPointerException` on every single test.

**Fix:** Removed `private Page page;` from `DocumentLibraryTest`. The inherited `page` field from `BaseTest` is now used directly.

---

### Fix 2 — Missing `.pdf` Extension on `PDF_FILE` Path
**File:** `DocumentLibraryPage.java`

**Problem:** The `PDF_FILE` constant was missing the file extension:
```java
// Before
public static final String PDF_FILE = "src/main/resources/testfiles/Autopost Done Notification";

// After
public static final String PDF_FILE = "src/main/resources/testfiles/Autopost Done Notification.pdf";
```

**Fix:** Added the `.pdf` extension. Without it, `setInputFiles()` would fail to locate or handle the file during upload tests.

---

### Fix 3 — Hardcoded Test Data in Page Object
**File:** `DocumentLibraryPage.java`

**Problem:** A no-arg `searchResult()` locator had the document name `"ewewew test"` baked directly into it. Page objects should never contain test data — they should only define how to find and interact with elements.
```java
// The offending locator that was removed:
private Locator searchResult() {
    return page.locator("//td[normalize-space()='ewewew test']");
}
```

**Fix:** Removed the hardcoded no-arg `searchResult()` locator and its corresponding `getSearchResultText()` method. The parameterized versions `searchResult(String text)` / `getSearchResultText(String text)` already existed and are now the sole implementation.

---

### Fix 4 — Hardcoded Expected Search Value in Test
**Files:** `DocumentLibraryTest.java`, `config.properties`

**Problem:** `test_TC_DL_37` was asserting against the hardcoded string `"ewewew test"` and calling the now-removed no-arg `getSearchResultText()`.

**Fix:** Added `doc.search.expected=ewewew test` to `config.properties`. The test now reads this as the `SEARCH_EXPECTED` constant and passes it to the parameterized `getSearchResultText(SEARCH_EXPECTED)`. Both the search input and expected result now live in one place (config), not scattered across the code.

---

## Design Improvements

### Fix 5 — Index-Based Locator for Document Library Link
**File:** `DocumentLibraryPage.java`

**Problem:** The `documentLibraryLink()` locator used a positional index to find the menu item:
```java
// Before — breaks if menu order changes
return page.locator("(//a[@class='dropdown-item'])[5]");

// After — stable regardless of position
return page.locator("//a[normalize-space()='Document Library']");
```

**Fix:** Replaced with a text-based locator that matches by visible label. If the menu ever reorders, this will still correctly find the right item.

---

### Fix 6 — Dead Logout Methods
**File:** `DocumentLibraryPage.java`

**Problem:** `clickOnLogoutButton()` and `clickOnLogoutButtonTwo()` were defined but never called from any test class across the entire project. Unused public methods make the page object API confusing.

**Fix:** Removed both methods. `clickOnLogoutOption()` was kept as it may still be needed for a logout test.

---

### Fix 7 — Missing `waitFor` Inside `getDialogBoxText()`
**File:** `DocumentLibraryPage.java`

**Problem:** `innerText()` reads the element immediately without waiting for it to appear. Every test that called `getDialogBoxText()` had to do `page.waitForTimeout(2000)` beforehand just to give the dialog time to render — a fragile workaround.

**Fix:** Added `waitFor(VISIBLE)` inside `getDialogBoxText()` itself:
```java
public String getDialogBoxText() {
    dialogBox().waitFor(new Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
    return dialogBox().innerText();
}
```
The wait now lives in the page object where it belongs, and tests no longer need to sleep before calling it.

---

### Fix 8 — Duplicate Time-Selection Logic
**File:** `DocumentLibraryPage.java`

**Problem:** An identical 10-line block (try the active/highlighted time, fall back to the first available time) was copy-pasted into both `selectDateOfYourChoice()` and `selectCurrentActiveTimeThree()`. Any future change to the logic would need to be made in two places.

**Fix:** Extracted the block into a private helper method `selectActiveOrFirstTime()`. Both public methods now delegate to it, so the logic lives in exactly one place.

---

### Fix 9 — Excessive `page.waitForTimeout()` Calls (~25 instances)
**File:** `DocumentLibraryTest.java`

**Problem:** Hardcoded sleeps were scattered throughout almost every test. They slow down the entire suite and are unreliable — too short on a slow machine, wasted time on a fast one. Playwright already has built-in auto-waiting mechanisms that make most of these sleeps unnecessary.

**Fix:** Removed or replaced every `waitForTimeout` call:

| Original | Replaced With | Reason |
|---|---|---|
| `waitForTimeout(3000)` after `attachThumbnail()` | Removed | `resizeCroppingArea()` already calls `waitFor(VISIBLE)` on the crop handle internally |
| `waitForTimeout(3000)` after `enterValueInDescriptionField()` | Removed | Filling a text field is synchronous — no async activity follows |
| `waitForTimeout(3000)` after `clickOnUploadButton()` | Removed | The `assertThat(page).hasURL()` that follows auto-waits up to 60s |
| `waitForTimeout(2000)` between radio/toggle/hashtag clicks | Removed | Playwright `click()` auto-waits for each element to be visible and actionable |
| `waitForTimeout(3000)` at start of TC_DL_37/38/40 | `page.waitForLoadState(NETWORKIDLE)` | Table data loads via AJAX — `NETWORKIDLE` correctly waits for that to complete |
| `waitForTimeout(2000)` after `clickOnDeleteOption()` in TC_DL_38 | Removed | `getDialogBoxText()` now has its own internal `waitFor` (Fix 7 above) |
| `waitForTimeout(3000)` after `clickOnOkButton()` in TC_DL_39 | `page.waitForLoadState(DOMCONTENTLOADED)` | Server reloads the table after deletion — a load-state wait is semantically correct here |
