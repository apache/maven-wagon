 ------
 Maven Wagon HTTP
 ------
 Carlos Sanchez
 Olivier Lamy
 ------
 2013-02-05
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
 ~~ https://maven.apache.org/doxia/references/apt-format.html

Maven Wagon HTTP

 This component is an implementation of Wagon provider for HTTP access.
 It uses {{{https://hc.apache.org/httpcomponents-client-4.5.x/}Apache HttpComponents Client 4.5.x}} as lower level layer.

 It enables Maven to use remote repositories stored in HTTP servers.


Features

 Starting with version 2.0, a pooled http connection manager is used.
 You can configure it through the following system properties:

 * <<<maven.wagon.http.pool>>> = true/false (default true), enable/disable the pool mechanism.

 * <<<maven.wagon.httpconnectionManager.maxPerRoute>>> = integer (default 20), maximum number of http(s) connection per destination.

 * <<<maven.wagon.httpconnectionManager.maxTotal>>> = integer (default 40), maximum number of http(s) connection.

 []

 Other features can be configured through system properties:

 * <<<maven.wagon.http.ssl.insecure>>> = <<<true>>>/<<<false>>> (<<<false>>> by default), enable/disable relaxed check of public key certificates (e.g. self-signed ones). Relaxed check means that any chain with 1 or more certificates will be considered valid if all the certificate dates in the chain are valid (dates check can be overridden as well - see below). Setting it to <<<true>>> also enables usage of the following properties:

    * <<<maven.wagon.http.ssl.allowall>>> = <<<true>>>/<<<false>>> (<<<false>>> by default), whether to match the server's X.509 certificate against a requested IP/DNS name. If <<<false>>>/unset, a regular server check will be used, which means that the server's IP/DNS must match either the first CN, the Subject field or one of the Subject Alternative Name extension values (in case Subject or SAN type is either <<<dNSName>>> or <<<iPAddress>>> - see {{{https://tools.ietf.org/html/rfc5280}RFC 5280 for more details}}). Otherwise, no such matching will be applied.

    * <<<maven.wagon.http.ssl.ignore.validity.dates>>> = <<<true>>>/<<<false>>> (<<<false>>> by default), whether to ignore issues with certificate dates (i.e. when a certificate is expired or not yet valid).

 * <<<maven.wagon.rto>>> = time in ms (default 1800000), read time out.

 []

 Since version 3.2, the retry handler can be configured with system properties:

 * <<<maven.wagon.http.retryHandler.class>>> supports this set of values:

    * <<<default>>> will use an instance of {{{https://hc.apache.org/httpcomponents-client-4.5.x/httpclient/apidocs/org/apache/http/impl/client/DefaultHttpRequestRetryHandler.html}<<<DefaultHttpRequestRetryHandler>>>}} respecting <<<requestSentEnabled>>>, <<<count>>> and <<<nonRetryableClasses>>>.

    * <<<standard>>> will use an instance of {{{https://hc.apache.org/httpcomponents-client-4.5.x/httpclient/apidocs/org/apache/http/impl/client/StandardHttpRequestRetryHandler.html}<<<StandardHttpRequestRetryHandler>>>}} respecting <<<requestSentEnabled>>> and <<<count>>>.

    * Any fully qualified name of a {{{https://hc.apache.org/httpcomponents-client-4.5.x/httpclient/apidocs/org/apache/http/client/HttpRequestRetryHandler.html}<<<HttpRequestRetryHandler>>>}} implementation will be instantiated with its default constructor.

        [Attention] This will not work with the shaded version bundled with Maven.

 * <<<maven.wagon.http.retryHandler.requestSentEnabled>>> = <<<requestSentEnabled>>> for <<<default>>> or <<<standard>>> implementations.

 * <<<maven.wagon.http.retryHandler.count>>> = number of retries for <<<default>>> or <<<standard>>> implementations.

 * <<<maven.wagon.http.retryHandler.nonRetryableClasses>>> = a comma-separated list of fully qualified class names bypassing the retries (only the <<<default>>> implementation).
 If not set, the default value from {{{https://hc.apache.org/httpcomponents-client-4.5.x/httpclient/apidocs/org/apache/http/impl/client/DefaultHttpRequestRetryHandler.html}<<<DefaultHttpRequestRetryHandler>>>}} will be used.

 []

        [Attention] Any retry handler can only react to exceptions when executing the request and receiving the response head.
                    It will <not> salvage in-flight failures of ongoing response body streams.
