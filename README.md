# qbit
![Build Status](https://travis-ci.com/d-r-q/qbit.svg?branch=master)

**While early stages of development qbit code base has accumulated quite a few mistakes and design flaws, which blocking futher evolution. Currently I'm working on fixing these issues, please check [Clean up](https://github.com/d-r-q/qbit/milestone/5) milestone for current status**

## Vision
qbit is a ACID kotlin-multiplatform embeddable distributed DB with lazy replication and flexible write conflicts resolution toolset.

qbit implements:
 1) Automatic synchronization of user data with cloud, if cloud is presented in the system;
 2) Automatic synchronization of user data between devices, when direct connection between devices is available;
 3) Automatic write conflicts resolution with sane defaults and rich custom policies;
 4) CRDT on DB level;
 5) Encryption of user data with user provided password, so it's is protected from access by third-parties including cloud provider.
 
qbit stores data locally and uses entity graph information model, so for developers it's means that there are no usual persistence points of pain:
 1) There are no more monstrous queries for round-trips minimization, since there is no more round-trips;
 2) There are no more object-relational mappers, since there is no more object-relational mismatch.
 
## Mission

Make internet decentralized again. And make development fun again.

## Roadmap
 * Datastore
   * :white_check_mark: ~Fetch entity by id~
   * :white_check_mark: ~FileSystem storage~
   * :white_check_mark: ~Reference attributes~
   * :white_check_mark: ~Multivalue attributes~
   * Component attributes
   
 * DataBase
   * :white_check_mark: ~Query by attribute value~
   * :white_check_mark: ~Schema~
   * :white_check_mark: ~Unique constraints~
   * :white_check_mark: ~Programmatic range queries~
   * :white_check_mark: ~Pull entites via reference attributes~
   * :white_check_mark: ~Typed entities~
   * :white_check_mark: ~Local ACID transactions~
   * Programmatic joins
   * Query language (Datalog and/or something SQL-like)
   
 * p2p DataBase
   * Concurrent writes support
   * Automatic conflict resolution
   * CRDTs
   * p2p data synchronization
   
 * Cloud platform
   * qbit DBMS
   
## Platforms

 * Supported
   * JVM >= 8
   * Android >= 21
 
 * Planned
   * JS
   * Kotlin/Native
   * iOS
