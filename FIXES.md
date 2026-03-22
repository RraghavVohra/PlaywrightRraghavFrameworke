# Playwright Test Fixes

Three problems identified and their solutions are documented below.
Each section has the exact code you need to add or replace.

---

## Problem 1 — Browser Window Shifted to the Right & Content Hidden on the Right Side

**Root Cause:**
Two things working against each other:

1. The browser window has no position constraint, so Chromium picks a default
   position that may be partially off-screen to the right.
2. `setViewportSize(1920, 1080)` forces page content to always render at 1920px
   wide, even if the actual screen is smaller (e.g. 1366px wide). This means the
   right portion of the page goes beyond the visible screen — it is rendered but
   invisible, which is exactly why it feels "hidden".

**The fix requires TWO changes in the same file, not just one.**
Adding `--start-maximized` alone is NOT enough because the hardcoded
`setViewportSize(1920, 1080)` still overrides the window size.
Both changes must be made together.

**File to edit:** `src/test/java/base/BaseTest.java`

---

### Change A — Add the import at the top of BaseTest.java

Add this alongside the existing imports:

```java
import java.util.Arrays;
```

---

### Change B — Replace the browser launch block inside globalSetup()

**BEFORE (current code):**
```java
browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions().
        setHeadless(headless)
        .setSlowMo(slowMo)); // helps debugging
```

**AFTER (replace with this):**
```java
browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions()
        .setHeadless(headless)
        .setSlowMo(slowMo)
        .setArgs(Arrays.asList(
            "--start-maximized",       // opens browser filling the full screen
            "--window-position=0,0"    // anchors it to the top-left, not off-screen
        )));
```

---

### Change C — Remove setViewportSize from setUp()

This is the most important change. The existing line forces page content to
render at 1920px wide no matter what. Remove it entirely.

**BEFORE (current code in setUp()):**
```java
context = browser.newContext(
        new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("auth.json"))
                .setViewportSize(1920,1080) // This line was added afterwards
);
```

**AFTER (replace with this):**
```java
context = browser.newContext(
        new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("auth.json"))
                // No setViewportSize — Playwright will use the actual window size.
                // Combined with --start-maximized, this matches the full screen,
                // exactly like a normal user opening Chrome.
);
```

**Why this works together:**
- `--start-maximized` → window fills your entire screen (e.g. 1366x768 or 1920x1080)
- No `setViewportSize` → Playwright uses the real window dimensions as the viewport
- Result: the page renders exactly within what is visible on screen — no hidden content

---

## Problem 2 — Form Not Scrolling to Show All Fields After Filling

**Root Cause:**
When a form has many fields (name, message, category, image, link, date/time,
submit button), the lower fields are below the visible viewport. Playwright fills
them but does not scroll to them, so the user watching the browser never sees
those fields being filled. Sometimes the submit button is also off-screen which
can cause it to be missed or overlapped by a sticky header/footer.

**Fix:** Call `scrollIntoViewIfNeeded()` before filling or clicking each field.
This tells Playwright to scroll the page until the element is visible before
interacting with it.

**File to edit:** `src/main/java/pageObjects/PushNotificationPageImproved.java`

### Replace each of these methods one by one:

---

#### enterNotificationName()

**BEFORE:**
```java
public void enterNotificationName(String name) {
    notificationNameField.fill(name);
}
```

**AFTER:**
```java
public void enterNotificationName(String name) {
    notificationNameField.scrollIntoViewIfNeeded();
    notificationNameField.fill(name);
}
```

---

#### enterNotificationMessage()

**BEFORE:**
```java
public void enterNotificationMessage(String message) {
    notificationMessageField.fill(message);
}
```

**AFTER:**
```java
public void enterNotificationMessage(String message) {
    notificationMessageField.scrollIntoViewIfNeeded();
    notificationMessageField.fill(message);
}
```

---

#### openCategoryDropdown()

**BEFORE:**
```java
public void openCategoryDropdown() {
    categoryDropdown.click();
}
```

**AFTER:**
```java
public void openCategoryDropdown() {
    categoryDropdown.scrollIntoViewIfNeeded();
    categoryDropdown.click();
}
```

