package org.vino9.eipp.config;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedBodyAggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.vino9.eipp.data.Presentment;
import org.vino9.eipp.misc.RandomDocProducer;
import org.vino9.eipp.processor.PresentmentPackager;

@Component
public class PresentmentPollerFlowBuilder extends RouteBuilder {

    @Autowired
    private PresentmentPackager packer;

    @Autowired
    private RandomDocProducer docProducer;

    @Value("${presentment-sender.output-batch-size:3}")
    int batchSize;

    @Value("${presentment-sender.poll-delay:5000}")
    String pollDelay;

    @Value("${batch.producer.enabled:false}")
    boolean enableProducer;

    @Override
    public void configure() {
        String options = String.join("&", new String[]{
            "namedQuery=Presentment.findPendingItems",
            "maximumResults=" + batchSize,
            "delay=" + pollDelay,
            "consumeDelete=false",
            "joinTransaction=true"
        });

        log.debug("options: {}", options);

        from(String.format("jpa:%s?%s", Presentment.class.getCanonicalName(), options))
            .routeId("eipp-batch-doc-receiver")
            .aggregate(new GroupedBodyAggregationStrategy()).constant(true)
            .completionSize(batchSize)
            .completionTimeout(1000L)
            .bean(packer, "pack")
            .log("#produced# [${body}]")
            .end();

        if (enableProducer) {
            // randomly produce documents to be consumed by other pollers
            from("timer://docProducer?repeatCount=1000")
                .id("random document producer")
                .delay(simple("${random(2000,10000)}"))
                .loop().expression(simple("${random(1,15)}"))
                .bean(docProducer, "produce")
                .log("${body}")
                .to(String.format("jpa:%s", Presentment.class.getCanonicalName()));
        }
    }
}
