# Interview Notes — Playwright Java Automation Framework

A running reference of design decisions, patterns, and "why we did it this way" explanations.
Use this to confidently answer questions in interviews.

---

## 0. Overview — Two Projects, One Journey

You have built two automation frameworks from scratch against a real SaaS application (SalesPanda / preprod + prod environments). Both are on GitHub and demonstrate a clear learning arc.

### Project 1 — Selenium Java PageObjectModel (`/PageObjectModel`)
- **Stack:** Selenium 4, TestNG, Java, Extent Reports, Maven
- **Pattern:** Page Object Model — locators and actions in page classes, test logic in test classes
- **Features automated:** Login, Push Notification, Document Library, Social Auto-Post, Image/PDF/Video Creation, Search
- **Supporting utilities:** `WaitUtils` (centralised explicit waits), `Utilities` (screenshot, scroll, properties), `RetryAnalyzer`, `ExtentReporter`, `MyListeners`
- **Auth:** Full login before every test (no session reuse)

### Project 2 — Playwright Java (`/playwright-java-learning`)
- **Stack:** Playwright for Java, TestNG, Java, Allure Reports, Extent Reports, Maven
- **Pattern:** Page Object Model + centralized config + auth state reuse
- **Features automated:** Push Notification (standard + improved + data-driven), Document Library, Social Auto-Post
- **Supporting utilities:** `AuthManager`, `ConfigReader`, `ExcelReader`, `TestContext` (ThreadLocal), `DataProviders`, `RetryAnalyzer` + `RetryAnnotationTransformer`, `ExtentReportListener`, `ExtentReportManager`
- **Auth:** Login once per suite run, session saved to `auth.json`, restored instantly per test via `setStorageStatePath`

---

## 00. Why did you switch from Selenium to Playwright?

This is the most important interview question for your profile. Here is a full, honest, confident answer.

### The short answer
> "I started with Selenium on a real SaaS app — not a demo site. I ran into enough real pain points (stale elements, fragile waits, session management, screenshot capture via reflection) that I wanted to understand if a modern tool solved them by design. Playwright did. So I rebuilt the same feature set in Playwright to directly compare the two."

### The long answer — problem by problem

---

### Problem 1: StaleElementReferenceException

**In Selenium:** When React or Angular re-renders a component after a click or navigation, the `WebElement` reference you have becomes stale — the DOM node it pointed to no longer exists. The test throws `StaleElementReferenceException`. I had to write a dedicated `waitForElementStalenessAndRefresh()` method in `WaitUtils.java`:

```java
public static WebElement waitForElementStalenessAndRefresh(WebDriver driver,
        WebElement staleElement, By locator) {
    getWait(driver, DEFAULT_TIMEOUT)
        .until(ExpectedConditions.stalenessOf(staleElement));
    return waitForElementClickable(driver, locator);
}
```

You're telling the framework: "wait for this element to go stale, then re-fetch it." It's defensive boilerplate you repeat constantly.

**In Playwright:** This doesn't exist. Playwright does not cache element references the way Selenium does. Every time you call `.click()`, `.fill()`, or any action on a `Locator`, Playwright re-queries the DOM at that moment. A Locator is a description of how to find the element — not a snapshot of it. You never hold a stale reference.

**Interview talking point:**
> "Stale element exceptions were one of the biggest sources of false failures in my Selenium work. Every time the React app re-rendered, I had to re-fetch the element. Playwright eliminates this entirely because Locators are lazy — they query the DOM at action time, not at the point you define them. I never had a single stale element error in the Playwright project."

---

### Problem 2: Manual wait management — building WaitUtils from scratch

**In Selenium:** Every action potentially needs an explicit wait. The framework gives you `WebDriverWait` + `ExpectedConditions`, but you have to wire it up yourself every time. In my Selenium project, I ended up building an entire `WaitUtils.java` class with 8+ methods — `waitForElementClickable`, `waitForElementVisible`, `waitForElementPresent`, `waitForElementToDisappear`, `waitForPageLoad`, `waitForTextPresent`, `waitForUrlToContain`, `waitForAllElements`, `isElementPresent`. That's a lot of infrastructure before you've written a single test.

On top of that: if you accidentally leave `implicitlyWait` active alongside `WebDriverWait`, the two interact in unpredictable ways — the effective timeout becomes `implicit + explicit` and tests become slower and flakier. I had to explicitly set `implicitlyWait(0)` to disable it and rely purely on explicit waits.

```java
// Had to do this to prevent implicit+explicit conflict:
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
```

**In Playwright:** Auto-waiting is built into every action. When you call `locator.click()`, Playwright automatically waits for the element to be visible, enabled, stable (not animating), and not obscured — before performing the click. There is no `WebDriverWait`. There is no `ExpectedConditions`. There is no `WaitUtils`. I deleted the entire class when moving to Playwright. `page.setDefaultTimeout(60000)` sets a global cap and that's it.

**Interview talking point:**
> "In Selenium I had to build a WaitUtils class with 8 different wait methods just to handle the most common scenarios. In Playwright, every action has auto-waiting built in — click, fill, navigate, all of them wait for the element to be in the right state automatically. I went from maintaining a 150-line utility class to setting one default timeout. The framework does what you'd expect a modern tool to do."

---

### Problem 3: Implicit wait + explicit wait conflict

**In Selenium:** This is a well-known trap. `driver.manage().timeouts().implicitlyWait()` applies globally to every `findElement` call. `WebDriverWait` applies per-wait. When both are active, the actual timeout becomes unpredictable — Selenium adds the two together in some situations, and conditions like `invisibilityOfElementLocated` can wait the full `implicit + explicit` duration even when the element disappears quickly. I had to document the decision to set implicit wait to 0:

```java
// Removed 35s implicit wait — mixing implicit + explicit waits causes unpredictable behavior.
// All page objects already use explicit WebDriverWait, so implicit wait is unnecessary.
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
```

**In Playwright:** This concept doesn't exist. There is one timeout model — `page.setDefaultTimeout()` — and it applies uniformly. No interaction between two competing timeout systems.

---

### Problem 4: Getting the driver into the listener — Java reflection

**In Selenium:** When a test fails, the `MyListeners` class needs to take a screenshot. But `ITestListener` doesn't know about `WebDriver` — it only knows about `ITestResult`. To get the driver, I had to use Java reflection to reach into the test class instance and pull the private field out:

```java
// In MyListeners.java — getting the driver from the test instance
Field field = testInstance.getClass().getSuperclass().getDeclaredField("driver");
field.setAccessible(true);  // bypass access control
driver = (WebDriver) field.get(testInstance);
```

This works but it's fragile: if the field is renamed, moved to a grandparent class, or made static, the reflection breaks silently. It's also hard to understand for anyone reading the code.

**In Playwright:** I created `TestContext.java` — a `ThreadLocal<Page>` holder. `BaseTest.setUp()` writes the page in after creating it. The listener reads it out. No reflection, no coupling:

```java
// BaseTest.setUp() — writes the page in
TestContext.setPage(page);

// ExtentReportListener.onTestFailure() — reads it out
Page page = TestContext.getPage();
byte[] screenshot = page.screenshot(...);
```

`ThreadLocal` also makes this safe for parallel tests — each thread has its own page reference.

**Interview talking point:**
> "In Selenium I had to use Java reflection to get the WebDriver into the test listener for screenshots. That's a code smell — you're breaking encapsulation with setAccessible(true). In Playwright I replaced that with a ThreadLocal page holder called TestContext. BaseTest writes the page in, the listener reads it out. Clean, type-safe, and parallel-safe."

---

### Problem 5: Screenshot management — file paths vs Base64

**In Selenium:** Screenshots are saved as PNG files to disk. The `Utilities.captureScreenshot()` method creates the file and returns a relative path. That path is then passed to `extentTest.addScreenCaptureFromPath()`. The problem: the path is relative from the report file to the screenshot folder. If the report is at `target/reports/ExtentReport.html` and screenshots are at `screenshots/`, the relative path is `../../screenshots/testName_timestamp.png`. Move either file, change the report output path, or open the HTML on a different machine and the images break.

```java
// Selenium — fragile relative path
return "../../screenshots/" + fileName;
```

**In Playwright:** Screenshots are captured as a `byte[]` and encoded as Base64. The image is embedded directly inside the HTML as a `data:image/png;base64,...` string. The report is completely self-contained — no external files, no relative paths, works on any machine:

```java
// Playwright — self-contained Base64 embedding
byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));
String base64 = Base64.getEncoder().encodeToString(screenshotBytes);
test.fail("<img src='data:image/png;base64," + base64 + "'/>");
```

**Interview talking point:**
> "In Selenium, screenshots were saved as separate PNG files and linked via relative paths in the Extent report. Send the HTML to someone else and the images are broken. In Playwright, screenshots are Base64-encoded and embedded directly in the HTML — the report is a single portable file that works anywhere."

