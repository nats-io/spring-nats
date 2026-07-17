# CI Release Workflow Decisions

Status: Accepted

## Rules

1. `build-common.yml` resolves `resolved_version`, `commit_sha`, and `build_output_timestamp` once.
2. Publish and release jobs restore the `build-workspace` artifact instead of rebuilding from source state again.
3. Build artifact retention is one day.
4. `spring-nats` keeps the published artifact names already used on Maven Central:
   - `nats-spring-parent`
   - `nats-spring`
   - `nats-spring-boot-starter`
   - `nats-spring-cloud-stream-binder`
5. Release numbering uses the latest reachable semver tag only.
6. If no reachable semver tag exists, version resolution bootstraps from `0.0.0`.
7. `dry_run=true` validates the Maven Central packaging path only.
8. `dry_run=true` must not publish GitHub Packages, create tags, or create GitHub releases in the public repository.
9. `project.build.outputTimestamp` is the checked-out commit timestamp and must stay aligned with `git.commit.time`.
10. Release assets are:
    - parent POM
    - starter POM
    - core JAR, sources JAR, javadoc JAR
    - binder JAR, sources JAR, javadoc JAR
11. Maven Central publishing uses the `publish` profile; GitHub Packages uses the normal Maven `deploy` path without that profile.

## Required Permissions

- publish-central:
  - `actions: read`
  - `contents: read`
  - `deployments: write`
- publish-github-packages:
  - `actions: read`
  - `contents: read`
  - `deployments: write`
  - `packages: write`
- create-release:
  - `actions: read`
  - `contents: write`

## Notes

- Hidden files must stay in the build artifact so `.mvn` survives restore.
- Publish jobs must `chmod +x mvnw` after artifact restore.
- External actions are pinned to immutable SHAs.
