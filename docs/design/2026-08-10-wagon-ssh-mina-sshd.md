# Design: replace JSch with Apache MINA SSHD in wagon-ssh

Date: 2026-08-10 (updated 2026-09-04)
Target: `org.apache.maven.wagon:wagon-ssh` 4.0.0-M1
Status: approved design, baseline verified on master, implementation plan prepared

## Motivation

`wagon-ssh` originally pinned `com.jcraft:jsch:0.1.55` (last released 2018) and
`com.jcraft:jsch.agentproxy:0.0.9`. Neither was maintained. JSch 0.1.55 lacked
`rsa-sha2-256`/`rsa-sha2-512` and could not read OpenSSH-format ed25519 keys.

As an intermediate milestone, `wagon-ssh` on `master` was migrated to the
maintained fork `com.github.mwiede:jsch:2.28.6` (commit `e81e5db4`), removing
`jsch.agentproxy` and restoring key compatibility without breaking existing APIs.

However, the strategic direction for Wagon 4.x is to move entirely off 3rd-party
JSch forks and standardize on **Apache MINA SSHD** (`org.apache.sshd`). Apache MINA
SSHD 2.19.0 is already an established dependency in the build: `wagon-ssh-common-test`
runs the embedded test server on `sshd-core`, `sshd-scp` and `sshd-sftp`. Moving the
client side onto SSHD removes the external fork dependency, provides native ASF governance,
and unifies both client and server on the same stack.

## Decisions taken

| Question | Decision |
|---|---|
| Migration shape | Replace in place. Keep artifactId `wagon-ssh` and the `scp`/`sftp` role hints; rewrite internals under `org.apache.maven.wagon.providers.ssh.sshd` and delete the `jsch` package. |
| HTTP/SOCKS5 proxy | Ship SOCKS5 only. HTTP CONNECT is rejected with an actionable error. |
| ssh-agent | SSHD `SshAgentFactory` with the agent module declared `<optional>true</optional>`. |
| known_hosts | Delegate to SSHD's `KnownHostsServerKeyVerifier`; deprecate the wagon `KnownHostsProvider` SPI. |

## Current state

JSch coupling is ~1350 lines across seven classes in one package:

- `AbstractJschWagon` (415) — session, auth, proxy, known_hosts
- `ScpWagon` (388) — `ChannelExec` plus a hand-rolled scp wire protocol
- `SftpWagon` (451) — `ChannelSftp`, `SftpATTRS`
- `WagonUserInfo` (76)
- `interactive/UserInfoUIKeyboardInteractiveProxy` (93)
- `interactive/PrompterUIKeyboardInteractive` (83)
- `ScpCommandExecutor` (41)

`wagon-ssh-common` is JSch-free — the name appears only in javadoc prose.
`wagon-ssh-external` likewise. Consumers bind to the `scp`/`sftp` role hints,
not to class names.

The only JSch type in a public wagon signature is
`AbstractJschWagon.setUIKeyboardInteractive(com.jcraft.jsch.UIKeyboardInteractive)`.

Outside this repo, exactly one file imports the package:
`maven-it-plugin-uses-wagon` imports `...ssh.jsch.ScpWagon`, present in
`core/maven`, `core/maven-4.0.x` and `core/3.x/its-3`.

## Module layout

`wagon-ssh` keeps its GAV. Dependency changes:

Removed:
- `com.github.mwiede:jsch:2.28.6` (and any legacy `com.jcraft:jsch*` artifacts)

Added:
- `org.apache.sshd:sshd-core`
- `org.apache.sshd:sshd-sftp`
- SSHD agent module, `<optional>true</optional>`

`sshd-scp` is deliberately **not** added — see `ScpWagon` below.

`sshdVersion` moves out of `wagon-ssh-common-test`'s `<properties>` into the
root pom's `dependencyManagement`, so the client and the embedded test server
cannot drift apart.

`wagon-ssh-common` and `wagon-ssh-external` are untouched.

