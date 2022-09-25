# a batch document processor

Query presentment of PENDING status from a table, group documents into chunk of N, process them, and update the status.

## Prepare PostgreSQL database
```
create database eipp;
create user svc1 with encrypted password 'svc1';
grant all privileges on database eipp to svc1;

```

### start processor with producer enabled
The producer periodically create random documents that will be processed by the processor
```
# add -Dspring.config.location=file:application.yml if using alternative config file
java -Dbatch.producer.enabled=true -jar target/presentment-sender-0.0.1-SNAPSHOT.jar 
```

### start as many processors as needed
this step is designed to test multiple processor operating on the same table and see if any racing conditions occurs.
``` 
java -jar target/presentment-sender-0.0.1-SNAPSHOT.jar 

```

### run script to check log files for errors
the script will read output of all processor and print document IDs that appears to more than 1 log
this utility is out-of-date, needs to be updated 
```
watch -n 10 python check.py batch*.log

```