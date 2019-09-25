#Task 2

##Tests:
1. test /ping method
2. test /authorize method with **valid** credentials
3. test /authorize method with **invalid** credentials (4 data sets)
4. test /api/save_data with **json valid** values payloads (2 data sets)
5. test /api/save_data with **url-encoded valid** values payloads (2 data sets)
6. test /api/save_data with **json invalid** values payloads (2 data sets)
7. test /api/save_data with **url-encoded invalid** values payloads (2 data sets)
8. test access token expiration

##Run:
`mvn clean test -Ddbpath="<path to the sqlite database file>"`
`mvn clean test -Ddbpath="/Users/juliankolodzey/projects/main.db"`

##Reporting:
The test seport should be available at /task2/target/surefire-reports/index.html as soon as the test run will be finished.