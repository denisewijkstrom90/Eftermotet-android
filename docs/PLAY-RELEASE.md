# Före publicering av EfterMötet

- Skapa en månatlig, automatiskt förnyande prenumeration i Play Console med produkt-ID `eftermotet_manad`. Basplanen ska vara en månad och ha priset 29 SEK i Sverige. Lägg till ett erbjudande med 14 dagars gratis provperiod för berättigade nya prenumeranter. Appen visar bara erbjudanden som Google Play returnerar för kontot; en användare som redan har utnyttjat provperioden kan få betala från första dagen.
- Testa både berättigad och icke berättigad användare, köp, avbrutet köp, väntande köp, återställning, förfall och uppsägning med Play-testkonton. Prenumerationsköpet startar bara efter användarens eget godkännande i Google Play.
- Produktionsversionen bör verifiera köptoken med Google Play Developer API i en säker backend. Nuvarande klientkontroll är en prototyp och bör inte användas som enda behörighetskontroll för betalande kunder.
- Kontrollera Google ML Kit och Google Play Billings faktiska nätverks- och datahantering och fyll i Datasäkerhet sanningsenligt. Publicera integritetspolicyn på en publik URL och lägg till länken i Play Console. Ange denwijappar@gmail.com som offentlig supportadress och kontrollera att den tar emot e-post.
- Ta fram en signerad release-AAB med en beständig privat uppladdningsnyckel. Lägg aldrig nyckeln i GitHub. Debug-APK:n från Actions kan inte användas som Play-release.
- Granska exportintyget och appens krypteringsberoenden innan kontoinnehavaren godkänner exportdeklarationen.
- Slutför appinnehåll, åldersgrupp, butikstext, bilder, prenumerationsvillkor och stängt test enligt Play Console.