---

### Problem 6: No built-in session reuse — full login before every test

**In Selenium:** Selenium has no native mechanism to save and restore browser session state. In my Selenium project, every test started from scratch — open browser, navigate to login, fill credentials, submit, wait for home page. For a 20-test suite, that's 20 logins. Each login takes 3–5 seconds, so it's adding a minute to every run, plus 20 extra failure points (if login flakes, all 20 tests fail).

The alternative — sharing a browser instance across tests — creates a different problem: test isolation. If test 5 leaves the app in a broken state, test 6 inherits it.

**In Playwright:** `AuthManager.ensureLogin()` runs once in `@BeforeSuite`, performs the login, and saves cookies + local storage to `auth.json` via `context.storageState()`. Every `@BeforeMethod` creates a fresh `BrowserContext` and loads `auth.json` via `setStorageStatePath()`. You get isolation (each test has its own context) AND speed (no login — just session restore):

```java
// @BeforeSuite — runs once
AuthManager.ensureLogin(browser);  // login + save to auth.json

// @BeforeMethod — runs per test
context = browser.newContext(
    new Browser.NewContextOptions()
        .setStorageStatePath(Paths.get("auth.json"))  // restore instantly
);
```

For a 20-test suite: 1 login instead of 20. auth.json is cached between runs — if it still exists from last run, `ensureLogin()` skips the login entirely.

**Interview talking point:**
> "Selenium has no built-in session reuse — every test had to do a full login. In Playwright, I login once per suite, save the session to auth.json, and every test restores it via setStorageStatePath on a fresh context. Tests are isolated from each other but don't pay the cost of a login. For a 20-test suite that's the difference between 1 login and 20 logins."

---

### Problem 7: Strict mode — Selenium silently picks the wrong element

**In Selenium:** If your locator matches multiple elements, `findElement()` silently returns the first one. You might be clicking the wrong button and not know it — the test passes, but it's testing the wrong thing. There's no warning.

**In Playwright:** Strict mode is on by default. If a locator matches more than one element, Playwright throws immediately:

```
Error: strict mode violation: locator("//button[contains(@class,'xdsoft_next')]") resolved to 2 elements
```

I ran into this with the xdsoft datetime picker — it renders two "next" buttons (one for months, one for time). Playwright caught it instantly. Selenium would have silently clicked the first one, which was the time-scroll button, and the test would have appeared to work while navigating to the wrong month.

The fix was scoping the locator to the date panel container. Playwright's strictness forced me to write a better locator.

**Interview talking point:**
> "Playwright's strict mode threw an error the moment my locator matched two elements. Selenium would have silently used the first one — which was the wrong button. Strict mode is actually a feature. It caught an ambiguous locator that would have been a silent false-positive in Selenium. It forced me to be precise."

---

### Problem 8: ChromeOptions workarounds in Selenium

**In Selenium:** Getting Chrome to behave reliably required a list of workarounds that had nothing to do with testing:

```java
options.addArguments("--force-device-scale-factor=0.8");  // fix viewport scaling
options.addArguments("--disable-gpu");                    // prevent renderer crashes
options.addArguments("--no-sandbox");                     // fix renderer timeouts

// Disable password save / breach detection popups
Map<String, Object> prefs = new HashMap<>();
prefs.put("credentials_enable_service", false);
prefs.put("profile.password_manager_enabled", false);
prefs.put("profile.password_manager_leak_detection", false);
options.setExperimentalOption("prefs", prefs);
options.addArguments("--disable-features=PasswordLeakDetection");
```

Every one of these was added to fix a real problem encountered during automation. The viewport scaling fix exists because Windows DPI scaling was hiding the right side of the navigation bar. The GPU flags fix renderer crashes. The password prefs stop Chrome popups from blocking test actions.

**In Playwright:** None of these were needed. Playwright manages the browser process itself with a controlled launch — no OS-level DPI interference, no password popups, no renderer timeout issues. The only launch args I added were window size flags (`--start-maximized`, `--window-size=1366,768`) for consistent resolution, which is an application-level concern, not a framework workaround.

---

### Problem 9: Retry count was hardcoded — not configurable

**In Selenium `RetryAnalyzer`:**
```java
private static final int MAX_RETRY = 2;  // hardcoded
```

Changing the retry count means changing Java code, recompiling, redeploying.

**In Playwright `RetryAnalyzer`:**
```java
private static final int MAX_RETRY = Integer.parseInt(ConfigReader.get("retry.count"));
```

Change it in `config.properties`. No code change needed. Works per-environment if needed.

---

### Problem 10: Config loading with FileInputStream — fragile path

**In Selenium `Utilities.loadPropertiesFile()`:**
```java
String path = System.getProperty("user.dir") + "\\src\\test\\resources\\projectdata.properties";
prop.load(new FileInputStream(path));
```

`user.dir` is the current working directory of the JVM — it depends on how the JVM was launched. From IntelliJ it's the project root. From a Maven Surefire fork it may differ. Hardcoding `\\` also breaks on Mac/Linux.

**In Playwright `ConfigReader`:**
```java
InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties");
```

`getResourceAsStream` loads from the classpath regardless of how the JVM was started — works in IDE, Maven, CI, any OS. No path separators. No `user.dir`.

---

### Summary comparison table

| Concern | Selenium (PageObjectModel) | Playwright (playwright-java-learning) |
|---|---|---|
| Stale elements | `waitForElementStalenessAndRefresh()` workaround | Doesn't exist — Locators re-query on every action |
| Waits | Manual `WaitUtils` (8 methods) + implicit/explicit conflict | Auto-waiting built-in on every action |
| Session reuse | Not supported — full login before every test | `auth.json` via `setStorageStatePath` — 1 login per suite |
| Screenshot in listener | Java reflection (`getDeclaredField().setAccessible(true)`) | `TestContext` ThreadLocal — clean and parallel-safe |
| Screenshot storage | PNG files on disk, fragile relative paths | Base64 embedded in HTML — single portable file |
| Ambiguous locators | Silent — picks first match, may be wrong | Strict mode — throws immediately |
| Browser setup | Long list of ChromeOptions workarounds | Clean launch, no workarounds needed |
| Video recording | Not built-in | Native `setRecordVideoDir()` — WebM per test |
| Traces | Not available | Full DOM + network + source trace, viewer built-in |
| Retry config | Hardcoded `MAX_RETRY = 2` | Read from `config.properties` |
| Config loading | `FileInputStream` with `user.dir` — fragile | `getResourceAsStream` — classpath-safe |
| Parallel safety | `driver` instance field — not ThreadLocal | `BrowserContext` per test — naturally isolated |
| Cross-browser API | Different driver class per browser | Same API — swap `playwright.chromium()` to `firefox()` |

---

## 1. How do you run tests against a different environment (e.g. preprod vs prod)?

### The problem
The framework reads all config (URL, credentials) from `config.properties`. If you hardcode the URL there, switching environments means editing a file — risky, error-prone, and not CI-friendly.

### The solution — System property override in ConfigReader
`ConfigReader.get(key)` was updated to check `System.getProperty(key)` **first**, before falling back to `config.properties`:

```java
public static String get(String key) {
    String sysProp = System.getProperty(key);
    return sysProp != null ? sysProp : prop.getProperty(key);
}
```

This means you can pass `-Dkey=value` from the Maven command line to override any config value **without touching any file**.

### How to run Push Notification tests on preprod

```bash
# Step 1 — delete the existing prod session (auth.json is environment-specific)
del auth.json

# Step 2 — run with preprod overrides
mvn test -Dbase.url=https://app.sppreprod.in -Dvalidusername=raghav.vohra@salespanda.com -Dvalidpassword=Sbtest@1234

# Step 3 — open the Allure report
mvn allure:serve
```

### Why delete auth.json?
`AuthManager.ensureLogin()` checks if `auth.json` already exists. If it does, it **skips login entirely**. Since `auth.json` stores browser cookies/session for prod, using it against preprod would either fail silently or land on the wrong state. Deleting it forces a fresh login against the target environment.

### Interview talking point
> "We use a system property override pattern in ConfigReader — any `-D` flag passed via Maven takes priority over the properties file. This keeps the config file as a safe default and lets CI pipelines or developers switch environments without modifying source files."

---

## 2. How is Allure reporting set up?

### Dependencies added (pom.xml)
- `allure-testng` — auto-registers an Allure listener via Java ServiceLoader. No manual wiring needed.
- `aspectjweaver` — required for `@Step` annotations to be intercepted at runtime via AOP.

### Surefire plugin — argLine
The AspectJ weaver jar is passed as a `-javaagent` to the JVM running tests. Without this, `@Step` methods execute but aren't captured by Allure:

```xml
<argLine>
    -javaagent:"${settings.localRepository}/org/aspectj/aspectjweaver/1.9.21/aspectjweaver-1.9.21.jar"
</argLine>
```

