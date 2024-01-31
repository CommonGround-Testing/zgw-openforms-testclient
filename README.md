# ZGW OpenForms TestClient

![github-actions-ci](https://github.com/CommonGround-Testing/zgw-openforms-testclient/actions/workflows/ci.yml/badge.svg) ![Maven Central](https://img.shields.io/maven-central/v/io.github.commonground-testing/zgw-openforms-testclient)

## De library

Deze library is bedoeld om het eenvoudiger te maken om nieuwe OpenForms-submissions aan te maken voor integratie- en ketentestdoeleinden.

Een voorbeeld van het gebruik van deze library:

```java
@Test
public void aFormSubmittedUsingTheOpenFormsApiClient_ShouldBeVisibleInGzac() {

    final String formulier = "voorbeeld-formulier";
    
    final Map<String, String> cookies = new HashMap<>();
    
    // TODO: cookies vullen met cookies voor bestaande OpenForms-sessie
 
    final List<FormStepData> formData = <insert code om data voor de formulierstappen te genereren>

    final OpenFormsApiClient openFormsApi = new OpenFormsApiClient(formulier);
    openFormsApi.createSubmission(cookies, formData);
    
    assertTrue(<insert verificatie dat zaak correct in GZAC is aangemaakt>);
}
```

Om succesvol een submission aan te maken moet het volgende worden meegegeven aan `createSubmission()`:

| parameter     | type                  | functie                                                                                                          | 
|---------------|-----------------------|------------------------------------------------------------------------------------------------------------------|
| cookies       | `Map<String, String>` | Cookies gekoppeld aan een actieve OpenForms-gebruikerssessie                                                     |
| formStepdata  | `List<FormStepData>`  | Een geordende lijst van `FormStepData`-objecten die in de verschillende formulierstappen moeten worden verzonden |

Het `FormStepData`-object heeft 4 properties:

| property | type | functie | verplicht |
|----------| --- | --- | --- |
| data     | `HashMap<String, Object>` | De data die voor een specifieke formulierstap moet worden ingevuld | ja |
| metadata | `HashMap<String, Object>` | Eventueel gewenste meta-data die moet worden meegezonden in een formulierstap | nee (default: leeg) |
| state    | String | De gewenste status van de formulierstap in de submission | nee (default: `"submitted"`) |
| slug     | String | Unieke string per stap zodat de juiste volgorde aan stappen bepaald kan worden voor verschillende workflows | ja |

De OpenFormsApiClient zorgt dat de inhoud van deze velden op de juiste manier naar JSON wordt geserialiseerd.

## Anonieme submissions
Wanneer een formulier anonieme submissions toestaat (er hoeft dus niet te worden ingelogd), kan dat met de methode `createAnonymousSubmission()`. Deze heeft slechts 1 argument:

| parameter | type | functie                                                                                                         |
| --- | --- |-----------------------------------------------------------------------------------------------------------------|
| formStepdata | `List<FormStepData>` | Een lijst van `FormStepData`-objecten die in de verschillende formulierstappen moeten worden verzonden |

## Configuratie
Om de library te configureren en default settings te overschrijven, voeg een `openforms.properties` file toe aan je classpath.

Deze settings worden ondersteund door de library:

| setting | omschrijving | default waarde        |
| --- | --- |-----------------------|
| `csrf.cookie.name` | Naam van het CSRF-cookie gebruikt door OpenForms | `csrftoken`           |
| `csrf.header.name` | Naam van de CSRF-header | `X-CSRFToken`         |
| `session.cookie.name` | Naam van de OpenForms session cookie | `openforms_sessionid` |
| `base.uri` | Base URI voor de endpoints waar de formulieren te vinden zijn | _geen waarde_         |
| `base.path` | Base path voor de endpoints | `api/v2`              |
| `polling.timeout` | Timeout voor polling-mechanisme (gebruikt om te checken of submission de gewenste status heeft) | `120` (seconden)      |
| `polling.interval` | Polling-interval | `2` (seconden)        |

## Voorbeeldimplementatie

We zullen starten met een voorbeeld configuratie. Dit is de configuratie voor de testomgeving van openformulieren voor de gemeente den haag.
Op de roadmap staat nog de taak om dit environment onafhankelijk te maken.



    csrf.cookie.name=csrftoken
    csrf.header.name=X-CSRFToken
    session.cookie.name=openforms_sessionid
    base.uri=https://openformulieren-zgw.test.denhaag.nl
    base.path=api/v2
    polling.timeout=120
    polling.interval=2
    


Vervolgens zal er met een automation framework zoals Serenity ingelogd moeten worden in een openforms applicatie. Bij
de gemeente Den Haag is dit inloggen via Digid. Hierna kunnen de gesette cookies met een automation framework uit de 
browser gehaald worden.

    final Map<String, String> cookies = this.login_bij_openforms_op(this.bezwaarMakenPage);

Het opzetten van de lijst aan FormStepData zal het meest tijdconsumerend zijn. Per stap wordt er een FormStepData 
aangemaakt. De slugs en meer metadata van alle stappen kan gevonden worden met het endpoint `/forms/<naam formulier>`.
De data kan je vinden als je alle calls bekijkt die gemaakt worden als je een formulier doorloopt (in je developer console).

    final HashMap<String, Object> formStep02Data = new HashMap<>();

    formStep02Data.put("voorletters-machtiginggever", "S.");
    formStep02Data.put("tussenvoegsels-machtiginggever", ZgwDigidUser1.LAST_NAME_PREFIX);
    formStep02Data.put("achternaam-machtiginggever", ZgwDigidUser1.LAST_NAME);
    formStep02Data.put("postcode", ZgwDigidUser1.ADDRESS_ZIPCODE);
    formStep02Data.put("huisnummer-machtiginggever", ZgwDigidUser1.ADDRESS_HOUSE_NUMBER);
    formStep02Data.put("straatnaam-machtiginggever", ZgwDigidUser1.ADDRESS_STREET);
    formStep02Data.put("woonplaats", ZgwDigidUser1.ADDRESS_CITY);

    FormStepData formData = new FormStepData(formStep02Data, "machtiginggever");

Uiteindelijk kunnen we vervolgens met de naam van het formulier een nieuwe submission submitten.

    final String formulier = "testformulier-zgw-platform-bezwaar";

    new OpenFormsApiClient(formulier).createSubmission(cookies, formData);