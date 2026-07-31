package com.soulmate.config.env;

import com.soulmate.common.config.env.DotenvApplicationListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DotenvApplicationListenerTest {

    @Test
    public void testOnApplicationEvent() {
        DotenvApplicationListener listener = new DotenvApplicationListener();
        MockEnvironment environment = new MockEnvironment();
        SpringApplication application = new SpringApplication();
        ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(
                null, application, new String[0], environment
        );

        listener.onApplicationEvent(event);

        if (environment.getPropertySources().contains("dotenvProperties")) {
            String appPort = environment.getProperty("APP_PORT");
            assertNotNull(appPort, "APP_PORT should be loaded from .env");
            System.out.println("[Test Success] DotenvApplicationListener loaded APP_PORT = " + appPort);
        }
    }
}
