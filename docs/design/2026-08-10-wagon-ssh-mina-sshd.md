# Design: replace JSch with Apache MINA SSHD in wagon-ssh

Date: 2026-08-10
Target: `org.apache.maven.wagon:wagon-ssh` 4.0.0-M1
Status: approved design, implementation plan not yet written

## Motivation

`wagon-ssh` pins `com.jcraft:jsch:0.1.55` (last released 2018) and
`com.jcraft:jsch.agentproxy:0.0.9`. Neither is maintained. JSch 0.1.55 does not
implement `rsa-sha2-256`/`rsa-sha2-512` and cannot read OpenSSH-format ed25519
keys, so it fails against the defaults of current OpenSSH servers.

Apache MINA SSHD 2.19.0 is already a dependency of the build:
`wagon-ssh-common-test` runs the embedded test server on `sshd-core`,
`sshd-scp` and `sshd-sftp`. Moving the client side onto SSHD adds no new
third-party surface and puts both ends of the test on the same stack.

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
- `com.jcraft:jsch:0.1.55`
- `com.jcraft:jsch.agentproxy.connector-factory:0.0.9`
- `com.jcraft:jsch.agentproxy.jsch:0.0.9`

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

1. Before touching anything, enable `ssh-embedded` and record the green
   baseline. **If the embedded tests do not pass when un-excluded, fixing them
   is a prerequisite, not part of this migration.**
2. Migrate with those tests as the gate. `SftpWagonTest` and
   `SshCommandExecutorTest` appear in every `<excludes>` block in
   `wagon-ssh/pom.xml`; they must be un-excluded against the embedded server or
   the sftp rewrite ships untested.
3. New tests: a SOCKS5 proxy test with a minimal SOCKS5 listener in
   `wagon-ssh-common-test`, mirroring the existing `ScpWagonWithProxyTest`
   shape; and an assertion on rsa-sha2 negotiation that would have failed on
   JSch 0.1.55.
4. Manual matrix before release: real OpenSSH 9.x, `~/.ssh/config` in play,
   ed25519 key, agent present and absent.

## Sequencing

Five PRs, each independently green:

1. Hoist `sshdVersion` into the root `dependencyManagement`; un-exclude the
   embedded tests; record the baseline. No behaviour change.
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
