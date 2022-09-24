package org.vino9.lib.batchdocreceiver.processor;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.vino9.lib.batchdocreceiver.data.Document;
import org.vino9.lib.batchdocreceiver.data.Document.Status;

@Component
public class RandomDocProducer {
    private int counter = 0;
    public void produce(Exchange exchange) {
        counter++;

        var docName = String.format("doc_%-5d", counter);
        var doc = new Document();
        doc.setName(docName);
        doc.setPath("/random_path/tmp/" + docName);
        doc.setAttempts(0);
        doc.setStatus(Status.PENDING);

        exchange.getMessage().setBody(doc);
    }
}
