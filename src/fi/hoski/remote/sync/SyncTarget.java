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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * @author Timo Vesalainen
 */
public class SyncTarget
{
    public static final String TABLES = "tables";
    public static final String SYNCHRONIZE = "synchronize";
    public static final String KEY = "key-%s";
    public static final String INDEXES = "indexes-%s";
    public static final String EXCLUDE = "exclude-%s";
    public static final String SQL = "sql-%s";
    public static final String SELECT_FROM = "select * from %s";
    public static final String FOREIGN = "foreign-%s";
    public static final String CHANGECASE = "changeCase-%s";

    private String name;
    private boolean remoteMaster;
    private String key;
    private Set<String> indexes = new HashSet<>();
    private Set<String> excluded = new HashSet<>();
    private Map<String,SyncTarget> foreigns = new HashMap<>();
    private Set<SyncTarget> references = new HashSet<>();
    private Set<String> prettify = new HashSet<>();
    private String sql;
    private Map<String,Integer> sqlMetadata = new HashMap<>();
    private Set<Key> deletedReferences = new HashSet<>();

    public SyncTarget(String name)
    {
        this.name = name;
    }
    
    public static List<SyncTarget> create(Properties properties)
    {
        List<SyncTarget> list = new ArrayList<>();
        Map<String,SyncTarget> map = new HashMap<>();
        for (String name : getList(properties, TABLES))
        {
            SyncTarget target = new SyncTarget(name);
            list.add(target);
            map.put(name, target);
        }
        for (String name : getList(properties, SYNCHRONIZE))
        {
            SyncTarget st = map.get(name);
            st.remoteMaster = true;
        }
        for (Entry<String,SyncTarget> entry : map.entrySet())
        {
            String tableName = entry.getKey();
            SyncTarget target = entry.getValue();
            //key-jasenet = Jasennumero
            String keyProperty = String.format(KEY, tableName);
            String keyField = properties.getProperty(keyProperty);
            if (keyField == null)
            {
                throw new IllegalArgumentException(keyProperty+" not found");
            }
            target.key = keyField;
            String indexesProperty = String.format(INDEXES, tableName);
            target.indexes.addAll(getList(properties, indexesProperty));
            //# Fields that are not replicated 
            //exclude-jasenet = Henkilotunnus,Fax,Muutettu
            String excludeProperty = String.format(EXCLUDE, tableName);
            target.excluded.addAll(getList(properties, excludeProperty));
            //# synchronize SQL statement. Default is select * from <tablename>
            //sql-jasenet = select * from jasenet where eronnut not null and kuollut not null
            String sqlProperty = String.format(SQL, tableName);
            target.sql = properties.getProperty(sqlProperty, String.format(SELECT_FROM, tableName));
            //# Foreign key. Format is <localname>-><foreign table>,...
            //foreign-jasenet=Jasenkoodi->jasenkoodit
            String foreignProperty = String.format(FOREIGN, tableName);
            Map<String,String> foreignMap = getMap(properties, foreignProperty);
            for (Entry<String,String> e : foreignMap.entrySet())
            {
                String field = e.getKey();
                SyncTarget foreign = map.get(e.getValue());
                if (foreign == null)
                {
                    throw new IllegalArgumentException(tableName+" -> "+e.getValue()+" undefined");
                }
                target.foreigns.put(field, foreign);
                foreign.references.add(target);
            }
            String changeCase = String.format(CHANGECASE, tableName);
            target.prettify.addAll(getList(properties, changeCase));
            
        }
        //sort(list);
        return list;
    }

    public void addDeleted(Collection<Key> keys)
    {
        deletedReferences.addAll(keys);
    }
    public Collection<Key> removeDeletedReferences(Map<Key,Entity> entityMap)
    {
        List<Key> list = new ArrayList<>();
        if (!deletedReferences.isEmpty())
        {
            Iterator<Entity> it = entityMap.values().iterator();
            while (it.hasNext())
            {
                Entity entity = it.next();
                for (Entry<String,Object> entry : entity.getProperties().entrySet())
                {
                    Object value = entry.getValue();
                    if (value instanceof Key)
                    {
                        if (deletedReferences.contains(value))
                        {
                            System.err.println("Delete by reference "+entity);
                            it.remove();
                            list.add(entity.getKey());
                            break;
                        }
                    }
                }
            }
        }
        return list;
    }
    public Iterable<SyncTarget> getReferences()
    {
        return references;
    }
    public boolean references(SyncTarget target)
    {
        return references.contains(target);
    }
    public SyncTarget getForeign(String col)
    {
        return foreigns.get(col);
    }
    public boolean isForeign(String col)
    {
        return foreigns.containsKey(col);
    }
    public boolean isIndex(String col)
    {
        return indexes.contains(col);
    }
    public boolean prettify(String col)
    {
        return prettify.contains(col);
    }
    public boolean isExcluded(String col)
    {
        return excluded.contains(col);
    }
    public String getKey()
    {
        return key;
    }

    public String getName()
    {
        return name;
    }

    public boolean isRemoteMaster()
    {
        return remoteMaster;
    }

    public String getSql()
    {
        return sql;
    }

    void setColumnMetaData(String columnName, int columnType)
    {
        sqlMetadata.put(columnName, columnType);
    }
    public Map<String, Integer> getColumnMetaData()
    {
        return sqlMetadata;
    }
    public Collection<String> getColumns()
    {
        return sqlMetadata.keySet();
    }
    private static Map<String,String> getMap(Properties properties, String key)
    {
        Map<String,String> map = new HashMap<>();
        List<String> list = getList(properties, key);
        for (String item : list)
        {
            String[] split = item.split("->");
            if (split.length != 2)
            {
                throw new IllegalArgumentException(item+" illegal map value");
            }
            map.put(split[0], split[1]);
        }
        return map;
    }
    private static List<String> getList(Properties properties, String key)
    {
        List<String> list = new ArrayList<>();
        String value = properties.getProperty(key);
        if (value != null)
        {
            String[] splitValue = value.split("[ ,]+");
            Collections.addAll(list, splitValue);
        }
        return list;
    }

    @Override
    public String toString()
    {
        return "SyncTarget{" + "name=" + name + ", remoteMaster=" + remoteMaster + ", key=" + key + '}';
    }

}