### Screenshot on failure (BaseTest)
`@AfterMethod(alwaysRun = true)` receives `ITestResult` — if the status is `FAILURE`, a screenshot is taken via Playwright and attached to the report using `@Attachment`. `alwaysRun = true` is critical — without it, TestNG skips `@AfterMethod` when the test itself fails.

### Allure annotations used
| Annotation | Level | Purpose |
|---|---|---|
| `@Epic` | Class | Top-level grouping (e.g. "Agency Communication") |
| `@Feature` | Class | Module grouping (e.g. "Push Notification") |
| `@Story` | Method | User story / test category (e.g. "Form Validation") |
| `@Description` | Method | Human-readable explanation of what the test verifies |

### Commands
```bash
mvn test              # runs tests only, produces raw JSON in target/allure-results/
mvn allure:serve      # manually generates report and opens it in browser
mvn allure:report     # manually generates static HTML in target/site/allure-maven-plugin/
mvn verify            # runs tests AND auto-generates the report — recommended way
```

### How to run — `mvn verify`
Run this from the **root of the project** (where `pom.xml` is). In Eclipse/IntelliJ you can right click the project → Run As → Maven build → type `verify` in the Goals field. Or from terminal, `cd` into the project folder and run `mvn verify`.

After it finishes, open the report at:
```
target/site/allure-maven-plugin/index.html
```
Just double-click that file in your file explorer to open it in the browser.

### Why `mvn verify` instead of `mvn test`?
The `verify` phase runs after the `test` phase in Maven's lifecycle. By binding `allure:report` to the `verify` phase in `pom.xml`, the report generation is triggered automatically after all tests complete — no extra manual command needed.

```xml
<execution>
    <id>allure-report</id>
    <phase>verify</phase>   <!-- runs after test phase -->
    <goals>
        <goal>report</goal> <!-- generates HTML from target/allure-results/ -->
    </goals>
</execution>
```

### Interview talking point
> "Allure is integrated via the allure-testng dependency which auto-discovers itself through ServiceLoader — no listener registration needed in testng.xml. Screenshots are captured on failure inside AfterMethod using Playwright's screenshot API and attached via the @Attachment annotation. The hierarchy in the report is driven by @Epic, @Feature, @Story annotations on the test classes and methods. We also bound the allure:report goal to Maven's verify phase so the report is generated automatically after every run — no separate command needed, just open target/site/allure-maven-plugin/index.html."

---

## 3. How is authentication handled?

`AuthManager.ensureLogin()` runs once in `@BeforeSuite`. It checks if `auth.json` exists:
- If yes → skips login (session reuse, faster suite startup)
- If no → performs a full browser login, then saves cookies/storage to `auth.json`

Each test then loads `auth.json` into a fresh `BrowserContext` via `setStorageStatePath()`. This means every test gets a clean context (no shared state between tests) but doesn't pay the cost of a full login each time.

### Interview talking point
> "We separate authentication from test execution using Playwright's storage state feature. Login happens once per suite run and is saved to auth.json. Each test loads that saved session into a new browser context — so tests are isolated from each other but we avoid the overhead of logging in 18 times."

---

## 4. Why use BrowserContext per test instead of sharing one context?

Sharing a single context across tests means cookies, local storage, and page state can bleed between tests — a failure in test 5 could corrupt the state for test 6. Creating a new context per test in `@BeforeMethod` and closing it in `@AfterMethod` gives each test a clean slate, making failures truly independent.

---

## 5. Why DOMCONTENTLOADED instead of NETWORKIDLE?

`NETWORKIDLE` waits until there are no network requests for 500ms — on a busy SPA this can be extremely slow or flaky. `DOMCONTENTLOADED` fires as soon as the HTML is parsed. For most navigations in this app it's sufficient, and it significantly reduces wait time. If a specific test needs the full network to settle, `NETWORKIDLE` is added only inside that test.

---

## 6. How do you handle locators that differ between environments?

### The problem
The same feature can have different locators across environments (dev, preprod, prod) because the app may be at different release stages. For example, the Push Notification menu link is:
- Dev/Preprod: `//a[normalize-space()='New Push Notification']`
- Prod: `//a[normalize-space()='Push Notification']`

Hardcoding one locator means the test breaks on every other environment.

### The solution — `env` key in config.properties

We add a single `env` key to `config.properties` that identifies which environment is active:

```properties
# Values: dev | preprod | prod
env=prod
```

The page object reads this value and picks the correct locator at runtime:

```java
public void openPushNotificationPage() {
    communicationTab.click();

    // Use the correct locator based on the active environment.
    // The menu link text differs between prod and dev/preprod.
    if (ConfigReader.get("env").equals("prod")) {
        notificationsprod.click();   // prod: "Push Notification"
    } else {
        notifications.click();       // dev/preprod: "New Push Notification"
    }
}
```

### How to switch environments
Only two lines change in `config.properties` — everything else is automatic:

```properties
# Switching to prod:
base.url=https://app.technochimes.com
env=prod
validusername=eduadmin@gmail.com
validpassword=12345

# Switching to dev:
base.url=https://app.spdevmfp.com
env=dev
validusername=prem.chandra@bizight.com
validpassword=Sbtest@123
```

### Why not just change the locator directly?
Because that means editing code every time you switch environments — error-prone and not scalable. Config is the right place for anything that changes between environments. Code should be environment-agnostic.

### Interview talking point
> "Our app has three environments — dev, preprod, and prod — and some locators differ between them because features are at different release stages. We handle this with an `env` key in config.properties. The page object reads that value and picks the correct locator at runtime using a simple if/else. This means switching environments is a two-line config change — no code edits, no risk of breaking things."

---

## 7. What makes a good locator in Playwright? How do you evaluate locator quality?

### The hierarchy (best to worst)

| Priority | Type | Example | Why |
|---|---|---|---|
| 1 | ID | `#pushnotify_name` | Unique by design, fastest, most stable |
| 2 | Name / stable attribute | `//input[@name='channel'][@value='1']` | Semantic, tied to functionality not layout |
| 3 | Playwright role-based | `page.getByRole(AriaRole.BUTTON, ...)` | Playwright's recommended approach — uses accessibility tree |
| 4 | Text-based | `//a[normalize-space()='Push Notification']` | Readable but breaks if copy changes |
| 5 | Class-based | `//span[@class='fs-2 fw-bolder']` | Breaks if CSS is refactored |
| 6 | Position-based | `(//span[@class='ms-2'])[3]` | Very fragile — breaks if anything is added before it |
| 7 | Tag-only | `(//*[name()='svg'])[1]` | Worst — grabs first matching element on the whole page |

### Real example from this project

`actionsButton = page.locator("(//*[name()='svg'])[1]")` worked on dev but failed on prod because the first SVG on the page was a different element entirely. It was replaced with:

```java
actionsButtonprod = page.locator("//span[text()='PUSH NOTIFICATION']/following::button[1]//*[local-name()='svg']");
```

This anchors the locator to a known, stable text element (`PUSH NOTIFICATION` heading) and traverses from there — much more reliable.

### Which locators are currently stable in this project?
- **Good:** All `#id`-based locators, `[@name]`/`[@value]` attribute locators, `[@for]` label locators
- **Risky:** `(//*[name()='svg'])[1]`, `(//span[@class='ms-2'])[3]`, `//button[@type='submit']`, `//button[@class='btn btn-primary']`

The risky ones work today but are vulnerable to DOM restructuring or CSS refactoring. They'll be improved as failures occur rather than upfront, since they haven't broken yet.

### Interview talking point
> "I evaluate locators based on how tightly they're coupled to the DOM structure versus the actual functionality. ID-based locators are ideal — they're unique and stable. Position-based or tag-only locators like 'first SVG on the page' are the worst because any DOM change upstream breaks them. We had a real case where the actions button locator worked on dev but failed on prod for exactly this reason — the first SVG element was different. We fixed it by anchoring the locator to a nearby stable text element and traversing from there."

---

## 8. How do you handle JS-driven dropdowns that are in the DOM but stay hidden?

### The problem
On prod (technochimes), clicking the actions button wasn't opening the dropdown menu. The `createAppNotification` element was found in the DOM immediately — but always reported as **hidden**. The `waitFor(VISIBLE)` kept timing out.

The button uses **KTMenu** (Keenthemes JS library), triggered via `data-kt-menu-trigger="click"`. This means the dropdown visibility is controlled by JavaScript, not just CSS. If the click doesn't land cleanly or the JS hasn't had time to run, the menu stays hidden.

### The fix — scroll + pause after click

```java
actionsButtonprod.scrollIntoViewIfNeeded();  // ensure button is in viewport before clicking
actionsButtonprod.click();
page.waitForTimeout(500);  // give KTMenu JS time to run and make the dropdown visible
```

