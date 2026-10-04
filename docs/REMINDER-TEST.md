# Påminnelser 1.4.2

Ägarens Androidtest av 1.4.1: granskaråtkomst och åtkomst efter omstart fungerar; OCR och sparande till möte fungerar. Aviseringar tillåtna men ingen avisering observerad vid planerad tid.

Koden använde inexact setAndAllowWhileIdle. Synkronisering avbröt alla gamla larm, även försenade, och återskapade bara framtida larm. 1.4.2 bevarar oförändrade larm och använder setExactAndAllowWhileIdle när användaren har tillåtit SCHEDULE_EXACT_ALARM. Knappen Tillåt exakta påminnelser förklarar behovet och öppnar Androids inställningar. Utan tillstånd finns fortsatt fördröjd reservhantering. Androidkanalen skapas vid appstart. Framtida larm återställs vid återkomst, omstart och när tillståndet ges.

Verifiera på Android: tillåt aviseringar och exakta påminnelser; välj möte 65 minuter framåt och en timme före; spara och stäng appen. Kontrollera avisering omkring fem minuter senare. Upprepa med appöppning strax efter påminnelsetiden. Ändra och ta bort möten och kontrollera att gamla tider inte aviseras. Testa nekat tillstånd och telefonens omstart. Androidleverans är inte verifierad enbart genom CI.
