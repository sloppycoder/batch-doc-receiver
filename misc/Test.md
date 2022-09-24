## steps to test

### start as many consumer as needed 
``` 
java -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar >  batch_1.log & 
java -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar >  batch_2.log &
java -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar >  batch_3.log &
java -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar >  batch_4.log &

```

### start another consumer with producer enabled 
```
java -Dbatch.producer.enabled=true -jar target/batch-doc-receiver-0.0.1-SNAPSHOT.jar > batch_z.log 
```


### run script to check log files for errors
```
watch -n 10 python check.py batch*.log

```