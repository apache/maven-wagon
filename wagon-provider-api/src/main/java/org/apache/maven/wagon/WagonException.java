package org.apache.maven.wagon;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

/**
 * Root class for all exception in Wagon API
 * 
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public abstract class WagonException extends Exception
{
    /** the throwable that caused this throwable to get thrown */
    private Throwable cause;


    /**
     * Constructs a new WagonException with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to initCause
     * 
     * @param message - the detail message (which is saved for later retrieval by the getMessage() method).
     * @param cause   - the cause (which is saved for later retrieval by the getCause() method).
     *                (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public WagonException( final String message, final Throwable cause )
    {
        super( message );
        initCause( cause );
    }

    /**
     * Constructs a new WagonException with the specified detail message and cause.
     * 
     * @param message - the detail message (which is saved for later retrieval by the getMessage() method).
     */
    public WagonException( final String message )
    {
        super( message );
    }

    /**
     * Returns the cause of this throwable or null if the cause is nonexistent or unknown.
     * (The cause is the throwable that caused this throwable to get thrown.)
     * 
     * @return the cause of this throwable or null if the cause is nonexistent or unknown.
     */
    public Throwable getCause()
    {
//		try
//		{
//		   Class clazz = getClass().getSuperclass();
//		   
//		   Method method = clazz.getMethod( "gatCause" , null );
//		   
//		   Throwable retValue = (Throwable) method.invoke( this, null );
//         return retValue;
//		}   
//		catch( Exception e)
//		{
//        
//		}
        
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
    public Throwable initCause( final Throwable cause )
    {
//        try
//        {
//           Class clazz = getClass().getSuperclass();
//           Class[] parameterTypes = new Class[1];
//		   parameterTypes[0] = Throwable.class;
//           Method method = clazz.getMethod( "initCause" , parameterTypes);
//           Object[] params = { cause };
//           method.invoke( this, params );
//        }   
//        catch( Exception e)
//        {
//        
//        }        
        this.cause = cause;
        return this;
    }

}
