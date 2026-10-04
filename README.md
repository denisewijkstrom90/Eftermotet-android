# EfterMötet för Android – kandidat 1.4

Androidapp för möten, beslut, uppgifter, frågor, dokument och lokal OCR med sex sparade färgteman. Senaste HTML-gränssnittet är återhämtat från test-APK 1.3; Androidkoden är sammanförd med det sparade källprojektet och prenumerationsflödet. Gamla APK-ompackningar används inte i detta bygge.

Se [publiceringsguiden](docs/PLAY-RELEASE.md) för exakta produkter, signering och återstående kontoinställningar/tester. Detta är inte en verifierad betalrelease.

Bygg separat demo och kandidat med Java 17, Gradle 8.13, Android SDK 36:

```sh
gradle assembleDemoDebug assemblePlayDebug lintDemoDebug lintPlayDebug
```

Demo öppnar redigering och startar inga köp. Play-kandidaten öppnar redigering efter kontroll av Google Play-köp och RSA-signatur. Konfigurera appens offentliga licensnyckel för Play-köptester. Ingen privat nyckel, signeringsfil eller servicekontonyckel ingår.

Gränssnittstest: installera Playwright och Chromium och kör `node tests/smoke.cjs`. Testet använder en simulerad Androidbrygga; det verifierar inte Google Play eller faktisk Androidkörning.

APK från debug har testnyckel. En signerad release-AAB kräver appens slutliga uppladdningsnyckel och de inställningar som guiden anger.
