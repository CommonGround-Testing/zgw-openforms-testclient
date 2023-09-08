# ZGW OpenForms TestClient

![github-actions-ci](https://github.com/CommonGround-Testing/zgw-openforms-testclient/actions/workflows/ci.yml/badge.svg) ![Maven Central](https://img.shields.io/maven-central/v/io.github.commonground-testing/zgw-openforms-testclient)


Deze library is bedoeld om het eenvoudiger te maken om nieuwe OpenForms-submissions aan te maken voor integratie- en ketentestdoeleinden.

Een voorbeeld van het gebruik van deze library:

```java
@Test
public void aFormSubmittedUsingTheOpenFormsApiClient_ShouldBeVisibleInGzac() {

    final String formulier = "voorbeeld-formulier";

    final String csrfCookie = <insert code om CSRF-cookie voor OpenForms-sessie op te halen> 
    final String sessionCookie = <insert code om session-cookie voor OpenForms-sessie op te halen> 
    
    final List<FormStepData> formData = <insert code om data voor de formulierstappen te genereren>

    final OpenFormsApiClient openFormsApi = new OpenFormsApiClient(formulier);
    openFormsApi.createSubmission(csrfCookie, sessionCookie, formData);
    
    assertTrue(<insert verificatie dat zaak correct in GZAC is aangemaakt>);
}
```

Om succesvol een submission aan te maken moet het volgende worden meegegeven aan `createSubmission()`:

| parameter | type | functie                                                                                                          | 
| --- | --- |------------------------------------------------------------------------------------------------------------------|
| csrfCookie | `String` | CSRF cookie gekoppeld aan een actieve OpenForms-gebruikerssessie                                                 |
| sessionCookie | `String` | Session cookie gekoppeld aan dezelfde actieve OpenForms-gebruikerssessie                                         |
| formStepdata | `List<FormStepData>` | Een geordende lijst van `FormStepData`-objecten die in de verschillende formulierstappen moeten worden verzonden |

Het `FormStepData`-object heeft 3 properties:

| property | type | functie | verplicht |
| --- | --- | --- | --- |
| data | `HashMap<String, Object>` | De data die voor een specifieke formulierstap moet worden ingevuld | ja |
| metadata | `HashMap<String, Object>` | Eventueel gewenste meta-data die moet worden meegezonden in een formulierstap | nee (default: leeg) |
| state | String | De gewenste status van de formulierstap in de submission | nee (default: `"submitted"`) |

De OpenFormsApiClient zorgt dat de inhoud van deze velden op de juiste manier naar JSON wordt geserialiseerd.