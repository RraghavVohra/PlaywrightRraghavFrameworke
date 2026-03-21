# Playwright Java Project — Improvement Guide

This document covers all identified issues and their exact implementation steps to make the project efficient and reliably runnable.

---

## Table of Contents

1. [Fix ConfigReader — Classpath Loading](#fix-1--configreader--classpath-loading)
2. [Fix config.properties — Add All Configuration](#fix-2--configproperties--add-all-configuration)
3. [Fix AuthManager — Read Credentials from Config](#fix-3--authmanager--read-credentials-from-config)
4. [Fix BaseTest — Read Settings from Config](#fix-4--basetest--read-settings-from-config)
5. [Fix Redundant Navigation in Test BeforeMethods](#fix-5--fix-redundant-navigation-in-test-beforemethods)
6. [Fix Hardcoded Dates — Use Dynamic Dates](#fix-6--fix-hardcoded-dates--use-dynamic-dates)
7. [Fix False-Passing Assertions](#fix-7--fix-false-passing-assertions)
8. [Fix Test Resource File Paths](#fix-8--fix-test-resource-file-paths)
9. [Fix pom.xml — TestNG Scope and Surefire Plugin](#fix-9--fix-pomxml--testng-scope-and-surefire-plugin)
10. [Implementation Order](#implementation-order)

---

## Fix 1 — ConfigReader — Classpath Loading

**File:** `src/main/java/utils/ConfigReader.java`

**Problem:**
`new FileInputStream("src/main/resources/config.properties")` uses a relative path that breaks when Maven runs tests, because the working directory context is different from the IDE.

**Fixed Code:**

```java
package utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static Properties prop = new Properties();

    static {
        try {
            InputStream is = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties");
            if (is == null) throw new RuntimeException("config.properties not found on classpath");
            prop.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return prop.getProperty(key);
    }

    // Overload with a default value so callers don't need null checks
    public static String get(String key, String defaultValue) {
        return prop.getProperty(key, defaultValue);
    }
}
```

**What changed:**
- Replaced `FileInputStream` with `getResourceAsStream` — works from both IDE and Maven.
- Added a null check with a meaningful error message.
- Added a `get(key, defaultValue)` overload to avoid null checks at call sites.

---

## Fix 2 — config.properties — Add All Configuration

**File:** `src/main/resources/config.properties`

**Problem:**
The file only has credentials. URLs, browser settings, and other configuration are hardcoded across multiple Java files.

**Replace file contents with:**

```properties
# App URL
base.url=https://app.spdevmfp.com

# Credentials
validusername=prem.chandra@bizight.com
validpassword=Sbtest@123

# Browser settings (true = headless, false = shows browser window)
browser.headless=false
browser.slowmo=0
```

**What changed:**
- Added `base.url` so URLs are defined in one place.
- Added `browser.headless` and `browser.slowmo` so these can be changed without touching Java code.
- Set `slowmo=0` by default. Change to `500` temporarily when debugging to slow down actions.

---

## Fix 3 — AuthManager — Read Credentials from Config

**File:** `src/main/java/utils/AuthManager.java`

**Problem:**
Credentials `prem.chandra@bizight.com` / `Sbtest@123` are hardcoded directly in the source file. The `config.properties` already has them but `AuthManager` never reads from it.

**Fixed Code:**

```java
package utils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.io.File;
import java.nio.file.Paths;

public class AuthManager {

    public static void ensureLogin(Browser browser) {

        File file = new File("auth.json");

        if (file.exists()) {
            System.out.println("Session exists. Skipping login.");
            return;
        }

        System.out.println("No session found. Performing login...");

        String baseUrl  = ConfigReader.get("base.url");
        String username = ConfigReader.get("validusername");
        String password = ConfigReader.get("validpassword");

        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        page.navigate(baseUrl + "/");
        page.fill("#username", username);
        page.fill("#password", password);
        page.click("xpath=(//button[@type='submit'])[1]");

        page.waitForURL("**/home/**");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        context.storageState(
            new BrowserContext.StorageStateOptions()
                .setPath(Paths.get("auth.json")));

        context.close();
        System.out.println("Login completed & saved.");
    }
}
```

**What changed:**
- Removed hardcoded credentials.
- Added three `ConfigReader.get()` calls to read `base.url`, `validusername`, and `validpassword` from config.

---

## Fix 4 — BaseTest — Read Settings from Config

**File:** `src/test/java/base/BaseTest.java`

**Problem:**
- URL `https://app.spdevmfp.com/home` is hardcoded.
- `slowMo(50)` is always on, slowing down every action by 50ms across all tests.
- `NETWORKIDLE` is slow for a React SPA — `DOMCONTENTLOADED` is sufficient for most tests.

**Fixed Code:**

```java
package base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import utils.AuthManager;
import utils.ConfigReader;

import java.nio.file.Paths;

public class BaseTest {

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    @org.testng.annotations.BeforeSuite
    public void globalSetup() {
        playwright = Playwright.create();

        boolean headless = Boolean.parseBoolean(ConfigReader.get("browser.headless", "false"));
        int slowMo = Integer.parseInt(ConfigReader.get("browser.slowmo", "0"));

        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(slowMo));

        AuthManager.ensureLogin(browser);
    }

    @org.testng.annotations.BeforeMethod
    public void setUp() {
        context = browser.newContext(
            new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("auth.json"))
                .setViewportSize(1920, 1080));

        page = context.newPage();
        page.setDefaultTimeout(60000);
        page.setDefaultNavigationTimeout(60000);

        page.navigate(ConfigReader.get("base.url") + "/home");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    @org.testng.annotations.AfterMethod
    public void tearDown() {
        context.close();
    }

    @org.testng.annotations.AfterSuite
    public void closeAll() {
        browser.close();
        playwright.close();
    }
}
```

**What changed:**
- `headless` and `slowMo` now read from `config.properties`.
- URL now reads from `config.properties` via `ConfigReader.get("base.url")`.
- Changed `NETWORKIDLE` to `DOMCONTENTLOADED` — significantly faster. If a specific test becomes flaky, add `NETWORKIDLE` only inside that test.

---

## Fix 5 — Fix Redundant Navigation in Test BeforeMethods

**Files:**
- `src/test/java/tests/PushNotificationTest.java`
- `src/test/java/tests/PushNotificationTestImproved.java`

**Problem:**
`BaseTest.setUp()` already navigates to the home URL and waits for page load. Both test classes repeat this in their own `@BeforeMethod`, which doubles the page load time for every single test.

---

**In `PushNotificationTest.java`:**

```java
// BEFORE
@BeforeMethod
public void initPage() {
    page.navigate("https://app.spdevmfp.com/home/");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    pushPage = new PushNotificationPage(page);
    pushPage.openNotificationPage();
}

// AFTER
@BeforeMethod
public void initPage() {
    pushPage = new PushNotificationPage(page);
    pushPage.openNotificationPage();
}
```

Also remove the unused `LoadState` import if nothing else in the file uses it.

---

**In `PushNotificationTestImproved.java`:**

```java
// BEFORE
@BeforeMethod
public void initPage() {
    page.navigate("https://app.spdevmfp.com/home/");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    pushNotificationPage = new PushNotificationPageImproved(page);
}

// AFTER
@BeforeMethod
public void initPage() {
    pushNotificationPage = new PushNotificationPageImproved(page);
}
```

**What changed:**
- Removed the duplicate `page.navigate()` and `waitForLoadState()` calls from test `@BeforeMethod`s.
- `BaseTest.setUp()` handles this once — test classes just initialize their page objects.

---

## Fix 6 — Fix Hardcoded Dates — Use Dynamic Dates

**File:** `src/test/java/tests/PushNotificationTestImproved.java`

**Problem:**
`"27/04/2026"` is hardcoded in 6 different tests. After that date passes, all 6 tests will fail because most scheduling forms reject past dates.

**Step 1 — Add imports at the top of the file:**

```java
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
```

**Step 2 — Add a helper method inside the class (before the first test):**

```java
private String futureDate() {
    return LocalDate.now().plusDays(30)
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
}
```

**Step 3 — Replace all 6 occurrences of the hardcoded date:**

```java
// BEFORE
pushNotificationPage.enterSchedulingDateTime("27/04/2026", "11:30");

// AFTER
pushNotificationPage.enterSchedulingDateTime(futureDate(), "11:30");
```

Tests affected (search for `"27/04/2026"` in the file):
- `test_TC_PN_07_fillsAllFieldsExceptNotificationName`
- `test_TC_PN_08_fillsAllFieldsExceptNotificationMessage`
- `test_TC_PN_017_missingCategoryField`
- `test_TC_PN_019_missingCustomLinkField`
- `test_TC_PN_023_verifyNotificationStatus`
- `test_TC_PN_020_verifyContentLink`

---

## Fix 7 — Fix False-Passing Assertions

**File:** `src/test/java/tests/PushNotificationTestImproved.java`

**Problem:**
Three tests contain `Assert.assertTrue(true)` which always passes regardless of what happened. These tests give false confidence — they will pass even if the feature is completely broken.

---

### Test 1: `test_TC_PN_012_imagesInDifferentFormat`

This test uploads a `.png` image but never checks if the upload succeeded.

```java
// BEFORE
Assert.assertTrue(true);

// AFTER — verify the image preview or upload indicator appears
Assert.assertTrue(pushNotificationPage.isImageUploaded(),
    "Image preview should appear after uploading a PNG file");
```

Add this method to `PushNotificationPageImproved.java`:
```java
public boolean isImageUploaded() {
    // Adjust the selector to match the actual image preview element in your app
    return page.locator(".uploaded-image-preview").isVisible();
}
```

---

### Test 2: `test_TC_PN_049_pushNotificationWithSpecialCharacter`

This test types special characters into the message field but never validates the value was accepted.

```java
// BEFORE
Assert.assertTrue(true);

// AFTER — verify the field accepted the special characters
Assert.assertEquals(pushNotificationPage.getNotificationMessageValue(),
    "!@#$%^&*() Notification",
    "Special characters should be accepted in the message field");
```

Add this method to `PushNotificationPageImproved.java`:
```java
public String getNotificationMessageValue() {
    return notificationMessageField.inputValue();
}
```

---

### Test 3: `test_TC_PN_036_pushNotificationWithCsvUpload`

This test uploads a CSV but never checks if it was accepted.

```java
// BEFORE
Assert.assertTrue(true);

// AFTER — verify the CSV file name appears after upload
Assert.assertTrue(pushNotificationPage.isCsvUploaded(),
    "CSV file name should appear after upload");
```

Add this method to `PushNotificationPageImproved.java`:
```java
public boolean isCsvUploaded() {
    // Adjust selector to match the filename label shown after upload
    return page.locator(".csv-file-name").isVisible();
}
```

> **Note:** The exact selectors for `isImageUploaded()` and `isCsvUploaded()` depend on your app's UI. Inspect the page after a successful upload to find the correct selector.

---

## Fix 8 — Fix Test Resource File Paths

**Files:** `src/test/java/tests/PushNotificationTestImproved.java` and the file system.

**Problem:**
- Tests look for files under `src/test/resources/images/` and `src/test/resources/csv/`.
- The actual files are in `src/main/resources/images/` and `src/main/resources/csv/`.
- The referenced filenames (`notification.jpg`, `notification.png`, `partners.csv`) don't match the actual filenames (`Amsterdam.png`, `budapest.jpg`, `pushnotificationsspuat.csv`).

**Step 1 — Create the missing directories:**
```
src/test/resources/images/
src/test/resources/csv/
```

**Step 2 — Copy files and rename them to match what the tests expect:**

| Source (current location) | Destination (what tests expect) |
|---|---|
| `src/main/resources/images/budapest.jpg` | `src/test/resources/images/notification.jpg` |
| `src/main/resources/images/Amsterdam.png` | `src/test/resources/images/notification.png` |
| `src/main/resources/csv/pushnotificationsspuat.csv` | `src/test/resources/csv/partners.csv` |

**Step 3 — Update file path construction in `PushNotificationTestImproved.java`:**

```java
// BEFORE — depends on working directory, fragile
Paths.get(System.getProperty("user.dir"), "src/test/resources/images/notification.jpg")

// AFTER — reads from classpath, works from both IDE and Maven
Paths.get(getClass().getClassLoader().getResource("images/notification.jpg").toURI())
```

Apply this change to all `uploadImage()` and `uploadCsv()` calls in the test file. You will also need to add `throws Exception` or a try-catch to the test method signature since `toURI()` can throw `URISyntaxException`.

---

## Fix 9 — Fix pom.xml — TestNG Scope and Surefire Plugin

**File:** `pom.xml`

**Two problems:**

### Problem A — TestNG scope is `compile` instead of `test`

Test dependencies should only be available during test compilation and execution. Having them as `compile` scope unnecessarily adds them to the production classpath.

```xml
<!-- BEFORE -->
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.9.0</version>
    <scope>compile</scope>
</dependency>

<!-- AFTER -->
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.9.0</version>
    <scope>test</scope>
</dependency>
```

### Problem B — Surefire plugin is inside `<pluginManagement>` instead of `<build><plugins>`

`<pluginManagement>` only declares plugin configuration for child modules to inherit — it does not activate the plugin. The Surefire plugin must be in `<build><plugins>` to actually run.

**Replace the entire `<build>` section with:**

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>3.2.5</version>
      <configuration>
        <suiteXmlFiles>
          <suiteXmlFile>testng.xml</suiteXmlFile>
        </suiteXmlFiles>
      </configuration>
    </plugin>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.11.0</version>
      <configuration>
        <source>11</source>
        <target>11</target>
      </configuration>
    </plugin>
  </plugins>
</build>
```

**What changed:**
- Removed `<pluginManagement>` wrapper — plugins now live directly in `<build><plugins>`.
- This makes `mvn test` actually respect the `testng.xml` suite file.

---

## Implementation Order

Apply the fixes in this sequence — each fix builds on the previous one:

| # | Fix | File(s) | Why This Order |
|---|-----|---------|----------------|
| 1 | Fix `ConfigReader` classpath loading | `ConfigReader.java` | Everything else depends on config loading correctly |
| 2 | Populate `config.properties` | `config.properties` | Must have values before other classes read them |
| 3 | Use `ConfigReader` in `AuthManager` | `AuthManager.java` | Removes hardcoded credentials |
| 4 | Use `ConfigReader` in `BaseTest` | `BaseTest.java` | Removes hardcoded URL and browser settings |
| 5 | Remove duplicate navigation | Both test files | Reduces test execution time |
| 6 | Replace hardcoded dates | `PushNotificationTestImproved.java` | Prevents future test failures |
| 7 | Fix false assertions | `PushNotificationTestImproved.java` | Makes tests meaningful |
| 8 | Fix test resource paths | Test files + file system | Prevents `FileNotFound` errors |
| 9 | Fix `pom.xml` | `pom.xml` | Makes `mvn test` work correctly |

After all fixes are applied, run the full suite with:
```bash
mvn clean test
```
