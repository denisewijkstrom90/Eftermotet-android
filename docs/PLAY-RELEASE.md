# EfterMötet 1.4 – nästa steg i Google Play

4 oktober 2026. Detta är en kandidat, inte en verifierad betalrelease.

## Förberett i källkoden

- Produktionspaket `se.denise.eftermotet`; separat demo `se.denise.eftermotet.demo`.
- Android API 36, versionCode 10. Demo kan endast byggas som debug.
- Google Play Billing 9.1.0: produkt- och erbjudandehämtning, återställning, väntande köp, köpbekräftelse med återförsök, hantering av prenumeration och kontroll vid återkomst till appen.
- Signaturkontroll med appens offentliga Google Play-licensnyckel och kontroll av paketnamn. Köp som väntar eller är avstängda ger ingen åtkomst. Ingen lokal provperiodstimer startar betalning.
- Läsning och export finns kvar efter avslutad tillgång. Pris och 14-dagars erbjudande visas bara utifrån Play-erbjudanden. Svenskt pris måste vara 29 kr/månad.
- Ingen egen köpserver. Kontroll sker på enheten mot Play och RSA-signaturen. Google rekommenderar säker serververifiering för starkare skydd mot manipulerade klienter; den här implementationen har inte det skyddet. Inga servicekontohemligheter ska läggas i Androidappen.
- Integritetspolicy i `docs/integritetspolicy.html` och i appen. Policyn beskriver den här arkitekturen. Offentlig adress: https://denisewijkstrom90.github.io/Eftermotet-android/.

## Exakta värden i Play Console

| Inställning | Värde |
| --- | --- |
| Appens paketnamn | `se.denise.eftermotet` |
| Prenumerationsprodukt | `eftermotet_manad` |
| Basprenumeration | `manad` |
| Typ | Automatisk förnyelse varje månad |
| Sverige | 29 SEK/månad |
| Erbjudande | `gratis-14-dagar` |
| Gratis fas | 14 dagar (P14D eller P2W), en fas/cykel |
| Efter gratis fas | Ordinarie månadsprenumeration |
| Behörighet | Nya prenumeranter enligt den regel du väljer i Google Play |
| Support | denwijappar@gmail.com |

Aktivera produkten, basprenumerationen och erbjudandet. Testa även ett konto som inte får gratisperioden. Appen måste installeras från Play-testspåret med rätt konto och signering för realistiska köp-/återställningstester.

## Uppgifter som saknas från kontot

1. Appens offentliga RSA-licensnyckel är mottagen 4 oktober och införd som standard i app/build.gradle. Den validerades som RSA 2048 bitar (exponent 65537). PLAY_PUBLIC_KEY kan fortfarande åsidosätta standardvärdet. Detta verifierar inte riktiga köp.
2. Policyn är publicerad på https://denisewijkstrom90.github.io/Eftermotet-android/ och används som standard i bygget.
3. Slutlig uppladdningsnyckel. Skapa och förvara den säkert en gång. Återanvänd den vid framtida versioner; byt inte ut en befintlig uppladdningsnyckel.
4. Aktuellt läge i Play Console: konto/app, verifiering, betalningsprofil och testspår.

Bygginställningar läses från Gradle-properties eller miljövariabler:
`PLAY_PUBLIC_KEY`, `PUBLIC_POLICY_URL`, `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
Signeringslösenord och den privata uppladdningsnyckeln ska inte läggas i Git eller skickas i chatten. Använd säker lokal konfiguration eller GitHub Actions secrets.

## Byggkommandon

Java 17, Gradle 8.13, Android SDK 36.

```sh
gradle assembleDemoDebug assemblePlayDebug lintDemoDebug lintPlayDebug
node tests/smoke.cjs
gradle bundlePlayRelease
```

Sista kommandot stoppas utan licensnyckel, offentlig policyadress och slutlig signeringskonfiguration. Att kontrollen går igenom bevisar inte att köpen fungerar. Release-AAB ligger efter ett lyckat bygge i `app/build/outputs/bundle/playRelease/app-play-release.aab`.

## Kvar att verifiera före publicering

- Installation, OCR från kamera/bildväljare, avbrutna filval, aviseringar med nekad/tillåten behörighet, omstart, rotation, stor text, TalkBack och systemets Tillbaka-knapp på Android.
- Verklig start av provperiod, direkt betalt köp utan provperiod, köp som avbryts eller väntar, bekräftelsefel/återförsök, förnyelse, uppsägning med kvarvarande betald tid, utgång, paus/betalningsproblem, återbetalning och återinstallation.
- Verifiera att läsning och export fungerar efter att tillgången upphör och vid nätverksfel.
- Slutlig signerad AAB: paket, behörigheter, 16 KB-kompatibilitet och ML Kit/Billing-data mot Data safety-formuläret.
- Fyll i innehållsklassificering, målgrupp, appåtkomst, Data safety och aktuella deklarationer. Ange policyadress och support.
- Slutliga butiksskärmbilder från Android. Kontots eventuella krav på sluten testning och produktionsåtkomst måste uppfyllas.

Ingen produktionspublicering eller aktivering av riktiga debiteringar ingår i den automatiska byggkontrollen.

## Bekräftat kontoläge 4 oktober, kväll

Användaren bekräftar att befintligt Play Console-konto används via whydontyoutry90@gmail.com. Utvecklarnamnet är Denwijappar; offentlig supportadress är fortsatt denwijappar@gmail.com. Appen finns som utkast, se.denise.eftermotet. Console visar 7 av 11 konfigurationsdelar slutförda och krav på minst 12 testare under minst 14 dagar inför ansökan om produktionsåtkomst. Prenumerationssidan kräver ett uppladdat Billing-bygge innan produkter kan skapas.

Appåtkomst ska deklareras som begränsad för prenumerationsversionen. En fungerande kostnadsfri granskaråtkomst behöver implementeras och verifieras före instruktioner lämnas och intyget om full åtkomst markeras. Eget Google-lösenord ska inte lämnas till granskarna. Målgruppsformuläret är blockerat tills appåtkomst är slutförd. Granskaråtkomsten förbereds enligt avsnittet nedan. Ingen signerad release-AAB är skapad. Console har nu kontrollerats: inga uppladdade AAB-filer och inget registrerat uppladdningscertifikat.

## Granskaråtkomst, version 1.4.1 (versionCode 11)

Native code-entry dialog reachable through “Reviewer access / Granskaråtkomst” on the subscription screen. The confidential 32-character code grants full access without payment and persists across restarts. The visible reviewer banner allows ending access. Only the SHA-256 digest is in source. The confidential code is retained privately. No personal Google password is required. Native code-validation unit tests and UI bridge tests cover invalid codes, valid-code normalization, the dialog entry point and ending access. Real-device testing is still required before certifying full reviewer access in Play Console.

Screenshots confirm Google manages app signing; the upload certificate appears after the first AAB upload. No AAB is uploaded. A proposed CI step to generate the first upload key and export a signed AAB plus encrypted recovery backup was blocked by automatic approval review. It has not been executed or committed. No upload key was generated. Explicit approval of that credential-handling step is pending. Future builds must always reuse the first accepted upload key.
