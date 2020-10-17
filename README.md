[![Build Status](https://travis-ci.org/cronn/jira-sync.png?branch=master)](https://travis-ci.org/cronn/jira-sync)
[![Apache 2.0](https://img.shields.io/github/license/cronn/jira-sync.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Coverage Status](https://coveralls.io/repos/github/cronn/jira-sync/badge.svg?branch=master)](https://coveralls.io/github/cronn/jira-sync?branch=master)

## Jira-to-Jira Synchronisation

Asynchronous synchronisation of two Jira instances implemented with [Spring Boot][spring-boot].

                                                   ╭────╮
       _ _                                         │ cr │
      (_|_)_ __ __ _      ___ _   _ _ __   ___     │ nn │ cronn
      | | | '__/ _` |____/ __| | | | '_ \ / __|    ╰────╯
      | | | | | (_| |____\__ \ |_| | | | | (__
     _/ |_|_|  \__,_|    |___/\__, |_| |_|\___|
    |__/                      |___/


### Prerequisites

- Java 8

- Access to [Jira REST API][jira-rest-api]


### Running

On Linux:

#### Option 1

```
./gradlew bootRun -Dspring.config.location=file:/path/to/config/
```

#### Option 2

```
./gradlew assemble
```

Execute the ['fat' JAR][spring-fat-jar]:
```
build/libs/jira-sync-1.0.jar --spring.config.location=file:/path/to/config/
```


### Configuration

`config/application.yml`

```yaml
de.cronn.jira.sync.source:
  url: https://jira.source/
  username: user
  password: pass
  # Optional
  # sslTrustStore: file:/path/to/truststore.jks
  # sslTrustStorePassphrase: secret

de.cronn.jira.sync.target:
  url: https://jira.target/
  username: user
  password: pass
  # Optional
  # basicAuth:
  #   username: user
  #   password: pass

# Optional
# de.cronn.jira.sync.cache:
#   persistent: true
#   directory: cache

### General Jira Mappings ###

# cf. https://jira-source/rest/api/2/priority and https://jira-target/rest/api/2/priority
de.cronn.jira.sync.priorityMapping:
  Highest: Blocker
  High: Critical
  Medium: Major
  Low: Minor
  Lowest: Trivial

# cf. https://jira-source/rest/api/2/issue/createmeta and https://jira-target/rest/api/2/issue/createmeta
de.cronn.jira.sync.issueTypeMapping:
  Bug: Bug
  Improvement: New Feature
  New Feature: New Feature

# cf. https://jira-source/rest/api/2/resolution and https://jira-target/rest/api/2/resolution
de.cronn.jira.sync.resolutionMapping:
  Fixed: Fixed
  Won't Fix: Won't Fix
  Duplicate: Duplicate
  Incomplete: Incomplete
  Cannot Reproduce: Cannot Reproduce

# cf. https://jira-source/rest/api/2/field and https://jira.target/rest/api/2/field
de.cronn.jira.sync.fieldMapping:
  Found in version: Found in software version

### Project Configuration ###

de.cronn.jira.sync.projects[EX]:
  sourceProject: EXAMPLE
  targetProject: EX
  sourceFilterIds: 12345
  remoteLinkIconInSource: ${de.cronn.jira.sync.source.url}/favicon.ico
  remoteLinkIconInTarget: ${de.cronn.jira.sync.target.url}/favicon.ico

  # Optional
  # labelsToKeepInTarget=internal, readyToAssign

  transitions:
    ResolveInSourceWhenClosedInTarget:
      sourceStatusIn: Open, In Progress
      targetStatusIn: Closed
      sourceStatusToSet: Resolved
      copyResolutionToSource: true
      copyFixVersionsToSource: true
      customFieldsToCopyFromTargetToSource[Bug]:
        - field-name-in-source: field-name-in-target

    TakeInProgressInSource:
      sourceStatusIn: Open
      targetStatusIn: Open, Blocked, In Progress, In Review
      sourceStatusToSet: In Progress
      onlyIfAssignedInTarget: true
      assignToMyselfInSource: true

    ReopenInTarget:
      sourceStatusIn: Reopened
      targetStatusIn: Resolved, Closed
      targetStatusToSet: Reopened
      onlyIfStatusTransitionNewerIn: SOURCE

  # Optional mapping of (custom) field values
  fieldValueMappings[field-name-in-source]:
    source-value-1: target_value_1
    source-value-2: target_value_2
    source-value-3: target_value_3

  skipUpdateInTargetWhenStatusIn: Resolved, Closed

  targetIssueTypeFallback: Task

  # cf. https://jira-source/rest/api/2/project/EXAMPLE/versions and https://jira.target/rest/api/2/project/EX/versions
  versionMapping:
    10.0: 10
    11.0: 11
    12.0: 12

  versionsToIgnore: Undefined
```


### Building

On Unix:

```
./gradlew build
```

On Windows:

```
gradlew.bat build
```


[spring-boot]: https://projects.spring.io/spring-boot/
[jira-rest-api]: https://docs.atlassian.com/jira/REST/cloud/
[spring-fat-jar]: http://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html
