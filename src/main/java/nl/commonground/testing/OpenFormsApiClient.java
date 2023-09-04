package nl.commonground.testing;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import nl.commonground.testing.data.FormStep;
import nl.commonground.testing.data.FormStepData;
import nl.commonground.testing.data.OpenFormsApiDataCompiler;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/**
 * OpenFormsApiClient is a class providing helper methods for easier creation of OpenForms
 * submissions for your integration and end-to-end testing purposes.
 */
public class OpenFormsApiClient {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OpenFormsApiClient.class);

    private String formId;
    private String csrfToken;
    private String submissionId;

    private RequestSpecification openFormsRequestSpec;
    private Response response, formDetailsResponse;

    private final String formName;
    private final OpenFormsApiConfig config;

    /**
     * Creates a new instance of the <code>OpenFormsApiClient</code> for use with the supplied <code>formName</code>.
     * @param formName the name of the form for which a submission is to be created.
     */
    public OpenFormsApiClient(String formName) {
        this.formName = formName;
        this.config = new OpenFormsApiConfig(loadProperties());
    }

    /**
     * Starts an anonymous submission for the form that this <code>OpenFormsApiClient</code> is associated with.
     * @param formStepData a list of <code>FormStepData</code> objects to be used when creating the submission.
     */
    public void createAnonymousSubmission(List<FormStepData> formStepData) {

        String formUrl = String.format("%s/%s", this.config.getBaseUri(), this.formName);

        LOGGER.info("Initializing anonymous submission of form '{}'", this.formName);

        this.response = given()
                .contentType(ContentType.JSON)
                .header("Referer", formUrl)
                .when()
                .get(formUrl)
                .then()
                .statusCode(200)
                .extract().response();

        String csrfCookie = this.response.getCookie(this.config.getCsrfCookieName());
        String sessionCookie = this.response.getCookie(this.config.getSessionCookieName());

        createRequestSpecificationUsing(csrfCookie, sessionCookie);

        getFormDetailsFor(formUrl);

        startFormSubmissionFor(formUrl);

        completeFormSteps(formStepData);

        finalizeSubmission();
    }

    /**
     * Completes a submission for the form that this <code>OpenFormsApiClient</code> is associated with for the user
     * associated with the supplied <code>csrfCookie</code> and <code>sessionCookie</code>.
     * @param csrfCookie The CSRF cookie used to prevent cross-site request forgery attacks.
     * @param sessionCookie The session cookie returned when a user is successfully authenticated for this form.
     */
    public void createSubmission(String csrfCookie, String sessionCookie, List<FormStepData> formStepData) {

        String formUrl = String.format("%s/%s", this.config.getBaseUri(), this.formName);

        LOGGER.info("Initializing submission of form '{}'", this.formName);

        createRequestSpecificationUsing(csrfCookie, sessionCookie);

        getFormDetailsFor(formUrl);

        startFormSubmissionFor(formUrl);

        completeFormSteps(formStepData);

        finalizeSubmission();
    }

    private void completeFormSteps(List<FormStepData> formStepData) {

        LOGGER.info("Compiling form data for form '{}'", this.formName);

        List<FormStep> formSteps = OpenFormsApiDataCompiler.compileDataForFormSteps(this.formDetailsResponse, formStepData);

        LOGGER.info("Submitting form steps using form data...");

        for(FormStep formStep : formSteps) {

            String referer = String.format("%s/%s/stap/%s", this.config.getBaseUri(), this.formName, formStep.getName());
            String formStepEndpoint = String.format("/submissions/%s/steps/%s", this.submissionId, formStep.getUuid());

            this.response = given().log().ifValidationFails()
                    .spec(this.openFormsRequestSpec)
                    .header("Referer", referer)
                    .header(this.config.getCsrfHeaderName(), this.csrfToken)
                    .when()
                    .get(formStepEndpoint)
                    .then().log().ifValidationFails()
                    .statusCode(200)
                    .extract().response();

            this.csrfToken = this.response.header(this.config.getCsrfHeaderName());

            this.response = given().log().ifValidationFails()
                    .spec(this.openFormsRequestSpec)
                    .header("Referer", referer)
                    .header(this.config.getCsrfHeaderName(), this.csrfToken)
                    .body(formStep.getData())
                    .when()
                    .post(String.format("%s/validate", formStepEndpoint))
                    .then().log().ifValidationFails()
                    .statusCode(204)
                    .extract().response();

            this.csrfToken = this.response.header(this.config.getCsrfHeaderName());

            this.response = given().log().ifValidationFails()
                    .spec(this.openFormsRequestSpec)
                    .header("Referer", referer)
                    .header(this.config.getCsrfHeaderName(), this.csrfToken)
                    .body(formStep.getData())
                    .when()
                    .put(formStepEndpoint)
                    .then().log().ifValidationFails()
                    .statusCode(201)
                    .body("slug", equalTo(formStep.getName()))
                    .body("completed", equalTo(true))
                    .extract().response();

            this.csrfToken = this.response.header(this.config.getCsrfHeaderName());
        }
    }

    private void finalizeSubmission() {

        LOGGER.info("Finalize submission of form '{}'", this.formName);

        this.response = given()
                .spec(this.openFormsRequestSpec)
                .header("Referer", String.format("%s/%s/overzicht", this.config.getBaseUri(), this.formName))
                .when()
                .get(String.format("/submissions/%s/summary", this.submissionId))
                .then()
                .statusCode(200)
                .extract().response();

        this.csrfToken = this.response.header(this.config.getCsrfHeaderName());

        HashMap<String, Object> formData = new HashMap<>();
        formData.put("privacyPolicyAccepted", true);

        this.response = given()
                .spec(this.openFormsRequestSpec)
                .header("Referer", String.format("%s/%s/overzicht", this.config.getBaseUri(), this.formName))
                .header(this.config.getCsrfHeaderName(), this.csrfToken)
                .body(formData)
                .when()
                .post(String.format("/submissions/%s/_complete", this.submissionId))
                .then()
                .statusCode(200)
                .extract().response();

        String statusUrl = this.response.path("statusUrl");

        await()
                .atMost(this.config.getPollingTimeout(), SECONDS)
                .pollInterval(this.config.getPollingInterval(), SECONDS)
                .untilAsserted(() -> this.submissionIsComplete(statusUrl));
    }

    private Properties loadProperties() {

        Properties prop = new Properties();

        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("openforms.properties"))
        {
            if (stream == null) {
                throw new FileNotFoundException("Could not find file 'openforms.properties' on the classpath.");
            }
            prop.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
    }

    private void createRequestSpecificationUsing(String csrfCookie, String sessionCookie) {

        this.openFormsRequestSpec = new RequestSpecBuilder()
                .setBaseUri(this.config.getBaseUri())
                .setBasePath(this.config.getBasePath())
                .setContentType(ContentType.JSON)
                .addCookie(this.config.getCsrfCookieName(), csrfCookie)
                .addCookie(this.config.getSessionCookieName(), sessionCookie)
                .build();
    }

    private void getFormDetailsFor(String formUrl) {

        LOGGER.info("Retrieving form details for form '{}'", formUrl);

        this.formDetailsResponse = given()
                .spec(this.openFormsRequestSpec)
                .header("Referer", formUrl)
                .when()
                .get(String.format("/forms/%s", this.formName))
                .then()
                .statusCode(200)
                .extract().response();

        this.formId = this.formDetailsResponse.path("uuid");
        this.csrfToken = this.formDetailsResponse.header(this.config.getCsrfHeaderName());
    }

    private void startFormSubmissionFor(String formUrl) {

        LOGGER.info("Start submission of form '{}'", formUrl);

        String formEndpoint = String.format("%s/%s/forms/%s", this.config.getBaseUri(), this.config.getBasePath(), this.formId);

        HashMap<String, Object> formData = new HashMap<>();
        formData.put("form", formEndpoint);
        formData.put("formUrl", formUrl);

        this.response = given()
                .spec(this.openFormsRequestSpec)
                .header(this.config.getCsrfHeaderName(), this.csrfToken)
                .header("Referer", formUrl)
                .body(formData)
                .when()
                .post("/submissions")
                .then()
                .statusCode(201)
                .extract().response();

        this.submissionId = this.response.path("id");
        this.csrfToken = this.response.header(this.config.getCsrfHeaderName());
    }

    private void submissionIsComplete(String statusUrl) {

        LOGGER.info("Waiting until submission has status 'done'...");

        given()
                .spec(this.openFormsRequestSpec)
                .header("Referer", String.format("%s/%s/overzicht", this.config.getBaseUri(), this.formName))
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body("status", equalTo("done"))
                .body("result", equalTo("success"))
                .body("errorMessage", equalTo(""));
    }
}
