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
mvn test              # runs tests, produces target/allure-results/
mvn allure:serve      # generates report and opens it in browser
mvn allure:report     # generates static HTML in target/site/allure-maven-plugin/
```

### Interview talking point
> "Allure is integrated via the allure-testng dependency which auto-discovers itself through ServiceLoader — no listener registration needed in testng.xml. Screenshots are captured on failure inside AfterMethod using Playwright's screenshot API and attached via the @Attachment annotation. The hierarchy in the report is driven by @Epic, @Feature, @Story annotations on the test classes and methods."

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
