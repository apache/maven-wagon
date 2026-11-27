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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class AbstractHttpClientWagonTest {

    @Test
    public void retryableConfigurationDefaultTest() throws Exception {
        doTestHttpClient(() -> {
            HttpRequestRetryStrategy handler = getCurrentHandler();
            assertNotNull(handler);
            assertTrue(handler instanceof DefaultHttpRequestRetryStrategy);
            assertEquals(3, field(handler, "maxRetries"));
        });
    }

    @Test
    public void retryableConfigurationCountTest() throws Exception {
        doTestHttpClient(() -> {
            System.setProperty("maven.wagon.http.retryHandler.class", "default");
            System.setProperty("maven.wagon.http.retryHandler.count", "5");

            HttpRequestRetryStrategy handler = getCurrentHandler();
            assertNotNull(handler);
            assertTrue(handler instanceof DefaultHttpRequestRetryStrategy);
            assertEquals(5, field(handler, "maxRetries"));
        });
    }

    @Test
    public void retryableConfigurationSentTest() throws Exception {
        doTestHttpClient(() -> {
            System.setProperty("maven.wagon.http.retryHandler.class", "default");
            System.setProperty("maven.wagon.http.retryHandler.requestSentEnabled", "true");

            HttpRequestRetryStrategy handler = getCurrentHandler();
            assertNotNull(handler);
            assertTrue(handler instanceof DefaultHttpRequestRetryStrategy);
            assertEquals(3, field(handler, "maxRetries"));
            assertTrue(isIdempotent(handler, new HttpHead("/")));
        });
    }

    @Test
    public void retryableConfigurationExceptionsTest() throws Exception {
        doTestHttpClient(() -> {
            System.setProperty("maven.wagon.http.retryHandler.class", "default");
            System.setProperty("maven.wagon.http.retryHandler.nonRetryableClasses", IOException.class.getName());

            HttpRequestRetryStrategy handler = getCurrentHandler();
            assertNotNull(handler);
            assertTrue(handler instanceof DefaultHttpRequestRetryStrategy);
            assertEquals(3, field(handler, "maxRetries"));

            try {
                Set<?> exceptions = (Set<?>) field(handler, "nonRetriableIOExceptionClasses");
                assertEquals(1, exceptions.size());
                assertTrue(exceptions.contains(IOException.class));
            } catch (RuntimeException e) {
                fail(e.getMessage());
            }
        });
    }

    /** HttpClient 5 has no fixed requestSentEnabled flag; the wagon maps it to idempotency handling. */
    private static boolean isIdempotent(HttpRequestRetryStrategy handler, HttpRequest request) {
        try {
            Class<?> clazz = handler.getClass();
            Method method = null;
            while (method == null && clazz != null) {
                try {
                    method = clazz.getDeclaredMethod("handleAsIdempotent", HttpRequest.class);
                } catch (NoSuchMethodException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (method == null) {
                return false;
            }
            method.setAccessible(true);
            return (boolean) method.invoke(handler, request);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object field(Object target, String name) {
        try {
            Class<?> clazz = target.getClass();
            Field field = null;
            while (field == null && clazz != null) {
                try {
                    field = clazz.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (field == null) {
                throw new IllegalStateException("No field " + name);
            }
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpRequestRetryStrategy getCurrentHandler() {
        try {
            Class<?> impl = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass("org.apache.maven.wagon.shared.http.AbstractHttpClientWagon");
            Method createRetryHandler = impl.getDeclaredMethod("createRetryHandler");
            createRetryHandler.setAccessible(true);
            return HttpRequestRetryStrategy.class.cast(createRetryHandler.invoke(null));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void doTestHttpClient(final Runnable test) throws Exception {
        final String classpath = System.getProperty("java.class.path");
        final String[] paths = classpath.split(File.pathSeparator);
        final Collection<URL> urls = new ArrayList<>(paths.length);
        for (final String path : paths) {
            urls.add(new File(path).toURI().toURL());
        }
        final URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[paths.length]), new ClassLoader() {
            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("org.apache.maven.wagon.shared.http")) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        });
        final Thread thread = Thread.currentThread();
        final ClassLoader contextClassLoader = thread.getContextClassLoader();
        String retryClass = System.getProperty("maven.wagon.http.retryHandler.class");
        String retryCount = System.getProperty("maven.wagon.http.retryHandler.count");
        String retrySent = System.getProperty("maven.wagon.http.retryHandler.requestSentEnabled");
        String retryExceptions = System.getProperty("maven.wagon.http.retryHandler.nonRetryableClasses");
        try {
            System.clearProperty("maven.wagon.http.retryHandler.class");
            System.clearProperty("maven.wagon.http.retryHandler.count");
            System.clearProperty("maven.wagon.http.retryHandler.requestSentEnabled");
            System.clearProperty("maven.wagon.http.retryHandler.nonRetryableClasses");
            thread.setContextClassLoader(loader);
            test.run();
        } finally {
            restore("maven.wagon.http.retryHandler.class", retryClass);
            restore("maven.wagon.http.retryHandler.count", retryCount);
            restore("maven.wagon.http.retryHandler.requestSentEnabled", retrySent);
            restore("maven.wagon.http.retryHandler.nonRetryableClasses", retryExceptions);
            thread.setContextClassLoader(contextClassLoader);
            loader.close();
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
