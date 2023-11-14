package io.github.commonground.testing;

import io.github.commonground.testing.data.FormStep;
import io.github.commonground.testing.data.FormStepData;
import io.github.commonground.testing.data.OpenFormsApiDataCompiler;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

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
     *
     * @param formName the name of the form for which a submission is to be created.
     */
    public OpenFormsApiClient(String formName) {
        this.formName = formName;
        this.config = new OpenFormsApiConfig(loadProperties());

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Starts an anonymous submission for the form that this <code>OpenFormsApiClient</code> is associated with.
     *
     * @param formStepData a list of <code>FormStepData</code> objects to be used when creating the submission.
     */
    public void createAnonymousSubmission(List<FormStepData> formStepData) {

        this.createAnonymousSubmission(formStepData, false);
    }

    /**
     * Starts an anonymous submission for the form that this <code>OpenFormsApiClient</code> is associated with.
     *
     * @param formStepData            a list of <code>FormStepData</code> objects to be used when creating the submission.
     * @param failOnStepCountMismatch flag indicating whether or not to throw an exception when the supplied number
     *                                of form step data elements does not match the number of steps in the form.
     */
    public void createAnonymousSubmission(List<FormStepData> formStepData, boolean failOnStepCountMismatch) {

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

        Map<String, String> cookies = new HashMap<>();

        cookies.put(this.config.getCsrfCookieName(), csrfCookie);
        cookies.put(this.config.getSessionCookieName(), sessionCookie);

        createRequestSpecificationUsing(cookies);

        getFormDetailsFor(formUrl);

        if (failOnStepCountMismatch) {
            OpenFormsApiDataCompiler.verifyNumberOfSteps(this.formDetailsResponse, formStepData);
        }

        startFormSubmissionFor(formUrl);

        completeFormSteps(formStepData);

        finalizeSubmission();

        deleteSubmissionSession();
    }

    /**
     * Completes a submission for the form that this <code>OpenFormsApiClient</code> is associated with for the user
     * associated with the supplied <code>cookies</code>.
     * @param cookies A map containing all cookies in order to successfully authenticate, prevent cross-site forgery
     * attacks, etc.
     * @param formStepData a list of <code>FormStepData</code> objects to be used when creating the submission.
     */
    public void createSubmission(Map<String, String> cookies, List<FormStepData> formStepData) {

        this.createSubmission(cookies, formStepData, false);
    }

    /**
     * Completes a submission for the form that this <code>OpenFormsApiClient</code> is associated with for the user
     * associated with the supplied <code>cookies</code>.
     * @param cookies A map containing all cookies in order to successfully authenticate, prevent cross-site forgery
     * attacks, etc.
     *
     * @param formStepData            a list of <code>FormStepData</code> objects to be used when creating the submission.
     * @param failOnStepCountMismatch flag indicating whether or not to throw an exception when the supplied number
     *                                of form step data elements does not match the number of steps in the form.
     */
    public void createSubmission(Map<String, String> cookies, List<FormStepData> formStepData, boolean failOnStepCountMismatch) {

        String formUrl = String.format("%s/%s", this.config.getBaseUri(), this.formName);

        LOGGER.info("Initializing submission of form '{}'", this.formName);

        createRequestSpecificationUsing(cookies);

        getFormDetailsFor(formUrl);

        if (failOnStepCountMismatch) {
            OpenFormsApiDataCompiler.verifyNumberOfSteps(this.formDetailsResponse, formStepData);
        }

        startFormSubmissionFor(formUrl);

        completeFormSteps(formStepData);

        finalizeSubmission();

        deleteSubmissionSession();
    }

    private void createRequestSpecificationUsing(Map<String, String> cookies) {

        this.openFormsRequestSpec = new RequestSpecBuilder()
                .setBaseUri(this.config.getBaseUri())
                .setBasePath(this.config.getBasePath())
                .setContentType(ContentType.JSON)
                .addCookies(cookies)
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

    private void completeFormSteps(List<FormStepData> formStepData) {

        LOGGER.info("Compiling form data for form '{}'", this.formName);

        List<FormStep> formSteps = OpenFormsApiDataCompiler.compileDataForFormSteps(this.formDetailsResponse, formStepData);

        LOGGER.info("Submitting form steps using form data...");

        for (FormStep formStep : formSteps) {

            String referer = String.format("%s/%s/stap/%s", this.config.getBaseUri(), this.formName, formStep.getName());
            String formStepEndpoint = String.format("/submissions/%s/steps/%s", this.submissionId, formStep.getUuid());

            this.response = given()
                    .spec(this.openFormsRequestSpec)
                    .header("Referer", referer)
                    .when()
                    .get(formStepEndpoint)
                    .then()
                    .statusCode(200)
                    .extract().response();

            this.csrfToken = this.response.header(this.config.getCsrfHeaderName());

            this.response = given()
                    .spec(this.openFormsRequestSpec)
                    .header("Referer", referer)
                    .header(this.config.getCsrfHeaderName(), this.csrfToken)
                    .body(formStep.getData())
                    .when()
                    .post(String.format("%s/validate", formStepEndpoint))
                    .then()
                    .statusCode(204)
                    .extract().response();

            this.csrfToken = this.response.header(this.config.getCsrfHeaderName());

            this.response = given()
                    .spec(this.openFormsRequestSpec)
                    .header("Referer", referer)
                    .header(this.config.getCsrfHeaderName(), this.csrfToken)
                    .body(formStep.getData())
                    .when()
                    .put(formStepEndpoint)
                    .then()
                    .statusCode(201)
                    .body("slug", equalTo(formStep.getName()))
                    .body("completed", equalTo(true))
                    .extract().response();

            this.csrfToken = this.response.header(this.config.getCsrfHeaderName());
        }
    }

    private void finalizeSubmission() {

        LOGGER.info("Finalizing submission of form '{}'", this.formName);

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
        formData.put("statementOfTruthAccepted", true);

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

        this.csrfToken = this.response.header(this.config.getCsrfHeaderName());

        await()
                .atMost(this.config.getPollingTimeout(), SECONDS)
                .pollInterval(this.config.getPollingInterval(), SECONDS)
                .untilAsserted(() -> this.submissionIsComplete(statusUrl));
    }

    private void deleteSubmissionSession() {

        LOGGER.info("Deleting session for submission with ID {}...", this.submissionId);

        given()
                .spec(this.openFormsRequestSpec)
                .header("Referer", String.format("%s/%s/overzicht", this.config.getBaseUri(), this.formName))
                .header(this.config.getCsrfHeaderName(), this.csrfToken)
                .when()
                .delete(String.format("/authentication/%s/session", this.submissionId))
                .then()
                .statusCode(anyOf(is(204), is(403)));

        LOGGER.info("Deleted session for submission with ID {}", this.submissionId);
    }

    private Properties loadProperties() {

        Properties prop = new Properties();

        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("openforms.properties")) {
            if (stream == null) {
                throw new FileNotFoundException("Could not find file 'openforms.properties' on the classpath.");
            }
            prop.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
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
