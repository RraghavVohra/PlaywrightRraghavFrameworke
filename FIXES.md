# Playwright Fix Summary

## Issues Fixed

### 1. Page not loaded before interaction (`PushNotificationTest.java`)
- `waitForLoadState()` defaulted to `load` event, too early for React SPA
- Changed to `waitForLoadState(LoadState.NETWORKIDLE)`

### 2. Expired session (`auth.json`)
- `AuthManager` only checks if `auth.json` exists, not if session is still valid
- Fix: delete `auth.json` to force a fresh login when session expires

### 3. Wrong XPath predicate (`PushNotificationPage.java`)
- `//a/span[text()='Create App Notification'][1]` — `[1]` was on `span`, not `a`
- Changed to `(//a/span[text()='Create App Notification'])[1]`

### 4. Actions button not triggering dropdown
- `(//*[name()='svg'])[1]` was clicking the wrong SVG (not the row actions button)
- Added `hover()` on the first table row before clicking to reveal the button
