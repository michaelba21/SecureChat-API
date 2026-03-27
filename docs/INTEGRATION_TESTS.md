##  Integratietests (Testcontainers)

Algemeen overzicht
Deze integratietests worden uitgevoerd met Testcontainers en PostgreSQL, waarbij Flyway-migraties worden toegepast. Er wordt een echte PostgreSQL-container gestart, wat betekent dat deze tests lokaal op een machine met Docker moeten draaien.

# Vereisten
Geïnstalleerde en actieve Docker

JDK 17

Maven

## Tests uitvoeren
Een enkele testklasse uitvoeren:

# bash
mvn -Dtest=com.securechat.integration.ChatRoomControllerIT test
Alle tests (unit- en integratietests) uitvoeren:

# bash
mvn test

### Belangrijke informatie

Deze tests gebruiken het **integrationtest**-profiel. Hierdoor blijven ze gescheiden van andere testconfiguraties. Voor de testuitvoering zet Flyway de database op aan de hand van de migratiescripts in `src/main/resources/db/migration`.