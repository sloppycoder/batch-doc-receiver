package org.vino9.eipp.processor;
/*
 package documents into batch of N
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.vino9.eipp.data.Presentment;
import org.vino9.eipp.data.Presentment.Status;
import org.vino9.eipp.data.PresentmentRepository;

@Component
@Slf4j
public class PresentmentPackager {

    @Autowired
    private PresentmentRepository repo;

    private XmlMapper mapper = new XmlMapper();

    @Value("${presentment-sender.output-batch-size:3}")
    int batchSize;

    HashMap<Long, Integer> registry = new HashMap<>();

    public void pack(Exchange exchange) {
        var payload = (List<Presentment>) exchange.getIn().getBody();
        log.debug("packer received {} of documents", payload.size());

        var result = payload.stream()
            .map(this::process)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(String::valueOf)
            .collect(Collectors.toList());

        exchange.getMessage().setBody(String.join(",", result));
    }

    private Optional<Long> process(Presentment doc) {
        var id = doc.getId();

        try {
            log.debug("doc[{}]->{}", id, mapper.writeValueAsString(doc));
        } catch (JsonProcessingException e) {
            log.debug("unable to convert doc[{}] to XML. {}", id, e);
        }
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
