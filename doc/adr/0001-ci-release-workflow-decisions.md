# CI Release Workflow Decisions

Status: Accepted

1. Versioning uses the UTC date, not Maven POM or git tags.
2. Pull requests and merges use YYYY.M.D-SNAPSHOT. Manual stable releases use YYYY.M.D.
3. The build sets the version and commit-derived output timestamp once. Publish jobs restore that verified workspace.
4. Merge snapshots publish to Maven Central and GitHub Packages without a tag or GitHub release.
5. Stable publishing runs Central and GitHub Packages in parallel. Central auto-publishes asynchronously after validation. A tag and GitHub release follow when Central validation and GitHub Packages succeed.
6. Stable builds include Javadocs. Release assets are two POMs and the core and binder JAR, sources, and Javadocs.
7. Native NATS tests use nats-server-junit, including its version selection and lifecycle. CI does not build or install NATS Server.
8. Build artifacts expire after one day.
9. Publishing uses the maven-central and github-packages environments.
10. External actions are pinned by immutable commit SHA and jobs receive the minimum required permissions.
