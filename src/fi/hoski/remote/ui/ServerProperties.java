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

package fi.hoski.remote.ui;

import com.google.appengine.api.datastore.Key;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.DataObjectModel;
import fi.hoski.datastore.repository.MapData;
import java.util.Properties;

/**
 * @author Timo Vesalainen
 */
public class ServerProperties extends DataObject
{
    public static final String KIND = "ServerCredentials";
    
    public static final String SERVER = "remoteserver";
    public static final String USERNAME = "remoteuser";
    public static final String PASSWORD = "remotepassword";
    public static final String TABLES = "tables";
    
    public static final DataObjectModel MODEL = new DataObjectModel(KIND);

    static
    {
        MODEL.property(SERVER);
        MODEL.property(USERNAME);
        MODEL.property(PASSWORD);
        MODEL.setPassword(PASSWORD);
        MODEL.property(TABLES);
    }
    
    private Properties properties;
    
    public ServerProperties(Properties properties)
    {
        super(new MapData(MODEL, properties));
        this.properties = properties;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public boolean allSet()
    {
        String s = getServer();
        String u = getUsername();
        String p = getPassword();
        return s != null && !s.isEmpty() && u != null && !u.isEmpty() && p != null && !p.isEmpty();
    }
    public String[] getTables()
    {
        String tables = (String) get(TABLES);
        if (tables != null)
        {
            return tables.split(",");
        }
        else
        {
            return new String[]{};
        }
    }
    public String getPassword()
    {
        return (String) get(PASSWORD);
    }

    public void setPassword(String password)
    {
        set(PASSWORD, password);
    }

    public String getUsername()
    {
        return (String) get(USERNAME);
    }

    public void setUsername(String username)
    {
        set(USERNAME, username);
    }

    public String getServer()
    {
        return (String) get(SERVER);
    }

    public void setServer(String server)
    {
        set(SERVER, server);
    }

    @Override
    public Key createKey()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
