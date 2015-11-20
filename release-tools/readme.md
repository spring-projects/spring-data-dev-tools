1. Add an `application-local.properties` to the project root and add the following properties:

- `git.username` - Your GitHub username.
- `git.password` - Your GitHub password.
- `git.author` - Your full name (used for preparing commits).
- `git.email` - Your email (used for preparing commits).

2. Run `mvn package appassembler:assemble && sh target/appassembler/bin/spring-data-release-shell`
