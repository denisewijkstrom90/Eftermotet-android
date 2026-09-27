# EfterMötet för Android – projektutkast

Det här är källprojektet till en installerbar Android-app för telefon och surfplatta. Det är **inte en färdig APK**. Projektet behöver byggas och provas i Android Studio på en Android-enhet innan det kan installeras eller publiceras.

## Vad som finns

- Egen appikon och en app som öppnas utan webbläsare.
- Möten, dokument och frågor lagras lokalt på enheten.
- Fotografera eller välj en dokumentbild. Textigenkänningen hämtar ett kodbibliotek och språkdata via internet första gången.
- Välj ett möte för dokumentet eller spara det enbart bland Dina dokument.
- Android-aviseringar för valda mötespåminnelser. Telefonens batterisparläge kan förskjuta tiden något. Användaren måste tillåta aviseringar.
- ”Så gör du” i appen och import/export av säkerhetskopia.

## Bygg utan dator

Projektet innehåller `.github/workflows/android-apk.yml`. Om projektfilerna läggs i ett privat GitHub-förråd kan GitHub Actions bygga en test-APK. Den hämtas under Actions → Bygg Android-app → Artifacts på mobilen. Bygget har ännu inte körts och kan behöva rättas innan APK:n fungerar. Lägg inte personliga dokument eller säkerhetskopior i förrådet.

## Bygg i Android Studio

Projektet använder Android Gradle Plugin 8.13.2, Gradle 8.13, JDK 17 och Android SDK 36. Öppna projektmappen i Android Studio, installera SDK vid behov och konfigurera Gradle 8.13. En Gradle-wrapper ingår inte i detta källpaket. Skapa den med `gradle wrapper --gradle-version 8.13` om du har Gradle installerat, eller konfigurera Android Studio att använda en lokal Gradle 8.13. Bygg sedan en debug-APK och testa på telefon och surfplatta.

## Flytta uppgifter från webbversionen

Uppgifterna följer inte med automatiskt eftersom appen har egen lagring. Öppna webbversionens ”Om appen”, välj ”Ladda ner säkerhetskopia” och läs sedan in filen i Android-appen under ”Om appen”.

## Kvar före distribution

Byggning och verklig testning av APK, särskilt kamera, textigenkänning, återställning och påminnelser. Därefter signering och distribution. iPad/iPhone kräver ett separat iOS-projekt.
