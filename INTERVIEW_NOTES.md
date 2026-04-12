# Interview Notes — Playwright Java Automation Framework

A running reference of design decisions, patterns, and "why we did it this way" explanations.
Use this to confidently answer questions in interviews.

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
