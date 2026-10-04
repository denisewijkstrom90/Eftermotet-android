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

- Signerad 1.4.1-AAB (versionCode 11) accepterades av Play Console och sparades som internt testutkast. Ingen test- eller produktionslansering bekräftades.
- Prenumerationsprodukten skapades.
- Månadsbasplanen förbereddes för Sverige. Prisraden verifierades som 29,00 SEK. Google svarade att ändringarna inte kunde sparas; sparande och aktivering är därför inte verifierade. Gratiserbjudandet är inte skapat i denna session.
- Ägarens Androidtest av debugkandidaten 1.4.2 bekräftar påminnelse en timme före möte, öppning från avisering, backup/import och beständigt färgval. Se REMINDER-TEST.md.
- Vid det senare fortsättningsförsöket kunde webbläsaren inte nå Google-inloggningen. Aktuell status och ändringar i andra sessioner måste kontrolleras innan samma utkast eller produkt ändras.

## Signerat bygge och nästa release

Den tidigare signerade 1.4.1-AAB:n byggdes från commit `60e9da6d41a85da084ba80961bf809643c20fe9f`, Actions-run 37219695782.
Fil: EfterMotet-1.4.1.aab, 21 780 198 byte.
SHA-256: `e08d21aae0fa8affa72ad7936e6e5c1cc084ab2a791fbf5599be6a9b6e912f18`.
Bygge, lint, enhetstester och signaturverifiering godkändes. ML Kit-biblioteken för arm64-v8a och x86_64 kontrollerades för 16 KB-alignment.

En ny signerad 1.4.2-AAB återstår. Återanvänd samma uppladdningsnyckel som för 1.4.1. Signeringsnyckel, lösenord och granskarens kod ska hållas privata och inte läggas i Git.

Java 17, Gradle 8.13 och Android SDK 36 används. Bygginställningar läses från Gradle-properties eller miljövariabler:
`PLAY_PUBLIC_KEY`, `PUBLIC_POLICY_URL`, `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

```sh
gradle assembleDemoDebug assemblePlayDebug lintDemoDebug lintPlayDebug
node tests/smoke.cjs
gradle bundlePlayRelease
```

## Kvar före lansering

1. Kontrollera aktuell verifiering, prenumerationsstatus och testspår i Play Console.
2. Bygg och ladda upp signerad 1.4.2 före testlanseringen.
3. Spara och aktivera rätt basplan samt 14-dagarserbjudandet.
4. Testa riktiga Play-köp: provperiod, köp utan provperiod, avbrutna/väntande köp, förnyelse, uppsägning, utgång och återställning.
5. Testa nekade behörigheter, telefonens omstart, ändrade/borttagna påminnelser, TalkBack, stor text och surfplatta.
6. Slutför appåtkomst, målgrupp, innehållsklassificering, Data safety, butiksskärmbilder och aktuella deklarationer utifrån releaseversionen.
7. Uppfyll kontots krav på sluten testning och produktionsåtkomst. Tidigare Console-visning angav minst 12 testare under 14 dagar; kontrollera aktuellt krav.

CI och debugtester verifierar inte riktiga debiteringar eller fullständig releaseberedskap.
