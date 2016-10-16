[![Build Status](https://travis-ci.org/cronn-de/jira-sync.png?branch=master)](https://travis-ci.org/cronn-de/jira-sync)

## Jira-to-Jira Synchronisation

Asynchronous synchronisation of two Jira instances implemented with [Spring Boot][spring-boot].

                                                   ·----·
       _ _                                         | cr |
      (_|_)_ __ __ _      ___ _   _ _ __   ___     | nn | cronn
      | | | '__/ _` |____/ __| | | | '_ \ / __|    ·----·
      | | | | | (_| |____\__ \ |_| | | | | (__
     _/ |_|_|  \__,_|    |___/\__, |_| |_|\___|
    |__/                      |___/


### Prerequisites

- Java 8

- Access to [Jira REST API][jira-rest-api]

### Building

On Unix:

```
./gradlew build
```

On Windows:


```
gradlew.bat build
```


### Example Configuration

`config/application.properties`

```properties
de.cronn.jira.sync.source.url=https://jira.source/
de.cronn.jira.sync.target.url=https://jira.target/

de.cronn.jira.sync.source.username=user
de.cronn.jira.sync.source.password=pass

de.cronn.jira.sync.target.username=user
de.cronn.jira.sync.target.password=pass


# Optional
# de.cronn.jira.sync.source.sslTrustStore=file:/path/to/truststore.jks
# de.cronn.jira.sync.source.sslTrustStorePassphrase=secret

# Optional
# de.cronn.jira.sync.target.basicAuth.username=user
# de.cronn.jira.sync.target.basicAuth.password=pass


### General Jira Mappings ###

# cf. https://jira-source/rest/api/2/priority and https://jira-target/rest/api/2/priority
de.cronn.jira.sync.priorityMapping[Highest]=Blocker
de.cronn.jira.sync.priorityMapping[High]=Critical
de.cronn.jira.sync.priorityMapping[Medium]=Major
de.cronn.jira.sync.priorityMapping[Low]=Minor
de.cronn.jira.sync.priorityMapping[Lowest]=Trivial

# cf. https://jira-source/rest/api/2/issue/createmeta and https://jira-target/rest/api/2/issue/createmeta
de.cronn.jira.sync.issueTypeMapping[Bug]=Bug
de.cronn.jira.sync.issueTypeMapping[Improvement]=New Feature
de.cronn.jira.sync.issueTypeMapping[New\ Feature]=New Feature


# cf. https://jira-source/rest/api/2/resolution and https://jira-target/rest/api/2/resolution
de.cronn.jira.sync.resolutionMapping[Fixed]=Fixed
de.cronn.jira.sync.resolutionMapping[Won't\ Fix]=Won't Fix
de.cronn.jira.sync.resolutionMapping[Duplicate]=Duplicate
de.cronn.jira.sync.resolutionMapping[Incomplete]=Incomplete
de.cronn.jira.sync.resolutionMapping[Cannot\ Reproduce]=Cannot Reproduce

### Project Configuration ###

de.cronn.jira.sync.projects[0].sourceProject=EXAMPLE
de.cronn.jira.sync.projects[0].targetProject=EX
de.cronn.jira.sync.projects[0].sourceFilterId=12345
de.cronn.jira.sync.projects[0].remoteLinkIconInSource=${de.cronn.jira.sync.source.url}/favicon.ico
de.cronn.jira.sync.projects[0].remoteLinkIconInTarget=${de.cronn.jira.sync.target.url}/favicon.ico

# Optional
# de.cronn.jira.sync.projects[0].labelsToKeepInTarget=internal,readyToAssign

de.cronn.jira.sync.projects[0].transitions[0].sourceStatusIn=Open,Reopened,In Progress
de.cronn.jira.sync.projects[0].transitions[0].targetStatusIn=Closed
de.cronn.jira.sync.projects[0].transitions[0].sourceStatusToSet=Resolved
de.cronn.jira.sync.projects[0].transitions[0].copyResolutionToSource=true
de.cronn.jira.sync.projects[0].transitions[0].copyFixVersionsToSource=true

de.cronn.jira.sync.projects[0].transitions[1].sourceStatusIn=Open,Reopened
de.cronn.jira.sync.projects[0].transitions[1].targetStatusIn=Open,Reopened,Blocked,In Progress,In Review
de.cronn.jira.sync.projects[0].transitions[1].sourceStatusToSet=In Progress
de.cronn.jira.sync.projects[0].transitions[1].onlyIfAssignedInTarget=true
de.cronn.jira.sync.projects[0].transitions[1].assignToMyselfInSource=true

de.cronn.jira.sync.projects[0].targetIssueTypeFallback=Task

# cf. https://jira-source/rest/api/2/project/EXAMPLE/versions and https://jira.target/rest/api/2/project/EX/versions
de.cronn.jira.sync.projects[0].versionMapping[10.0]=10
de.cronn.jira.sync.projects[0].versionMapping[11.0]=11
de.cronn.jira.sync.projects[0].versionMapping[12.0]=12
```

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


[spring-boot]: https://projects.spring.io/spring-boot/
[jira-rest-api]: https://docs.atlassian.com/jira/REST/cloud/
[spring-fat-jar]: http://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html
