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
package org.apache.maven.wagon;

/**
 * Root class for all exception in Wagon API
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public abstract class WagonException extends Exception {
    /**
     * the throwable that caused this exception to get thrown
     */
    private Throwable cause;

    /**
     * Constructs a new WagonException with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to initCause
     *
     * @param message - the detail message (which is saved for later retrieval by the getMessage() method).
     * @param cause   - the cause (which is saved for later retrieval by the getCause() method).
     *                (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public WagonException(final String message, final Throwable cause) {
        super(message);
        initCause(cause);
    }

    /**
     * Constructs a new WagonException with the specified detail message and cause.
     *
     * @param message - the detail message (which is saved for later retrieval by the getMessage() method).
     */
    public WagonException(final String message) {
        super(message);
    }

    /**
     * Returns the cause of this throwable or null if the cause is nonexistent or unknown.
     * (The cause is the throwable that caused this exception to get thrown.)
     *
     * @return the cause of this exception or null if the cause is nonexistent or unknown.
     */
    @Override
    public Throwable getCause() {
        return cause;
    }

    /**
     * Initializes the cause of this throwable to the specified value.
     * (The cause is the throwable that caused this throwable to get thrown.)
     * This method can be called at most once.
     * It is generally called from within the constructor, or immediately after creating the throwable.
     * If this throwable was created with WagonException(Throwable) or WagonException(String,Throwable),
     * this method cannot be called even once.
     *
     * @return a reference to this Throwable instance.
     */
    @Override
    public Throwable initCause(final Throwable cause) {
        this.cause = cause;
        return this;
    }
}
