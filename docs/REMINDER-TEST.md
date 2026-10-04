# Påminnelser och Androidtest 1.4.2

Ägarens Androidtest av 1.4.1: granskaråtkomst och åtkomst efter att appen stängts och öppnats fungerar; OCR och sparande av dokument till möte fungerar. Aviseringar var tillåtna men ingen avisering observerades vid den först planerade tiden.

Koden använde inexact setAndAllowWhileIdle. Synkronisering avbröt alla gamla larm, även försenade, och återskapade bara framtida larm. 1.4.2 bevarar oförändrade larm och använder setExactAndAllowWhileIdle när användaren har tillåtit SCHEDULE_EXACT_ALARM. Knappen Tillåt exakta påminnelser förklarar behovet och öppnar Androids inställningar. Utan tillstånd finns fortsatt fördröjd reservhantering. Androidkanalen skapas vid appstart. Framtida larm återställs vid återkomst, omstart och när tillståndet ges.

## Ägarens tester 4 oktober 2026

- Påminnelse verifierad på Android i 1.4.2: skärmbilden visar avisering kl. 20:21 för ett möte kl. 21:21, en timme före mötet.
- Ägaren bekräftar att tryck på aviseringen öppnar appen.
- Export och import av säkerhetskopia: ägaren bekräftar att möte, dokument och frågor finns kvar.
- Färgteman: ägaren bekräftar att byte fungerar och att valet finns kvar efter att appen stängts och öppnats.

Detta är ägarrapporterade tester av debugkandidaten. De verifierar inte en signerad release eller verkliga Google Play-köp.

## Kvar att testa

Verifiera med nekade aviserings- och exakta larmtillstånd, telefonens omstart och appöppning strax efter påminnelsetiden. Ändra och ta bort möten och kontrollera att gamla tider inte aviseras. Testa stor text, TalkBack, rotation och surfplatta. Androidleverans är inte verifierad enbart genom CI.
