/*
 * Copyright (C) 2012 Helsingfors Segelklubb ry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package fi.hoski.remote.sync;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import fi.hoski.datastore.Repository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Timo Vesalainen
 */
public class DatastoreAccess implements DataAccess
{
    /*
     * Maximum count of entities that are allowed in one put.
     */
    private static final int MAXUPDATECOUNT = 500;
    
    private DatastoreService datastore;
    private Date timestamp = new Date();
    private String creator;
    private List<Entity> updateList = new ArrayList<>();

    public DatastoreAccess(String creator)
    {
        this.creator = creator;
        datastore = DatastoreServiceFactory.getDatastoreService();
    }

    @Override
    public Map<Key,Entity> getAllEntities(SyncTarget target) throws IOException
    {
        Map<Key,Entity> map = new HashMap<>();
        String kind = target.getName();
        Query query = new Query(kind);
        PreparedQuery prepared = datastore.prepare(query);
        for (Entity entity : prepared.asIterable(FetchOptions.Builder.withChunkSize(MAXUPDATECOUNT)))
        {
            map.put(entity.getKey(), entity);
        }
        return map;
    }

    @Override
    public Key insert(SyncTarget target, Entity entity) throws IOException
    {
        entity.setUnindexedProperty(Repository.TIMESTAMP, timestamp);
        entity.setUnindexedProperty(Repository.CREATOR, creator);
        System.err.println("insert "+entity);
        remoteUpdate(entity);
        return null;
    }

    @Override
    public void update(SyncTarget target, Entity entity) throws IOException
    {
        entity.setUnindexedProperty(Repository.TIMESTAMP, timestamp);
        entity.setUnindexedProperty(Repository.CREATOR, creator);
        remoteUpdate(entity);
        System.err.println("update "+entity);
    }

    @Override
    public void delete(SyncTarget target, Collection<Key> keys) throws IOException
    {
        flush();
        datastore.delete(keys);
    }

    @Override
    public void move(SyncTarget target, Key from, Key to) throws IOException
    {
        try
        {
            Entity of = datastore.get(from);
            Entity entity = new Entity(to);
            entity.setPropertiesFrom(of);
            flush();
            Transaction tr = datastore.beginTransaction();
            try
            {
                datastore.put(entity);
                datastore.delete(from);
                tr.commit();
            }
            finally
            {
                if (tr.isActive())
                {
                    tr.rollback();
                }
            }
        }
        catch (EntityNotFoundException ex)
        {
            throw new IOException(ex);
        }
    }
    private void remoteUpdate(Entity entity) throws IOException
    {
        if (updateList.size() >= MAXUPDATECOUNT)
        {
            flush();
        }
        entity.setUnindexedProperty(Repository.TIMESTAMP, timestamp);
        entity.setUnindexedProperty(Repository.CREATOR, creator);
        updateList.add(entity);
    }

    public void flush() throws IOException
    {
        System.err.println(updateList);
        datastore.put(updateList);
        updateList.clear();
    }


    private class KeyIterable implements Iterable<Key>, Iterator<Key>
    {
        private Iterator<Entity> iterator;

        public KeyIterable(Iterable<Entity> iterable)
        {
            iterator = iterable.iterator();
        }
        
        @Override
        public Iterator<Key> iterator()
        {
            return this;
        }

        @Override
        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        @Override
        public Key next()
        {
            Entity entity = iterator.next();
            Key key = entity.getKey();
            return key;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}
