# Monetization Integration Plan: AdMob & Yandex Ads

Integrating **AdMob** and **Yandex Ads** into Arrow Flow using a centralized `AdsManager` to manage both SDKs independently.

## User Review Required

> [!IMPORTANT]
> - **Ad Unit IDs:** I will use test IDs initially. You will need to replace them with your real production IDs in `AdsManager` or `strings.xml`.
> - **SDK Initialization:** I will create a custom `ArrowFlowApp` class to initialize the SDKs on startup.
> - **Ad Logic:** The `AdsManager` will be designed to handle both SDKs. Should I implement a "Priority" system (e.g., try AdMob first, then Yandex) or a "Manual Switch" (e.g., use one or the other based on a flag)? I'll assume a **Priority System** for maximum fill rate, unless you prefer otherwise.

## Proposed Changes

### Configuration & Dependencies

#### [MODIFY] [libs.versions.toml](file:///C:/Users/User/AndroidStudioProjects/ArrowFlow/gradle/libs.versions.toml)
- Add versions and library entries for `play-services-ads` and `mobileads`.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/User/AndroidStudioProjects/ArrowFlow/app/build.gradle.kts)
- Add the new ad dependencies.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/User/AndroidStudioProjects/ArrowFlow/app/src/main/AndroidManifest.xml)
- Add INTERNET and AD_ID permissions.
- Add AdMob App ID metadata.
- Register the new `Application` class.

---

### Core Ads Logic

#### [NEW] [AdsManager.kt](file:///C:/Users/User/AndroidStudioProjects/ArrowFlow/app/src/main/java/uz/kmax/arrowflow/logic/AdsManager.kt)
- Create a singleton to manage loading and showing:
    - Banners
    - Interstitials (between levels)
    - Rewarded Ads (for hints/lives)
- Handle callbacks and errors for both AdMob and Yandex.

#### [NEW] [ArrowFlowApp.kt](file:///C:/Users/User/AndroidStudioProjects/ArrowFlow/app/src/main/java/uz/kmax/arrowflow/ArrowFlowApp.kt)
- Initialize AdMob and Yandex SDKs here to ensure they are ready before the first ad request.

---

### UI Integration

#### [MODIFY] [MenuFragment.kt](file:///C:/Users/User/AndroidStudioProjects/ArrowFlow/app/src/main/java/uz/kmax/arrowflow/ui/menu/MenuFragment.kt)
- Load and display a Banner ad at the bottom of the main menu.

#### [MODIFY] [GameFragment.kt](file:///C:/Users/User/AndroidStudioProjects/ArrowFlow/app/src/main/java/uz/kmax/arrowflow/ui/game/GameFragment.kt)
- Show an Interstitial ad when a level is completed or when returning to the menu.
- (Optional) Add a button to watch a Rewarded ad for a free hint.

## Verification Plan

### Automated Tests
- N/A for Ads (usually requires manual testing with test devices).

### Manual Verification
- Verify AdMob test banner appears in Menu.
- Verify Yandex test interstitial appears after level completion.
- Verify logcat for "Ad Loaded" and "Ad Failed to Load" events from both SDKs.
- Test SDK initialization in `ArrowFlowApp`.
