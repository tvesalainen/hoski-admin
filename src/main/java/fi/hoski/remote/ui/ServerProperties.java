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
import java.util.Map.Entry;
import java.util.Properties;

/**
 * @author Timo Vesalainen
 */
public final class ServerProperties extends DataObject
{
    public static final String Kind = "ServerCredentials";
    
    public static final String Server = "remoteserver";
    public static final String Username = "remoteuser";
    public static final String Tables = "tables";
    public static final String SuperUser = "super-user";
    public static final String SupportsZonerSMS = "supports-zoner-sms";
    
    public static final DataObjectModel Model = new DataObjectModel(Kind);

    static
    {
        Model.property(Server);
        Model.property(Username);
        Model.property(Tables);
        Model.property(SuperUser, String.class);
        Model.property(SupportsZonerSMS, String.class);
    }
    
    public ServerProperties(Properties properties)
    {
        super(new MapData(Model, properties));
        String server = getServer();
        if (server == null || server.isEmpty())
        {
            server = LastInput.get(Server);
            setServer(server);
        }
        String username = getUsername();
        if (username == null || username.isEmpty())
        {
            username = LastInput.get(Username);
            setUsername(username);
        }
    }

    public Properties getProperties()
    {
        Properties properties = new Properties();
        for (Entry<String,Object> entry : getAll().entrySet())
        {
            properties.setProperty(entry.getKey(), entry.getValue().toString());
        }
        return properties;
    }
    
    public String[] getTables()
    {
        String tables = (String) get(Tables);
        if (tables != null)
        {
            return tables.split(",");
        }
        else
        {
            return new String[]{};
        }
    }

    @Override
    public void set(String property, Object value)
    {
        super.set(property, value);
        switch (property)
        {
            case Server:
            case Username:
                LastInput.set(property, (String) value);
                break;
        }
    }
    
    public String getUsername()
    {
        return (String) get(Username);
    }

    public void setUsername(String username)
    {
        set(Username, username);
    }

    public String getServer()
    {
        return (String) get(Server);
    }

    public void setServer(String server)
    {
        set(Server, server);
    }

    public boolean isZonerSMSSupported()
    {
        return Boolean.parseBoolean((String)get(SupportsZonerSMS));
    }
    public boolean isSuperUser()
    {
        return Boolean.parseBoolean((String)get(SuperUser));
    }
    @Override
    public Key createKey()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
