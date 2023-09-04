# ZGW OpenForms TestClient

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