/*
 * $HeadURL$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 *  Copyright 1999-2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.axis2.transport.http.server;

public interface ConnectionListenerFailureHandler {

    /**
     * The associated connection listener IOProcessor has failed
     *
     * @param connectionListener the associated connection listener
     * @param cause              cause of failure
     * @return true if the listener should attempt to re-establish itself, false if it should terminate.
     */
    public boolean failed(IOProcessor connectionListener, Throwable cause);

    /**
     * The associated connection listener IOProcessor is terminating abnormally
     *
     * @param connectionListener the associated connection listener
     * @param message            explanation of termination
     * @param cause              last exception that is causing termination
     */
    public void notifyAbnormalTermination(IOProcessor connectionListener, String message,
                                          Throwable cause);

}
