/*
 * Copyright (C) 2013 Helsingfors Segelklubb ry
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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.PhoneNumber;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import fi.hoski.datastore.RemoteAppEngine;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * @author Timo Vesalainen
 */
public class Fixer
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        try
        {
            RemoteAppEngine.init("hsk-members.appspot.com", "timo.vesalainen@gmail.com", "db7f3ea2");
            RemoteAppEngine rae = new RemoteAppEngine()
            {

                @Override
                protected Object run() throws IOException
                {
                    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
                    Query query = new Query("Reservation");
                    PreparedQuery prepared = datastore.prepare(query);
                    for (Entity entity : prepared.asIterable())
                    {
                        boolean upd = false;
                        Object email = entity.getProperty("Jasenet.Email");
                        if (email instanceof Email)
                        {
                            Email e = (Email) email;
                            entity.setProperty("Jasenet.Email", e.getEmail());
                            upd = true;
                        }
                        Object mobile = entity.getProperty("Jasenet.Mobile");
                        if (mobile instanceof PhoneNumber)
                        {
                            PhoneNumber p = (PhoneNumber) mobile;
                            entity.setProperty("Jasenet.Mobile", p.getNumber());
                            upd = true;
                        }
                        if (upd)
                        {
                            datastore.put(entity);
                        }
                    }
                    return null;
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
