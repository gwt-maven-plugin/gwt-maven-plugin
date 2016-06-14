package org.codehaus.mojo.gwt.shell;

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

import org.apache.maven.plugin.testing.SilentLog;
import org.junit.Assert;
import org.junit.Test;

public class JavaCommandTest {

    @Test
    public void testExceptionMessageContainsCommandByDefault() {
        JavaCommand javaCommand = new JavaCommand();
        javaCommand.setLog( new SilentLog() );
        javaCommand.setMainClass( "MyMainClass" );

        try {
            javaCommand.execute();
            Assert.fail( "Expecting JavaCommandException to be thrown!" );
        } catch ( JavaCommandException e ) {
            // the exception message should contain the command and thus the configured main class name
            Assert.assertTrue( "Exception message did not contain expected main class", e.getMessage().contains( "MyMainClass" ) );
        }
    }

    @Test
    public void testExceptionMessageDoesNotContainCommandWhenConfigured() {
        JavaCommand javaCommand = new JavaCommand();
        javaCommand.setLog( new SilentLog() );
        javaCommand.setMainClass( "MyMainClass" );
        javaCommand.setPrintCommandOnError( false );

        try {
            javaCommand.execute();
            Assert.fail( "Expecting JavaCommandException to be thrown!" );
        } catch ( JavaCommandException e ) {
            // the exception message should _not_ contain the command and thus _not_ the main class name
            Assert.assertFalse( "Exception message did contain expected main class", e.getMessage().contains( "MyMainClass" ) );
        }
    }
}
