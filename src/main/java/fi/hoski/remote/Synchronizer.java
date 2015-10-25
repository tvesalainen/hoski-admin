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
package fi.hoski.remote;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.*;
import fi.hoski.datastore.RemoteAppEngine;
import fi.hoski.datastore.Repository;
import fi.hoski.util.Utils;
import fi.hoski.util.Day;
import fi.hoski.util.Time;
import java.io.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @deprecated 
 * Synchronizer class reads tables stored in MS-Access DB. 
 * @author Timo Vesalainen
 */
public class Synchronizer
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
    
    private static final SimpleDateFormat TIMEFORMAT1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final SimpleDateFormat TIMEFORMAT2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
    /*
     * Maximum count of entities that are allowed in one put.
     */
    private static final int MAXUPDATECOUNT = 500;
    
    private Map<Key,Entity> cacheMap;
    private Properties properties;
    private DatastoreService datastore;
    private Progress progress;
    private List<Entity> updateList = new ArrayList<>();
    private Set<Key> localKeySet = new HashSet<>();
    private File cacheFile;
    private boolean debug;
    private Date timestamp = new Date();
    private String creator;
    
    // gather metadata
    private Properties messageProperties;
    private Map<String,String> fieldTypes;

    public Synchronizer(Properties properties, DatastoreService datastore)
    {
        this.properties = properties;
        this.datastore = datastore;
        this.creator = properties.getProperty("remoteuser");
    }

    public void synchronize(Progress progress) throws ClassNotFoundException, SQLException, IOException, EntityNotFoundException, InterruptedException, ExecutionException, TimeoutException, ParseException
    {
        this.progress = progress;
        List<String> replicateList = getList(properties, TABLES);
        progress.setBounds(0, replicateList.size());
        
        debug = Boolean.parseBoolean(properties.getProperty("debug"));
        String cacheFilename = properties.getProperty("cache");
        if (cacheFilename == null)
        {
            throw new IllegalArgumentException("cache property not set");
        }
        progress.setNote("Reading cache...");
        cacheMap = null;
        cacheFile = new File(cacheFilename);
        if (cacheFile.exists())
        {
            try (FileInputStream fis = new FileInputStream(cacheFile))
            {
                ObjectInputStream ois = new ObjectInputStream(fis);
                try
                {
                    @SuppressWarnings("unchecked")
                    Map<Key,Entity> m = (Map<Key,Entity>) ois.readObject();
                    cacheMap = m;
                }
                catch (Exception ex)
                {
                    System.err.println(ex.getMessage());
                    System.err.println("cache rebuild...");
                    progress.setNote("cache rebuild...");
                }
            }
        }
        if (cacheMap == null)
        {
            cacheMap = new HashMap<>();
        }
        String driverName = properties.getProperty("driver");
        progress.setNote("Open driver "+driverName+"...");
        Class.forName(driverName);
        String databaseURL = properties.getProperty("databaseURL")+properties.getProperty("dsn");
        progress.setNote("Open DB "+databaseURL+"...");
        if (debug) DriverManager.setLogWriter(new PrintWriter(System.err));
        Connection conn = DriverManager.getConnection(databaseURL, properties);

        progress.setNote("starts copying access -> google");
        copyEntities(properties, conn);
        progress.setNote("starts deleting...");
        boolean again = deleteEntities(properties);
        if (again)
        {
            progress.setNote("repeat copying access -> google");
            copyEntities(properties, conn);
        }
        progress.close();
    }
    private void copyEntities(Properties properties, Connection connection) throws SQLException, IOException, ParseException, EntityNotFoundException
    {
        List<String> replicateList = getList(properties, TABLES);
        List<String> synchronizeList = getList(properties, SYNCHRONIZE);

        Statement statement = connection.createStatement();
        int progessCount = 0;
        for (String tableName : replicateList)
        {
            progress.setProgress(progessCount++);
            
            boolean sync = synchronizeList.contains(tableName);
            Map<Key,Entity> syncMap = null;
            if (sync)
            {
                syncMap = new HashMap<Key,Entity>();
                Query query = new Query(tableName);
                PreparedQuery prepared = datastore.prepare(query);
                for (Entity entity : prepared.asIterable(FetchOptions.Builder.withChunkSize(MAXUPDATECOUNT)))
                {
                    Key key = entity.getKey();
                    syncMap.put(key, entity);
                }
            }
            progress.setNote("replicating "+tableName);
            //key-jasenet = Jasennumero
            String keyProperty = String.format(KEY, tableName);
            String keyField = properties.getProperty(keyProperty);
            if (keyField == null)
            {
                throw new IllegalArgumentException(keyProperty+" not found");
            }
            String indexesProperty = String.format(INDEXES, tableName);
            List<String> indexesList = getList(properties, indexesProperty);
            //# Fields that are not replicated 
            //exclude-jasenet = Henkilotunnus,Fax,Muutettu
            String excludeProperty = String.format(EXCLUDE, tableName);
            List<String> excludeList = getList(properties, excludeProperty);
            //# synchronize SQL statement. Default is select * from <tablename>
            //sql-jasenet = select * from jasenet where eronnut not null and kuollut not null
            String sqlProperty = String.format(SQL, tableName);
            String sql = properties.getProperty(sqlProperty, String.format(SELECT_FROM, tableName));
            //# Foreign key. Format is <localname>-><foreign table>,...
            //foreign-jasenet=Jasenkoodi->jasenkoodit
            String foreignProperty = String.format(FOREIGN, tableName);
            Map<String,String> foreignMap = getMap(properties, foreignProperty);
            String changeCase = String.format(CHANGECASE, tableName);
            List<String> changeCaseList = getList(properties, changeCase);
            
            ResultSet resultSet = statement.executeQuery(sql);
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<String> columnList = new ArrayList<>();
            Map<String,Integer> typeMap = new HashMap<>();
            int columnCount = metaData.getColumnCount();
            for (int ii=1;ii<=columnCount;ii++)
            {
                String columnName = metaData.getColumnName(ii);
                if (!containsIgnoreCase(excludeList, columnName))
                {
                    columnList.add(columnName);
                    typeMap.put(columnName, metaData.getColumnType(ii));
                    gatherMetaData(tableName, columnName, metaData.getColumnTypeName(ii));
                }
            }
            while (resultSet.next())
            {
                Object keyObject = resultSet.getObject(keyField);
                Key key = createKey(tableName, keyObject);
                Entity localEntity = new Entity(key);
                for (String columnName : columnList)
                {
                    Object columnValue = null;
                    if (keyField.equalsIgnoreCase(columnName))
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
                        boolean set = false;
                        for (String foreignField : foreignMap.keySet())
                        {
                            if (columnName.equalsIgnoreCase(foreignField))
                            {
                                if (!columnValue.toString().isEmpty())
                                {
                                    String foreignTable = foreignMap.get(foreignField);
                                    Key foreignKey = createKey(foreignTable, columnValue);
                                    localEntity.setProperty(foreignField, foreignKey);
                                    set = true;
                                    break;
                                }
                            }
                        }
                        if (!set)
                        {
                            if (
                                    (columnValue instanceof String) &&
                                    containsIgnoreCase(changeCaseList, columnName)
                                    )
                            {
                                columnValue = Utils.convertName((String)columnValue);
                            }
                            if (containsIgnoreCase(indexesList, columnName))
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
                Entity remoteEntity = null;
                if (sync)
                {
                    remoteEntity = syncMap.get(key);
                    if (remoteEntity != null)
                    {
                        if (!equals(localEntity, remoteEntity))
                        {
                            localUpdate(connection, keyField, columnList, typeMap, remoteEntity);
                        }
                    }
                    else
                    {
                        remoteUpdate(localEntity);
                    }
                }
                else
                {
                    if (!matchesCache(localEntity))
                    {
                        remoteUpdate(localEntity);
                    }
                }
                localKeySet.add(localEntity.getKey());
            }
            if (sync)
            {
                Set<Key> remoteKeySet = syncMap.keySet();
                remoteKeySet.removeAll(localKeySet);
                if (!remoteKeySet.isEmpty())
                {
                    localInsert(connection, tableName, keyField, columnList, typeMap, remoteKeySet);
                }
            }
        }
        flush();
    }
    private boolean deleteEntities(Properties properties)
    {
        boolean again = false;
        // remoteKeySet contains all the replicated table keys at web
        Set<Key> remoteKeySet = new HashSet<Key>();
        List<String> tableList = getList(properties, "tables");
        for (String kind : tableList)
        {
            Query query = new Query(kind);
            query.setKeysOnly();
            PreparedQuery prepared = datastore.prepare(query);
            for (Entity entity : prepared.asIterable(FetchOptions.Builder.withChunkSize(MAXUPDATECOUNT)))
            {
                Key key = entity.getKey();
                remoteKeySet.add(key);
            }
        }
        // cacheMap contains all the entities that are copied from access to web.
        Set<Key> cacheKeySet = cacheMap.keySet();
        System.err.println("remote keys = "+remoteKeySet.size());
        System.err.println("cached keys = "+cacheKeySet.size());
        System.err.println("access keys = "+localKeySet.size());    // localKeySet has all the keys present at access
        int size = cacheKeySet.size();
        cacheKeySet.retainAll(remoteKeySet);    // cacheKeySet has now only the keys that also exists at web
        size -= cacheKeySet.size();
        System.err.println("deleted from cache = (remote difference)"+size);
        again = (size != 0);    // size != 0 if there exists keys in cache that are not found at web.
                                // meaning that entities are deleted at web 
        size = cacheKeySet.size();
        cacheKeySet.retainAll(localKeySet);
        size -= cacheKeySet.size();
        System.err.println("deleted from cache = (local difference)"+size);
        remoteKeySet.removeAll(localKeySet);
        datastore.delete(remoteKeySet);
        System.err.println("deleted from remote = "+remoteKeySet.size());
        return again;
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
    private boolean matchesCache(Entity entity)
    {
        boolean matches;
        Entity old = cacheMap.get(entity.getKey());
        if (old == null)
        {
            matches = false;
        }
        else
        {
            matches = equals(old, entity);
        }
        return matches;
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

    private void flush() throws IOException
    {
        System.err.println(updateList);
        datastore.put(updateList);
        for (Entity entity : updateList)
        {
            cacheMap.put(entity.getKey(), entity);
        }
        try (FileOutputStream fos = new FileOutputStream(cacheFile))
        {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(cacheMap);
        }
        updateList.clear();
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
    private static boolean containsIgnoreCase(Collection<String> collection, String value)
    {
        for (String v : collection)
        {
            if (value.equalsIgnoreCase(v))
            {
                return true;
            }
        }
        return false;
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
    private void gatherMetaData(String tableName, String columnName, String columnTypeName)
    {
        if (messageProperties != null)
        {
            messageProperties.put(tableName+"."+columnName, columnName);
            messageProperties.put(columnName, columnName);
            fieldTypes.put(tableName+"."+columnName, columnTypeName);
            fieldTypes.put(columnName, columnTypeName);
        }
    }
    private void populateRepositoryBundle(String messagePropertiesFilename) throws IOException
    {
        if (messagePropertiesFilename != null)
        {
            try (FileOutputStream fos = new FileOutputStream(messagePropertiesFilename))
            {
                if (messagePropertiesFilename.endsWith(".xml"))
                {
                    messageProperties.storeToXML(fos, "");
                }
                else
                {
                    messageProperties.store(fos, "");
                }
            }
        }
    }
    private void printMetaData()
    {
        List<String> list = new ArrayList<>();
        list.addAll(messageProperties.stringPropertyNames());
        Collections.sort(list);
        for (String p : list)
        {
            String type = fieldTypes.get(p);
            System.err.println("public static final String "+p.replace('.', '_').toUpperCase()+" = \""+p+"\";\t\t// "+javaType(type));
        }
        System.err.println("public static final String[] FIELDS = new String[]{");
        for (String p : list)
        {
            if (p.indexOf('.') != -1)
            {
                System.err.println("\t\t\t\t"+p.replace('.', '_').toUpperCase()+",");
            }
        }
        System.err.println("};");
    }

    private void localUpdate(Connection connection, String keyField, List<String> columnList, Map<String,Integer> typeMap, Entity remoteEntity) throws SQLException
    {
        List<String> fieldList = new ArrayList<String>();
        fieldList.addAll(columnList);
        fieldList.remove(keyField);
        String updateSql = "update "+remoteEntity.getKind()+" set "+getAssignList(fieldList)+" where "+keyField+" = ?";
        PreparedStatement updateStatement = connection.prepareStatement(updateSql);
        setValues(fieldList, typeMap, updateStatement, remoteEntity);
        updateStatement.setInt(fieldList.size()+1, (int)remoteEntity.getKey().getId());
        updateStatement.executeUpdate();
    }

    private void localInsert(Connection connection, String tableName, String keyField, List<String> columnList, Map<String,Integer> typeMap, Set<Key> remoteKeySet) throws SQLException, EntityNotFoundException
    {
        for (Key key : remoteKeySet)
        {
            Entity remoteInsertedEntity = datastore.get(key);
            List<String> fieldList = new ArrayList<String>();
            fieldList.addAll(columnList);
            fieldList.remove(keyField);
            if (remoteInsertedEntity.getProperties().keySet().isEmpty())
            {
                System.err.println("unexcpectly deleted "+remoteInsertedEntity);
                datastore.delete(remoteInsertedEntity.getKey());    // delete empty entity
            }
            else
            {
                String insertSql = "insert into "+tableName+" ("+getFieldList(fieldList)+") values ("+getBindList(fieldList.size())+")";
                String selectSql = "select "+keyField+" from "+tableName+" where "+getWhereList(fieldList);
                String deleteSql = "delete from "+tableName+" where "+keyField+" = ?";
                PreparedStatement insertStatement = connection.prepareStatement(insertSql);
                PreparedStatement selectStatement = connection.prepareStatement(selectSql);
                PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                setValues(fieldList, typeMap, insertStatement, remoteInsertedEntity);
                setValues(fieldList, typeMap, selectStatement, remoteInsertedEntity);
                ResultSet resultSet = selectStatement.executeQuery();
                if (!resultSet.next())
                {   // insert only if it wasn't there already
                    insertStatement.executeUpdate();
                    resultSet = selectStatement.executeQuery();
                    if (resultSet.next())
                    {
                        Number id = (Number) resultSet.getObject(keyField);
                        if (resultSet.next())
                        {
                            throw new IllegalArgumentException(selectSql+" returned more than one row "+remoteInsertedEntity);
                        }
                        deleteStatement.setObject(1, id);
                        Transaction tr = datastore.beginTransaction();
                        try
                        {
                            Key nKey = KeyFactory.createKey(key.getParent(), key.getKind(), id.longValue());
                            Entity nEntity = new Entity(nKey);
                            nEntity.setPropertiesFrom(remoteInsertedEntity);
                            datastore.put(nEntity);
                            datastore.delete(key);
                            tr.commit();
                            cacheMap.put(nKey, nEntity);
                        }
                        finally
                        {
                            if (tr.isActive())
                            {
                                tr.rollback();
                                deleteStatement.execute();
                            }
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException(selectSql+" failed");
                    }
                }
            }
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
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
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

            RemoteAppEngine.init(server);
            RemoteAppEngine rae = new RemoteAppEngine<Object>()
            {

                @Override
                protected Object run() throws IOException
                {
                    try
                    {
                        Synchronizer replicator = new Synchronizer(
                                properties, 
                                DatastoreServiceFactory.getDatastoreService()
                                );
                        replicator.synchronize(new StreamProgress());
                        return null;
                    }
                    catch (ClassNotFoundException ex)
                    {
                        throw new IOException(ex);
                    }
                    catch (SQLException ex)
                    {
                        throw new IOException(ex);
                    }
                    catch (EntityNotFoundException ex)
                    {
                        throw new IOException(ex);
                    }
                    catch (InterruptedException ex)
                    {
                        throw new IOException(ex);
                    }
                    catch (ExecutionException ex)
                    {
                        throw new IOException(ex);
                    }
                    catch (TimeoutException ex)
                    {
                        throw new IOException(ex);
                    }
                    catch (ParseException ex)
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
