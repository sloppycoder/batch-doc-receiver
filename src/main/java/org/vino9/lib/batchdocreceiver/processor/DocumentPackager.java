package org.vino9.lib.batchdocreceiver.processor;
/*
 package documents into batch of N
 */

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.vino9.lib.batchdocreceiver.entity.Document;

@Component
@Slf4j
public class DocumentPackager {

    @Value("${batch-doc-processor.output-batch-size:3}")
    int batchSize;

    public void pack(List<Exchange> docs) throws ProcessingError {
        if (isBatchTooBig(docs.size())) {
            throw new ProcessingError(
                "received message that contains more than " + batchSize + " documents");
        }

        log.debug("packer received {} of documents", docs.size());
        docs.forEach(
            ex -> {
                var doc = (Document) ex.getIn().getBody();
                doc.markProcessed();
                log.debug("processed document {}", doc.getId());
            }
        );

    }

    // make it a standalone method so that we can mock it during testing
    public boolean isBatchTooBig(int size) {
        return size > batchSize;
    }
}
