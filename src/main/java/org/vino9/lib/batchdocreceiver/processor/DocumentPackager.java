package org.vino9.lib.batchdocreceiver.processor;
/*
 package documents into batch of N
 */

import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.vino9.lib.batchdocreceiver.data.Document;
import org.vino9.lib.batchdocreceiver.data.Document.Status;
import org.vino9.lib.batchdocreceiver.data.DocumentRepository;

@Component
@Slf4j
public class DocumentPackager {

    @Autowired
    private DocumentRepository repo;

    @Value("${batch-doc-processor.output-batch-size:3}")
    int batchSize;

    HashMap<Long, Integer> registry = new HashMap<>();

    public void pack(List<Exchange> exchanges) throws ProcessingError {
        if (isBatchTooBig(exchanges.size())) {
            throw new ProcessingError(
                "received message that contains more than " + batchSize + " documents");
        }

        log.debug("packer received {} of documents", exchanges.size());

        for (var ex : exchanges) {
            var doc = (Document) ex.getIn().getBody();
            if (doc.getId() == 6) {
                throw new ProcessingError("6 is bad!!");
            } else {
                process(doc);
            }
        }
    }

    private void process(Document doc) {
        register(doc);

        doc.setStatus(Status.PROCESSED);
        repo.save(doc);
        log.debug("processed document {}", doc.getId());
    }

    private void register(Document doc) {
        var id = doc.getId();
        var counter = 1;
        if (registry.containsKey(id)) {
            counter = registry.get(id) + 1;
        }
        registry.put(id, counter);
    }

    public void dumpRegistry() {
        log.debug("=====");
        for (var entry : registry.entrySet()) {
            log.debug("{}={}", entry.getKey(), entry.getValue());
        }
        log.debug("=====");
    }

    // make it a standalone method so that we can mock it during testing
    public boolean isBatchTooBig(int size) {
        return size > batchSize;
    }
}
