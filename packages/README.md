# profile file syntax
A profile list is a text file whose name begins with `packages.` and ends with the profile name.
The profile list contains names of packages (submodules) that should be included and also
possible waivers for test failures.
The syntax is as follows:

- use # for comments
- specify the package name directly e.g. `CDM` or as group:name:version e.g. `org.renjin.cran:CDM:7.3-17`
- optionally add waivers to as new lines under the package. A waver is specified by
    starting the line with `-` followed by the name of the test that should be waived
    after the name of the test you can optionally add a reason starting with `:`
    Here is an example
```
# This is the list of test that must pass before a release is made
date
digest
- sha1Test.R: floating point values are different, but within tolerances
MASS
```

### Running the mustpass tests
`./gradlew -Dprofile=mustpass runTests`

### Checking the test results of the mustpass tests
`./gradlew -Dprofile=mustpass checkTests`
This task depends on the reunTests task so there is no need to first run the runTests task
and then the checkTests task

### Force rerun of a test
`./gradlew --rerun-tasks -a test`
Note: `-a` is to not rebuild project dependencies (renjin etc.)

e.g.
`./gradlew --rerun-tasks -a cran:date:test`