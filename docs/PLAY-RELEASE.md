# EfterMötet – Google Play-status

Uppdaterad 4 oktober 2026. Appen är en testkandidat; en betalrelease är ännu inte verifierad.

## Aktuell kod

- Paket: `se.denise.eftermotet`. Separat demo: `se.denise.eftermotet.demo`.
- Version: 1.4.2, versionCode 12, mål-API 36.
- Färgteman, möten, dokument, OCR, säkerhetskopia och påminnelser.
- Google Play Billing 9.1.0. Produkt- och erbjudandehämtning, återställning, väntande köp, bekräftelse och åtkomstkontroll.
- Köp verifieras på klienten mot Play och RSA-signaturen. Ingen egen verifieringsserver är implementerad.
- Läsning och export finns kvar efter avslutad åtkomst. Pris och provperiod hämtas från Play.
- Offentlig integritetspolicy: https://denisewijkstrom90.github.io/Eftermotet-android/.

## Prenumerationsvärden

| Inställning | Värde |
| --- | --- |
| Produkt | `eftermotet_manad` |
| Basplan | `manad` |
| Period | Automatisk förnyelse varje månad |
| Sverige | 29 SEK/månad |
| Erbjudande | `gratis-14-dagar` |
| Gratis fas | 14 dagar, därefter ordinarie månadspris |
| Behörighet | Nya prenumeranter enligt Play-regeln |
| Offentlig support | denwijappar@gmail.com |

## Bekräftade steg i denna session

- Signerad 1.4.2-AAB (versionCode 12, mål-SDK 36) accepterades av Play Console och sparades i det interna testutkastet `1.4.2 – internt test`. Den äldre 1.4.1-filen togs bort ur utkastet men finns kvar i artefaktbiblioteket. Den interna testversionen lanserades den 4 oktober 2026 kl. 23:39 svensk tid och verifierades som Aktiva / Tillgänglig för interna testare. Ingen produktionslansering har gjorts.
- Ägarens bekräftade Google Play-konto lades till i en sparad e-postlista med en användare, och listan valdes för det interna testet. Konto-adressen hålls utanför detta offentliga dokument.
- Efter val av testare visade versionskontrollen två varningar: ingen deobfuskeringsfil och inga integrerade felsökningssymboler. Inga blockerande versionsfel visades.
- Testlänken hämtades från den aktiva kanalen: https://play.google.com/apps/internaltest/4700382073966090356 . Användaren måste öppna den med det inbjudna kontot och själv gå med i testet. Installation och Play-köptester har ännu inte verifierats.
- Appöversikten visar 8 av 11 konfigurationsuppgifter klara. Granskaråtkomst, målgrupp och datasäkerhet återstår. Målgruppsformuläret kräver att granskaråtkomsten slutförs först.
- Prenumerationsprodukten skapades.
- Basplanen `manad` sparades och aktiverades för Sverige: automatisk månadsförnyelse, 29,00 SEK inklusive moms.
- Erbjudandet `gratis-14-dagar` sparades och aktiverades: 14 dagar gratis för kunder som aldrig haft denna prenumeration, därefter basplanens månadspris. Båda raderna verifierades som Aktiva i Play Console.
- Ägarens Androidtest av debugkandidaten 1.4.2 bekräftar påminnelse en timme före möte, öppning från avisering, backup/import och beständigt färgval. Se REMINDER-TEST.md.
- Google-inloggningen fungerade vid det senaste försöket. Prenumerationens aktivering innebär inte att appen är publicerad.

## Signerat bygge och nästa release

Den tidigare signerade 1.4.1-AAB:n byggdes från commit `60e9da6d41a85da084ba80961bf809643c20fe9f`, Actions-run 37219695782.
Fil: EfterMotet-1.4.1.aab, 21 780 198 byte.
SHA-256: `e08d21aae0fa8affa72ad7936e6e5c1cc084ab2a791fbf5599be6a9b6e912f18`.
Bygge, lint, enhetstester och signaturverifiering godkändes. ML Kit-biblioteken för arm64-v8a och x86_64 kontrollerades för 16 KB-alignment.

Signerad 1.4.2 byggdes från commit `d0e44321fe565e24784837de00b0a91abf424574`, Actions-run 37236186409. Releasebygge, `testPlayDebugUnitTest` och `lintPlayRelease` godkändes. CI skapade en osignerad bundle i en tillfällig checkout utan privat nyckel; slutfilen signerades lokalt med samma uppladdningsnyckel som för 1.4.1. Certifikatmatchning och jarsigner-verifiering godkändes.
Fil: EfterMotet-1.4.2.aab, 21 797 042 byte.
SHA-256: `58ed2020eba8f976842379ffc47c52e517961115e7242dee66e4907189a8fa7e`.
Uppladdningscertifikat SHA-256: `fb6430e13394453f8e4ce67a2d07af3a67c20178ecfb961aaf2ad752b314f629`.
Signeringsnyckel, lösenord och granskarens kod ska hållas privata och inte läggas i Git.

Java 17, Gradle 8.13 och Android SDK 36 används. Bygginställningar läses från Gradle-properties eller miljövariabler:
`PLAY_PUBLIC_KEY`, `PUBLIC_POLICY_URL`, `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

```sh
gradle assembleDemoDebug assemblePlayDebug lintDemoDebug lintPlayDebug
node tests/smoke.cjs
gradle bundlePlayRelease
```

## Kvar före lansering

1. Kontrollera aktuell verifiering, prenumerationsstatus och testspår i Play Console.
2. Ägaren behöver gå med via den aktiva interna testlänken och installera 1.4.2 från Google Play med sitt bekräftade testkonto. Konfigurera och verifiera licenstestning före testköp så att testflödet inte debiterar ett riktigt köp.
3. Kontrollera att testversionen hämtar den aktiva basplanen och 14-dagarserbjudandet från Play.
4. Testa riktiga Play-köp: provperiod, köp utan provperiod, avbrutna/väntande köp, förnyelse, uppsägning, utgång och återställning.
5. Testa nekade behörigheter, telefonens omstart, ändrade/borttagna påminnelser, TalkBack, stor text och surfplatta.
6. Slutför granskaråtkomst, målgrupp och datasäkerhet; kontrollera att redan ifyllda deklarationer och butiksuppgifter stämmer med releaseversionen.
7. Uppfyll kontots krav på sluten testning och produktionsåtkomst. Den aktuella appöversikten bekräftar minst 12 testare som deltagit kontinuerligt under minst 14 dagar; 0 deltar nu. Intern testning räknas inte som detta slutna test.

CI och debugtester verifierar inte riktiga debiteringar eller fullständig releaseberedskap.
