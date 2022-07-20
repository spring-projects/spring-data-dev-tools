## General Notes

* Use the command `help` to get a list of all commands in the release tools.
* After fixing a problem use `workspace cleanup` to cleanup any mess left behind by the previous step.

## One Time Setup

### Infrastructure requirements

- Ensure you have the credentials for `buildmaster` accounts on https://repo.spring.io.
- Ensure yoiu have the credentials for https://oss.sonatype.org (to deploy and promote GA and service releases, need deployment permissions for `org.springframework.data`) in `settings.xml` for server with id `sonatype`.

Both are available in the Spring/Pivotal Last Pass repository.

### Prepare local configuration and credentials

Add an `application-local.properties` to the project root and add the following properties:

- `git.username` - Your GitHub username.
- `git.password` - Your GitHub Password (or API key with scopes: `public_repo, read:org, repo:status, repo_deployment, user` when using 2FA).
- `git.author` - Your full name (used for preparing commits).
- `git.email` - Your email (used for preparing commits).
- `maven.mavenHome` - Pointing to the location of your Maven installation.
- `deployment.username` - Your Artifactory user.
- `deployment.api-key` - The Artifactory API key to use for artifact promotion.
- `deployment.password` - The encrypted Artifactory password..
- `gpg.keyname` - The GPG key name.
- `gpg.passphrase` - The password of your GPG key.
- `gpg.executable` - Path to your GPG executable, typically `/usr/local/MacGPG2/bin/gpg2`
  or `/usr/local/bin/gpg`.
- `sagan.key` - Sagan authentication token. Must be a valid GitHub token. Can be the same
  as `git.password` when using a GitHub token as password.

After that, run the `verify` command (`$ verify`) to verify your settings (authentication,
correct Maven, Java, and GPG setup).

See `application-local.template` for details.

## The release process


| Action | Command |
|--------|---------|
| Build and execute the release shell | `mvn package && java -jar target/spring-data-release-cli.jar` |
|  | *All following commands are run in the release shell* |
| **Pre-release checks** | |
| Ensure all work on CVEs potentially contained in the release is done (incl. backports etc.) | N.A. |
| Upgrade dependencies in Spring Data Build parent pom (mind minor/major version rules) | N.A. |
| All release tickets are present | `$ tracker releasetickets $trainIteration` |
| Review open tickets for release | N.A. |
| Self-assign release tickets | `$ tracker prepare $trainIteration` |
| Announce release preparations to mailing list (https://groups.google.com/forum/#!forum/spring-data-dev) | N.A. |
| **Release the binaries** ||
| | `$ release prepare $trainIteration` |
| Build the artefacts and push them to the apropriate maven repository | `$ release build $trainIteration` |
| |`$ release conclude $trainIteration` |
| Push the created commits to GitHub |`$ github push $trainIteration` |
| Push new maintanance branches if the release version was a GA release (`X.Y.0` version)|`$ git push $trainIteration.next`|
| Distribute documentation and static resources from tag |`$ release distribute $trainIteration`|
| **Post-release tasks** ||
|Close JIRA tickets and GitHub release tickets.|`$ tracker close $trainIteration`|
|Create new release versions and tickets for upcoming version|`$ tracker setup-next $trainIteration.next`|
| Update versions in Sagan. `$targets` is given as comma separated lists of code names, without spaces. E.g. `Moore,Neumann` | `$ sagan update $releasetrains`|
|  Create list of docs for release announcements | `$ announcement $trainIteration`|
| Announce release (Blog, Twitter) and notify downstream dependency projects as needed. | N.A. |

### Utilities

#### GitHub Labels

`ProjectLabelConfiguration` contains a per-project configuration which labels should be present in a project. To apply that configuration (create or update), use:

```
$ github update labels $project
```

#### Dependency Upgrade

`ProjectDependencies` contains a per-project configuration of dependencies.

Workflow:

* Check for dependency upgrades `$ dependency check $trainIteration`

Reports upgradable dependencies for Build and Modules and
creates `dependency-upgrade-build.properties` file.
Edit `dependency-upgrade-build.properties` to specify the dependency version to upgrade.
Removing a line will omit that dependency upgrade.

* Apply dependency upgrade with `$ dependency upgrade $trainIteration`. Applies dependency
  upgrades currently only to Spring Data Build.
* Report store-specific dependencies to Spring Boot's current upgrade
  ticket ([sample](https://github.com/spring-projects/spring-boot/issues/24036)) `$ dependency report $trainIteration`

#### CI Properties Distribution

To distribute `ci/pipeline.properties` across all modules use:

`$ infra distribute ci-properties $trainIteration`
