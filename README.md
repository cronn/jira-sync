[![Build Status](https://travis-ci.org/cronn-de/jira-sync.png?branch=master)](https://travis-ci.org/cronn-de/jira-sync)
[![Apache 2.0](https://img.shields.io/github/license/cronn-de/jira-sync.svg)](http://www.apache.org/licenses/LICENSE-2.0)

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

# Optional
# de.cronn.jira.sync.cache.persistent=true
# de.cronn.jira.sync.cache.directory=cache

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

de.cronn.jira.sync.projects[EX].sourceProject=EXAMPLE
de.cronn.jira.sync.projects[EX].targetProject=EX
de.cronn.jira.sync.projects[EX].sourceFilterId=12345
de.cronn.jira.sync.projects[EX].remoteLinkIconInSource=${de.cronn.jira.sync.source.url}/favicon.ico
de.cronn.jira.sync.projects[EX].remoteLinkIconInTarget=${de.cronn.jira.sync.target.url}/favicon.ico

# Optional
# de.cronn.jira.sync.projects[EX].labelsToKeepInTarget=internal,readyToAssign

de.cronn.jira.sync.projects[EX].transitions[ResolveWhenClosed].sourceStatusIn=Open,Reopened,In Progress
de.cronn.jira.sync.projects[EX].transitions[ResolveWhenClosed].targetStatusIn=Closed
de.cronn.jira.sync.projects[EX].transitions[ResolveWhenClosed].sourceStatusToSet=Resolved
de.cronn.jira.sync.projects[EX].transitions[ResolveWhenClosed].copyResolutionToSource=true
de.cronn.jira.sync.projects[EX].transitions[ResolveWhenClosed].copyFixVersionsToSource=true

de.cronn.jira.sync.projects[EX].transitions[TakeInProgress].sourceStatusIn=Open,Reopened
de.cronn.jira.sync.projects[EX].transitions[TakeInProgress].targetStatusIn=Open,Reopened,Blocked,In Progress,In Review
de.cronn.jira.sync.projects[EX].transitions[TakeInProgress].sourceStatusToSet=In Progress
de.cronn.jira.sync.projects[EX].transitions[TakeInProgress].onlyIfAssignedInTarget=true
de.cronn.jira.sync.projects[EX].transitions[TakeInProgress].assignToMyselfInSource=true

de.cronn.jira.sync.projects[EX].skipUpdateInTargetWhenStatusIn=Resolved,Closed

de.cronn.jira.sync.projects[EX].targetIssueTypeFallback=Task

# cf. https://jira-source/rest/api/2/field and https://jira.target/rest/api/2/field
de.cronn.jira.sync.fieldMapping[Found\ in\ version]=Found in software version

# cf. https://jira-source/rest/api/2/project/EXAMPLE/versions and https://jira.target/rest/api/2/project/EX/versions
de.cronn.jira.sync.projects[EX].versionMapping[10.0]=10
de.cronn.jira.sync.projects[EX].versionMapping[11.0]=11
de.cronn.jira.sync.projects[EX].versionMapping[12.0]=12
de.cronn.jira.sync.projects[EX].versionsToIgnore=Undefined
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
