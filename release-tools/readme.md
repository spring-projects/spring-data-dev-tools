1. Add an `application-local.properties` to the project root and add the following properties:

- `git.username` - Your GitHub username.
- `git.password` - Your GitHub password or API key.
- `git.author` - Your full name (used for preparing commits).
- `git.email` - Your email (used for preparing commits).
- `maven.mavenHome` - Pointing to the location of your Maven installation.
- `deployment.api-key` - The API key to use for artifact promotion.
- `deployment.password` - The password of the deployment user (buildmaster).

2. Run `mvn package appassembler:assemble && sh target/appassembler/bin/spring-data-release-shell`
