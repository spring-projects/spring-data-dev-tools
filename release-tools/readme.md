## Setup

### Infrastructure requirements

- Credentials for `buildmaster` accounts on https://repo.spring.io.
- Credentials for https://oss.sonatype.org (to deploy and promote GA and service releases, need deployment permissions for `org.springframework.data`) in `settings.xml` for server with id `sonatype`.

### Prepare local configuration and credentials

Add an `application-local.properties` to the project root and add the following properties:

- `git.username` - Your GitHub username.
- `git.password` - Your GitHub password or API key.
- `git.author` - Your full name (used for preparing commits).
- `git.email` - Your email (used for preparing commits).
- `maven.mavenHome` - Pointing to the location of your Maven installation.
- `deployment.api-key` - The API key to use for artifact promotion.
- `deployment.password` - The password of the deployment user (buildmaster).
- `gpg.keyname` - The GPG key name.
- `gpg.password` - The password of your GPG key.
- `gpg.executable` - Path to your GPG executable, typically `/usr/local/MacGPG2/bin/gpg2` or `/usr/local/bin/gpg`.


See `application-local.template` for details.

### Build and execute the release shell

Run `mvn package && java -jar target/spring-data-release-cli.jar`

## The release process

### Pre-release checks

Make sure that:

* All work on CVEs potentially contained in the release is done (incl. backports etc.)
* Upgrade dependencies in Spring Data Build parent pom (mind minor/major version rules)
* All release tickets are present (`$ tracker releasetickets $trainIteration`)
* Review open tickets for release
* Self-assign release tickets (`$ tracker prepare $trainIteration`)
* Announce release preparations to mailing list

### Release the binaries

```
$ release prepare $trainIteration
$ release build $trainIteration
$ release conclude $trainIteration
$ git push $trainIteration
$ git push $trainIteration --tags
$ git backport changelog $trainIteration --target $targets
$ foreach $target -> git push $target
```

### Distribute documentation and static resources from tags

```
$ release distribute $trainIteration
```

### Post-release tasks

* Close JIRA tickets and GitHub release tickets.
* Create release tickets for the next train iteration, archive old release versions. Close Jira versions/GitHub milestones.

```
$ tracker close $trainIteration
$ tracker create releaseversions $trainIteration.next
$ tracker create releasetickets $trainIteration.next
$ tracker archive $trainIteration.previous
```

* Update versions in Sagan with `$ sagan update $releasetrains`.
* Announce release (Blog, Twitter) and notify downstream dependency projects as needed. Dev-tools can assist you with `$ announcement $trainIteration`. Make sure to remove the changelog link to Envers as this module has no changelog.

### Utilities

#### GitHub Labels

`ProjectLabelConfiguration` contains a per-project configuration which labels should be present in a project. To apply that configuration (create or update), use:

```
$ github update labels $project
```

