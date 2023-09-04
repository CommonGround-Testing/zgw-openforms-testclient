package io.github.commonground.testing.data;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;

public class OpenFormsApiDataCompiler {

    public static List<FormStep> compileDataForFormSteps(Response response, List<FormStepData> formStepData) {

        JsonPath jsonPath = new JsonPath(response.asString());
        int numberOfStepsInResponse = jsonPath.getInt("steps.uuid.size()");

        if (numberOfStepsInResponse != formStepData.size()) {
            throw new RuntimeException(
                    String.format(
                            "Form contains %d steps, but data for %d steps was supplied",
                            numberOfStepsInResponse,
                            formStepData.size()
                    )
            );
        }

        List<FormStep> steps = new ArrayList<>();

        for(int i = 0; i < formStepData.size(); i++) {
            steps.add(
                    new FormStep(
                            response.path(String.format("steps[%d].uuid", i)),
                            response.path(String.format("steps[%d].slug", i)),
                            formStepData.get(i)
                    )
            );
        }

        return steps;
    }
}
