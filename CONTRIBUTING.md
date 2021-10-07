# Contributors #

Pull requests, issues and comments welcome. For pull requests:

* Add tests for new features and bug fixes
* Follow the existing style (checkstyle checking is enabled by default in builds)
* Separate unrelated changes into multiple pull requests

Please ensure that your branch builds successfully before you open your PR. The Pipelines build won't run by default on
a remote branch, so either enable Pipelines for your fork or run the build locally:

```
mvn clean verify javadoc:javadoc
```

See the existing [issues](https://bitbucket.org/atlassian/swagger-request-validator/issues) for things to start
contributing. If you want to start working on an issue, please assign the ticket to yourself and mark it as `open`
so others know it is in progress.

For bigger changes, make sure you start a discussion first by creating an issue and explaining the intended change.

## Which release to target ##

The 'current' release train is made from the `master` branch. New features and improvements should target `master`.

Previous major-version releases are maintained on branches. Only critical bugfixes should target these branches. No new
features will be accepted on these branches except under special circumstances.

## Contributor License Agreement (CLA) ##

Atlassian requires contributors to sign a Contributor License Agreement, known as a CLA. This serves as a record stating
that the contributor is entitled to contribute the code/documentation/translation to the project and is willing to have
it used in distributions and derivative works
(or is willing to transfer ownership).

Prior to accepting your contributions we ask that you please follow the appropriate link below to digitally sign the
CLA. The Corporate CLA is for those who are contributing as a member of an organization and the individual CLA is for
those contributing as an individual.

* [CLA for corporate contributors](https://opensource.atlassian.com/corporate)
* [CLA for individuals](https://opensource.atlassian.com/individual)

# Maintainers #

## Publishing a new release ##

**Important**: Please think carefully about what type of release you are performing (patch or minor). The project aims
to follow [semver](https://semver.org/) where possible. In general:

- Patch version releases are for bug fixes and minor improvements that don't change API. Patch version bumps of
  dependencies are patch version changes.
- Minor version releases are for new features, additions to the API, or minor version bumps of dependencies.
- Major version releases are reserved for large changes, breaking changes, or moving between major versions of the
  OpenAPI parser and/or spec. Major version changes will be planned and managed. Please discuss this with other
  maintainers.

Publishing a release to Maven Central is currently a two step process:

1. Prepare a new release locally using

        ./bin/prepare-release.sh [patch | minor]

   This will run `mvn release:prepare` with the appropriate values depending on what type of release is being performed.
   When successful the tag `latest-release` will be updated to point to the new release.

2. Run the [release build](https://jira-software-bamboo.internal.atlassian.com/browse/JNAV-SRVR)

   This builds, signs and publishes the release from the `latest-release` tag.

3. Update the `RELEASE-NOTES.md` with details on what issues are included in the release (and ideally links to the PRs)

   Running `git log` can give you a hint as to what is included in the release
   (see the output from the `prepare-release.sh` script).

4. (Optional) Update the issues and PRs included in the release with a comment of the released version.

   This is a nice to have, but helps communicate to consumers when changes are available.