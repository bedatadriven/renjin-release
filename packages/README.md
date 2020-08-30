# Testsuite file syntax
A profile list is a text file whose name ends with `.testsuite` and starts with the testsuite name.

The testsuite file contains names of packages (submodules) that should be included and also
possible waivers for test failures.

The syntax is as follows:

- use # for comments
- specify the package name directly e.g. `CDM` 
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

### Running the smoketest test suite
The smoketest testsuite includes a small number of packages and their test waivers
that can be run in short amount of time.

`./gradlew -Ptestsuite=smoketest checkTests`

### Force rerun of a test

`./gradlew --rerun-tasks -a test`

Note: `-a` is to not rebuild project dependencies (renjin etc.)

e.g.
`./gradlew --rerun-tasks -a cran:date:test`
