# qbit
qbit is embeddable decentralized DB with automatic concurrent writes resolution. Heavily inspired by [Datomic](https://www.datomic.com/)

## Vision

qbit - it's next gen cloud development platform. qbit implements:
 1) Automatic synchronization of user data with cloud, if cloud is presented in the system;
 2) Automatic synchronization of user data between devices, when direct connection between devices is available;
 3) Automatic concurrent writes conflicts resolution with sane defaults and rich custom policies;
 4) CRDT on DB level;
 5) Encryption of user data with user provided password, so it's is protected from access by third-parties including cloud provider.
 
qbit stores data locally and uses entity graph information model, and for developers it's means that there are no usual persistence points of pain:
 1) There are no more monstrous queries for round-trips optimizations, since there is no more round-trips;
 2) There are no more object-relational mappers, since there is no more object-relational mismatch.
 
## Mission

Protect users privacy. And make development fun again.

## Evolution progress: [=====---------] (36%)

 * Datastore
   * :white_check_mark: ~Fetch data by id~
   * :white_check_mark: ~FileSystem storage~
   * Multivalue attributes
   * Component attributes
   
 * DataBase
   * :white_check_mark: ~Query by attribute value~
   * :white_check_mark: ~Schema~
   * :white_check_mark: ~Unique constraints~
   * Programmatic range queries
   * Programmatic joins
   * Query language (Datalog and/or something SQL-like)
   
 * p2p DataBase
   * Concurrent writes support
   * Automatic conflict resolution
   * CRDTs
   * p2p data synchronization
   
 * Cloud platform
   * qbit DBMS