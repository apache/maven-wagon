---
title: User Guide
date: 2026-08-08
---

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# User Guide

This guide is for someone who wants to *use* a Wagon: transfer a file to or from a
remote repository, or configure the Wagon that Maven is already using on their
behalf. If you want to *write* a Wagon provider, read the
[Development and Testing Guide](./developer-guide.html) instead.

## What a Wagon is

A Wagon is a transport. It knows how to move a byte stream between the local file
system and a remote repository over one particular protocol, and nothing more. It
does not know about artifacts, POMs, checksums or repository layout; it moves
resources identified by a path relative to a repository base URL.

The whole contract is the
[`org.apache.maven.wagon.Wagon`](./apidocs/org/apache/maven/wagon/Wagon.html)
interface, which has four groups of methods:

- **transfer** — `get`, `getIfNewer`, `put`, `putDirectory`
- **query** — `resourceExists`, `getFileList`, `supportsDirectoryCopy`, `getRepository`
- **session** — `connect`, `disconnect`, `setTimeout`/`getTimeout`,
  `setReadTimeout`/`getReadTimeout`, `setInteractive`/`isInteractive`
- **events** — `addTransferListener`, `addSessionListener` and their `remove`/`has`
  counterparts

Providers that can also expose raw streams implement
[`StreamingWagon`](./apidocs/org/apache/maven/wagon/StreamingWagon.html), which adds
`getToStream`, `getIfNewerToStream` and `putFromStream`.

## The providers and the URL schemes they handle

A provider registers itself under one or more *role hints*. The role hint is the URL
scheme: when something asks for a Wagon for `https://repo.example.com/`, it looks up
the `Wagon` component with hint `https`.

| Module | Role hints (URL schemes) | Implementation |
| --- | --- | --- |
| [wagon-file](./wagon-providers/wagon-file/) | `file` | `FileWagon` |
| [wagon-http](./wagon-providers/wagon-http/) | `http`, `https` | `HttpWagon` (Apache HttpClient 4.5.x) |
| [wagon-http-lightweight](./wagon-providers/wagon-http-lightweight/) | `http`, `https` | `LightweightHttpWagon`, `LightweightHttpsWagon` (`java.net.HttpURLConnection`) |
| [wagon-ftp](./wagon-providers/wagon-ftp/) | `ftp`, `ftps`, `ftph` | `FtpWagon`, `FtpsWagon`, `FtpHttpWagon` (Commons Net) |
| [wagon-ssh](./wagon-providers/wagon-ssh/) | `scp`, `sftp` | `ScpWagon`, `SftpWagon` (JSch) |
| [wagon-ssh-external](./wagon-providers/wagon-ssh-external/) | `scpexe` | `ScpExternalWagon` (invokes the system `ssh`/`scp`) |
| [wagon-webdav-jackrabbit](./wagon-providers/wagon-webdav-jackrabbit/) | `dav`, `davs`, `dav+http`, `dav+https` | `WebDavWagon` |
| [wagon-scm](./wagon-providers/wagon-scm/) | `scm` | `ScmWagon` (Maven SCM) |

`wagon-http` and `wagon-http-lightweight` both claim `http` and `https`; only one of
them can be on the classpath for a given scheme.

`WebDavWagon` rewrites its own URL before connecting, so `dav://`, `davs://`,
`dav+http://` and `dav+https://` all become plain `http://`/`https://` requests. The
Maven 2.0.x form `dav:http://host/path` also works, because the scheme parsed out of
it is `dav`.

`ScmWagon` passes the repository URL straight to Maven SCM, so the URL is a full SCM
URL such as `scm:svn:https://svn.example.com/repo`.

## Getting a Wagon

### In a Maven build

Declare the provider as a build extension. Maven will then resolve the URL scheme to
that provider for repositories and for `distributionManagement`:

```xml
<build>
  <extensions>
    <extension>
      <groupId>org.apache.maven.wagon</groupId>
      <artifactId>wagon-ftp</artifactId>
      <version>3.5.3</version>
    </extension>
  </extensions>
</build>
```

### Programmatically

