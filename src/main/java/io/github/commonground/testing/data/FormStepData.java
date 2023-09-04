package io.github.commonground.testing.data;

import lombok.Data;

import java.util.HashMap;

@Data
public class FormStepData {

    private static final String DEFAULT_STATE = "submitted";

    public final HashMap<String, Object> data;
    public final HashMap<String, Object> metadata;
    public final String state;

    public FormStepData(HashMap<String, Object> data) {
        this(data, new HashMap<String, Object>(), DEFAULT_STATE);
    }

    public FormStepData(HashMap<String, Object> data, HashMap<String, Object> metadata) {
        this(data, metadata, DEFAULT_STATE);
    }

    public FormStepData(HashMap<String, Object> data, HashMap<String, Object> metadata, String state) {
        this.data = data;
        this.metadata = metadata;
        this.state = state;
    }
}
