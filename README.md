# EfterMötet för Android

Android-projekt för EfterMötet. Appen sparar möten, frågor och dokument lokalt på enheten, kan känna igen text i bilder med ML Kit och visa mötespåminnelser. Säkerhetskopior kan exporteras och importeras i appen.

## Testversion

GitHub Actions bygger en debug-APK under **Actions → Bygg Android-app → Artifacts**. Den är avsedd för teknisk testning, inte som signerad Google Play-version. Gör en säkerhetskopia av dina uppgifter innan du byter eller avinstallerar en tidigare testversion.

## Google Play

Grenen `codex/play-readiness` innehåller ett utkast för prenumeration med Google Play Billing, ett utkast till integritetspolicy och en konkret checklista i [`docs/PLAY-RELEASE.md`](docs/PLAY-RELEASE.md). Prenumerationsprodukten måste konfigureras i Play Console och köptillstånd måste verifieras säkert på en server före skarp publicering. Kontaktadress, färdig integritetspolicy, uppgifter om datasäkerhet, testning och signerad AAB återstår också.

## Bygg lokalt

Projektet använder Android Gradle Plugin 8.13.2, Gradle 8.13, JDK 17 och Android SDK 36. Öppna projektet i Android Studio och kör `gradle assembleDebug` med rätt SDK installerat. En Gradle-wrapper ingår inte.

## Flytta uppgifter från webbversionen

Uppgifterna följer inte med automatiskt. Exportera en säkerhetskopia från webbversionens ”Om appen” och importera den i Android-appen under ”Om appen”.