Providers are Plexus components with role `org.apache.maven.wagon.Wagon` — the
constant `Wagon.ROLE` — and role hint equal to the URL scheme. Each is declared with
`instantiation-strategy="per-lookup"`, so every lookup gives you a fresh instance;
a Wagon is not safe to share between concurrent sessions.

```java
PlexusContainer container = new DefaultPlexusContainer();
Wagon wagon = (Wagon) container.lookup(Wagon.ROLE, "http");
```

Put the provider artifact on the classpath and the component descriptor it carries in
`META-INF/plexus/components.xml` is picked up automatically.

## The lifecycle

Connect, transfer as many times as you like, disconnect:

```java
Wagon wagon = (Wagon) container.lookup(Wagon.ROLE, "https");

AuthenticationInfo auth = new AuthenticationInfo();
auth.setUserName("deploy");
auth.setPassword("secret");

Repository repository = new Repository("central", "https://repo.example.com/maven2");

wagon.connect(repository, auth);
try {
    wagon.get("org/example/lib/1.0/lib-1.0.jar", new File("lib-1.0.jar"));
    wagon.put(new File("lib-1.0.jar"), "org/example/lib/1.0/lib-1.0.jar");
} finally {
    wagon.disconnect();
}
```

`connect` is overloaded six ways, covering the presence or absence of an
`AuthenticationInfo` and of a proxy (either a fixed `ProxyInfo` or a
`ProxyInfoProvider`). All of them funnel into
`connect(Repository, AuthenticationInfo, ProxyInfoProvider)`, which stores the
repository, fires `SESSION_OPENING`, opens the connection and fires `SESSION_OPENED`.
`disconnect` fires `SESSION_DISCONNECTING`, closes, and fires `SESSION_DISCONNECTED`.

`connect(Repository)` with a `null` repository throws `NullPointerException`.

`openConnection()` is deprecated and is an internal method; use `connect`.

### get, getIfNewer and put

- `get(String resourceName, File destination)` downloads unconditionally.
- `getIfNewer(String resourceName, File destination, long timestamp)` downloads only
  if the remote resource is newer than `timestamp` (milliseconds since the epoch,
  GMT), and returns `true` when it actually transferred something. Passing `0`
  always transfers.
- `put(File source, String destination)` uploads a single file.
- `putDirectory(File sourceDirectory, String destinationDirectory)` uploads a whole
  tree — but only if `supportsDirectoryCopy()` returns `true`.

Resource names are relative to the repository URL. The destination file's parent
directories are created for you before the transfer begins; if the transfer fails,
the partially written destination file is deleted (or marked `deleteOnExit` if it
cannot be deleted).

### Operations a provider may not implement

`AbstractWagon` — the base class for every provider in this project — implements
three methods by throwing `UnsupportedOperationException`:

- `putDirectory` — "The wagon you are using has not implemented putDirectory()"
- `getFileList` — "The wagon you are using has not implemented getFileList()"
- `resourceExists` — "The wagon you are using has not implemented resourceExists()"

and `supportsDirectoryCopy()` returns `false` by default. Check
`supportsDirectoryCopy()` before calling `putDirectory`, and be prepared for
`UnsupportedOperationException` from the other two.

Where the shipped providers stand:

| Provider | `supportsDirectoryCopy()` | `getFileList` | `resourceExists` | `StreamingWagon` |
| --- | --- | --- | --- | --- |
| `file` | yes | yes | yes | yes |
| `http`, `https` (wagon-http) | no | no | yes | yes |
| `http`, `https` (lightweight) | no | no | yes | yes |
| `ftp`, `ftps`, `ftph` | yes | yes | yes | yes |
| `scp`, `sftp` | yes | yes | yes | yes |
| `scpexe` | yes | yes | yes | no |
| `dav`, `davs`, `dav+http`, `dav+https` | yes | yes | yes | yes |
| `scm` | yes | yes | yes | no |

"no" in the `getFileList` column means the inherited `UnsupportedOperationException`.

## Repository URLs and credentials

