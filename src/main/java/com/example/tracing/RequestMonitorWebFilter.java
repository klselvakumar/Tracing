package com.example.tracing;


import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import reactor.core.publisher.Mono;

@Component
public class RequestMonitorWebFilter implements WebFilter {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    	MDC.put("uuid", exchange.getRequest().getHeaders().getFirst("uuid"));
        long startTime = System.currentTimeMillis();
        return chain.filter(exchange)
                /** !! IMPORTANT STEP !!
                 * Preparing context for the Tracer Span used in TracerConfiguration
                 */
                .contextWrite(context -> {
                	ContextSnapshotFactory.builder().build().setThreadLocalsFrom(context, ObservationThreadLocalAccessor.KEY);
                    return context;
                })
                    /**
                     * Logging the metrics for the API call, not really required to have this section for tracing setup
                     */
                .doFinally(signalType -> {
                    long endTime = System.currentTimeMillis();
                    long executionTime = endTime - startTime;
                    /**
                     * Extracting traceId added in TracerConfiguration Webfilter bean
                     */
                    List<String> traceIds = Optional.ofNullable(exchange.getResponse().getHeaders().get("traceId")).orElse(List.of());
                    MetricsLogTemplate metricsLogTemplate = new MetricsLogTemplate(
                            String.join(",", traceIds),
                            exchange.getLogPrefix().trim(),
                            executionTime
                    );
                    try {
                    	logger.info(new ObjectMapper().writeValueAsString(metricsLogTemplate));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    record MetricsLogTemplate(String traceId, String logPrefix, long executionTime){}
}
