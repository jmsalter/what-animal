What Animal

Build:

`mvn -Dmaven.repo.local=.m2 package`

Run:

`java -cp "target/what-animal-1.0-SNAPSHOT.jar:target/lib/*" AnimalServer`

Open this in your browser:

`http://localhost:8080/`

Docker:

`docker build -t what-animal .`

`docker run -p 8080:8080 what-animal`

Or:

`docker compose up --build`
