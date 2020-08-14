
# Renjin Release Repository

This repository is a container for (nearly) all projects related to Renjin so that 
they can be built, tested, and developed together quickly.

The structure includes:


`renjin`: The main renjin repo, including the intrepreter and core packages

`ci`: Continuous integration system

`packages`: CRAN and BioConductor builds (generated)

`replacement`: Packages that have been replaced for Renjin

`patched`: Packages that have been patched for Renjin

`libstdc++`: Renjin's build of the C++ standard library


## Testing 

The [packages/packages.list] contains a list of CRAN (and eventually BioConductor) packages to build. 
The intention is that this list only includes packages that 


To download the included packages and prepare the build, run:

    cd tools && ./gradlew setupPackages


Then an individual package can be built and tested by running:

    cd packages && ./gradlew cran:ada:test

You should be able to make changes to Renjin in ./renjin and rerun the tests.
Gradle will only re-run the neccessary build tasks.

You can build and test the entire suite by running:

    cd packages && ./gradlew cran:ada:test








