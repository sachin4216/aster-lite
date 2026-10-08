# Command sheet

Every command used while building Aster Lite, grouped by tool. New commands are added to the matching section when they are first used.

Run them from `C:\dev\aster-lite` (the IntelliJ terminal, Alt+F12, opens there). Docker Desktop must be running for every `docker` command.

| Container | What it is | Port | Used from |
|---|---|---|---|
| `aster-mysql` | MySQL 8.4, database `patient_db`, user `root` / password `root` | 3306 | Day 1 |
| `aster-redis` | Redis 7, the `patients` cache | 6379 | Day 1 |
| `aster-mongo` | MongoDB 7 | 27017 | Day 3 |
| `aster-kafka` | Apache Kafka 4.1 | 9092 | Day 3 |

---

## 1. Docker Compose: all containers together

| Command | What it does |
|---|---|
| `docker compose up -d` | Start all four containers in the background |
| `docker compose ps` | Show the containers of this project and their state |
| `docker compose logs -f mysql` | Follow one service's logs (`mysql`, `redis`, `mongo`, `kafka`). Ctrl+C stops following. |
| `docker compose down` | Stop and remove the containers, keep the data |
| `docker compose down -v` | Stop and remove the containers **and delete the data volumes** |

## 2. Docker: one container

| Command | What it does |
|---|---|
| `docker ps` | Running containers |
| `docker ps -a` | All containers, including stopped ones, with their exit status |
| `docker stop aster-redis` | Stop one container (the "break it" drills) |
| `docker start aster-redis` | Start it again |
| `docker restart aster-mysql` | Stop and start in one step |
| `docker logs aster-mysql` | Print a container's logs |
| `docker logs -f aster-mysql` | Follow the logs live. Ctrl+C stops following. |
| `docker logs --tail 50 aster-mysql` | Only the last 50 lines |
| `docker exec -it aster-mysql bash` | Open a shell inside a container. `exit` leaves it. |
| `docker --version` / `docker compose version` | Check the installation |

`docker exec -it <container> <program>` is the general form for "run a program inside a running container". `-it` gives you an interactive prompt.

## 3. MySQL

### Open the MySQL prompt

```
docker exec -it aster-mysql mysql -uroot -proot
```

You are now at `mysql>`. Every statement ends with `;`. Type `exit` to leave.

To open it directly on the patient database:

```
docker exec -it aster-mysql mysql -uroot -proot patient_db
```

### Inside the `mysql>` prompt

| Statement | What it does |
|---|---|
| `SHOW DATABASES;` | List databases |
| `USE patient_db;` | Switch to a database |
| `SHOW TABLES;` | List tables in the current database |
| `DESCRIBE patients;` | Columns and types of a table |
| `SHOW CREATE TABLE patients\G` | Full table definition, including the unique key on `email` |
| `SELECT * FROM patients;` | All rows |
| `SELECT id, email, status FROM patients ORDER BY id;` | Chosen columns |
| `SELECT * FROM patients WHERE id = 1\G` | One row, one column per line (easier to read) |
| `SELECT COUNT(*) FROM patients;` | Number of rows |

### One-off statements without opening the prompt

```
docker exec aster-mysql mysql -uroot -proot -e "SELECT id, email, status FROM patient_db.patients ORDER BY id"
docker exec aster-mysql mysql -uroot -proot -e "SHOW CREATE TABLE patient_db.patients\G"
```

### Changing data by hand (test setup and cleanup)

```
docker exec aster-mysql mysql -uroot -proot -e "UPDATE patient_db.patients SET status='INACTIVE' WHERE id=1"
docker exec aster-mysql mysql -uroot -proot -e "DELETE FROM patient_db.patients WHERE email LIKE 'review.%'"
```

The second one removes the test rows created during story reviews.

### appointment_db (Day 2)

```
docker exec -it aster-mysql mysql -uroot -proot appointment_db
docker exec aster-mysql mysql -uroot -proot -e "SELECT id, doctor_id, start_time, status, version FROM appointment_db.slots ORDER BY id"
docker exec aster-mysql mysql -uroot -proot -e "SELECT * FROM appointment_db.appointments ORDER BY id"
```

The last two are the check after a booking: the slot's `status` and `version`, and the appointment rows. After a rolled-back booking the slot is still `AVAILABLE` and no new appointment row exists.

## 4. Redis

### Open the Redis prompt

```
docker exec -it aster-redis redis-cli
```

You are now at `127.0.0.1:6379>`. Type `exit` to leave.

### Inside the Redis prompt

