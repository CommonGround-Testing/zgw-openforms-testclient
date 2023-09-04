package nl.commonground.testing;

import lombok.Getter;

import java.util.Properties;

@Getter
public class OpenFormsApiConfig {

    private final String csrfCookieName;
    private final String csrfHeaderName;
    private final String sessionCookieName;
    private final String baseUri;
    private final String basePath;
    private final int pollingTimeout;
    private final int pollingInterval;

    OpenFormsApiConfig(Properties properties) {
        this.csrfCookieName = properties.getProperty("csrf.cookie.name", "csrftoken");
        this.csrfHeaderName = properties.getProperty("csrf.header.name", "X-CSRFToken");
        this.sessionCookieName = properties.getProperty("session.cookie.name", "openforms_sessionid");
        this.baseUri = properties.getProperty("base.uri", "");
        this.basePath = properties.getProperty("base.path", "api/v2");
        this.pollingTimeout = Integer.parseInt(properties.getProperty("polling.timeout", "120"));
        this.pollingInterval = Integer.parseInt(properties.getProperty("polling.interval", "2"));
    }
}
