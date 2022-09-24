package org.vino9.lib.batchdocreceiver.processor;
/*
 package documents into batch of N
 */

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

    public void pack(Exchange exchange) {
        var payload = (List<Document>) exchange.getIn().getBody();
        log.debug("packer received {} of documents", payload.size());

        var result = payload.stream()
            .map(this::process)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(String::valueOf)
            .collect(Collectors.toList());

        exchange.getMessage().setBody(String.join(",", result));
    }

    private Optional<Long> process(Document doc) {
        var id = doc.getId();

//        // simulate processing error for 1 record
//        if (id == 6L) {
//            return Optional.empty();
//        }
//
        doc.setStatus(Status.PROCESSED);
        doc.setAttempts(doc.getAttempts()+1);
        repo.save(doc);
        log.debug("processed document {}", id);

        return Optional.of(id);
    }

}
