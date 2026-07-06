# HMPPS Support for Additional Needs - API

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-support-additional-needs-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-support-additional-needs-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-support-additional-needs-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://san-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs)

Support for additional needs enables staff to better support prisoners with neurodiversity and other additional learning needs.

This API allows for the recording and retrieval of a prisoner's challenges, strengths, conditions and support recommendations.

# Instructions
## Running the application locally

### Preparation
Obtain API client credentials
- populate those value from kubernetes secrets `hmpps-support-additional-needs-api`.
  ```shell
  kubectl -n hmpps-support-additional-needs-dev get secret hmpps-support-additional-needs-api-client-creds -o json | jq '.data | map_values(@base64d)' 
  ```
- fill in the API client credentials in these files: `*_CLIENT_ID` and `*_CLIENT_SECRET`
    - `.env` for running outside docker
    - `.env.docker` for running in docker

---
### Running with docker compose
The easiest way to run the app is to use docker compose to create the service and all dependencies.
1. Prepare `.env.docker` (from `.env.docker.sample`)
    ```shell
    cp .env.docker.sample .env.docker
    ```
    - fill in the API client credentials in `.env.docker`
      see above to obtain these
    - in case of `$` in value, escape them (with `$$`)
2. Then run
   ```shell
   docker compose --profile api up
   ```
   will run the application (from latest image) and PostgreSQL within a local docker instance.
3. Check if application is up and running
    * See `http://localhost:8080/health` to check the app is running.
    * See `http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs` to explore the OpenAPI spec document.
    * See `http://localhost:8080/info` to check the app info

It connects HMPPS Auth and other upstream APIs in `dev` environment. Thus, a set of valid dev API clients are required to run the application.

---
### Running the application in IntelliJ
1. Prepare `.env` (from `.env.local.sample`)
    ```shell
    cp .env.local.sample .env
    ```
    - fill in the API client credentials in `.env`:
      see above to obtain these
2. Run this
    ```shell
   docker compose up -d 
    ```
    * will start dependencies only without the API application
    * `-d` for detached run
3. Run `bootRun` with  `.env` file prepared above
    * either IntelliJ
        - run `bootRun` with `EnvFile` plugin
        - add `.env`
        - enable integrations
    * or Gradle wrapper
      ```shell
      export $(grep -v '^#' .env | xargs)
      ./gradlew bootRun
      ```


## Environment variables
The following environment variables are required in order for the app to start:

### General

| Name           | Description                                |
|----------------|--------------------------------------------|
| SERVER_PORT    | The port that the application will run on  |
| HMPPS_AUTH_URL | The URL for OAuth 2.0 authorisation server |

### Database

| Name      | Description                       |
|-----------|-----------------------------------|
| DB_SERVER | The host of the DB server         |
| DB_NAME   | The name of the database instance |        
| DB_USER   | The application's DB username     |
| DB_PASS   | The DB user's password            |

### DPR
For DPR Digital Prison Reporting

| Name         | Description       |
|--------------|-------------------|
| DPR_USER     | DPR's DB username |
| DPR_PASSWORD | DPR's DB password |

### Application Insights

| Name                                   | Description                              |
|----------------------------------------|------------------------------------------|
| APPLICATIONINSIGHTS_CONNECTION_STRING  | The connection string for App Insights   |
| APPLICATIONINSIGHTS_CONFIGURATION_FILE | A configuration file for App Insights    |

### APIs

| Name                              | Description                                                                 |
|-----------------------------------|-----------------------------------------------------------------------------|
| SERVICE_BASE_URL                  | Base URL of this backend service                                            |
| UI_SERVICE_BASE_URL               | Base URL of the corresponding frontend service                              |
| PRISONER_SEARCH_API_URL           | The URL of the Prisoner Search API                                          |
| PRISONER_SEARCH_API_CLIENT_ID     | hmpps-auth oauth2 client-id for connecting to the Prisoner Search API       |
| PRISONER_SEARCH_API_CLIENT_SECRET | hmpps-auth oauth2 client-secret for connecting to the Prisoner Search API   |
| CURIOUS_API_URL                   | The URL of the Meganexus Curious API                                        |
| CURIOUS_API_CLIENT_ID             | hmpps-auth oauth2 client-id for connecting to the Meganexus Curious API     |
| CURIOUS_API_CLIENT_SECRET         | hmpps-auth oauth2 client-secret for connecting to the Meganexus Curious API |
| MANAGE_USERS_API_URL              | The URL of the HMPPS Manage Users API                                       |
| MANAGE_USERS_API_CLIENT_ID        | hmpps-auth oauth2 client-id for connecting to the Manage Users API          |
| MANAGE_USERS_API_CLIENT_SECRET    | hmpps-auth oauth2 client-secret for connecting to the Manage Users API      |
| BANK_HOLIDAYS_API_URL             | The URL of the Government Bank Holidays API                                 |

### Feature flags or parameters

| Name                         | Description                                        |
|------------------------------|----------------------------------------------------|
| PES_CONTRACT_DATE            | Contract start date of Prisoner Education Services |
| REVIEW_DEADLINE_DAYS_TO_ADD  | Number of days to add for review deadline          |
| ADDITIONAL_REVIEW_DATE_LOGIC | Feature flag: apply additional plan/review logic   |

### More for dev/test or local run

| Name                           | Description                         |
|--------------------------------|-------------------------------------|
| HMPPS_SQS_ENABLED              | Enable or disable SQS queue         |
| HMPPS_SAR_ADDITIONALACCESSROLE | Additional role to test SAR request |