### Why scrollIntoViewIfNeeded?
If the button is partially off-screen, Playwright's click can silently "miss" it — the event fires but doesn't land on the button correctly. Scrolling it into view first ensures the click registers.

### Why waitForTimeout?
KTMenu toggles a CSS class to show the dropdown after the click event. This is asynchronous — there's a brief gap between the click and the DOM update. `waitForTimeout(500)` bridges that gap. Without it, `waitFor(VISIBLE)` starts checking before the JS has run.

### Why not just increase the waitFor timeout?
`waitFor(VISIBLE)` polls the element state — but if the JS never fires (e.g. the click missed), it will never become visible no matter how long you wait. The root fix is ensuring the click lands correctly, not waiting longer.

### Interview talking point
> "We had a case where a dropdown menu item was in the DOM but always hidden — the waitFor kept timing out. The button used a JS library (KTMenu) to control visibility. The fix was two things: scroll the button into view before clicking so the click lands cleanly, then add a short pause after clicking to let the JS run before checking visibility. This is a common pattern with JS-driven UI components — the element exists in the DOM from the start, but its visible state is toggled by JavaScript after the click event."

---

## 9. What is a strict mode violation in Playwright and how did you fix one?

### The problem
6 out of 7 Social Auto-Post tests were failing with:
```
Error: strict mode violation: locator("//button[contains(@class,'xdsoft_next')]") resolved to 2 elements
```

Playwright operates in **strict mode by default** — if a locator matches more than one element, it throws immediately instead of silently picking one. This protects you from accidentally interacting with the wrong element.

### Root cause
The xdsoft datetime picker renders **two** `xdsoft_next` buttons in the DOM at the same time:
1. Inside `xdsoft_datepicker` — the "next month" arrow (what we wanted)
2. Inside `xdsoft_timepicker` — a "scroll time" arrow (not what we wanted)

The bare locator matched both, so Playwright threw a strict mode violation every time the test needed to navigate to a future month.

### Why only 1 test passed?
TC_SAP_01 used `getScheduleDate(10)` which landed on a date in the **current month** — so the next button was never clicked, and the error never triggered. All other tests used larger offsets (20, 25, 140 days) and needed to navigate to a future month.

### The fix — scope the locator to the date panel
```java
// BEFORE — matches both date panel and time panel next buttons
Locator nextButton = page.locator("//button[contains(@class,'xdsoft_next')]");

// AFTER — scoped to xdsoft_datepicker, only matches the month-navigation button
Locator nextButton = page.locator(
    "//div[contains(@class,'xdsoft_datepicker')]//button[contains(@class,'xdsoft_next')]"
);
```

### Interview talking point
> "We hit a Playwright strict mode violation where our locator matched two elements — the xdsoft datetime picker renders two 'next' buttons, one for months and one for time scrolling. Playwright doesn't silently pick one like Selenium would — it throws an error, which is actually safer behaviour. The fix was to scope the locator to the parent date panel container so only the month-navigation button matched. This is a good example of why strict mode is valuable — it caught an ambiguous locator that Selenium would have silently gotten wrong half the time."

---

## 10. How do you keep scheduled date fields future-safe in tests?

### The problem
Tests that schedule posts or access control use a date picker. If you hardcode a date like "April 2026", that date becomes a **past date** as time moves on — the picker disables past dates and the test fails silently.

### The solution — dynamic date helper
A `getScheduleDate(int daysFromNow)` method in the base class computes the target date at runtime by adding an offset to today:

```java
protected String[] getScheduleDate(int daysFromNow) {
    LocalDate future = LocalDate.now().plusDays(daysFromNow);
    String day       = String.valueOf(future.getDayOfMonth());
    String monthYear = future.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                       + " " + future.getYear();
    return new String[]{ day, monthYear };
}
```

All 7 Social Auto-Post tests use `getScheduleDate(1)` — always picks tomorrow. Simple, consistent, and self-maintaining forever.

### Interview talking point
> "We had hardcoded dates in our tests — things like 'April 2026'. Those dates become past dates as time passes, and the date picker disables them, causing silent failures. We replaced all hardcoded dates with a helper method that computes a future date by adding an offset to today's date using Java's LocalDate. Every test now calls getScheduleDate(1) for tomorrow — the tests will never need updating regardless of when they run."

---

## 11. Why was the Social Auto-Post suite logging in before every test, and how was it fixed?

### The problem
`SocialAutoPostBaseTest` had a full login sequence inside `@BeforeMethod`:
```java
page.navigate(preprod.base.url);
page.locator("#username").fill(...);
page.locator("#password").fill(...);
page.locator("...submit...").evaluate("el => el.click()");
page.waitForURL("**/AssetLibrary**");
```

This ran **before every single test** — 7 full logins for a 7-test suite. Each login costs several seconds, slows the suite, and adds an unnecessary point of failure.

### Why it was missed
`SocialAutoPostBaseTest` was written as a standalone base class and never wired up to use `AuthManager`, which the rest of the framework already used.

### The fix
Same pattern as `BaseTest`:
1. `@BeforeSuite` calls `AuthManager.ensureLogin(browser)` — logs in **once**, saves cookies to `auth.json`
2. `@BeforeMethod` loads `auth.json` via `setStorageStatePath` — restores the session instantly, no login page

```java
// @BeforeSuite
AuthManager.ensureLogin(browser);

// @BeforeMethod
context = browser.newContext(
    new Browser.NewContextOptions()
        .setStorageStatePath(Paths.get("auth.json"))
        .setViewportSize(null)
);
page.navigate(ConfigReader.get("preprod.base.url") + "/home");
```

### Interview talking point
> "We noticed the Social Auto-Post suite was logging in before every test even though the rest of the framework had an auth state reuse pattern in place. The base class for that feature was written in isolation and never connected to AuthManager. The fix was straightforward — call ensureLogin once in BeforeSuite and load the saved session in BeforeMethod using Playwright's setStorageStatePath. This reduced 7 logins down to 1 per suite run."

---

## 13. How do you handle flaky tests? What is a Retry Analyzer?

### The problem
Some tests are genuinely flaky — not because the code is wrong, but because of network latency, animation timing on preprod, or a slow server response. Without retries, a single flaky failure marks the entire test as FAILED in the report, making it hard to distinguish real bugs from environmental noise.

### The solution — RetryAnalyzer + IAnnotationTransformer

**Two classes, two responsibilities:**

`RetryAnalyzer` implements `IRetryAnalyzer` — TestNG calls `retry()` after each failure. If we haven't hit the max, we increment and return `true` (retry). Once the limit is reached, return `false` (mark as FAILED).

```java
public class RetryAnalyzer implements IRetryAnalyzer {
    private int retryCount = 0;
    private static final int MAX_RETRY = Integer.parseInt(ConfigReader.get("retry.count"));

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            return true;  // retry
        }
        return false;  // give up, mark as FAILED
    }
}
```

`RetryAnnotationTransformer` implements `IAnnotationTransformer` — called once at suite startup for every `@Test` method. It injects `RetryAnalyzer` into every test annotation automatically.

```java
public class RetryAnnotationTransformer implements IAnnotationTransformer {
    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);  // applied to every @Test globally
    }
}
```

### Why IAnnotationTransformer instead of @Test(retryAnalyzer=...)?
Without the transformer, you'd write `@Test(retryAnalyzer = RetryAnalyzer.class)` on every single test method — easy to miss and tedious to maintain. The transformer injects it globally in one place.

### Registration in testng.xml
```xml
<listeners>
    <listener class-name="listeners.RetryAnnotationTransformer"/>
</listeners>
```
This must be at the `<suite>` level (not inside `<test>`) so it fires before TestNG processes any test annotations.

### Configuration
```properties
# config.properties
retry.count=2   # 1 original run + 2 retries = 3 total attempts
```

### Interview talking point
> "We use a RetryAnalyzer to handle flaky tests caused by environmental instability — things like preprod slowness or animation timing. The key design decision was using IAnnotationTransformer alongside it, which automatically injects the retry analyzer into every test at suite startup. This means we don't have to annotate every single @Test method — one registration in testng.xml covers the entire suite. The max retry count lives in config.properties so we can tune it without touching code."

---

## 15. How is Extent Reports set up and how does it differ from Allure?

### Why two reporting tools?
They solve different problems:
- **Allure** — rich, interactive report but requires a CLI (`allure serve`) or a server to render. Better for CI pipelines with a reporting dashboard.
- **Extent** — generates a **single self-contained HTML file**. No server, no CLI, just open `index.html` in any browser. Better for sharing results quickly or running locally.

Having both means you always have a report you can open instantly (Extent) and a deep-dive report for CI (Allure).

### Files created

| File | Purpose |
|---|---|
| `listeners/ExtentReportManager.java` | Singleton that creates `ExtentReports` + configures `ExtentSparkReporter` |
| `listeners/ExtentReportListener.java` | `ITestListener` — logs pass/fail/skip, embeds screenshot on failure |
| `utils/TestContext.java` | `ThreadLocal<Page>` holder so the listener can grab the page without coupling to BaseTest |