`Repository.setUrl(String)` parses the URL into protocol, host, port and base
directory. It also understands credentials embedded in the URL: if the authority
contains `user` or `user:password`, they are extracted into
`Repository.getUsername()`/`getPassword()` **and stripped from the stored URL**, so
`getUrl()` afterwards no longer contains them.

When you call `connect` without an `AuthenticationInfo`, or with one whose user name
is `null`, `AbstractWagon` falls back to the credentials parsed out of the URL.

If no port is present in the URL, `getPort()` returns
`WagonConstants.UNKNOWN_PORT` (`-1`). If no host is present, `getHost()` returns
`localhost`.

## Authentication

Credentials are carried by
[`AuthenticationInfo`](./apidocs/org/apache/maven/wagon/authentication/AuthenticationInfo.html),
which has exactly four properties:

| Property | Used for |
| --- | --- |
| `userName` | user name |
| `password` | password |
| `privateKey` | absolute path to a private key file |
| `passphrase` | passphrase protecting that private key |

`privateKey` and `passphrase` are only meaningful for providers that authenticate
with a key pair — in practice the SSH providers.

Which authentication mechanisms are actually attempted, and how, is entirely up to
the provider. For the HTTP providers see the
[HTTP Configuration guide](./http-configuration.html).

## Proxies

A proxy is described by
[`ProxyInfo`](./apidocs/org/apache/maven/wagon/proxy/ProxyInfo.html): `host`, `port`,
`userName`, `password`, `type`, `nonProxyHosts`, plus `ntlmHost` and `ntlmDomain` for
NTLM proxies. The class defines three type constants — `PROXY_HTTP` (`"HTTP"`),
`PROXY_SOCKS4` (`"SOCKS4"`) and `PROXY_SOCKS5` (`"SOCKS_5"`) — but the value that
matters at runtime is described next.

Two things decide whether a proxy is used for a given request:

1. **Protocol match.** When you pass a plain `ProxyInfo` to `connect`, it is wrapped
   in a `ProxyInfoProvider` that returns the proxy only when the requested protocol
   is `null` or equals the proxy's `type`, case-insensitively. The HTTP providers ask
   for the repository's own protocol, so a `ProxyInfo` with type `http` is not used
   for an `https` repository. Pass a `ProxyInfoProvider` yourself if you need
   different behaviour.
2. **Non-proxy hosts.** `nonProxyHosts` is a `|`-separated list of patterns in which
   `*` is a wildcard, for example `*.example.com|localhost`. If the target host
   matches any pattern, the proxy is skipped.

## Timeouts

Two independent timeouts, both in milliseconds:

| Setter | Default | Constant |
| --- | --- | --- |
| `setTimeout(int)` — connection timeout | 60000 (1 minute) | `Wagon.DEFAULT_CONNECTION_TIMEOUT` |
| `setReadTimeout(int)` — read timeout | 1800000 (30 minutes) | `Wagon.DEFAULT_READ_TIMEOUT` |

The read timeout default can be changed globally with the system property
`maven.wagon.rto`; `AbstractWagon` reads it when the instance is created.

## Streaming versus file transfer

`Wagon.get`/`Wagon.put` work with `java.io.File`. Providers that also implement
[`StreamingWagon`](./apidocs/org/apache/maven/wagon/StreamingWagon.html) let you skip
the file:

```java
StreamingWagon wagon = (StreamingWagon) container.lookup(Wagon.ROLE, "https");
wagon.connect(repository);
try (OutputStream out = Files.newOutputStream(target)) {
    wagon.getToStream("org/example/lib/1.0/lib-1.0.jar", out);
}
```

For uploads, prefer
`putFromStream(InputStream, String destination, long contentLength, long lastModified)`.
The two-argument `putFromStream(InputStream, String)` is deprecated: without a
content length the HTTP providers fall back to chunked transfer encoding, which not
every server accepts.

Note that "streaming" describes the API you use, not the implementation: every
provider in this project streams the payload internally. `StreamWagon`, the base
class most providers extend, expresses a transfer as "fill in an `InputStream`" or
"fill in an `OutputStream`" and lets `AbstractWagon` pump the bytes.

