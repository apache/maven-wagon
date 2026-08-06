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

# Deprecation Notice

This Wagon provider is deprecated and will be removed in version 4.0.0.

# Maven Wagon SSH

This component is an implementation of Wagon provider for SCP and SFTP access.

It enables Maven to deploy artifacts and sites to SSH servers. It uses [JSch - Java Secure Channel](http://www.jcraft.com/jsch/) as lower level layer.

Getting files from SSH servers is not fully tested.

# Features

- Deploy files and directories to SSH servers