## Class-by-class

### `AbstractSshdWagon` (was `AbstractJschWagon`)

One `SshClient.setUpDefaultClient()` per wagon instance, started in
`openConnectionInternal`, `client.stop()` in `closeConnection`.

- `session.setTimeout(getTimeout())` becomes `connect(...).verify(Duration)`
  plus `CoreModuleProperties.IDLE_TIMEOUT`.
- `executeCommand` swaps `session.openChannel("exec")` for
  `session.createExecChannel(cmd)`. `getInvertedOut()`/`getInvertedErr()` feed
  the existing `CommandExecutorStreamProcessor` unchanged, so stdout/stderr
  handling and the exit-code contract are preserved.

### `ScpWagon`

**Keeps its hand-rolled scp wire protocol.** The `C0644` header, ack bytes and
`E\n` terminator are server-side protocol, not a JSch artifact, and
`StreamWagon.fillInputData`/`fillOutputData` must hand raw streams back to the
caller — which `sshd-scp`'s `ScpClient` does not do. Only channel acquisition
changes.

This is the single largest risk reduction in the plan. In particular it retains
the already-tuned divergence where OpenSSH reports a missing file with the scp
warning code (1) and MINA sshd reports it with the fatal code (2).

### `SftpWagon`

The one genuine rewrite.

- `ChannelSftp` → `SftpClientFactory.instance().createSftpClient(session)`
- `SftpATTRS` → `SftpClient.Attributes`
- `channel.ls()` → `readDir()`
- `stat`/`mkdir`/`chmod` map directly

`SftpClient` has no working directory, so the `cd`-relative navigation in
`changeToRepositoryDirectory` becomes absolute-path construction.

### Interaction classes

`WagonUserInfo`, `UserInfoUIKeyboardInteractiveProxy` and
`PrompterUIKeyboardInteractive` collapse into a single
`WagonUserInteraction implements org.apache.sshd.client.auth.keyboard.UserInteraction`,
driven by the existing `InteractiveUserInfo` and the plexus `Prompter`.

This deletes `setUIKeyboardInteractive(com.jcraft.jsch.UIKeyboardInteractive)`,
the only JSch type that ever leaked into a public wagon signature.

### `transfer()` override

`AbstractJschWagon` overrides `StreamWagon.transfer` solely to work around JSch
issue #122, as its own comment records. Delete the override and let
`StreamWagon.transfer` run. The embedded-server tests are the check.

## Behaviour changes

All intentional, all confined to 4.0.0-M1.

| Area | Before | After |
|---|---|---|
| known_hosts | wagon `KnownHostsProvider` SPI (`file`/`single`/`null`) | `KnownHostsServerKeyVerifier` on `~/.ssh/known_hosts`. SPI deprecated and no longer consulted. `strictHostKeyChecking` maps `no` → accept-all, `yes` → reject-unknown, `ask` → verifier plus `UserInteraction` prompt. `UnknownHostException` and `KnownHostChangedException` are still thrown. Gains hashed-host and host-certificate support. |
| HTTP proxy | `ProxyHTTP` | Rejected at connect time with a message pointing at `ProxyCommand` and `wagon-ssh-external`. |
| SOCKS5 proxy | `ProxySOCKS5` | wagon-owned SOCKS5 handshake on SSHD's `ClientProxyConnector`. The legacy port-1080 fallback is preserved; that fallback's non-1080 branch now errors instead of silently using HTTP. |
| ssh-agent | jsch.agentproxy (JNA, Pageant) | SSHD `SshAgentFactory`, optional dependency. Works when the jar is present; falls through to key/password authentication when it is not. |
| Algorithms | no `rsa-sha2-*`, no OpenSSH-format ed25519 | whatever SSHD 2.19 supports. This is the point of the exercise. |

### Compatibility note that needs dev@ visibility

