 ------
 Introduction
 ------
 Carlos Sanchez
 ------
 2006-03-07
 ------

 ~~ Licensed to the Apache Software Foundation (ASF) under one
 ~~ or more contributor license agreements.  See the NOTICE file
 ~~ distributed with this work for additional information
 ~~ regarding copyright ownership.  The ASF licenses this file
 ~~ to you under the Apache License, Version 2.0 (the
 ~~ "License"); you may not use this file except in compliance
 ~~ with the License.  You may obtain a copy of the License at
 ~~
 ~~   http://www.apache.org/licenses/LICENSE-2.0
 ~~
 ~~ Unless required by applicable law or agreed to in writing,
 ~~ software distributed under the License is distributed on an
 ~~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~~ KIND, either express or implied.  See the License for the
 ~~ specific language governing permissions and limitations
 ~~ under the License.

 ~~ NOTE: For help with the syntax of this file, see:
 ~~ http://maven.apache.org/doxia/references/apt-format.html

Maven Wagon SCM

 This component is an implementation of Wagon provider for SCM (Source Control Management) systems,
 using {{{/scm/}Maven SCM}}.

 It enables Maven to use remote repositories stored in SCM systems (Subversion, CVS, ...)
 and to store Maven sites in SCMs.

Features

 * Deploy files and directories to several SCM using Maven SCM (only tested with CVS and SVN)

 * Get files from several SCM using Maven SCM (only tested with CVS and SVN)


Known issues

 * If the file is changed by the SCM, the checksum calculation may not work

   * SVN: when a file has properties <<<svn:eol-style>>> or <<<svn:keywords>>> set

   * CVS: when keywords are expanded


See {{{/plugins/maven-scm-publish-plugin/}maven-scm-publish-plugin}} to publish your site to SCM.
