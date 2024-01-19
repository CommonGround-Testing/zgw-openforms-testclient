package io.github.commonground.testing.data;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpenFormsApiDataCompiler {

    public static void verifyNumberOfSteps(Response response, List<FormStepData> formStepData) {

        JsonPath jsonPath = new JsonPath(response.asString());
        int numberOfStepsInResponse = jsonPath.getInt("steps.size()");
        if (numberOfStepsInResponse != formStepData.size()) {
            throw new RuntimeException(
                    String.format(
                            "Form contains %d steps, but data for %d steps was supplied",
                            numberOfStepsInResponse,
                            formStepData.size()
                    )
            );
        }
    }

    public static List<FormStep> compileDataForFormSteps(Response response, List<FormStepData> formStepData) {

        List<FormStep> steps = new ArrayList<>();

        for(int i = 0; i < formStepData.size(); i++) {
              int j = getStepNumberFromResponse(response, formStepData, i);
                steps.add(
                        new FormStep(
                                response.path(String.format("steps[%d].uuid", j)),
                                response.path(String.format("steps[%d].slug", j)),
                                formStepData.get(i)
                        )
                );

        }

        return steps;
    }

    private static int getStepNumberFromResponse(Response response, List<FormStepData> formStepData, int i) {
        List<HashMap<String, String>> stepsList = ((List<HashMap<String, String>>) response.path("steps"));

        int j=0;

        for (HashMap<String, String> step : stepsList){

            if (step.get("slug").equals(formStepData.get(i).getSlug())) {
                break;
            }
            j++;
        }
        return j;
    }
}
