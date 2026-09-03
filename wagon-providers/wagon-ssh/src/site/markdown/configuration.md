---
title: Wagon SSH provider configuration
author:
  - Michal Maczka
---

<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  -->

<a name="Configuration_1"></a>

# Configuration

This wagon can be configured in some ways:

<table>
  <tr>
    <th>What? / Interface</th>
    <th>Default Value</th>
    <th>Why?</th>
  </tr>
  <tr>
    <td>
      <a href="apidocs/org/apache/maven/wagon/providers/ssh/KnownHostsProvider.html">Known Hosts Provider</a>
    </td>
    <td>
      <a href="apidocs/org/apache/maven/wagon/providers/ssh/knownhost/FileKnownHostsProvider.html">FileKnownHostsProvider</a>
      with fallback to
      <a href="apidocs/org/apache/maven/wagon/providers/ssh/knownhost/NullKnownHostsProvider.html">NullKnownHostProvider</a>
    </td>
    <td>provides known hosts keys, needed to check the hosts
      identity. This is an important thing!
      <p>Some implementations:</p>
      <ul>
        <li>
          <a href="apidocs/org/apache/maven/wagon/providers/ssh/knownhost/NullKnownHostsProvider.html">NullKnownHostProvider</a>:
          Don't provide anything
        </li>
        <li>
          <a href="apidocs/org/apache/maven/wagon/providers/ssh/knownhost/SingleKnownHostsProvider.html">SingleKnownHostProvider</a>:
          One host key can be setuped
        </li>
        <li>
          <a href="apidocs/org/apache/maven/wagon/providers/ssh/knownhost/FileKnownHostsProvider.html">FileKnownHostProvider</a>:
          Load known hosts keys from a openssh's <code>~/.ssh/known_hosts</code>
          like stream
        </li>
        <li>
          <a href="apidocs/org/apache/maven/wagon/providers/ssh/knownhost/StreamKnownHostsProvider.html">StreamKnownHostProvider</a>:
          Load known hosts keys from <code>~/.ssh/known_hosts</code> (you can
          set another location)
        </li>
      </ul>
    </td>
  </tr>
  <tr>
    <td>
      <a href="apidocs/org/apache/maven/wagon/providers/ssh/HostCheckingEnum.html">Host Checking</a>
    </td>
    <td>Ask (type safe enum)</td>
    <td>The policy with the hosts keys:
      <ul>
        <li>
          <strong>Yes:</strong>
          Check host keys. If the incoming
          key is not available in the Known Hosts Provider
          fails
        </li>
        <li>
          <strong>Ask:</strong>
          If the incoming key is not
          available in the Known Hosts Provider it ask the user
          if the fingerprint is trusted
        </li>
        <li>
          <strong>No:</strong>
          don't check host keys at all.
          pretty unsafe!!
        </li>
      </ul>
    </td>
  </tr>
  <tr>
    <td>
      <a href="apidocs/org/apache/maven/wagon/providers/ssh/InteractiveUserInfo.html">Interactive User Info</a>
    </td>
    <td>
      <a href="apidocs/org/apache/maven/wagon/providers/ssh/knownhost/ConsoleInteractiveUserInfo.html">ConsoleInteractiveUserInfo</a>
    </td>
    <td>If the user choose
      <em>Ask</em>
      as
      <em>Host Checking</em>
      , this
      bean is used to interact with the user
      <p>Some implementations:</p>
      <ul>
        <li>
          <a href="apidocs/org/apache/maven/wagon/providers/ssh/knownhost/NullInteractiveUserInfo.html">NullInteractiveUserInfo</a>
        </li>
        <li>
          <a href="apidocs/org/apache/maven/wagon/providers/ssh/knownhost/ConsoleInteractiveUserInfo.html">ConsoleInteractiveUserInfo</a>
        </li>
      </ul>
    </td>
  </tr>
</table>

**TODO** Autogenerate some of this information with a xdoclet2 plugin?
