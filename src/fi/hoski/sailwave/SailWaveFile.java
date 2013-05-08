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

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fi.hoski.datastore.repository.RaceEntry;
import fi.hoski.datastore.repository.RaceFleet;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Timo Vesalainen
 */
public class SailWaveFile implements Serializable
{
    private static final long serialVersionUID = 1L;

    private List<String[]> list;
    private Map<Integer,Competitor> competitors = new TreeMap<>();
    private Map<Integer,Race> races = new TreeMap<>();
    private Map<Integer,Fleet> fleets = new TreeMap<>();
    private int maxCompetitor;
    private int maxFleet;

    protected SailWaveFile()
    {
    }

    public SailWaveFile(byte[] array) throws IOException
    {
        this(new ByteArrayInputStream(array));
    }

    public SailWaveFile(File file) throws IOException
    {
        this(new FileInputStream(file));
    }
    private SailWaveFile(InputStream is) throws IOException
    {
        is = new EuroInputStream(is);
        BufferedInputStream bis = new BufferedInputStream(is);
        InputStreamReader isr = new InputStreamReader(bis, "ISO-8859-1");
        CSVReader reader = new CSVReader(isr);
        list = new ArrayList<>();
        String[] ar = reader.readNext();
        while (ar != null)
        {
            boolean add = true;
            switch (ar[0])
            {
                case "comphigh":
                    int ci = Integer.parseInt(ar[2]);
                    maxCompetitor = Math.max(ci, maxCompetitor);
                    break;
                case Race.RACEDATE:
                case Race.RACENAME:
                case Race.RACESTART:
                case Race.RACERANK:
                    int raceNum = Integer.parseInt(ar[3]);
                    Race race = races.get(raceNum);
                    if (race == null)
                    {
                        race = new Race();
                        races.put(raceNum, race);
                    }
                    race.add(ar);
                    break;
                case "compnat":
                case "compsailno":
                case "compclass":
                    int compNum = Integer.parseInt(ar[2]);
                    Competitor comp = competitors.get(compNum);
                    if (comp == null)
                    {
                        comp = new Competitor();
                        competitors.put(compNum, comp);
                    }
                    comp.add(ar);
                    break;
                default:
                    if (ar[0].startsWith("scr"))
                    {
                        add = false;
                        int srcNum = Fleet.getNumber(ar);
                        maxFleet = Math.max(maxFleet, srcNum);
                        Fleet fleet = fleets.get(srcNum);
                        if (fleet == null)
                        {
                            fleet = new Fleet();
                            fleets.put(srcNum, fleet);
                        }
                        fleet.add(ar);
                    }
                    break;
            }
            if (add)
            {
                list.add(ar);
            }
            ar = reader.readNext();
        }
        reader.close();
    }

    public void deleteNotNeededFleets(List<RaceEntry> entries)
    {
        Set<String> names = new HashSet<>();
        for (RaceEntry re : entries)
        {
            names.add(re.getFleet());
        }
        Fleet defaultFleet = getDefaultFleet();
        Iterator<Entry<Integer, Fleet>> iterator = fleets.entrySet().iterator();
        while (iterator.hasNext())
        {
            Entry<Integer, Fleet> next = iterator.next();
            Fleet f = next.getValue();
            String name = f.getValue();
            if (!f.equals(defaultFleet) && !names.contains(name))
            {
                iterator.remove();
            }
        }
    }
    
    public void updateFleets(List<RaceFleet> fleetList)
    {
        Map<String,RaceFleet> names = new HashMap<>();
        for (RaceFleet rf : fleetList)
        {
            names.put(rf.getName(), rf);
        }
        Fleet defaultFleet = getDefaultFleet();
        Iterator<Entry<Integer, Fleet>> iterator = fleets.entrySet().iterator();
        while (iterator.hasNext())
        {
            Entry<Integer, Fleet> next = iterator.next();
            Fleet f = next.getValue();
            String name = f.getValue();
            if (!f.equals(defaultFleet))
            {
                RaceFleet rf = names.get(name);
                if (rf == null)
                {
                    iterator.remove();
                }
            }
        }
        for (String name : names.keySet())
        {
            Fleet fleet = getFleet(name);
            if (fleet == null)
            {
                fleet = new Fleet();
            }
            RaceFleet rf = names.get(name);
            fleet.setName(rf.getClazz());
            fleet.
        }
    }
    
    public void copyFleet(Fleet fleet)
    {
        assert fleets.containsValue(fleet);
        maxFleet++;
        fleets.put(maxFleet, fleet.copy(maxFleet));
    }
    
