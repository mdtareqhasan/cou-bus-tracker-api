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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real Actuator wiring the controller depends on: a
 * {@link HealthEndpoint} backed by a registry where "db" is the DataSource
 * indicator. Deliberately avoids Mockito's class mocking — ByteBuddy (via
 * Boot 3.3.2's Mockito) cannot instrument HealthEndpoint on newer JDKs.
 */
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

        // Minimal permissive group: membership/details always on, default
        // aggregators — everything a plain status probe needs.
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

        HealthController controller = new HealthController(healthEndpoint);
        // @Value fields are not injected in standalone setup.
        ReflectionTestUtils.setField(controller, "adminPanelUrl", "https://admin.kubijatra.com");
        ReflectionTestUtils.setField(controller, "superAdminPanelUrl", "https://super.kubijatra.com");
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void health_dbUp_returnsOkWithStatusAndPanelLinks() throws Exception {
        dbIndicator.health = Health.up().build();

        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.api").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.adminPanel").value("https://admin.kubijatra.com"))
                .andExpect(jsonPath("$.superAdminPanel").value("https://super.kubijatra.com"));
    }

    @Test
    void health_dbDown_returnsServiceUnavailableWithDownBody() throws Exception {
        dbIndicator.health = Health.down().build();

        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("DOWN"))
                // Links must survive a DB outage — they are static config.
                .andExpect(jsonPath("$.adminPanel").value("https://admin.kubijatra.com"));
    }

    @Test
    void health_dbComponentMissing_returnsServiceUnavailableWithUnknown() throws Exception {
        registry.unregisterContributor("db");

        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("UNKNOWN"))
                .andExpect(jsonPath("$.api").value("UP"));
    }
}