### Why TestContext.java?
The listener needs a Playwright `Page` to capture screenshots on failure, but listeners don't have a reference to `BaseTest`. A `public static` field on `BaseTest` would work but couples the base class to the listener. `TestContext` is a neutral holder: `BaseTest.setUp()` writes the page in, the listener reads it out. `ThreadLocal` makes it safe for parallel execution.

### ExtentReportManager — singleton pattern
```java
public static synchronized ExtentReports getInstance() {
    if (extent == null) {
        ExtentSparkReporter spark = new ExtentSparkReporter("test-output/extent-report/index.html");
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("SalesPanda Automation Report");
        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Environment", ConfigReader.get("env"));
    }
    return extent;
}
```
`synchronized` prevents a race condition if two threads both call `getInstance()` at the same time during parallel startup.

### ExtentReportListener — key methods
```java
public void onTestStart(ITestResult result) {
    ExtentTest test = extent.createTest(result.getMethod().getMethodName());
    extentTest.set(test);  // store in ThreadLocal — each thread has its own ExtentTest
}

public void onTestFailure(ITestResult result) {
    extentTest.get().fail(result.getThrowable());

    // Embed screenshot as Base64 — report stays a single portable file
    byte[] bytes = TestContext.getPage().screenshot(...);
    String base64 = Base64.getEncoder().encodeToString(bytes);
    extentTest.get().fail("<img src='data:image/png;base64," + base64 + "'/>");
}

public void onFinish(ITestContext context) {
    extent.flush();  // CRITICAL — without flush(), the HTML file is created but stays empty
}
```

### Retry interaction
When a test is retried, TestNG fires `onTestSkipped` (not `onTestFailure`) for each failed attempt that will be retried. The final result fires `onTestSuccess` or `onTestFailure`. This gives a visible audit trail in the report showing how many retries happened.

### Registration in testng.xml
```xml
<listener class-name="listeners.ExtentReportListener"/>
```

### Report location
```
test-output/extent-report/index.html
```
Open directly in any browser — no command needed.

### Interview talking point
> "We use both Allure and Extent Reports. Allure is richer but needs a server or CLI to view — it's great for CI pipelines. Extent generates a single HTML file you can open directly in a browser, which is better for quick sharing. The key design decision was adding a TestContext class — a ThreadLocal Page holder — so the Extent listener can capture screenshots on failure without coupling directly to the base test class. This also makes it safe for parallel execution since each thread has its own Page reference."

---

## 14. How do you capture video recordings and traces in Playwright?

### Why both?
They serve different debugging purposes:
- **Video** — lets you *watch* exactly what the browser did during the test. Useful for understanding timing issues, wrong clicks, or UI state before a failure.
- **Trace** — gives you a structured, step-by-step timeline: every action, DOM snapshot, network request, and console log. It's like Chrome DevTools recorded for your entire test. Opened with `npx playwright show-trace`.

### Video Recording

Video is enabled by setting `recordVideoDir` on the `BrowserContext` at creation time:

```java
if (Boolean.parseBoolean(ConfigReader.get("video.enabled"))) {
    contextOptions.setRecordVideoDir(Paths.get("test-output/videos"));
}
```

Playwright writes the `.webm` file only when `context.close()` is called — that's when the video is finalised.

**On pass:** delete the video to save disk space (no point keeping green test evidence).
**On fail:** keep the video, print its path so you can watch it immediately.

```java
context.close();  // must come first — this writes the video file

if (Boolean.parseBoolean(ConfigReader.get("video.enabled")) && page.video() != null) {
    if (failed) {
        System.out.println("[Video] Saved → " + page.video().path());
    } else {
        Files.deleteIfExists(page.video().path());  // clean up on pass
    }
}
```

### Tracing

Tracing is started after context creation and stopped in `@AfterMethod`:

```java
// @BeforeMethod — start tracing
context.tracing().start(new Tracing.StartOptions()
    .setScreenshots(true)  // screenshot at each action
    .setSnapshots(true)    // full DOM snapshot — inspect page state at any step in the viewer
    .setSources(true)      // embeds Java source lines so you see which line triggered each action
);

// @AfterMethod — save on failure, discard on pass
if (failed) {
    context.tracing().stop(new Tracing.StopOptions()
        .setPath(Paths.get("test-output/traces/" + result.getName() + "-" + System.currentTimeMillis() + ".zip")));
} else {
    context.tracing().stop();  // no path = no file written
}
```

Timestamp in the filename prevents overwriting when a test is retried (retry 1 and retry 2 both save traces).

### Viewing a trace
```bash
npx playwright show-trace test-output/traces/<testName>-<timestamp>.zip
```
Opens a browser-based viewer showing the full timeline, screenshots, network tab, console, and source code line.

### Config flags
Both features are controlled by flags in `config.properties` so they can be turned off for faster local runs:
```properties
video.enabled=true
tracing.enabled=true
```

### Key ordering rule
`context.tracing().stop()` must be called **before** `context.close()`. `context.close()` must be called **before** reading `page.video().path()`. Getting this order wrong either corrupts the trace or returns a null video path.

### Interview talking point
> "Playwright has native support for both video recording and tracing. We record video for every test but only keep it on failure — on pass we delete it to avoid filling disk with green test evidence. Traces go further: they capture the full DOM state, network requests, and console logs at every step, and you can open them in Playwright's trace viewer which looks like Chrome DevTools for your test. Both are toggle-controlled in config so you can disable them for a fast local run and re-enable when you're chasing a bug."

---

## 16. How did you implement parallel execution and why did you choose parallel="classes"?

### The options in TestNG

| Mode | What runs in parallel | Safe with instance fields? |
|---|---|---|
| `parallel="methods"` | Each `@Test` method on its own thread | No — same class instance, setUp() overwrites context/page |
| `parallel="classes"` | Each class on its own thread | Yes — each class gets its own instance |
| `parallel="tests"` | Each `<test>` XML block on its own thread | Yes — separate instances per block |

### Why `parallel="classes"`

`context` and `page` are **instance fields** on `BaseTest`. When TestNG creates a separate instance for each class (`parallel="classes"`), those fields are naturally isolated — `DocumentLibraryTest` has its own `page`, `PushNotificationTestImproved` has its own `page`. No ThreadLocal, no code changes.

With `parallel="methods"`, multiple methods of the same class run on different threads but share one instance. Two threads calling `setUp()` simultaneously would overwrite each other's `page` — a race condition requiring ThreadLocal everywhere. Not worth the complexity for browser tests.

### Zero code changes required
`BaseTest` needed no modifications. Instance fields are already the right scope. The only change was adding `parallel="classes" thread-count="3"` to `testng.xml`.

```xml
<suite name="SalesPanda Automation Suite" parallel="classes" thread-count="3">
    <test name="SalesPanda Feature Tests">
        <classes>
            <class name="tests.DocumentLibraryTest"/>
            <class name="tests.PushNotificationTestImproved"/>
            <class name="tests.SocialAutoPostPlaywrightTest"/>
        </classes>
    </test>
</suite>
```

### Consolidating SocialAutoPostBaseTest
`SocialAutoPostBaseTest` was previously a standalone class with its own `@BeforeSuite`, `@BeforeMethod`, `@AfterSuite` — duplicating `BaseTest` entirely. It was refactored to **extend `BaseTest`** and keep only its unique `getScheduleDate()` helper. This meant:
- One browser lifecycle for the entire framework
- `SocialAutoPostPlaywrightTest` can run in the same parallel pool as other feature tests
- No risk of two `@BeforeSuite` methods conflicting

### thread-count tuning
`thread-count="3"` is a safe default for browser tests on a standard dev machine. Each browser context uses ~200-300MB RAM. Going higher than 4-5 threads usually causes more slowdown than speedup due to resource contention. Tune based on your machine.

### Interview talking point
> "We run tests in parallel at the class level — parallel='classes' in testng.xml. Each test class gets its own instance of BaseTest, so the context and page instance fields are naturally isolated between threads — no ThreadLocal needed. We chose this over parallel='methods' because parallel methods would share one class instance and cause concurrent setUp() calls to overwrite each other's page object. The class-level approach gave us parallelism with zero code changes. We also consolidated our two base classes into one inheritance chain so all three feature suites share the same browser lifecycle."

---

## 17. How did you implement Data Driven Testing?

### The approach — three layers

| Layer | File | Responsibility |
|---|---|---|
| Data | `PushNotificationData.xlsx` | Holds the test data — editable by non-coders without touching Java |
| Reader | `utils/ExcelReader.java` | Opens the workbook, reads rows, returns `Object[][]` |
| Provider | `dataproviders/DataProviders.java` | `@DataProvider` method that calls ExcelReader — test classes reference this |
| Test | `PushNotificationDataDrivenTest.java` | `@Test` method receives parameters, runs once per Excel row |

