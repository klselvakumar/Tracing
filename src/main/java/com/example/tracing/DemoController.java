package com.example.tracing;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebFilter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@RestController
public class DemoController {

	private static AtomicInteger atomicInteger = new AtomicInteger();

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@GetMapping("/tracing/demo")
	public String helloSleuth() throws InterruptedException {
		int i = atomicInteger.getAndIncrement();
		logger.info("Before Sleep {}", i);
		// Thread.sleep(5000);
		logger.info("After Sleep {}", i);
		return "success";
	}

	@Bean
	WebFilter traceIdInResponseFilter(Tracer tracer) {
		return (exchange, chain) -> {
			Span currentSpan = tracer.currentSpan();
			if (currentSpan != null) {
				// putting trace id value in [traceId] response header
				exchange.getResponse().getHeaders().add("traceId", currentSpan.context().traceId());
			}
			return chain.filter(exchange);
		};
	}

}
