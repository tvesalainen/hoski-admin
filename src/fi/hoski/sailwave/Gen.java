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

package fi.hoski.sailwave;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Timo Vesalainen
 */
public class Gen
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        try
        {
            Properties prop = new Properties();
            FileInputStream fis = new FileInputStream("Y:\\SailWave\\Sailwave.properties");
            prop.load(new InputStreamReader(fis, "ISO-8859-1"));
            Map<String,String> map = new HashMap<>();
            for (String p : prop.stringPropertyNames())
            {
                map.put(p.toUpperCase(), p);
            }
            for (CompetitorField cf : CompetitorField.values())
            {
                //System.err.println("public static final String "+cf.name()+" = \""+map.get(cf.name())+"\";");
                String t = prop.getProperty(map.get(cf.name()));
                if (t.isEmpty())
                {
                    System.err.println(map.get(cf.name())+"="+map.get(cf.name()));
                }
                else
                {
                    System.err.println(map.get(cf.name())+"="+t);
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
