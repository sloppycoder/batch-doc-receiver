package org.vino9.lib.batchdocreceiver.config;

import javax.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.jpa.dsl.Jpa;
import org.vino9.lib.batchdocreceiver.data.Document;
import org.vino9.lib.batchdocreceiver.processor.DocumentPackager;

@Configuration
@EnableIntegration
public class DocumentPollerFlowBuilder {

    @Autowired
    private EntityManagerFactory em;

    @Autowired
    private DocumentPackager packager;

    @Value("${batch-doc-processor.output-batch-size:3}")
    int batchSize;

    @Bean
    public IntegrationFlow batchDocReceiverFlow() {
        return IntegrationFlows
            .from(Jpa.inboundAdapter(em)
                     .entityClass(Document.class)
                     .namedQuery("Document.findPendingDocuments")
                     .maxResults(batchSize)
                     .expectSingleResult(false),
                  c -> c.poller(Pollers.fixedRate(5000)))
            //.aggregate(a -> a.releaseExpression("size() > " + batchSize))
            .handle(packager)
            .log()
            .get();
    }
}