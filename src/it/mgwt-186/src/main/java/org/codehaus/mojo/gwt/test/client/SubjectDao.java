package org.codehaus.mojo.gwt.test.client;

import java.util.List;

public interface SubjectDao
    extends GenericDao<Subject, Long>
{
    public List<Subject> getEnabledSubjects();
}
