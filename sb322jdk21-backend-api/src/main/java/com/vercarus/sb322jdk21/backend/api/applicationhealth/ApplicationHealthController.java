package com.vercarus.sb322jdk21.backend.api.applicationhealth;

import com.vercarus.sb322jdk21.backend.integration.core.fiber.FiberHttpServlet;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;

@RestController
@RequestMapping("/applicationhealth")
public class ApplicationHealthController {
    @Autowired
    MetricsEndpoint metricsEndpoint;

    private static final Double bytesInOneMb = 1048576.0;

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/getStatus"
    )
    public ApplicationHealthResponse getStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
        FiberHttpServlet.serve(request, response, this, "getStatusImpl",
                FiberHttpServlet.getParameterNames(),
                FiberHttpServlet.getParameterArray(),
                null, null, null,
                true);
        return null;
    }
    public ApplicationHealthResponse getStatusImpl() throws Throwable {
        ApplicationHealthResponse response = new ApplicationHealthResponse();
        response.setStatus("UP");

        MetricsEndpoint.MetricDescriptor metricsResponse = metricsEndpoint.metric("jvm.memory.used", null);
        Double jvm_memory_used_bytes = metricsResponse.getMeasurements().get(0).getValue();
        response.getDetails().put("jvm.memory.used(MB)", (jvm_memory_used_bytes / bytesInOneMb));

        metricsResponse = metricsEndpoint.metric("jvm.memory.max", null);
        Double jvm_memory_max_bytes = metricsResponse.getMeasurements().get(0).getValue();
        response.getDetails().put("jvm.memory.max(MB)", (jvm_memory_max_bytes / bytesInOneMb));

        assignMetrics(response, "jvm.threads.live");
        response.getDetails().put("jvm.fibers.live", FiberHttpServlet.fiberHttpServletTracker.size());
        assignMetrics(response, "system.cpu.count");
        assignMetrics(response, "system.cpu.usage");
        assignMetrics(response, "hikaricp.connections.active");
        assignMetrics(response, "hikaricp.connections.idle");
        assignMetrics(response, "hikaricp.connections.pending");
        assignMetrics(response, "hikaricp.connections.max");
        assignMetrics(response, "tomcat.servlet.request");
        assignMetrics(response, "tomcat.servlet.request.max");
        assignMetrics(response, "tomcat.threads.current");
        assignMetrics(response, "tomcat.threads.config.max");
        response.getDetails().put("datasource.fiber.activeConnection", String.valueOf(CoreConfig.fiberConnectionActiveTracer.size()));
        response.getDetails().put("datasource.fiber.queuedConnection", String.valueOf(CoreConfig.fiberConnectionQueuedTracer.size()));

        for (int count = 0; count < CoreConfig.fiberConnectionActiveTracer_readerList.size(); count++) {
            response.getDetails().put("datasource.fiber.activeConnection.reader." + (count + 1), String.valueOf(CoreConfig.fiberConnectionActiveTracer_readerList.get(count).size()));
        }
        if (CoreConfig.fiberConnectionActiveTracer_readerList.size() > 0) {
            response.getDetails().put("datasource.fiber.queuedConnection.reader", String.valueOf(CoreConfig.fiberConnectionQueuedTracer_readerList.size()));
        }
        Connection datasourceConnection = null;
        try {
            datasourceConnection = CoreConfig.fiberConnectionGet();
            response.getDetails().put("datasource.health", "OK");
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            response.getDetails().put("datasource.health", "CLOGGED");
            response.getDetails().put("datasource.health.stackTrace", sw.toString());
            response.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            t.printStackTrace();
        } finally {
            if (datasourceConnection != null) {
                CoreConfig.fiberConnectionClose(datasourceConnection);
            }
        }
        return response;
    }

    private void assignMetrics(ApplicationHealthResponse response, String metricName) {
        MetricsEndpoint.MetricDescriptor metricsResponse = metricsEndpoint.metric(metricName, null);
        Double metricValue = null;
        try {
            metricValue = metricsResponse.getMeasurements().get(0).getValue();
        } catch (Exception e) {
            // do nothing, put null value into the metric
        }
        response.getDetails().put(metricName, metricValue);
    }
}
