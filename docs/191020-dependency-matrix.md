|            | api |   ns  | model | refl | fact | ser | query | typ | trx | index | stor | schem | qbit |     | 
| ---------- |:---:|:-----:|:-----:|:----:|:----:|:---:|:-----:|:---:|:----:|:----:|:----:|:-----:|:----:|:---:|
| **api**    | XXX |       |       |      |      |     |       |     |     |       |      |       |      |  0  | 
| **ns**     |     | XXX   |       |      |      |     |       |     |     |       |      |       |      |  0  | 
| **model**  | 1   |       | XXX   |      |      |     |       |     |     |       |      |       |      |  1  | 
| **refl**   | 1   |       |       | XXX  |      |     |       |     |     |       |      |       |      |  1  | 
| **fact**   | 1   |       |       | 1    | XXX  |     |       |     |     |       |      |       |      |  2  | 
| **ser**    | 1   | 1     |       |      |      | XXX |       |     |     |       |      |       |      |  2  | 
| **query**  | 1   |       |       | 1    |      |     | XXX   |     |     |       |      |       |      |  2  | 
| **typ**    | 1   |       |       | 1    | 1    |     |       | XXX |     |       |      |       |      |  3  | 
| **trx**    | 1   |       | 1     |      | 1    | 1   |       |     | XXX |       |      |       |      |  4  | 
| **index**  | 1   |       | 1     |      |      | 1   | 1     | 1   |     | XXX   |      |       |      |  5  | 
| **stor**   | 1   | 1     |       |      |      | 1   |       |     |     |       | XXX  |       |      |  3  | 
| **schem**  | 1   |       |       | 1    | 1    |     |       |     |     |       |      | XXX   |      |  3  | 
| **qbit**   | 1   | 1     | 1     |      | 1    | 1   |       |     | 1   | 1     |      |       | XXX  |  7  | 
|            | 11  | 3     | 3     | 4    | 4    | 4   | 1     | 1   | 1   | 1     | 0    | 0     | 0    |     | 