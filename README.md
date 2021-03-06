# qbit
![Build Status](https://travis-ci.com/d-r-q/qbit.svg?branch=master)

**While early stages of development qbit code base has accumulated quite a few mistakes and design flaws, which are blocking futher evolution.**

**I have made qbit truly multiplafrom and able to pass all the tests on native/linux and js/node platforms (see [Make qbit truly multiplatform](https://github.com/d-r-q/qbit/projects/1)).**

**The next goal is to [clean up](https://github.com/d-r-q/qbit/projects/2) major design flaws and API. [New information model and sync concept (on russian)](https://github.com/d-r-q/qbit/wiki/qbit-v4-information-model-and-sync-concept-(rus))**

## Vision
qbit is a research project aiming at creating distributed high availablity storage technology for Kotlin Multiplatform projects.

qbit will store an encrypted full copy of end user's data on his devices and use existing cloud storages only as media to synchronize the data between devices. Particular cloud storages would be easily replacable while life time of qbit-based system installation on end user's devices.

## Mission

Return the control of end user's data back to end users. And put the fun back into persisting.
   
## Platforms

 * Supported
   * JVM >= 8
   * Android >= 21
   * JS/Node
   * Native/Linux

 * Prospective
   * JS/Browser
   * Native/iOS
   * Native/Windows
   * Native/MacOS
