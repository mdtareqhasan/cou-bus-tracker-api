package com.cou.bustracker.controller;

import com.cou.bustracker.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthControllerTest {

    /** Mutable stand-in for the DataSource indicator; tests set its status. */
    private static class StubDbIndicator implements HealthIndicator {
        private Health health = Health.up().build();

        @Override
        public Health health() {
            return health;
        }
    }

    private MockMvc mockMvc;
    private DefaultHealthContributorRegistry registry;
    private StubDbIndicator dbIndicator;

    @BeforeEach
    void setUp() {
        dbIndicator = new StubDbIndicator();
        registry = new DefaultHealthContributorRegistry();
        registry.registerContributor("db", dbIndicator);

        HealthEndpointGroup group = new HealthEndpointGroup() {
            @Override
            public boolean isMember(String name) {
                return true;
            }

            @Override
            public boolean showComponents(SecurityContext context) {
                return true;
            }

            @Override
            public boolean showDetails(SecurityContext context) {
                return true;
            }

            @Override
            public StatusAggregator getStatusAggregator() {
                return StatusAggregator.getDefault();
            }

            @Override
            public HttpCodeStatusMapper getHttpCodeStatusMapper() {
                return HttpCodeStatusMapper.DEFAULT;
            }

            @Override
            public AdditionalHealthEndpointPath getAdditionalPath() {
                return null;
            }
        };

        HealthEndpoint healthEndpoint = new HealthEndpoint(
                registry, HealthEndpointGroups.of(group, Map.of()), Duration.ofSeconds(5));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new HealthController(healthEndpoint))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void health_dbUp_returnsOk() throws Exception {
        dbIndicator.health = Health.up().build();

        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.api").value("ok"))
                .andExpect(jsonPath("$.db").value("ok"));
    }

    @Test
    void health_dbDown_returnsServiceUnavailable() throws Exception {
        dbIndicator.health = Health.down().build();

        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.api").value("ok"))
                .andExpect(jsonPath("$.db").value("down"));
    }

    @Test
    void health_dbComponentMissing_returnsServiceUnavailable() throws Exception {
        registry.unregisterContributor("db");

        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.api").value("ok"))
                .andExpect(jsonPath("$.db").value("down"));
    }
}
