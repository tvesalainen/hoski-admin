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

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DataTypeUtils;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.ShortBlob;
import com.google.appengine.api.datastore.Text;
import fi.hoski.datastore.Repository;
import fi.hoski.util.Day;
import fi.hoski.util.Time;
import fi.hoski.util.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Timo Vesalainen
 */
public class DatabaseAccess extends SqlConnection implements DataAccess
{

    public DatabaseAccess(Properties properties) throws ClassNotFoundException, SQLException
    {
        super(properties);
    }
    
    @Override
    public Map<Key,Entity> getAllEntities(SyncTarget target) throws IOException
    {
        Map<Key,Entity> result = new HashMap<Key,Entity>();
        try
        {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(target.getSql());
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<String> columnList = new ArrayList<>();
            int columnCount = metaData.getColumnCount();
            for (int ii=1;ii<=columnCount;ii++)
            {
                String columnName = metaData.getColumnName(ii);
                if (!target.isExcluded(columnName))
                {
                    target.setColumnMetaData(columnName, metaData.getColumnType(ii));
                    columnList.add(columnName);
                }
            }
            while (resultSet.next())
            {
                Object keyObject = resultSet.getObject(target.getKey());
                Key key = createKey(target.getName(), keyObject);
                Entity localEntity = new Entity(key);
                result.put(localEntity.getKey(), localEntity);
                for (String columnName : columnList)
                {
                    Object columnValue = null;
                    if (target.getKey().equals(columnName))
                    {
                        columnValue = keyObject;
                    }
                    else
                    {
                        columnValue = resultSet.getObject(columnName);
                    }
                    if (columnValue != null)
                    {
                        // try to create index
                        if (target.isForeign(columnName))
                        {
                            if (!columnValue.toString().isEmpty())
                            {
                                SyncTarget foreign = target.getForeign(columnName);
                                Key foreignKey = createKey(foreign.getName(), columnValue);
                                localEntity.setProperty(columnName, foreignKey);
                            }
                        }
                        else
                        {
                            Class<?> type = target.getType(columnName);
                            if (type != null)
                            {
                                try
                                {
                                    Constructor constructor = type.getConstructor(columnValue.getClass());
                                    columnValue = constructor.newInstance(columnValue);
                                }
                                catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex)
                                {
                                    throw new IllegalArgumentException(ex);
                                }
                            }
                            if (
                                    (columnValue instanceof String) &&
                                    target.prettify(columnName)
                                    )
                            {
                                columnValue = Utils.convertName((String)columnValue);
                            }
                            if (target.isIndex(columnName))
                            {
                                localEntity.setProperty(columnName, googleObject(columnValue));
                            }
                            else
                            {
                                localEntity.setUnindexedProperty(columnName, googleObject(columnValue));
                            }
                        }
                    }
                }
            }
            return result;
        }
        catch (ParseException ex)
        {
            throw new IOException(ex);
        }
        catch (SQLException ex)
        {
            throw new IOException(ex);
        }
    }

    @Override
    public Key insert(SyncTarget target, Entity entity) throws IOException
    {
        try
        {
            String tableName = target.getName();
            String keyField = target.getKey();
            List<String> fieldList = new ArrayList<String>();
            fieldList.addAll(target.getColumns());
            fieldList.remove(target.getKey());
            String insertSql = "insert into "+tableName+" ("+getFieldList(fieldList)+") values ("+getBindList(fieldList.size())+")";
            String selectSql = "select "+keyField+" from "+tableName+" where "+getWhereList(fieldList);
            PreparedStatement insertStatement = connection.prepareStatement(insertSql);
            PreparedStatement selectStatement = connection.prepareStatement(selectSql);
            setValues(fieldList, target.getColumnMetaData(), insertStatement, entity);
            setValues(fieldList, target.getColumnMetaData(), selectStatement, entity);
            ResultSet resultSet = selectStatement.executeQuery();
            if (!resultSet.next())
            {   // insert only if it wasn't there already
                System.err.println(insertSql+"\n"+entity);
                insertStatement.executeUpdate();
                resultSet = selectStatement.executeQuery();
                if (resultSet.next())
                {
                    Number id = (Number) resultSet.getObject(keyField);
                    if (resultSet.next())
                    {
                        throw new IllegalArgumentException(selectSql+" returned more than one row "+entity);
                    }
                    return KeyFactory.createKey(entity.getParent(), entity.getKind(), id.longValue());
                }
                else
                {
                    throw new IllegalArgumentException(selectSql+" failed");
                }
            }
            else
            {
                System.err.println("Didnt' insert. Was there already!\n"+entity);
                Number id = (Number) resultSet.getObject(keyField);
                if (resultSet.next())
                {
                    throw new IllegalArgumentException(selectSql+" returned more than one row "+entity);
                }
                return KeyFactory.createKey(entity.getParent(), entity.getKind(), id.longValue());
            }
        }
        catch (SQLException ex)
        {
            throw new IOException(entity.toString(), ex);
        }
    }

    @Override
    public void update(SyncTarget target, Entity entity) throws IOException
    {
        try
        {
            List<String> fieldList = new ArrayList<String>();
            fieldList.addAll(target.getColumns());
            fieldList.remove(target.getKey());
            String updateSql = "update "+target.getName()+" set "+getAssignList(fieldList)+" where "+target.getKey()+" = ?";
            System.err.println(updateSql+"\n"+entity);
            PreparedStatement updateStatement = connection.prepareStatement(updateSql);
            setValues(fieldList, target.getColumnMetaData(), updateStatement, entity);
            updateStatement.setInt(fieldList.size()+1, (int)entity.getKey().getId());
            updateStatement.executeUpdate();
        }
        catch (SQLException ex)
        {
            throw new IOException(entity.toString(), ex);
        }
    }

    @Override
    public void delete(SyncTarget target, Collection<Key> keys) throws IOException
    {
        throw new UnsupportedOperationException("Not supported at all.");
    }
    @Override
    public void move(SyncTarget target, Key from, Key to) throws IOException
    {
    }
    private Key createKey(String kind, Object key)
    {
        Key parent = KeyFactory.createKey(Repository.ROOT_KIND, Repository.ROOT_ID);
        if (
                (key instanceof Integer) || 
                (key instanceof Long) || 
                (key instanceof Byte) || 
                (key instanceof Short)
                )
        {
            Number number = (Number) key;
            long id = number.longValue();
            if (id > 0)
            {
                return KeyFactory.createKey(parent, kind, id);
            }
            else
            {
                return KeyFactory.createKey(parent, kind, id-1);
            }
        }
        else
        {
            return KeyFactory.createKey(parent, kind, key.toString());
        }
    }
    private static Object googleObject(Object columnValue) throws IOException, ParseException
    {
        if (columnValue instanceof Date)
        {
            return convertAccessDate((Date)columnValue);
        }
        if (columnValue instanceof Float)
        {  
            return Double.parseDouble(columnValue.toString());
        }
        if (
                columnValue instanceof Byte ||
                columnValue instanceof Short ||
                columnValue instanceof Integer
                )
        {  
            return new Long((String)columnValue.toString());
        }
        if (DataTypeUtils.isSupportedType(columnValue.getClass()))
        {
            return columnValue;
        }
        if (columnValue instanceof String)
        {
            // longer than allowed
            return new Text((String)columnValue);
        }
        if (columnValue instanceof Number)
        {
            return googleObject(columnValue.toString());  // BigInteger, BigDecimal...
        }
        if (byte[].class.equals(columnValue.getClass()))
        {
            byte[] bb = (byte[]) columnValue;
            if (bb.length <= DataTypeUtils.MAX_SHORT_BLOB_PROPERTY_LENGTH)
            {
                return new ShortBlob(bb);
            }
            else
            {
                return new Blob(bb);
            }
        }
        if (columnValue instanceof Serializable)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos))
            {
                oos.writeObject(columnValue);
            }
            return googleObject(baos.toByteArray());
        }
        throw new IllegalArgumentException(columnValue+" not suitable for Google datastore");
    }

    private static Long convertAccessDate(Date date) throws ParseException
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        if (year < 1970)
        {
            return fi.hoski.util.Time.getValue(date);
        }
        else
        {
            return Day.getValue(date);
        }
    }

    private String getFieldList(List<String> columnList)
    {
        StringBuilder sb = new StringBuilder();
        for (String column : columnList)
        {
            if (sb.length() > 0)
            {
                sb.append(',');
            }
            sb.append(column);
        }
        return sb.toString();
    }
    private String getAssignList(List<String> columnList)
    {
        StringBuilder sb = new StringBuilder();
        for (String column : columnList)
        {
            if (sb.length() > 0)
            {
                sb.append(',');
            }
            sb.append(column).append("=?");
        }
        return sb.toString();
    }
    private String getWhereList(List<String> columnList)
    {
        StringBuilder sb = new StringBuilder();
        for (String column : columnList)
        {
            if (sb.length() > 0)
            {
                sb.append(" and ");
            }
            sb.append(column).append("=?");
        }
        return sb.toString();
    }
    private String getBindList(int count)
    {
        StringBuilder sb = new StringBuilder();
        for (int ii=0;ii<count;ii++)
        {
            if (ii != 0)
            {
                sb.append(',');
            }
            sb.append('?');
        }
        return sb.toString();
    }

    private void setValues(List<String> columnList, Map<String,Integer> typeMap, PreparedStatement statement, Entity entity) throws SQLException
    {
        int index = 1;
        for (String column : columnList)
        {
            Object ob = entity.getProperty(column);
            if (ob == null)
            {
                statement.setNull(index, typeMap.get(column));
            }
            else
            {
                if (ob instanceof Key)
                {
                    Key key = (Key) ob;
                    statement.setInt(index, (int)key.getId());
                }
                else
                {
                    if (ob instanceof Boolean)
                    {
                        Boolean b = (Boolean) ob;
                        statement.setBoolean(index, b);
                    }
                    else
                    {
                        if (typeMap.get(column) == Types.DATE)
                        {
                            Long l = (Long) ob;
                            Day day = new Day(l);
                            java.sql.Date sqlDate = new java.sql.Date(day.getDate().getTime());
                            statement.setDate(index, sqlDate);
                        }
                        else
                        {
                            if (typeMap.get(column) == Types.TIMESTAMP)
                            {
                                Long l = (Long) ob;
                                Day day = new Day(l);
                                java.sql.Timestamp timestamp = new java.sql.Timestamp(day.getDate().getTime());
                                statement.setTimestamp(index, timestamp);
                            }
                            else
                            {
                                if (typeMap.get(column) == Types.TIME)
                                {
                                    Long l = (Long) ob;
                                    Time time = new Time(l);
                                    java.sql.Time jtime = new java.sql.Time(time.gateDate().getTime());
                                    statement.setTime(index, jtime);
                                }
                                else
                                {
                                    statement.setString(index, ob.toString());
                                }
                            }
                        }
                    }
                }
            }
            index++;
        }
    }
    private class FastFuture<T> implements Future<T>
    {
        private T item;

        public FastFuture(T item)
        {
            this.item = item;
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isCancelled()
        {
            return false;
        }

        @Override
        public boolean isDone()
        {
            return true;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException
        {
            return item;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return item;
        }
        
    }
}
