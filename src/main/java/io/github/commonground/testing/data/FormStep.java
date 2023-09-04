package io.github.commonground.testing.data;

public class FormStep {

    private final String uuid;
    private final String name;
    private final FormStepData data;

    FormStep(String uuid, String name, FormStepData data) {
        this.uuid = uuid;
        this.name = name;
        this.data = data;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public FormStepData getData() {
        return data;
    }
}
