package com.siemens.cto.aem.service.configuration.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.siemens.cto.aem.service.configuration.lifecycle.HeartbeatStartupLifecycleListener;

@Configuration
@ImportResource({
    "classpath:META-INF/spring/integration.xml",
    "classpath:META-INF/spring/integration-state.xml",
    "classpath:META-INF/spring/integration-jmx.xml",
    "classpath:META-INF/spring/webserver-heartbeat-integration.xml",
    "classpath:META-INF/spring/jvm-heartbeat-integration.xml",
    "classpath:META-INF/spring/common-heartbeat-dependencies.xml"
})
public class AemIntegrationConfig {

    @Bean
    public HeartbeatStartupLifecycleListener getHeartbeatStartupLifecycleListener() {
        return new HeartbeatStartupLifecycleListener();
    }

}