### Why Excel over hardcoded data?
A `@DataProvider` can return inline `Object[][]` (hardcoded arrays). That works for a handful of values but becomes a maintenance burden — a non-technical stakeholder can't add test cases by editing Java. With Excel, anyone can open the file, add a row, and the next test run picks it up with zero code changes.

### ExcelReader — key design decisions

```java
public static Object[][] getTestData(String filePath, String sheetName) throws IOException {
    // row 0 = header, skipped. getLastRowNum() returns data row count exactly.
    int rowCount = sheet.getLastRowNum();
    Object[][] data = new Object[rowCount][colCount];
    for (int i = 1; i <= rowCount; i++) { ... }  // start at 1 to skip header
}
```

NUMERIC cells are a common trap — Excel stores integers as doubles (`287.0`). The reader checks if the value is a whole number and casts to `long` to avoid returning `"287.0"` instead of `"287"`.

### DataProviders — centralised, static, reusable
```java
@DataProvider(name = "pushNotificationData")
public static Object[][] pushNotificationData() throws IOException {
    return ExcelReader.getTestData(EXCEL_PATH, "NotificationData");
}
```
`static` means any test class can reference it via `dataProviderClass = DataProviders.class` without instantiating anything. Centralising all providers in one file makes the data sources easy to find.

### Test method — one method, N executions
```java
@Test(dataProvider = "pushNotificationData", dataProviderClass = DataProviders.class)
public void test_TC_DD_PN_createNotificationFromExcel(
        String testCaseId, String name, String message,
        String customLink, String scheduleTime, String expectedToast) {
    ...
    Assert.assertEquals(pushNotificationPage.getToastMessageText(), expectedToast,
            "Toast mismatch for: " + testCaseId);
}
```
TestNG calls this method once per row. With 3 rows it runs 3 times — each with different data. The `testCaseId` parameter is included in the assertion message so you know exactly which row failed without opening the Excel.

### Excel columns for Push Notification
| Column | Purpose |
|---|---|
| `testCaseId` | Identifier in reports — e.g. TC_DD_PN_01 |
| `notificationName` | Text entered in Name field |
| `notificationMessage` | Text entered in Message field |
| `customLink` | URL for Custom Link field |
| `scheduleTime` | Time in HH:mm — date is always +30 days dynamic in code |
| `expectedToast` | Expected success message to assert |

### Generating the Excel file
`TestDataGenerator.java` has a `main()` that creates the `.xlsx` using POI. Run it once after cloning the repo — it outputs to `src/test/resources/testdata/PushNotificationData.xlsx`. The generator is committed as Java source, so the data structure is version-controlled even though the binary file is gitignored.

### Interview talking point
> "We implemented data-driven testing with TestNG's DataProvider and Apache POI for Excel. The key design was keeping the data, reader, and provider in separate layers — ExcelReader handles all the POI boilerplate, DataProviders exposes named providers that test classes reference, and the test method itself just receives parameters and runs. One method handles N rows. We used Excel over JSON for this feature because stakeholders can add test cases by editing a spreadsheet without touching code. A known trap with POI is numeric cells — Excel stores integers as doubles, so we added a whole-number check in ExcelReader to avoid getting '287.0' instead of '287'."

---

## 12. Why does window resolution matter in Playwright and how did you fix a mismatch?

### The problem
Social Auto-Post tests were running at a smaller resolution than Document Library tests, even though both used `--start-maximized`. The UI was cramped — elements near the bottom or right edge were being partially cut off.

### Root cause
`--start-maximized` alone is **unreliable in Playwright** — it hints to the OS to maximise the window, but Playwright's viewport setting overrides the actual rendering size. `BaseTest` had already solved this with:

```java
.setArgs(Arrays.asList(
    "--start-maximized",
    "--window-position=0,0",
    "--window-size=1366,768"   // explicitly pins the resolution
))
```

`SocialAutoPostBaseTest` was missing `--window-size=1366,768` — so `--start-maximized` fired but with no explicit size, the browser defaulted to a smaller resolution.

### The fix
Add the same three launch args to `SocialAutoPostBaseTest` to match `BaseTest`.

### Interview talking point
> "We had inconsistent resolution between two test suites — one looked fine, the other was cramped. Both used --start-maximized but only one had --window-size set explicitly. In Playwright, --start-maximized alone doesn't guarantee a specific resolution because Playwright's viewport can override the OS window size. Pinning it with --window-size ensures consistent rendering across all suites regardless of the machine's screen settings."

---

## 18. Page Object Model — What it is, how you used it, and the OOP behind it

### What is POM and why use it?

Page Object Model is a design pattern where every page or feature of the application gets its own Java class. That class owns two things:
1. **Locators** — how to find the elements on that page
2. **Action methods** — what a user can do on that page

Test classes call those methods. They never touch a locator directly.

**Why this matters:**
- If a locator changes (e.g. a button ID is renamed), you fix it in one place — the page class — not across 30 test files
- Tests read like plain English: `pushPage.createNotification(name, message)` — no XPath noise in the test
- Each class has a single responsibility — easy to maintain, easy to review

### How you implemented it — both projects

**Selenium project (`/PageObjectModel`):**
```
src/main/java/pageObjects/
    LoginPage.java
    PushNotificationPage.java
    DocumentLibraryPage.java
    SocialAutoPostPage.java
    ImageCreationPage.java
    PdfCreationPage.java
    VideoCreationPage.java
    SearchPage.java

src/test/java/tests/
    PushNotification.java       ← calls methods from PushNotificationPage
    DocumentLibrary.java        ← calls methods from DocumentLibraryPage
    SocialAutoPostTest.java     ← calls methods from SocialAutoPostPage
```

**Playwright project (`/playwright-java-learning`):**
```
src/main/java/pageObjects/
    PushNotificationPage.java
    PushNotificationPageImproved.java
    DocumentLibraryPage.java
    SocialAutoPostPagePW.java

src/test/java/tests/
    PushNotificationTest.java
    PushNotificationTestImproved.java
    PushNotificationDataDrivenTest.java
    DocumentLibraryTest.java
    SocialAutoPostPlaywrightTest.java
```

---

### The OOP pillars — how each one appears in your code

#### 1. Encapsulation

Locators are `private` — hidden inside the page class. Action methods are `public` — the only thing tests can see. The test cannot accidentally click a raw locator; it must go through a named method.

```java
// Selenium — PushNotificationPage.java
private By notificationNameField = By.id("pushnotify_name");  // private — test can't touch this
private By submitButton = By.xpath("//button[@type='submit']");

public void enterNotificationName(String name) {
    driver.findElement(notificationNameField).sendKeys(name);  // public — test calls this
}
```

```java
// Playwright — PushNotificationPageImproved.java
private Locator notificationNameField = page.locator("#pushnotify_name");  // private
private Locator submitButton = page.locator("//button[@type='submit']");

public void enterNotificationName(String name) {
    notificationNameField.fill(name);  // public
}
```

**Why encapsulation matters here:**
If the name field changes from `#pushnotify_name` to `#notification_name`, you change one private field. Every test that calls `enterNotificationName()` still works — they don't even know the locator changed.

#### 2. Inheritance

`BaseTest` contains everything shared across all tests — browser setup, context creation, teardown, screenshot capture. Every test class extends it and inherits all of this for free.

```java
// Selenium
public class Base {
    protected WebDriver driver;
    public WebDriver openBrowserAndApplication(String browserName) { ... }
}

public class PushNotification extends Base {
    // inherits driver, openBrowserAndApplication() — no re-implementation needed
}
```

```java
// Playwright — two-level inheritance for Social Auto-Post
public class BaseTest {
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;
    // @BeforeSuite, @BeforeMethod, @AfterMethod all here
}

public class SocialAutoPostBaseTest extends BaseTest {
    // inherits the full browser lifecycle
    // adds only: getScheduleDate() helper specific to this feature
}

public class SocialAutoPostPlaywrightTest extends SocialAutoPostBaseTest {
    // inherits BaseTest lifecycle + getScheduleDate()
    // contains only: the actual @Test methods
}
```

**Why inheritance matters here:**
Without it, every test class would re-implement `@BeforeSuite` / `@BeforeMethod` / `@AfterMethod`. That's 5 classes × 30 lines each = 150 lines of duplicated lifecycle code. Change the timeout? Change it in 5 places. With inheritance you change it in `BaseTest` and every test class picks it up.

#### 3. Abstraction

The test doesn't know *how* a notification is created — it just calls `createNotification()`. The complexity (clicking the menu, waiting for the form, filling 6 fields, submitting, waiting for the toast) is hidden inside the page object.

