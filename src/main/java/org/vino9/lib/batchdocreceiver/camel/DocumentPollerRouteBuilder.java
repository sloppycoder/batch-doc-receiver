package org.vino9.lib.batchdocreceiver.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.vino9.lib.batchdocreceiver.data.Document;
import org.vino9.lib.batchdocreceiver.processor.DocumentPackager;

@Component
public class DocumentPollerRouteBuilder extends RouteBuilder {

    @Autowired
    private DocumentPackager packer;

    @Value("${batch-doc-processor.output-batch-size:3}")
    int batchSize;

    @Override
    public void configure() {
        String options = String.join("&", new String[]{
            "namedQuery=Document.findPendingDocuments",
            "maximumResults=" + batchSize,
            "delay=5000",
            "consumeDelete=false",
            "initialDelay=1000",
            "transacted=true",
            "joinTransaction=true"
        });

        log.debug("options: {}", options);

        from(String.format("jpa:%s?%s", Document.class.getCanonicalName(), options))
            .routeId("eipp-batch-doc-receiver")
            .aggregate(new GroupedExchangeAggregationStrategy()).constant(true)
            .completionSize(batchSize)
            .completionTimeout(1000L)
            .bean(packer, "pack")
            //.bean(packer, "dumpRegistry")
            .end();
    }
}
