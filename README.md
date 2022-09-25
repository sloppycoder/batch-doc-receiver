# a batch document processor

Query document of PENDING status from a table, group documents into chunk of N, process them, and update the status.

## Prepare PostgreSQL database
```
create database eipp;
create user svc1 with encrypted password 'svc1';
grant all privileges on database eipp to svc1;

```

### start processor with producer enabled
The producer periodically create random documents that will be processed by the processor
```
java -Dbatch.producer.enabled=true -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar > batch_z.log 
```

### start as many processors as needed
this step is designed to test multiple processor operating on the same table and see if any racing conditions occurs.
``` 
java -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar >  batch_1.log & 
java -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar >  batch_2.log &
java -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar >  batch_3.log &
java -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar >  batch_4.log &

```

### run script to check log files for errors
the script will read output of all processor and print document IDs that appears to more than 1 log
```
watch -n 10 python check.py batch*.log

```