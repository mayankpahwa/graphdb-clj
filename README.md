# graphdb-clj

A in-memory graph database in clojure using cypher query language.

Using redis to save the database.

(**_WIP_**)

## Installation

#### Software requirements :

- [Leiningen](https://leiningen.org/)
- [Redis](https://github.com/antirez/redis)
- [Carmine](https://github.com/ptaoussanis/carmine)

## Usage

```
lein run
```

## Examples

Supports basic syntax of the cypher query language.(create,set,remove,return,delete,match-where filter)

```
create (Neo:Crew {name:'Neo'}),(Morpheus:Crew {name: 'Morpheus'}),(Neo)-[:KNOWS {:since 2001}]->(Morpheus)
--
match (n:Crew) where n.name='Neo' set n.age=25
match (n:Crew) where n.name='Neo' set n:Person
--
match (n:Person) where n.name='Neo' remove n.name
--
match (n:Person) where n.age=25 set n.name='Neo'
match (n:Person) where n.name='Neo' return n
match (n:Person) where n.name='Neo' return n.age
---
match (n) delete n
```

### Bugs

Please file issues on github with minimal sample code that demonstrates the problem.

## License

Copyright © 2017

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