    public Fleet getFleet(String name)
    {
        for (Entry<Integer,Fleet> entry : fleets.entrySet())
        {
            Fleet fleet = entry.getValue();
            if (name.equals(fleet.getValue()))
            {
                return fleet;
            }
        }
        return null;
    }
    public Fleet getDefaultFleet()
    {
        for (Entry<Integer,Fleet> entry : fleets.entrySet())
        {
            Fleet fleet = entry.getValue();
            if (fleet.getParent() == 0)
            {
                return fleet;
            }
        }
        return null;
    }
    public Race getFirstRace()
    {
        for (Race race : races.values())
        {
            if (race.getStartNumber() == 1)
            {
                return race;
            }      
        }
        return null;
    }
    
    public List<Race> getRaces()
    {
        List<Race> list = new ArrayList<Race>();
        for (int number : races.keySet())
        {
            list.add(races.get(number));
        }
        return list;
    }
    public List<Fleet> getFleets()
    {
        List<Fleet> list = new ArrayList<Fleet>();
        if (fleets.size() == 1)
        {
            list.add(fleets.values().iterator().next());
        }
        else
        {
            for (Entry<Integer,Fleet> entry : fleets.entrySet())
            {
                Fleet fleet = entry.getValue();
                if (fleet.getParent() != 0)
                {
                    if (entry.getKey() != 0)
                    {
                        list.add(fleet);
                    }
                }
            }
        }
        return list;
    }
    private String get(String property)
    {
        for (String[] ar : list)
        {
            if (property.equals(ar[0]))
            {
                return ar[1].replace('|', '\n');
            }
        }
        return "";
    }
    private void set(String property, String value)
    {
        if (value == null)
        {
            value = "";
        }
        for (String[] ar : list)
        {
            if (property.equals(ar[0]))
            {
                ar[1] = value.replace('\n', '|');
                return;
            }
        }
        list.add(new String[] {property, value.replace('\n', '|'), "", ""});
    }
    public String getEvent()
    {
        return get("serevent");
    }

    public String getNotes()
    {
        return get("sermynotes");
    }

    public String getVenue()
    {
        return get("servenue");
    }
    
    public void setEvent(String event)
    {
        set("serevent", event);
    }

    public void setNotes(String notes)
    {
        set("sermynotes", notes);
    }

    public void setVenue(String venue)
    {
        set("servenue", venue);
    }
    
    /**
     * Return unique id for series
     * @return 
     */
    public String getEventId()
    {
        return get("sereventeid");
    }

    public void addCompetitor(Competitor competitor)
    {
        if (!competitors.containsValue(competitor))
        {
            maxCompetitor++;
            competitor.setNumber(maxCompetitor);
            competitors.put(maxCompetitor, competitor);
        }
    }
    
    public void saveAs(File file) throws IOException
    {
        saveAs(new FileOutputStream(file));
    }
    
    public byte[] getBytes() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        saveAs(baos);
        return baos.toByteArray();
    }
    
    private void saveAs(OutputStream os) throws IOException
    {
        BufferedOutputStream bos = new BufferedOutputStream(os);
        OutputStreamWriter osw = new OutputStreamWriter(bos, "ISO-8859-1");
        CSVWriter writer = new CSVWriter(osw, ',', '"', "\r\n");
        writer.writeAll(list);
        for (Fleet fleet : fleets.values())
        {
            fleet.write(writer);
        }
        for (Competitor competitor : competitors.values())
        {
            competitor.write(writer);
        }
        writer.close();
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)    
    {
        try
        {
            File f1 = new File("C:\\Users\\tkv\\Documents\\Helsinki RegattaVikla.blw");
            SailWaveFile swf = new SailWaveFile(f1);
            Fleet defaultFleet = swf.getDefaultFleet();
            swf.copyFleet(defaultFleet);
            File f2 = new File("C:\\Users\\tkv\\Documents\\Helsinki RegattaVikla2.blw");
            swf.saveAs(f2);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void setId(long id)
    {
        set("sereventeid", String.valueOf(id));
    }
    public static String join(String... cols)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : cols)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append('|');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static String[] split(String str)
    {
        int begin = 0;
        int end = str.indexOf('|');
        if (end == -1)
        {
            return new String[]{str};
        }
        List<String> list = new ArrayList<String>();
        while (end != -1)
        {
            list.add(str.substring(begin, end));
            begin = end + 1;
            end = str.indexOf('|', begin);
        }
        list.add(str.substring(begin));
        return list.toArray(new String[list.size()]);
    }

    private class EuroInputStream extends InputStream
    {
        private InputStream in;

        public EuroInputStream(InputStream in)
        {
            this.in = in;
        }
        
        @Override
        public int read() throws IOException
        {
            int cc = in.read();
            if (cc == 0x80)
            {
                return '€';
            }
            else
            {
                return cc;
            }
        }
        
    }
}