`FileKnownHostsProvider` already defaults to `~/.ssh/known_hosts`, so the common
path is unchanged. But anyone configuring `knownHostsProvider` in
`settings.xml`, and anyone using `NullKnownHostProvider` as an escape hatch,
breaks silently. This warrants an `[ATTENTION]` mail on dev@, not only a
release note.

## Testing

The safety net already exists — commit `d3ba3463` revived the embedded MINA
server tests — but it is opt-in, so CI being green today does not mean those
tests pass.

### Running Embedded Tests
In `wagon-ssh/pom.xml`, test execution is governed by two profiles:
- `<id>no-ssh-tests</id>` is active by default (via `!ssh-tests`) and excludes all SSH/SCP/SFTP tests.
- `<id>ssh-embedded</id>` is activated by `-Dssh-embedded=true` (on non-Windows).

To run the embedded suite:
```bash
mvn test -pl wagon-providers/wagon-ssh -Dssh-embedded=true -Dssh-tests
```

### Baseline Recorded (2026-09-04)
The current master baseline was verified on macOS against Apache MINA SSHD 2.19.0:
- `EmbeddedScpWagonTest`: 20 / 20 passed.
- `EmbeddedScpWagonWithKeyTest`: 22 / 22 passed.
- Total: **42 / 42 passing with 0 failures, 0 errors** (18 seconds).

### Identified Gap: SFTP Embedded Test Harness
`SftpWagonTest` currently inherits from `StreamingWagonTestCase` and connects to
`TestData.getTestRepositoryUrl(0)` (`localhost:22`), expecting an external SSH daemon.
It does not run against the embedded test server.

However, `SshServerEmbedded` in `wagon-ssh-common-test` already instantiates
and registers the SFTP subsystem:
```java
sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
```
Therefore, an `EmbeddedSftpWagonTest` (mirroring `EmbeddedScpWagonTest`) must be
introduced in PR 1 to establish a reproducible, green embedded SFTP baseline before
PR 3 rewrites `SftpWagon`.

### Test Gates
1. Before touching client code, verify the embedded baseline (SCP + SFTP).
2. Migrate with those tests as the gate. `EmbeddedScpWagon*Test` and the new
   `EmbeddedSftpWagonTest` must pass continuously.
3. New tests: a SOCKS5 proxy test with a minimal SOCKS5 listener in
   `wagon-ssh-common-test`, mirroring the existing `ScpWagonWithProxyTest`
   shape; and an assertion on rsa-sha2 negotiation that would have failed on
   legacy JSch 0.1.55.
4. Manual matrix before release: real OpenSSH 9.x, `~/.ssh/config` in play,
   ed25519 key, agent present and absent.

## Sequencing

Five PRs, each independently green:

1. Hoist `sshdVersion` (2.19.0) into the root `dependencyManagement`; add
   `EmbeddedSftpWagonTest` against `SshServerEmbedded`; verify 100% green
   embedded SCP & SFTP baseline with `-Dssh-embedded=true -Dssh-tests`.
   No client behaviour change.
2. `AbstractSshdWagon`, `ScpWagon` and `ScpCommandExecutor` on SSHD, with
   known_hosts and interaction rewired. Proxy and agent temporarily
   unsupported.
3. `SftpWagon` on `SftpClient`.
4. SOCKS5 `ClientProxyConnector` and its test.
5. Optional agent dependency, plus the docs sweep: `src/site/markdown/user-guide.md`
   (two places), `src/site/markdown/developer-guide.md`,
   `wagon-providers/wagon-ssh/src/site/markdown/index.md`, and the jcraft
   `<area>` in the root `src/site/markdown/index.md` image map. No page URL or
   anchor changes.

Follow-up outside this repo: the `ScpWagon` import in
`maven-it-plugin-uses-wagon`, in `core/maven`, `core/maven-4.0.x` and
`core/3.x/its-3`.

## Out of scope

- `wagon-ssh-external`
- The 3.x maintenance line — JSch stays there
- Retiring `wagon-ssh`
