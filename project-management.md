# Project Management

## CI/CD

- [CI Pull Request](.github/workflows/build-pr.yml) builds and tests pull requests.
- [CD Release Snapshot](.github/workflows/build-merge.yml) publishes snapshots after merges.
- [Manual Release](.github/workflows/release.yml) publishes a stable date-versioned release.

See [CI workflows](doc/spec/ci-workflows.md) for versioning, publishing, environments, and test-server lifecycle.

## Version Management

The build resolves a UTC date version and rewrites the Maven revision property for the build. Module POM versions do not need manual release commits.

Snapshots use YYYY.M.D-SNAPSHOT. Manual stable releases use YYYY.M.D.