---

#### uploadImage()

**BEFORE:**
```java
public void uploadImage(Path filePath) {
    imageUpload.setInputFiles(filePath);
}
```

**AFTER:**
```java
public void uploadImage(Path filePath) {
    imageUpload.scrollIntoViewIfNeeded();
    imageUpload.setInputFiles(filePath);
}
```

---

#### clickCustomLink()

**BEFORE:**
```java
public void clickCustomLink() {
    customLinkButton.click();
}
```

**AFTER:**
```java
public void clickCustomLink() {
    customLinkButton.scrollIntoViewIfNeeded();
    customLinkButton.click();
}
```

---

#### enterSchedulingDateTime()

**BEFORE:**
```java
public void enterSchedulingDateTime(String date, String time) {
    String formattedDateTime = formatToDateTimeLocal(date, time);
    schedulingDateTime.fill(formattedDateTime);
}
```

**AFTER:**
```java
public void enterSchedulingDateTime(String date, String time) {
    String formattedDateTime = formatToDateTimeLocal(date, time);
    schedulingDateTime.scrollIntoViewIfNeeded();
    schedulingDateTime.fill(formattedDateTime);
}
```

---

#### clickSubmit()

**BEFORE:**
```java
public void clickSubmit() {
    submitButton.click();
}
```

**AFTER:**
```java
public void clickSubmit() {
    submitButton.scrollIntoViewIfNeeded();
    submitButton.click();
}
```

---

## Problem 3 — "Create App Notification" Sometimes Not Clickable (Flaky)

**Root Cause:**
In `openCreateNotification()`, after `actionsButton.click()`, the dropdown menu
appears asynchronously (rendered by JavaScript). The very next line immediately
tries to click "Create App Notification" before the menu has finished rendering.
When the element is not yet attached to the DOM or not yet visible, the click
either misses or throws an error — making the test randomly pass or fail
on different runs.

**File to edit:** `src/main/java/pageObjects/PushNotificationPageImproved.java`

### Step 1 — Add this import at the top of PushNotificationPageImproved.java

Add alongside the existing imports:

```java
import com.microsoft.playwright.options.WaitForSelectorState;
```

### Step 2 — Replace openCreateNotification()

**BEFORE:**
```java
public void openCreateNotification() {
    openPushNotificationPage();
    actionsButton.click();
    // createAppNotification.click();
    createAppNotification.first().click();
    }
```

**AFTER:**
```java
public void openCreateNotification() {
    openPushNotificationPage();
    actionsButton.click();
    createAppNotification.first().waitFor(
        new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(10000)   // wait up to 10 seconds for the menu to appear
    );
    createAppNotification.first().click();
}
```

**What this does:**
- `waitFor(...)` pauses execution until "Create App Notification" is actually
  visible in the DOM — no more clicking on an element that hasn't rendered yet.
- `setTimeout(10000)` gives it up to 10 seconds, which is more than enough for
  any dropdown animation or JavaScript render to complete.
- This eliminates the race condition that caused the random click failures.

---

## Summary of All Files Changed

| File | What Changed |
|------|-------------|
| `src/test/java/base/BaseTest.java` | Added `Arrays` import + `--start-maximized` and `--window-position=0,0` launch args + removed `setViewportSize(1920,1080)` |
| `src/main/java/pageObjects/PushNotificationPageImproved.java` | Added `WaitForSelectorState` import + `scrollIntoViewIfNeeded()` before each form field interaction + `waitFor(VISIBLE)` before clicking "Create App Notification" |

---

## Quick Checklist Before Running

- [ ] Added `import java.util.Arrays;` in BaseTest.java
- [ ] Replaced the browser launch block with the new one (includes `--start-maximized` and `--window-position=0,0`)
- [ ] Removed `.setViewportSize(1920,1080)` from the context in setUp() — this is critical
- [ ] Added `import com.microsoft.playwright.options.WaitForSelectorState;` in PushNotificationPageImproved.java
- [ ] Added `scrollIntoViewIfNeeded()` in all 7 methods listed above
- [ ] Replaced `openCreateNotification()` with the new version that includes `waitFor(VISIBLE)`
