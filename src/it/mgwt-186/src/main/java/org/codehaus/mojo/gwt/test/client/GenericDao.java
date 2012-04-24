package org.codehaus.mojo.gwt.test.client;

import java.util.List;

public interface GenericDao<TEntity, TKey>
{
    public List<TEntity> getAll();

    public TEntity findById( TKey key );

    public TEntity persist( TEntity entity );

    public TEntity[] persist( TEntity[] entities );

    public void delete( TEntity entity );
}
