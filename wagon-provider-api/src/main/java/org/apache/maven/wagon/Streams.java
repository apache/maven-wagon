package org.apache.maven.wagon;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Streams
{
    private String out = "";

    private String err = "";

    public String getOut()
    {
        return out;
    }

    public void setOut( String out )
    {
        this.out = out;
    }

    public String getErr()
    {
        return err;
    }

    public void setErr( String err )
    {
        this.err = err;
    }
}