```java
// Test — clean, readable, no implementation detail
@Test
public void test_TC_PN_01_createBasicNotification() {
    pushPage.openNotificationPage();
    pushPage.enterNotificationName("Test Alert");
    pushPage.enterNotificationMessage("Hello world");
    pushPage.clickSubmit();
    Assert.assertEquals(pushPage.getToastMessageText(), "Notification created successfully");
}

// Page Object — all the complexity is here, hidden from the test
public void clickSubmit() {
    submitButton.scrollIntoViewIfNeeded();
    submitButton.click();
    page.waitForSelector(".toast-message", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
}
```

#### 4. Polymorphism

Polymorphism appears through **interface implementation** — multiple classes implement the same TestNG interface, and TestNG calls the methods uniformly without knowing which implementation it's talking to.

```java
// RetryAnalyzer implements IRetryAnalyzer
public class RetryAnalyzer implements IRetryAnalyzer {
    @Override
    public boolean retry(ITestResult result) { ... }
}

// ExtentReportListener implements ITestListener
public class ExtentReportListener implements ITestListener {
    @Override
    public void onTestStart(ITestResult result) { ... }
    @Override
    public void onTestFailure(ITestResult result) { ... }
    @Override
    public void onTestSkipped(ITestResult result) { ... }
    @Override
    public void onFinish(ITestContext context) { ... }
}

// MyListeners (Selenium) also implements ITestListener — same interface, different implementation
public class MyListeners implements ITestListener {
    @Override
    public void onTestFailure(ITestResult result) {
        // Selenium version: reflection to get driver, save screenshot as PNG file
    }
}
```

TestNG doesn't care which `ITestListener` is registered. It calls `onTestFailure()` and whichever implementation is wired up executes. That's polymorphism — same interface, different behaviour.

#### 5. Constructor injection (Dependency Injection)

Page objects don't create their own `WebDriver` or `Page`. They receive it through the constructor. This is the Dependency Injection pattern — the dependency is injected from outside.

```java
// Selenium
public class PushNotificationPage {
    private WebDriver driver;

    public PushNotificationPage(WebDriver driver) {
        this.driver = driver;  // injected — not created here
    }
}

// In test:
pushPage = new PushNotificationPage(driver);  // driver created by Base, passed in
```

```java
// Playwright
public class PushNotificationPageImproved {
    private Page page;

    public PushNotificationPageImproved(Page page) {
        this.page = page;  // injected — not created here
    }
}

// In test:
pushNotificationPage = new PushNotificationPageImproved(page);  // page created by BaseTest, passed in
```

**Why this matters:** The page object has no responsibility for browser lifecycle. It just uses whatever it receives. This makes it easy to test in isolation and easy to swap implementations.

#### 6. Static utility classes

`WaitUtils`, `Utilities`, `ConfigReader`, `ExcelReader` — these are utility classes with only `static` methods and no instance state. You call them via class name, never instantiate them.

```java
// No instantiation needed — static access
String baseUrl = ConfigReader.get("base.url");
Object[][] data = ExcelReader.getTestData(EXCEL_PATH, "NotificationData");
WaitUtils.waitForElementClickable(driver, By.id("submit"));
```

#### 7. Singleton pattern (Extent Report Manager)

`ExtentReportManager` uses the Singleton pattern — only one `ExtentReports` instance can exist. `synchronized` prevents a race condition in parallel execution where two threads could both enter `getInstance()` simultaneously and create two separate instances.

```java
public class ExtentReportManager {
    private static ExtentReports extent = null;

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {  // only create once
            ExtentSparkReporter spark = new ExtentSparkReporter("test-output/extent-report/index.html");
            extent = new ExtentReports();
            extent.attachReporter(spark);
        }
        return extent;  // every caller gets the same instance
    }
}
```

#### 8. ThreadLocal — per-thread instance isolation

`TestContext` wraps a `ThreadLocal<Page>`. In parallel execution, multiple threads run simultaneously. Without ThreadLocal, one shared `static Page` would be overwritten by each thread — a race condition. `ThreadLocal` gives each thread its own private copy.

```java
public class TestContext {
    private static final ThreadLocal<Page> pageHolder = new ThreadLocal<>();

    public static void setPage(Page page) { pageHolder.set(page); }  // each thread sets its own
    public static Page getPage() { return pageHolder.get(); }         // each thread reads its own
    public static void clear() { pageHolder.remove(); }               // clean up after test
}
```

### Interview talking point
> "POM is the backbone of both my frameworks. Each page class encapsulates locators as private fields and exposes only named action methods — so if a locator changes, I fix it in one place. Tests extend BaseTest which handles the full lifecycle through inheritance — browser setup, auth, teardown. The page objects receive driver or page through the constructor — that's dependency injection, keeping lifecycle management out of the page class. For reporting, I used the Singleton pattern in ExtentReportManager to ensure one shared report instance across parallel threads, and ThreadLocal in TestContext so each thread has its own Page reference. Polymorphism comes through TestNG interfaces — ITestListener, IRetryAnalyzer, IAnnotationTransformer — each implemented differently but called the same way by TestNG."

---

## 19. Selenium vs Playwright — Technical Comparison (Extended)

### Architecture — how each tool communicates with the browser

**Selenium:**
```
Your Java Code → ChromeDriver.exe (separate process) → Chrome Browser
```
Three hops. Your code sends a REST API call to ChromeDriver. ChromeDriver translates it into a browser command and sends it to Chrome via the W3C WebDriver protocol. Any network delay or process start lag compounds here.

**Playwright:**
```
Your Java Code → Chrome Browser (via CDP WebSocket)
```
Two hops. Playwright communicates directly with the browser over a persistent WebSocket connection using the Chrome DevTools Protocol (CDP). Lower latency, faster execution, and direct access to browser internals.

### What CDP gives Playwright that Selenium can't match natively

| Capability | Selenium | Playwright |
|---|---|---|
| Network interception | Needs BrowserMob Proxy (third-party) | `page.route()` — built in |
| Mock API responses | Third-party proxy setup | `page.route()` + `route.fulfill()` — built in |
| Wait for specific response | Not built in | `page.waitForResponse()` — built in |
| Iframe interaction | `driver.switchTo().frame()` — context switch | `page.frameLocator()` — inline scoping |
| Shadow DOM access | JavaScript executor hacks | Native `>>` deep combinator |
| Video recording | Not built in | `setRecordVideoDir()` — built in |
| Trace viewer | Not available | Full step-by-step trace with DOM snapshots |
| Browser binaries | Separate driver per version (version mismatch risk) | Playwright installs its own pinned browsers |

### Network interception example (Playwright only)
```java
// Intercept any API call and return a mocked response — no proxy needed
page.route("**/api/notifications", route -> {
    route.fulfill(new Route.FulfillOptions()
        .setBody("{\"status\": \"ok\", \"count\": 5}")
        .setContentType("application/json"));
});
```

### Iframe interaction comparison
```java
// Selenium — must switch context in and out
driver.switchTo().frame("iframeId");
driver.findElement(By.id("insideIframe")).click();
driver.switchTo().defaultContent();  // must switch back

// Playwright — scope inline, no context switch
page.frameLocator("#iframeId").locator("#insideIframe").click();
// continues working on main page immediately after
```

### Browser installation — no version mismatch
```bash
# Selenium — must match ChromeDriver version to Chrome version manually
# Chrome 123 needs ChromeDriver 123 — breaks on browser auto-updates

# Playwright — manages its own pinned browser binaries
npx playwright install  # installs the exact versions Playwright was tested against
```

### Interview talking point
> "The fundamental architectural difference is how they talk to the browser. Selenium goes through an intermediate driver server — three hops. Playwright connects directly to the browser via CDP WebSocket — two hops. That's why Playwright is faster and why it can do things Selenium can't natively, like network interception, tracing, and video recording. In Selenium you'd need BrowserMob Proxy just to mock an API call. In Playwright it's two lines of code."

---

## 20. Behavioural Questions — Real Stories from Your Projects

### "Tell me about a time you solved a difficult bug in your automation framework."

> "The toughest one was the window resolution inconsistency between two test suites. The Document Library tests ran fine but Social Auto-Post tests were failing — clicks were landing in the wrong place or elements couldn't be found. I initially thought it was a locator issue and spent time inspecting XPaths. After adding console logs I noticed the two suites were running at different window sizes. The root cause was that `--start-maximized` was set in both but only one had `--window-size=1366,768` explicitly pinned. Playwright's viewport setting overrides the OS window size — without pinning, the browser defaulted to something smaller. The fix was three lines of launch args. The lesson was to check environmental consistency first before assuming the locator is wrong."

### "Tell me about a time a test was passing but the feature was actually broken."

> "We had three tests with `Assert.assertTrue(true)` — a placeholder that was never replaced with a real assertion. The test always passed because it was literally asserting that `true` is `true`. The test names suggested they were checking image uploads and CSV uploads, so on paper we had coverage. In practice those features could have been completely broken and the suite would still be green. I identified these by reviewing the test code, replaced them with assertions against the actual page state — checking that an upload preview appeared — and documented them as false-passing tests. The lesson: a passing test isn't the same as a tested feature."

