/**
 * com.usharesoft.utils.maven.ArtifactItem.java
 *
 * Copyright (c) 2007-2015 UShareSoft SAS, All rights reserved
 *
 * @author UShareSoft
 */
package org.codehaus.mojo.gwt;

import org.apache.maven.artifact.Artifact;

public class ArtifactItem {

    private String groupId;
    private String artifactId;
    private String version;
    private String scope = Artifact.SCOPE_COMPILE;
    private String classifier;
    private String type = "jar";
    private boolean transitive = true;

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
