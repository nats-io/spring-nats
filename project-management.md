# Project Management

## CI/CD

- [🧪 CI · Pull Request](.github/workflows/build-pr.yml) builds and tests pull requests.
- [🔄 CD · Release Snapshot](.github/workflows/build-merge.yml) publishes snapshots after merges.
- [🏷️ Manual · Release](.github/workflows/release.yml) publishes `rc`, `patch`, `minor`, or `major` releases.

See [CI workflows](doc/spec/ci-workflows.md) for versioning, publishing, environments, and dry-run behavior.

## Version Management

Git tags are the release source of truth. The workflows calculate the next version and rewrite `${revision}` for the build; module POM versions do not need manual release commits.

Select the `major` release strategy to move the current `0.x` line to `1.0.0`.