| Command | What it does |
|---|---|
| `keys *` | Every key. Cached patients appear as `patients::1`, `patients::2`, ... |
| `exists "patients::1"` | `1` if that patient is cached, `0` if not |
| `ttl "patients::1"` | Seconds until the entry expires (starts near 600) |
| `get "patients::1"` | The stored value. It is Java-serialised bytes, so it is not readable text. |
| `del "patients::1"` | Remove one entry |
| `flushall` | Remove **every** key |
| `monitor` | Print every command Redis receives, live. Ctrl+C stops it. |
| `ping` | Answers `PONG` if Redis is alive |

### One-off commands without opening the prompt

```
docker exec -it aster-redis redis-cli keys "*"
docker exec -it aster-redis redis-cli ttl "patients::1"
```

## 5. Logs: where to look

| What | Where |
|---|---|
| discovery-server, config-server, patient-service | IntelliJ: **Services** window (Alt+8), select the service, **Console** tab |
| SQL statements run by patient-service | Same console, lines starting with `Hibernate:` (`show-sql: true`) |
| Cache failures when Redis is down | Same console, `WARN` lines from `CacheConfig` |
| Unexpected errors (500) | Same console, `ERROR` line with the stack trace from `GlobalExceptionHandler` |
| MySQL, Redis, MongoDB, Kafka | `docker logs -f <container>`, see section 2 |

## 6. Checking the services

| URL | What you should see |
|---|---|
| `http://localhost:8761` | Eureka dashboard, `PATIENT-SERVICE` with status `UP` |
| `http://localhost:8888/patient-service/default` | The configuration patient-service receives, as JSON |
| `http://localhost:8081/actuator/health` | `{"status":"UP"}` |
| `http://localhost:8081/actuator/caches` | The cache names known to patient-service |
| `http://localhost:8888/api-gateway/default` | The port and routes api-gateway receives, as JSON |
| `http://localhost:8080/actuator/health` | `{"status":"UP"}` from api-gateway |
| `http://localhost:8080/api/patients/1` | A patient, routed through the gateway to patient-service |
| `http://localhost:8888/appointment-service/default` | The configuration appointment-service receives. Keys must read `spring.datasource.url`, not `server.spring...` |
| `http://localhost:8082/actuator/health` | `{"status":"UP"}` from appointment-service |
| `http://localhost:8082/api/nothing` | `404` as `ApiErrorResponse`, with the `X-Instance-Port: 8082` header |

From a terminal, `curl.exe -i <url>` prints the status line and headers as well, including `X-Instance-Port`:

```
curl.exe -i http://localhost:8081/actuator/health
```

In PowerShell use `curl.exe`, not `curl`: plain `curl` is an alias for a different PowerShell command.

Through the gateway, repeat this call with patient-service running on 8081 and 8091 and watch `X-Instance-port` change:

```
curl.exe -i http://localhost:8080/api/patients/1
```

Start order: Docker, then discovery-server, then config-server, then patient-service, then appointment-service, then api-gateway. After changing a file under `config-server/src/main/resources/config`, restart config-server first and the service that reads the file second.

## 7. Maven

Run from the root folder `C:\dev\aster-lite`.

| Command | What it does |
|---|---|
| `mvn -pl patient-service compile` | Compile one module |
| `mvn -pl api-gateway compile` | Compile the gateway module |
| `mvn -pl appointment-service compile` | Compile appointment-service |
| `mvn -pl patient-service test` | Run one module's tests |
| `mvn -pl appointment-service test` | Run appointment-service's tests. `contextLoads` needs Docker, discovery-server and config-server running. |
| `mvn -pl appointment-service test -Dtest=SlotServiceImplTest` | Run one test class only |
| `mvn -pl appointment-service test-compile` | Compile the main code and the tests without running them |
| `mvn test` | Run every module's tests |
| `mvn -pl patient-service package` | Run the tests and build the jar in `patient-service/target` |
| `mvn -pl patient-service package -DskipTests` | Build the jar without running tests |

`-pl <module>` means "only this module". Add `-q` for quiet output.

## 8. Git: one branch per story

```
git checkout main
git pull
git checkout -b feature/PAT-7-redis-cache

git status
git add patient-service/src/main/java
git commit -m "PAT-7: cache patient lookups in Redis"
git push -u origin feature/PAT-7-redis-cache
```

Then open the pull request on GitHub, merge it, and start the next story from an updated `main`.

| Command | What it does |
|---|---|
| `git status` | What is changed, staged or untracked |
| `git diff` | The exact changes not yet staged |
| `git log --oneline -5` | The last five commits |
| `git branch -a` | All local and remote branches |
| `git fetch origin` | Download what changed on GitHub without touching your files |
