---
title: Development and Testing Guide
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

# Development and Testing Guide

This guide is for someone writing or testing a Wagon provider — either a new one, or
one of those in this repository. If you only want to *use* a Wagon, read the
[User Guide](./user-guide.html).

## The modules

| Module | Contents |
| --- | --- |
| `wagon-provider-api` | the `Wagon` interface, the base classes, events, exceptions |
| `wagon-provider-test` | reusable abstract JUnit test cases every provider is expected to pass |
| `wagon-providers/*` | the providers shipped with Wagon |
| `wagon-tcks/wagon-tck-http` | a behavioural test compatibility kit for HTTP providers |

`wagon-provider-test` and `wagon-tck-http` are both released artifacts, so a provider
maintained outside this repository can depend on them.

## The SPI

### `Wagon`

`org.apache.maven.wagon.Wagon` is the contract; see the
[User Guide](./user-guide.html#What_a_Wagon_is) for the shape of it. Two constants
matter to an implementor: `Wagon.ROLE` (the Plexus role, equal to the interface's
fully qualified name) and the timeout defaults `DEFAULT_CONNECTION_TIMEOUT` (60000)
and `DEFAULT_READ_TIMEOUT` (1800000).

### `AbstractWagon`

`AbstractWagon` implements everything that is protocol-independent, and you almost
certainly want to extend it (directly or via `StreamWagon`). It gives you:

- **The connect/disconnect skeleton.** All six `connect` overloads funnel into
  `connect(Repository, AuthenticationInfo, ProxyInfoProvider)`, which applies any
  `permissionsOverride`, defaults a `null` `AuthenticationInfo` to an empty one,
  falls back to credentials embedded in the repository URL, fires `SESSION_OPENING`,
  calls your `openConnectionInternal()`, then fires `SESSION_OPENED`. On failure it
  fires `SESSION_CONNECTION_REFUSED`. `disconnect()` fires `SESSION_DISCONNECTING`,
  calls your `closeConnection()` and fires `SESSION_DISCONNECTED`.
- **The byte pump.** `transfer(Resource, InputStream, OutputStream, int requestType,
  long maxSize)` copies bytes and fires `TRANSFER_PROGRESS` as it goes. The buffer
  size comes from `getBufferCapacityForTransfer(long)`, which aims for roughly 100
  progress notifications per resource, clamped between `DEFAULT_BUFFER_SIZE` (4 KiB)
  and `MAXIMUM_BUFFER_SIZE` (512 KiB). Data is only written once the buffer is at
  least half full, to avoid fragmenting the stream into tiny chunks.
- **`getTransfer(...)` / `putTransfer(...)`**, which wrap the pump with the right
  events, create the destination's parent directories, and delete a partially written
  destination file if the transfer fails.
- **All the `fire*` methods** — `fireGetInitiated`, `fireGetStarted`,
  `fireGetCompleted`, `firePutInitiated`, `firePutStarted`, `firePutCompleted`,
  `fireTransferProgress`, `fireTransferError`, `fireTransferDebug`, and the
  `fireSession*` family.
- **Listener plumbing**, timeouts, `interactive`, and `getProxyInfo(String protocol,
  String host)` which already applies the `nonProxyHosts` filter.
- **Default optional operations** — `putDirectory`, `getFileList` and
  `resourceExists` throw `UnsupportedOperationException`, and
  `supportsDirectoryCopy()` returns `false`. Override the ones your protocol can do.

The two methods you must supply are:

```java
protected abstract void openConnectionInternal() throws ConnectionException, AuthenticationException;

protected abstract void closeConnection() throws ConnectionException;
```

There are also four empty hooks you can override to release protocol resources at the
right moment: `finishGetTransfer`, `cleanupGetTransfer`, `finishPutTransfer`,
`cleanupPutTransfer`.

`postProcessListeners(Resource, File, int requestType)` is provided for providers that
do *not* stream through `transfer(...)` — an external process, say. It re-reads the
local file and fires progress events so that state-dependent listeners such as
`ChecksumObserver` still see the bytes.

### `StreamWagon`

`StreamWagon extends AbstractWagon implements StreamingWagon` and is the base class
most providers actually use. It reduces the job to two methods:

```java
public abstract void fillInputData(InputData inputData)
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

public abstract void fillOutputData(OutputData outputData) throws TransferFailedException;
```

`InputData` and `OutputData` are mutable holders of a `Resource` plus a stream. On a
download, `StreamWagon` hands you an `InputData` with the `Resource` filled in; you
open the remote stream, call `inputData.setInputStream(...)`, and set the resource's
content length and last-modified time if you know them. Everything else — the
`getIfNewer` timestamp comparison, event firing, writing to the destination file,
closing streams, `getToStream`, `putFromStream` — is handled for you.

`FileWagon` is the smallest complete example: `fillInputData`, `fillOutputData`,
`openConnectionInternal` and `closeConnection`, plus overrides for the optional
directory and existence operations.

If your protocol cannot express uploads as an `OutputStream` — `AbstractHttpClientWagon`
cannot, because HttpClient wants an entity that writes itself — override `put` and
`putFromStream` directly and make `fillOutputData` throw.

### `CommandExecutor`

`CommandExecutor extends Wagon` and adds `void executeCommand(String)` and
`Streams executeCommand(String, boolean ignoreFailures)`, the latter returning the
command's standard output and standard error. Its role constant is
`CommandExecutor.ROLE`, distinct from `Wagon.ROLE`, so it is a separate component
registration. `AbstractJschWagon` (`scp`, `sftp`) and `ScpExternalWagon` (`scpexe`)
implement the interface; the registration under the `CommandExecutor` role is done by
a thin empty subclass in each module — `ScpCommandExecutor extends ScpWagon` under
hint `scp`, `ScpExternalCommandExecutor` under hint `scpexe`. Failures are reported as
`CommandExecutionException`.

## Registering a provider

A provider is a Plexus component with role `org.apache.maven.wagon.Wagon` and a
role hint equal to the URL scheme it handles. There are two ways to declare it, and
both are in use in this repository.

### Annotation (most providers)

Annotate the class with JSR-330 `@Named`, using the role hint as the name:

```java
@Named("file")
public class FileWagon extends StreamWagon {
```

`sisu-maven-plugin` indexes the annotated classes into `META-INF/sisu/javax.inject.Named`
at build time. The plugin is declared once in the root POM and its executions come from
`maven-parent`, so a provider module needs nothing of its own.

Do not add `@Singleton`. A Wagon holds connection state, so it must not be shared across
sessions, and an unscoped JSR-330 bean is already created per lookup. Add `@Singleton`
only for a component that genuinely is one, such as
`LightweightHttpWagonAuthenticator`, which registers itself as the JVM-wide
`java.net.Authenticator`.

Sisu publishes a bean under every interface it implements, whereas the role in a Plexus
descriptor named exactly one. Where a class implements more than the role it should be
published under, pin it with `@Typed`:

```java
@Named("scp")
@Typed(Wagon.class)
public class ScpWagon extends AbstractJschWagon {
```

`AbstractJschWagon` implements both `SshWagon` and `CommandExecutor`, so without
`@Typed` every jsch Wagon would also register as a `CommandExecutor` — and
`ScpCommandExecutor`, which extends `ScpWagon`, would collide with its own parent on
the `scp` hint.

Collaborators are injected with `@Inject`, and `@Named` alongside it when a specific
hint is wanted:

```java
@Inject
@Named("file")
private volatile KnownHostsProvider knownHostsProvider;
```

The same mechanism registers the non-`Wagon` components in `wagon-ssh-common` —
`KnownHostsProvider` under hints `file`, `single` and `null`, and `InteractiveUserInfo`.

### Hand-written descriptor

When one implementation class serves several hints, it is simpler to write
`src/main/resources/META-INF/plexus/components.xml` yourself. `wagon-http` does this
to bind `HttpWagon` to both `http` and `https`:

```xml
<component-set>
  <components>
    <component>
      <role>org.apache.maven.wagon.Wagon</role>
      <role-hint>http</role-hint>
      <implementation>org.apache.maven.wagon.providers.http.HttpWagon</implementation>
      <instantiation-strategy>per-lookup</instantiation-strategy>
    </component>
    <component>
      <role>org.apache.maven.wagon.Wagon</role>
      <role-hint>https</role-hint>
      <implementation>org.apache.maven.wagon.providers.http.HttpWagon</implementation>
      <instantiation-strategy>per-lookup</instantiation-strategy>
    </component>
  </components>
</component-set>
```

`wagon-webdav-jackrabbit` does the same for `dav`, `davs`, `dav+http` and `dav+https`.

### Configurable properties

A field with a setter is settable from a `<configuration>` block — in a Maven build,
the one inside `<server>` in `settings.xml`. The element name is the field name, and the
setter is what makes it reachable from outside:

```java
private boolean passiveMode = true;

public void setPassiveMode(boolean passiveMode) {
    this.passiveMode = passiveMode;
}
```

Give the field its default as an initialiser. Configuration is applied to the instance
after it is created, by the wagon configurator in Maven Resolver, so a plain field
initialiser is the default and no annotation is involved. See the
[HTTP Configuration guide](./http-configuration.html) for the largest example of this in
the project.

## Testing with `wagon-provider-test`

`wagon-provider-test` contains abstract test cases that exercise a provider through
the public API against a real endpoint that you set up. It is a `compile`-scope
artifact that drags in JUnit 5, `plexus-testing`, Mockito and Jetty; the
`wagon-providers` parent POM already declares it at `test` scope for every provider
module.

### `WagonTestCase`

`WagonTestCase` is annotated `@PlexusTest` and implements `PlexusTestConfiguration`,
which is what gives it a container. Two abstract methods:

```java
protected abstract String getProtocol();          // the role hint, e.g. "file"
protected abstract String getTestRepositoryUrl(); // where the tests should write
```

`getWagon()` looks the provider up with `container.lookup(Wagon.class, getProtocol())` and
attaches an `observers.Debug` as both session and transfer listener, so a failing run
prints a transcript.

Hooks worth knowing:

| Hook | Purpose |
| --- | --- |
| `setupWagonTestingFixtures()` / `tearDownWagonTestingFixtures()` | start and stop your server before/after each test |
| `getAuthInfo()` | credentials to connect with; defaults to an empty `AuthenticationInfo` |
| `getPermissions()` | `RepositoryPermissions` for the test repository |
| `getWagon()` | override to configure the instance under test |
| `customizeContext()` | add values to the Plexus context |
| `supportsGetIfNewer()` | return `false` to skip the conditional-download tests |
| `getExpectedLastModifiedOnGet(Repository, Resource)` | what the provider should report as last-modified |
| `getExpectedContentLengthOnGet(int)` | what it should report as content length |
| `assertOnTransferProgress()` | return `false` if progress event counts cannot be asserted |
| `testSkipped` | set in `setUp` to make `runTest()` skip the test entirely |

What you inherit: a file round trip (`testWagon`), the three `getIfNewer` cases,
`putDirectory` in four shapes (including a deep destination, an existing directory,
and `.` as the destination), failure paths (`testFailedGet`, `testFailedGetIfNewer`),
`getFileList` present and absent, and `resourceExists` true and false. The tests use
a Mockito mock `TransferListener` to assert that the right events fire in the right
order with the right byte counts.

`FileWagonTest` is the shortest example of a subclass — it supplies a protocol and a
temporary directory URL and gets the whole suite.

### `StreamingWagonTestCase`

Extends `WagonTestCase` and adds the `StreamingWagon` coverage: `testStreamingWagon`
(a stream round trip), `testFailedGetToStream`, `testFailedGetIfNewerToStream` and
the three `getIfNewerToStream` cases. Extend this one if your provider implements
`StreamingWagon`.

### `CommandExecutorTestCase`

For `CommandExecutor` implementations. Supply `getTestRepository()`; you get
`testExecuteSuccessfulCommand`, `testErrorInCommandExecuted` and
`testIgnoreFailuresInCommandExecuted`.

### `HttpWagonTestCase`

`org.apache.maven.wagon.http.HttpWagonTestCase extends StreamingWagonTestCase` and is
the big one — roughly forty tests run against an embedded Jetty server that it starts
and stops for you: custom and default headers, user-agent handling, 403/404/500/429
responses on GET, PUT and HEAD, gzip and deflate content encodings, proxies with and
without authentication, redirects (GET and PUT, absolute and relative URLs, and the
non-repeatable-stream case), and Basic authentication in every combination.

Four methods to implement:

```java
protected abstract void setHttpConfiguration(StreamingWagon wagon, Properties headers, Properties params);
protected abstract boolean supportPreemptiveAuthenticationGet();
protected abstract boolean supportPreemptiveAuthenticationPut();
protected abstract boolean supportProxyPreemptiveAuthentication();
```

The three `support*` methods are not feature switches you can set freely — they are
assertions about what your provider does, and the preemptive-authentication tests
check the request count against them. `HttpWagonTest` (wagon-http) declares
`Get=false, Put=true, Proxy=true`; `HttpWagonPreemptiveTest` subclasses it, turns
`usePreemptive` on in `httpConfiguration`, and declares `Get=true`.

`verifyWagonExceptionMessage(Exception, int statusCode, String url, String reasonPhrase)`
is available to assert that your exception messages carry the URL, status code and
reason phrase the way `HttpMessageUtils` formats them.

## The HTTP TCK

`wagon-tcks/wagon-tck-http` is a separate JUnit 5 suite that tests *behaviour* rather
than mechanics: what a provider does about redirects, latency, timeouts, missing
hosts and authentication. Unlike `wagon-provider-test`, a provider can declare
individual use cases unsupported rather than failing them.

### Structure

- `HttpWagonTests` — the abstract base. Per test it starts a `ServerFixture`, looks
  the wagon up from a `DefaultPlexusContainer` using the configured hint, and tears
  everything down afterwards.
- `GetWagonTests extends HttpWagonTests` — the actual tests.
- `HttpsGetWagonTests extends GetWagonTests` — the same tests with `isSsl()` true.
- `WagonTestCaseConfigurator` — a Plexus component that names the wagon under test and
  carries per-use-case configuration.
- `fixture/ServerFixture` — an embedded Jetty with a webapp rooted at the
  `default-server-root` classpath resource, HTTP Basic authentication on
  `/protected/*` via a `HashLoginService`, an ephemeral port, and optional TLS from
  the `ssl/keystore` resource (password `wagonhttp`). `addServlet`, `addFilter` and
  `addUser` let a test wire in what it needs.
- `fixture/*` — the servlets and filters the tests wire in: `LatencyServlet`,
  `RedirectionServlet`, `ErrorCodeServlet`, `ServletExceptionServlet`,
  `ProxyAuthenticationFilter`, `ProxyConnectionVerifierFilter`, `AuthSnoopFilter`.
- `Assertions` — `assertFileContentsFromResource(...)` compares a downloaded file
  against a classpath resource, and `assertWagonExceptionMessage(...)` checks that an
  exception message carries the expected URL, status code and reason phrase.

### The test cases

`basic`, `proxied`, `highLatencyHighTimeout`, `highLatencyLowTimeout`,
`inifiniteLatencyTimeout` (spelled that way in the source), `nonExistentHost`,
`oneLevelPermanentMove`, `oneLevelTemporaryMove`, `sixLevelPermanentMove`,
`sixLevelTemporaryMove`, `infinitePermanentMove`, `infiniteTemporaryMove`,
`permanentMove_TooManyRedirects_limit20`, `temporaryMove_TooManyRedirects_limit20`,
`missing`, `error`, `proxyTimeout`, `forbidden`, `successfulAuthentication`,
`unsuccessfulAuthentication`.

### Wiring the TCK into a provider

Add `wagon-tck-http` and `junit-platform-suite` at test scope, declare a suite, and
configure the `WagonTestCaseConfigurator` component. `junit-platform-suite` is what
provides `@Suite`; without it the suite class compiles but selects nothing.
`wagon-http` does exactly this:

```java
@Suite
@SelectClasses({GetWagonTests.class, HttpsGetWagonTests.class})
public class TckTest {
}
```

and in `src/test/resources/META-INF/plexus/components.xml`:

```xml
<component-set>
  <components>
    <component>
      <role>org.apache.maven.wagon.tck.http.WagonTestCaseConfigurator</role>
      <implementation>org.apache.maven.wagon.tck.http.WagonTestCaseConfigurator</implementation>
      <configuration>
        <wagonHint>http</wagonHint>
        <useCaseConfigsResource>META-INF/wagon-tck/http-use-cases.xml</useCaseConfigsResource>
      </configuration>
    </component>
  </components>
</component-set>
```

`<wagonHint>` is the role hint to look up. `<useCaseConfigsResource>` points at a
classpath resource holding the per-use-case configuration, whose root element is
`<useCaseConfigs>`:

```xml
<useCaseConfigs>
  <highLatencyLowTimeout>
    <unsupported/>
  </highLatencyLowTimeout>
  <inifiniteLatencyTimeout>
    <unsupported/>
  </inifiniteLatencyTimeout>
</useCaseConfigs>
```

It lives in its own resource rather than inline in the descriptor because the Plexus
shim cannot inject a nested XML tree into a component. Each child of `<useCaseConfigs>`
is named
after a test method. An `<unsupported/>` child makes the TCK log the skip and let the
test pass; any other content is applied to the wagon instance as Plexus component
configuration before the test runs, which is how you turn on a provider-specific
option for one test case. The test method name is discovered by walking the stack, so
the element name must match the method name exactly.

A provider with no `WagonTestCaseConfigurator` configuration at all will fail to look
one up; the configurator component itself must be declared even if it names no
use cases.

### Run the TCK classes, do not subclass them from your own package

`HttpWagonTests.isSupported()` and `HttpWagonTests.initTest(...)` recover the use-case
id by walking the current stack and taking the last method name seen before the first
frame whose class name does not start with `getClass().getPackage().getName()` — the
package of the *runtime* class. That works for `GetWagonTests` and
`HttpsGetWagonTests`, which live in `org.apache.maven.wagon.tck.http` alongside the
frame they are looking for.

It does not work if you subclass a TCK test class from your own package. The very
first frame examined is then already outside your package, the walk stops with no
method name, and every test logs `Cannot run test: null` and is treated as
unsupported — so the suite passes without having tested anything.

Wire the TCK up the way `wagon-http` does: run the TCK classes as they are, through a
`@SelectClasses` aggregator, and express everything provider-specific through
the use-case configuration rather than through a subclass.

## Running the tests

The full build:

```
mvn verify
```

Some suites are gated:

**SSH.** `wagon-ssh` and `wagon-ssh-external` both define a `no-ssh-tests` profile
that activates when the `ssh-tests` property is *absent* and excludes the tests that
need a real SSH server (`SftpWagonTest`, `SshCommandExecutorTest`,
`KnownHostsProviderTest`, `ScpWagon*Test`, and in `wagon-ssh` also `Embedded*Test`).
So by default those tests do not run. Pass `-Dssh-tests` to run them — you need an
SSH server on localhost that accepts the account named by the `test.user` system
property, which the profiles set to `olamy`, so override it:

```
mvn verify -pl wagon-providers/wagon-ssh -Dssh-tests -Dtest.user=$USER
```

A `ssh-embedded` profile (activated by `-Dssh-embedded=true`) runs against the
embedded Apache MINA SSHD server from `wagon-ssh-common-test` — it excludes the same
tests minus `Embedded*Test`. On Windows a `windauze` profile applies the same
exclusions as `no-ssh-tests`.

`wagon-ssh-common-test` is where the embedded-server support lives:
`SshServerEmbedded`, `AbstractEmbeddedScpWagonTest`,
`AbstractEmbeddedScpWagonWithKeyTest`, `TestPasswordAuthenticator`,
`TestPublickeyAuthenticator`, `TestData`, plus test key pairs under
`src/main/resources/ssh-keys`.

**HTTP.** No profile is needed. The tests and the TCK start their own Jetty on an
ephemeral port.

There is no `run-its` profile in this repository, despite what `README.md` says.

## Building the site

```
mvn site
```

The site is aggregated from the root `src/site` plus each module's own `src/site`.
Pages live in `src/site/markdown/`; register new ones in `src/site/site.xml`.

## Code style

Formatting comes from the Apache Maven parent POM, which configures Spotless with
`spotless.action` defaulting to `apply` — a normal build reformats the sources it
compiles, so you do not need to format by hand. Follow the rest of the
[Maven code style](https://maven.apache.org/developers/conventions/code.html):
spaces only for indentation, and no drive-by reformatting of code you did not
otherwise touch. See `README.md` for the contribution process.
