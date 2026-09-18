# Releasing a new version

UniFIRE is released by pushing a git tag `v<version>` (e.g. `v5.1.2`), or by pushing a
`release/*` branch (e.g. `release/v5.1.2` or `release/5.1.2`) — both trigger the CI
pipelines to build and publish the `unifire/nextflow` Docker image tagged `<version>`
(never `latest`).

Pre-release artifacts are published by pushing a `snapshot/*` branch (e.g.
`snapshot/v0.1.0` or `snapshot/0.1.0`): the Docker image is published as
`<version>-SNAPSHOT` (e.g. `0.1.0-SNAPSHOT`), matching the Java versioning used by the
library release; `latest` is never updated.

## The version invariant

To keep the pipeline self-consistent, the committed value of `engine.unifireVersion` in
[getDefaultParams()](../nextflow/defaults.nf) must equal the image tag derived from the
pushed ref (`<version>` for `v*` tags and `release/*` branches,
`<version>-SNAPSHOT` for `snapshot/*` branches). On `master` the value stays at its
current value (`latest` or the last released version) unless a release or snapshot build
is intended.

The invariant is enforced at three levels, all using
[misc/release/check-nextflow-version.sh](../misc/release/check-nextflow-version.sh):

1. **Local git hook** - a `pre-push` hook refuses to push a `v*` tag or a `release/*` /
   `snapshot/*` branch whose commit does not carry the matching `engine.unifireVersion`.
   Enable it once per clone:

    ```bash
    git config core.hooksPath .githooks
    ```

   Note: hooks are a convenience and can be bypassed (`git push --no-verify`); the CI
   checks below are the authoritative enforcement.

2. **CI pipelines** - the GitHub and GitLab Docker jobs on `v*` tags and `release/*` /
   `snapshot/*` branches fail before building if the check fails.

3. **`nextflow-version-check` job** - a lightweight CI job on every push/MR pipeline
   validates that the committed value is either `latest` or a version-like string.
   It does not assert any specific version format.

## Release procedure

1. Update `nextflow/defaults.nf`: set `unifireVersion: "<major>.<minor>.<patch>"` in
   `getDefaultParams().engine`.
2. Commit and tag the release commit: `git tag v<version>` (or push the branch
   `release/v<version>` / `release/<version>` pointing at that commit).
3. Push the branch; the pre-push hook verifies the match.
4. GitLab library releases additionally require the pushed commit to be the HEAD of an
   allowed production branch (`master` by default); release branches must therefore have
   their tip at master HEAD for the library release to pass its validation.
5. The `latest` Docker tag is no longer published by the CI pipelines; pin the intended
   version (or override `--unifireVersion` at runtime) instead.

## Snapshot procedure (e.g. `snapshot/v0.1.0` or `snapshot/0.1.0`)

1. Update `nextflow/defaults.nf`: set `unifireVersion: "<major>.<minor>.<patch>-SNAPSHOT"` in
   `getDefaultParams().engine`.
2. Commit on the `snapshot/v<version>` (or `snapshot/<version>`) branch.
3. Push the branch; the snapshot Docker job publishes `<version>-SNAPSHOT` and the
   GitLab library job publishes Maven version `<version>-SNAPSHOT`.
