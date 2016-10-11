[![Build Status](https://travis-ci.org/cronn-de/jira-sync.png?branch=master)](https://travis-ci.org/cronn-de/jira-sync)

## Jira-to-Jira Synchronisation

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
de.cronn.jira.source.url=https://jira.source/
de.cronn.jira.target.url=https://jira.target/

de.cronn.jira.source.username=user
de.cronn.jira.source.password=pass

de.cronn.jira.target.username=user
de.cronn.jira.target.password=pass


# Optional
# de.cronn.jira.source.sslTrustStore=file:/path/to/truststore.jks
# de.cronn.jira.source.sslTrustStorePassphrase=secret

# Optional
# de.cronn.jira.target.basicAuth.username=user
# de.cronn.jira.target.basicAuth.password=pass


### General Jira Mappings ###

# cf. https://jira-source/rest/api/2/priority and https://jira-target/rest/api/2/priority
de.cronn.jira.priorityMapping[Highest]=Blocker
de.cronn.jira.priorityMapping[High]=Critical
de.cronn.jira.priorityMapping[Medium]=Major
de.cronn.jira.priorityMapping[Low]=Minor
de.cronn.jira.priorityMapping[Lowest]=Trivial

# cf. https://jira-source/rest/api/2/issue/createmeta and https://jira-target/rest/api/2/issue/createmeta
de.cronn.jira.issueTypeMapping[Bug]=Bug
de.cronn.jira.issueTypeMapping[Improvement]=New Feature
de.cronn.jira.issueTypeMapping[New\ Feature]=New Feature


# cf. https://jira-source/rest/api/2/resolution and https://jira-target/rest/api/2/resolution
de.cronn.jira.resolutionMapping[Fixed]=Fixed
de.cronn.jira.resolutionMapping[Won't\ Fix]=Won't Fix
de.cronn.jira.resolutionMapping[Duplicate]=Duplicate
de.cronn.jira.resolutionMapping[Incomplete]=Incomplete
de.cronn.jira.resolutionMapping[Cannot\ Reproduce]=Cannot Reproduce
de.cronn.jira.resolutionMapping[Done]=Fixed
de.cronn.jira.resolutionMapping[Won't\ Do]=Rejected

### Project Configuration ###

de.cronn.jira.projects[0].sourceProject=EXAMPLE
de.cronn.jira.projects[0].targetProject=EX
de.cronn.jira.projects[0].sourceFilterId=12345
de.cronn.jira.projects[0].remoteLinkIconInSource=https://jira.source/favicon.ico
de.cronn.jira.projects[0].remoteLinkIconInTarget=https://jira.target/favicon.ico

# Optional
# de.cronn.jira.projects[0].labelsToKeepInTarget=internal,readyToAssign

de.cronn.jira.projects[0].statusTransitions[Open,Closed]=Resolved
de.cronn.jira.projects[0].statusTransitions[Reopened,Closed]=Resolved
de.cronn.jira.projects[0].statusTransitions[In\ Progress,Closed]=Resolved

de.cronn.jira.projects[0].targetIssueTypeFallback=Task

# cf. https://jira-source/rest/api/2/project/EXAMPLE/versions and https://jira.target/rest/api/2/project/EX/versions
de.cronn.jira.projects[0].versionMapping[10.0]=10
de.cronn.jira.projects[0].versionMapping[11.0]=11
de.cronn.jira.projects[0].versionMapping[12.0]=12
```


[jira-rest-api]: https://docs.atlassian.com/jira/REST/cloud/
