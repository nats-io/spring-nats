# CI Release Workflow Decisions

Status: Accepted

## Rules

1. The latest reachable git tag is the version source of truth.
2. No reachable tag bootstraps from `0.0.0`.
3. The build resolves the version, commit SHA, and commit timestamp once.
4. Publish jobs restore the verified `build-workspace` artifact.
5. Build artifacts expire after one day.
6. Only the parent, core, starter, and binder coordinates are published.
7. Dry runs never upload to Maven Central.
8. Snapshot dry runs publish GitHub Packages and create deployments without tags or releases.
9. Manual release dry runs also create the tag, release, and release assets.
10. Live publishing uses only `maven-central` and `github-packages` environments.
11. Manual releases share one concurrency slot.
12. External actions are pinned to immutable commit SHAs.
13. After a partial release failure, rerun failed jobs in the same workflow run. Do not dispatch a new release.
14. Shared build and publish workflows are callable only.
15. Central receives only its four named secrets; GitHub Packages uses the automatic repository token.

## Release Assets

- parent POM
- starter POM
- core JAR, sources JAR, and Javadoc JAR
- binder JAR, sources JAR, and Javadoc JAR

## Permissions

- build: `contents: read`
- Central: `actions: read`, `contents: read`, `deployments: write`
- GitHub Packages: Central permissions plus `packages: write`
- GitHub release: `actions: read`, `contents: write`