`wagon-http-lightweight` is the exception worth knowing about — its own
documentation records that it cannot download data that does not fit in memory.

## Listening to transfers

Two listener interfaces, both registered on the Wagon:

[`TransferListener`](./apidocs/org/apache/maven/wagon/events/TransferListener.html)
receives `transferInitiated`, `transferStarted`, `transferProgress`,
`transferCompleted`, `transferError` and `debug`. The event carries a
`TransferEvent` whose `getEventType()` is one of `TRANSFER_INITIATED`,
`TRANSFER_STARTED`, `TRANSFER_PROGRESS`, `TRANSFER_COMPLETED`, `TRANSFER_ERROR`, and
whose `getRequestType()` is `REQUEST_GET` or `REQUEST_PUT`.

[`SessionListener`](./apidocs/org/apache/maven/wagon/events/SessionListener.html)
receives `sessionOpening`, `sessionOpened`, `sessionLoggedIn`, `sessionLoggedOff`,
`sessionDisconnecting`, `sessionDisconnected`, `sessionConnectionRefused`,
`sessionError` and `debug`.

Three implementations ship in the API module:

- `observers.AbstractTransferListener` — empty implementations of every
  `TransferListener` method, to subclass.
- `observers.Debug` — implements both interfaces and prints a running commentary to a
  `PrintStream` (`System.out` by default): one `#` per progress event, then a summary
  line with the byte count and elapsed time.
- `observers.ChecksumObserver` — a `TransferListener` that digests the bytes as they
  go past. The no-arg constructor uses MD5; the other takes any algorithm name the
  JDK's `MessageDigest` accepts. Read the result with `getActualChecksum()`.

`transferProgress` is called synchronously on the transfer thread, once per buffer.
A slow listener slows the transfer down. The buffer size is chosen from the resource
content length so that a transfer produces roughly 100 progress events, between 4 KiB
and 512 KiB per event; a resource of unknown length uses 4 KiB.

## Errors

Every checked exception extends
[`WagonException`](./apidocs/org/apache/maven/wagon/WagonException.html):

| Exception | Meaning |
| --- | --- |
| `ConnectionException` | `connect`/`disconnect` failed |
| `AuthenticationException` | the credentials supplied were not sufficient to establish the session |
| `AuthorizationException` | the server refused the operation on this resource |
| `ResourceDoesNotExistException` | the resource, or the directory being listed, is not there |
| `TransferFailedException` | anything else that went wrong during a transfer |
| `UnsupportedProtocolException` | no provider is registered for the URL scheme |
| `CommandExecutionException` | a `CommandExecutor` command failed |

A note on the HTTP providers: `401`, `403` and `407` all currently surface as
`AuthorizationException`, not `AuthenticationException`. The message distinguishes
them — "authentication failed for" for 401, "authorization failed for" for 403,
"proxy authentication failed for" for 407 — and includes the URL, the status code and
the reason phrase where the server sent one. `404` and `410` become
`ResourceDoesNotExistException` ("resource missing at ..."). Messages for failures
that went through a proxy also name the proxy.

The unchecked `UnsupportedOperationException` is not an error condition: it means the
provider does not implement that optional operation. See
[Operations a provider may not implement](#Operations_a_provider_may_not_implement).

## Interactive mode

`setInteractive(boolean)` tells a provider whether it may prompt the user. It
defaults to `true`. The JSch-based SSH providers (`scp`, `sftp`) act on it: when it
is `false` they discard the keyboard-interactive handler, install a
`NullInteractiveUserInfo` that answers nothing, and set JSch's `BatchMode` to `yes`.
Set it to `false` in an unattended build. `scpexe` does not honour it — its source
records that it never works in interactive mode anyway.

## File permissions on upload

For protocols that can set them, `RepositoryPermissions` carries a `group`, a
`fileMode` and a `directoryMode`; modes may be textual (`ugo+rx`) or octal (`644`).
Set them on the `Repository` before connecting, or set a `RepositoryPermissions` on
an `AbstractWagon` with `setPermissionsOverride(...)`, which replaces whatever the
repository carries at `connect` time. Providers that cannot change permissions ignore
them.
