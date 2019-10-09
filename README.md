## Test object deployment
1. install python -> python virtualenv
2. run the following commands to create new python virtual environment -> virtualenv venv
3. install script dependencies venv/bin/pip -> install sqlalchemy bottle
4. create local database structure -> venv/bin/python superservice.py migrate
5. start local web service -> venv/bin/python superservice.py run

## Tests:
1. test /ping method
2. test /authorize method with **valid** credentials
3. test /authorize method with **invalid** credentials (4 data sets)
4. test /api/save_data with **json valid** values payloads (2 data sets)
5. test /api/save_data with **url-encoded valid** values payloads (2 data sets)
6. test /api/save_data with **json invalid** values payloads (2 data sets)
7. test /api/save_data with **url-encoded invalid** values payloads (2 data sets)
8. test access token expiration

## Run:
 `mvn test -Ddbpath="<path to the sqlite database file>"`

## Reporting:
 `mvn io.qameta.allure:allure-maven:serve`
