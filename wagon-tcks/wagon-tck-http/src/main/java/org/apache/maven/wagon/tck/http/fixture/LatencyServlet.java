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
package org.apache.maven.wagon.tck.http.fixture;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class LatencyServlet extends HttpServlet {
    private static Logger logger = LoggerFactory.getLogger(LatencyServlet.class);

    private static final long serialVersionUID = 1L;

    private static final int BUFFER_SIZE = 32;

    private final int latencyMs;

    public LatencyServlet(final int latencyMs) {
        this.latencyMs = latencyMs;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        if (latencyMs < 0) {
            logger.info("Starting infinite wait.");
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            return;
        }

        String path = req.getPathInfo();

        // ignore the servlet's path here, since the servlet path is really only to provide a
        // binding for the servlet.
        String realPath = getServletContext().getRealPath(path);
        File f = new File(realPath);

        long total = 0;
        long start = System.currentTimeMillis();
        try (FileInputStream in = new FileInputStream(f);
                OutputStream out = resp.getOutputStream(); ) {

            logger.info("Starting high-latency transfer. This should take about "
                    + ((f.length() / BUFFER_SIZE * latencyMs / 1000) + (latencyMs / 1000)) + " seconds.");

            int read;
            byte[] buf = new byte[BUFFER_SIZE];
            while ((read = in.read(buf)) > -1) {
                try {
                    Thread.sleep(latencyMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("Writing bytes " + total + "-" + (total + read - 1) + " of " + f.length()
                        + ". Elapsed time so far: " + ((System.currentTimeMillis() - start) / 1000) + " seconds");
                out.write(buf, 0, read);
                total += read;
            }
        }
        logger.info("High-latency transfer done in " + (System.currentTimeMillis() - start) + "ms");
    }
}
