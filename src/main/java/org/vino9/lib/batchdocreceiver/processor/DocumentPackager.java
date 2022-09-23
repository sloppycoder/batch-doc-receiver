package org.vino9.lib.batchdocreceiver.processor;
/*
 package documents into batch of N
 */

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.vino9.lib.batchdocreceiver.data.Document;
import org.vino9.lib.batchdocreceiver.data.Document.Status;
import org.vino9.lib.batchdocreceiver.data.DocumentRepository;

@Component
@Slf4j
public class DocumentPackager implements GenericHandler<List<Document>> {

    @Autowired
    private DocumentRepository repo;

    @Value("${batch-doc-processor.output-batch-size:3}")
    int batchSize;

    HashMap<Long, Integer> registry = new HashMap<>();

    @Override
    public Object handle(List<Document> payload, MessageHeaders headers) {
        log.debug("packer received {} of documents", payload.size());

        var result = payload.stream()
            .map(this::process)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        return result;
    }

    private Optional<Long> process(Document doc) {
        var id = doc.getId();
        register(id);

        // simulate processing error for 1 record
        if (id == 6L) {
            return Optional.empty();
        }

        doc.setStatus(Status.PROCESSED);
        repo.save(doc);
        log.debug("processed document {}", doc.getId());

        return Optional.of(id);
    }

    private void register(long id) {
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
