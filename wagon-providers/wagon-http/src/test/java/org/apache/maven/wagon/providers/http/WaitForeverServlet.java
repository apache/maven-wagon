/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.wagon.providers.http;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: jdumay
 * Date: 24/01/2008
 * Time: 17:25:27
 */
public class WaitForeverServlet extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(WaitForeverServlet.class);

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("Calling WaitForeverServlet");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // eat
        }
    }
}
