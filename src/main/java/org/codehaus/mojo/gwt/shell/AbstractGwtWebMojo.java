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

import java.io.File;


/**
 * @author ndeloof
 * @version $Id$
 */
public abstract class AbstractGwtWebMojo
    extends AbstractGwtShellMojo
{
    /**
     * Source web.xml deployment descriptor that is used for GWT shell and for deployment WAR to "merge" servlet
     * entries.
     * 
     * @parameter default-value="${basedir}/src/main/webapp/WEB-INF/web.xml"
     * @required
     */
    private File webXml;

    /**
     * Specifies whether or not to add the module name as a prefix to the servlet path when merging web.xml. If you set
     * this to false the exact path from the GWT module will be used, nothing else will be prepended.
     * 
     * @parameter default-value="false"
     */
    private boolean webXmlServletPathAsIs;

    public File getWebXml()
    {
        return webXml;
    }

    public boolean isWebXmlServletPathAsIs()
    {
        return webXmlServletPathAsIs;
    }


}