### "Tell me about a time you improved framework performance."

> "The Social Auto-Post suite was logging in before every single test — 7 full login cycles for 7 tests. I discovered this when I noticed the `@BeforeMethod` in that suite's base class had a full login sequence that was completely separate from the `AuthManager` pattern the rest of the framework used. It was written in isolation early on and never connected. I refactored it to extend `BaseTest` and use `AuthManager.ensureLogin()` in `@BeforeSuite`. Login went from 7 times to 1 time per run. The broader fix was also making the suite extend `BaseTest` properly so it could participate in parallel execution alongside other feature suites."

### "What would you improve about your current framework?"

> "Three things. First, CI/CD pipeline — the framework is already CI-ready since headless mode is a single config flag, but I haven't wired up a GitHub Actions workflow yet. That would give automatic test runs on every push and publish the Allure report as a build artifact. Second, the file paths in config use absolute paths for upload files — they work on my machine but need editing on another machine. Moving to classpath-relative paths using `getClass().getClassLoader().getResource()` would fix that. Third, I'd increase parallel execution — currently running at `thread-count=3`. Profiling memory usage with more threads and tuning based on the CI machine specs would let me push that higher and cut total execution time further."

### "How do you approach making tests maintainable long-term?"

> "Three rules I follow. Externalize everything that changes — URLs, credentials, locator-specific values, file paths all live in `config.properties`, never hardcoded in test files. Generate everything that ages — dates, IDs, anything time-sensitive is computed at runtime using Java's `LocalDate`, not written as a string literal that expires. And document every non-obvious decision — I keep an `INTERVIEW_NOTES.md` in the repo with every architectural decision written down: what the problem was, what the solution is, and why that approach was chosen. A new team member can understand the entire framework without reading git history."

---

## 21. Quick Reference — Numbers, Versions, Commands

Use this to glance at facts before walking into the interview.

### Project stats
| Item | Value |
|---|---|
| Total test cases | 87+ across 3 modules |
| Modules covered | Push Notification, Document Library, Social Auto-Post |
| Environments | Dev, Preprod (`app.sppreprod.in`), Prod (`app.spdevmfp.com`) |
| Login cycles (before fix) | 18 per suite run |
| Login cycles (after fix) | 1 per suite run |
| Parallel thread count | 3 (class-level) |

### Versions
| Tool | Version |
|---|---|
| Playwright for Java | 1.44.0 |
| Java | 11 |
| TestNG | 7.9.0 |
| Allure TestNG | 2.27.0 |
| AspectJ Weaver | 1.9.21 |
| Maven Surefire Plugin | 3.2.5 |
| Extent Reports | 5.x |
| Apache POI (Excel) | 5.x |

### Commands
```bash
mvn test                          # run tests only
mvn verify                        # run tests + generate Allure report
mvn allure:serve                  # open Allure report in browser

# Switch environment via CLI (no file edits needed)
mvn test -Dbase.url=https://app.sppreprod.in -Denv=preprod -Dvalidusername=... -Dvalidpassword=...

# View a Playwright trace file
npx playwright show-trace test-output/traces/<testName>-<timestamp>.zip
```

### Key config keys
```properties
base.url=https://app.spdevmfp.com
env=prod                    # dev | preprod | prod — drives locator selection
browser.headless=false
browser.slowmo=0
retry.count=2
video.enabled=true
tracing.enabled=true
social.partner.value=287    # 287 on preprod, 478 on prod
```

### Key design decisions — one-liners
| Decision | Why |
|---|---|
| `getResourceAsStream` for config | Works from IDE + Maven — `FileInputStream` breaks in Maven |
| `DOMCONTENTLOADED` not `NETWORKIDLE` | Faster; `NETWORKIDLE` times out on SPAs with background polling |
| `BrowserContext` per test | Isolation without the cost of a full browser launch per test |
| `auth.json` storage state | Login once — every test restores session in milliseconds |
| `parallel="classes"` not `parallel="methods"` | Instance fields are naturally isolated per class; methods share one instance |
| Base64 screenshots in report | Self-contained HTML — no broken relative paths when sharing |
| `synchronized` on `getInstance()` | Prevents two parallel threads creating two `ExtentReports` instances |
| `ThreadLocal<Page>` in TestContext | Each thread has its own Page — no cross-contamination in parallel runs |
| `IAnnotationTransformer` for retry | Applies `RetryAnalyzer` globally — no need to annotate every `@Test` |
| Dynamic dates via `LocalDate` | Tests never expire — no manual date updates needed |

---

## 20. How do you handle file uploads in Playwright? (Image Creation feature)

This is one of the most common practical interview questions once you mention Playwright.
There are **two completely different techniques** depending on whether the upload control is an HTML element or a native OS dialog.

---

### Technique 1 — Native OS file dialog (`waitForFileChooser`)

Used when: clicking a **button** opens the operating system's "Open File" window (not an HTML element).

**Selenium approach (painful):**
You had to use the `Robot` class to simulate keyboard input — copy the file path to the clipboard, press CTRL+V, press ENTER. Fragile, platform-dependent, not readable.

```java
Robot robot = new Robot();
StringSelection selection = new StringSelection("C:\\path\\to\\file.png");
Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
robot.keyPress(KeyEvent.VK_CONTROL);
robot.keyPress(KeyEvent.VK_V);
robot.keyRelease(KeyEvent.VK_V);
robot.keyRelease(KeyEvent.VK_CONTROL);
robot.keyPress(KeyEvent.VK_ENTER);
robot.keyRelease(KeyEvent.VK_ENTER);
```

**Playwright approach (clean):**
`page.waitForFileChooser()` registers a listener for the upcoming OS dialog.
The lambda inside the call triggers the click. Playwright intercepts the dialog before it opens and fills it with `setFiles()`.

```java
// The click that opens the dialog goes INSIDE the lambda — critical ordering
FileChooser fileChooser = page.waitForFileChooser(() ->
    page.locator("//button[normalize-space()='Attach']").click()
);
fileChooser.setFiles(Paths.get(ConfigReader.get("image.attach.file")));
```

> **Why the lambda must contain the click:** If you click first and call `waitForFileChooser()` after, the event has already fired and Playwright misses it. The listener must be registered before the click.

---

### Technique 2 — Hidden HTML file input (`setInputFiles`)

Used when: there is a hidden `<input type="file">` element in the DOM (no button opens a dialog).

**Selenium approach:**
```java
WebElement fileInput = driver.findElement(By.xpath("//input[@type='file']"));
fileInput.sendKeys("C:\\path\\to\\image.jpg");  // sendKeys on a hidden input
```

**Playwright approach:**
```java
page.locator("//input[@type='file' and @accept='image/*']")
    .setInputFiles(Paths.get(ConfigReader.get("image.upload.file")));
```

Same concept (`sendKeys` → `setInputFiles`), just cleaner API. Works even on hidden inputs because `setInputFiles` bypasses the visibility check intentionally.

---

### How to tell which technique to use

| Scenario | Technique |
|---|---|
| A button click opens the OS "Open File" window | `waitForFileChooser()` |
| A hidden `<input type="file">` exists in the HTML | `setInputFiles()` |

Inspect the page in DevTools. If you can find an `<input type="file">` element → use `setInputFiles`. If the upload is triggered by a button with no visible file input in the DOM → use `waitForFileChooser`.

---

### Why `setForce(true)` throughout this feature

Several steps in this flow use `.click(new Locator.ClickOptions().setForce(true))`.

**The problem:** The app renders an overlay (`div.overlay-bg`) while dropdowns and dialogs are open. When that overlay is still fading out, Playwright's default `click()` detects that the target element is "covered" by the overlay and throws an error.

**The solution:** `setForce(true)` tells Playwright to skip the "is this element covered?" check and fire the click directly on the target. This is the Playwright equivalent of:
```java
// Selenium
((JavascriptExecutor) driver).executeScript("arguments[0].click()", element);
```

**Rule of thumb:** Try a regular `click()` first. If you consistently see `ElementInterceptedException` or similar, switch to `setForce(true)`. Don't use force everywhere by default — it can mask real UI bugs.

---

### Interview talking point
> "The Image Creation feature taught me that Playwright has two distinct file upload patterns. The first — `waitForFileChooser()` — handles native OS dialogs by intercepting the browser's file chooser event before the dialog even opens. In Selenium, the same thing required the Robot class with clipboard tricks, which was fragile and platform-specific. The second — `setInputFiles()` — targets a hidden `<input type="file">` element directly in the DOM, equivalent to Selenium's `sendKeys()` on a hidden input. Knowing which one to use comes down to inspecting the DOM: if there's a file input element, use `setInputFiles`; if it's a button that opens an OS dialog, use `waitForFileChooser`."

