
# Renjin Release Repository

This repository is a container for (nearly) all projects related to Renjin so that 
they can be built, tested, and developed together quickly.

The structure includes:


`renjin`: The main renjin repo, including the intrepreter and core packages

`packages`: CRAN and BioConductor builds (generated)

`replacement`: Packages that have been replaced for Renjin

`patched`: Packages that have been patched for Renjin

`libstdc++`: Renjin's build of the C++ standard library

`tools`: Java program that downloads package sources, sets up gradle build

## Setup

This repository includes related projects as git submodules. To checkout all the submodules, run:

    git submodule update --init --recursive

To update the submodules to the latest version of a checked out project you can do

    git submodule update --remote
    
See the git [documentation on submodules](https://git-scm.com/docs/git-submodule) for more information.    
    
Currently, in order to compile packages with C/C++/Fortran code, it is necessary to provide the 
absolute path to Renjin's (adapted) R Home directory and GCC plugin. You can add the following lines to your 
~/.gradle/gradle.properties file:

    renjinHomeDir=$REPO_PATH/renjin/tools/gnur-installation/src/main/resources
    gccBridgePlugin=$REPO_PATH/renjin/tools/gcc-bridge/compiler/build/bridge.so
    
   
Where REPO_PATH is the absolute path of the directory to which you have checked out the renjin-release repo. 

As a workaround for Issue #3, you may also need to first build Renjin to ensure that the GCC bridge plugin 
is compiled.

    cd renjin && ./gradlew build

After this, build the libstdc++ project:
    
    cd libstdc++ && ./gradlew build      

## Testing 

The [packages/packages.list] contains a list of CRAN (and eventually BioConductor) packages to build. 
The intention is that this list only includes packages that will work well with Renjin.


To download the included packages and prepare the build, run:

    cd tools && ./gradlew setupPackages

Then an individual package can be built and tested by running:

    cd packages && ./gradlew cran:ada:test

You should be able to make changes to Renjin in ./renjin and rerun the tests.
Gradle will only re-run the neccessary build tasks.

You can build and test the entire suite by running:

    cd packages && ./gradlew test

## Building with vagrant
You need GCC 4.7.4 to build renjin and libstdc++. If you do not have that installed (e.g you are on windows or
a Linux distribution that does not have it) you can use vagrant to build.

`vagrant up`
    /home/vagrant/.gradle/gradle.properties will be created with the proper paths for renjinHomeDir and
    gccBridgePlugin respectively
`vagrant ssh`
`cd /home/ubuntu/renjin-release`

Build the supporting projects
`cd renjin && ./gradlew build && cd ..`
`cd gradle-plugin && ./gradlew build && cd ..`
`cd libstdc++ && ./gradlew build && cd ..`

Set up the packages
`./gradlew tools:setupPackages`
Run a test
`cd packages && ./gradlew cran:digest:test`  

## Patching packages

Sometimes it neccessary to make a small change to a package, often because the package author inadvertendly
depends on an implementation detail of GNU (for [example](https://github.com/bedatadriven/org.renjin.cran.rlang/commit/c1fa55b0c594ba72b0e253efd8b5113cd87c4eb7)).

This is supported by forking the release from https://github.com/cran and making the change in the fork. When possible,
submit a PR to the original package so the change can be incorporated into future versions of the package.

Then add the forked repo as a submodule to this repo. For example:

    cd cran
    rm -rf rlang
    git submodule add -f https://github.com/bedatadriven/rlang


After you've added it a submodule, you'll need to run the `setupPackages` task again:

    cd tools && ./gradlew setupPackages


