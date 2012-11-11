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

import fi.hoski.remote.*;
import com.google.appengine.api.datastore.*;
import fi.hoski.datastore.RemoteAppEngine;
import fi.hoski.datastore.Repository;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Synchronizer class reads tables stored in MS-Access DB. 
 * @author Timo Vesalainen
 */
public class Synchronizer2
{
    private Properties properties;
    private Progress progress;
    private String creator;
    
    public Synchronizer2(Properties properties) throws SQLException, ClassNotFoundException
    {
        this.properties = properties;
        this.creator = properties.getProperty("remoteuser");
    }

    public void synchronize(Progress progress) throws IOException, SQLException, InterruptedException, ExecutionException, ClassNotFoundException
    {
        this.progress = progress;
        DatastoreAccess datastore = new DatastoreAccess(creator);
        DatabaseAccess database = new DatabaseAccess(properties);
        
        progress.setNote("starts synchronizing");
        
        List<SyncTarget> targetList = SyncTarget.create(properties);
        progress.setBounds(0, targetList.size());
        int progessCount = 0;
        
        for (SyncTarget target : targetList)
        {
            progress.setProgress(progessCount++);
            progress.setNote("replicating "+target.getName());
            Map<Key, Entity> remoteMap = datastore.getAllEntities(target);
            Map<Key, Entity> localMap = database.getAllEntities(target);
            if (target.isRemoteMaster())
            {
                update(
                        target,
                        remoteMap, 
                        localMap, 
                        datastore,  // master
                        database,   // slave
                        false   // doesn't delete from local database
                        );
            }
            else
            {
                update(
                        target,
                        localMap, 
                        remoteMap, 
                        database,   // master
                        datastore,  // slave
                        true
                        );
            }
            datastore.flush();
        }
        progress.close();
    }
    private void update(
            SyncTarget target,
            Map<Key, Entity> masterMap, 
            Map<Key, Entity> slaveMap, 
            DataAccess master, 
            DataAccess slave,
            boolean delete
            ) throws IOException
    {
        // deletes by referential rules
        Collection<Key> masterRefDel = target.removeDeletedReferences(masterMap);
        if (!masterRefDel.isEmpty())
        {
            master.delete(target, masterRefDel);
        }
        Collection<Key> slaveRefDel = target.removeDeletedReferences(slaveMap);
        if (!slaveRefDel.isEmpty())
        {
            slave.delete(target, slaveRefDel);
        }
        
        // keys inserted to slave
        Set<Key> inserted = new HashSet<Key>();
        inserted.addAll(masterMap.keySet());       // all master keys
        inserted.removeAll(slaveMap.keySet());     // minus slave keys
        // keys deleted from slave
        Set<Key> deleted = new HashSet<Key>();
        deleted.addAll(slaveMap.keySet());       // all slave keys
        deleted.removeAll(masterMap.keySet());     // minus master keys
        // keys modified at slave
        Set<Key> modified = new HashSet<Key>();
        modified.addAll(masterMap.keySet());       // all slave keys
        modified.removeAll(inserted);     // minus master keys
        assert inserted.size() + modified.size() == masterMap.size();
        assert deleted.size() + modified.size() == slaveMap.size();
        for (Key key : modified)
        {
            Entity masterEntity = masterMap.get(key);
            Entity slaveEntity = slaveMap.get(key);
            if (!equals(masterEntity, slaveEntity))
            {
                slave.update(target, masterEntity);
            }
        }
        for (Key key : inserted)
        {
            Entity masterEntity = masterMap.get(key);
            Key insertedKey = slave.insert(target, masterEntity);
            master.move(target, masterEntity.getKey(), insertedKey);
        }
        if (delete)
        {
            if ( !deleted.isEmpty())
            {
                slave.delete(target, deleted);
                for (SyncTarget refTarget : target.getReferences())
                {
                    refTarget.addDeleted(deleted);
                }
            }
        }
        /*
        else
        {
            // in insert mode we insert also to master side
            for (Key key : deleted)
            {
                Entity slaveEntity = slaveMap.get(key);
                master.insert(target, slaveEntity);
            }
        }
        */
    }

    private String javaType(String sqlType)
    {
        switch (sqlType)
        {
            case "VARCHAR":
                return "String";
            case "DOUBLE":
            case "REAL":
                return "Double";
            case "INTEGER":
            case "BYTE":
            case "COUNTER":
            case "SMALLINT":
                return "Long";
            case "BIT":
                return "Boolean";
            case "DATETIME":
                return "Date";
            default:
                return sqlType;
        }
    }

    private static boolean equals(Entity e1, Entity e2)
    {
        if (!e1.getKey().equals(e2.getKey()))
        {
            return false;
        }
        Map<String, Object> properties1 = new HashMap<String, Object>();
        Map<String, Object> properties2 = new HashMap<String, Object>();
        properties1.putAll(e1.getProperties());
        properties2.putAll(e2.getProperties());
        properties1.remove(Repository.TIMESTAMP);
        properties1.remove(Repository.CREATOR);
        properties2.remove(Repository.TIMESTAMP);
        properties2.remove(Repository.CREATOR);
        if (!properties1.equals(properties2))
        {
            return false;
        }
        for (String key : properties1.keySet())
        {
            if (e1.isUnindexedProperty(key) != e2.isUnindexedProperty(key))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            args = new String[] {"C:\\Jasenrekisteri\\replicator.properties"};
            if (args.length != 1)
            {
                System.err.println("usage: java ... fi.hoski.remote.Replicator <properties file>");
                System.exit(-1);
            }
            final Properties properties = new Properties();
            try (FileInputStream pFile = new FileInputStream(args[0]);)
            {
                properties.load(pFile);
            }
            String server = properties.getProperty("remoteserver");
            String user = properties.getProperty("remoteuser");
            String password = properties.getProperty("remotepassword");

            RemoteAppEngine.init(server, user, password);
            RemoteAppEngine rae = new RemoteAppEngine<Object>()
            {

                @Override
                protected Object run() throws IOException
                {
                    try
                    {
                        Synchronizer2 replicator = new Synchronizer2(properties);
                        replicator.synchronize(new StreamProgress());
                        return null;
                    }
                    catch (Exception ex)
                    {
                        throw new IOException(ex);
                    }
                }
            };
            rae.call();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
