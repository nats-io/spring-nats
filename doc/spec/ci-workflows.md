# CI/CD

~~~mermaid
flowchart TD
    PR["Pull request"] --> VERIFY["Build and verify"]
    MAIN["Push to main or master"] --> SNAPSHOT["Build snapshot"]
    SNAPSHOT --> CENTRAL["Maven Central"]
    SNAPSHOT --> PACKAGES["GitHub Packages"]
    RELEASE["Manual release from main or master"] --> STABLE["Build stable release"]
    STABLE --> CENTRAL
    STABLE --> PACKAGES
    CENTRAL --> GITHUB["Tag, GitHub release, and assets"]
    PACKAGES --> GITHUB
~~~

## Versioning

The build resolves the version once and writes it into the workspace before verification.

- pull requests and merges build YYYY.M.D-SNAPSHOT in UTC
- merges publish that snapshot to Maven Central and GitHub Packages
- a manual release from main or master builds and publishes YYYY.M.D
- the checked-in 1.0.0 is only a build placeholder

Snapshots never create a tag or GitHub release. A stable release creates both only after Maven Central and GitHub Packages succeed.

## Build and test

nats-server-junit downloads, starts, configures, and stops native NATS servers for the tests. It selects the normal server version by default and individual tests can request another version. CI does not build Go, clone NATS Server, cache a server binary, or require a local installation.

The verified workspace is retained for one day and restored by both publishing jobs. Stable builds create Javadocs before upload so the release job can attach the exact built JARs, sources, and Javadocs.

## Publishing

Only these Maven coordinates are published:

- io.nats:nats-spring-parent
- io.nats:nats-spring
- io.nats:nats-spring-boot-starter
- io.nats:nats-spring-cloud-stream-binder

Samples are verified but never published. Central and GitHub Packages run in parallel, each with its own environment. The release assets are the parent and starter POMs, plus JAR, sources, and Javadocs for the core and binder modules.

## Permissions

- build: contents: read
- Central: actions: read, contents: read, deployments: write
- GitHub Packages: Central permissions plus packages: write
- GitHub release: actions: read, contents: write

External actions are pinned to immutable commit SHAs. Central receives only the four named publishing and signing secrets. GitHub Packages uses the repository token.
