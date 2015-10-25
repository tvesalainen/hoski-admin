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

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import fi.hoski.datastore.RemoteAppEngine;
import fi.hoski.datastore.repository.RaceFleet;
import fi.hoski.datastore.repository.RaceSeries;
import fi.hoski.sailwave.Fleet;
import fi.hoski.sailwave.SailWaveFile;
import java.io.IOException;

/**
 * @author Timo Vesalainen
 */
public class Fixer2
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        try
        {
            RemoteAppEngine.init("helsinkiregatta.appspot.com");
            RemoteAppEngine rae = new RemoteAppEngine()
            {

                @Override
                protected Object run() throws IOException
                {
                    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
                    Query query = new Query("RaceSeries");
                    PreparedQuery prepared = datastore.prepare(query);
                    for (Entity series : prepared.asIterable())
                    {
                        Blob swb = (Blob) series.getProperty(RaceSeries.SAILWAVEFILE);
                        SailWaveFile swf = new SailWaveFile(swb.getBytes());
                        Query q2 = new Query("RaceFleet");
                        q2.setAncestor(series.getKey());
                        PreparedQuery prepared2 = datastore.prepare(q2);
                        for (Entity fleet : prepared2.asIterable())
                        {
                            String name = (String) fleet.getProperty(RaceFleet.Fleet);
                            Fleet f = swf.getFleet(name);
                            int number = f.getNumber();
                            fleet.setProperty(RaceFleet.SailWaveId, number);
                            datastore.put(fleet);
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
