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

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;
import fi.hoski.datastore.repository.RaceEntry;
import fi.hoski.google.docs.A1;
import fi.hoski.google.docs.RandomAccessWorksheet;
import fi.hoski.orc.pls.RankingPoints;
import fi.hoski.util.orc.GPHComparator;
import fi.hoski.util.orc.PLSBoat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Timo Vesalainen
 */
public class PLSSpreadsheet implements A1
{
    public static final int[] SECONDS = new int[]
    {
        1200, 1100, 1000, 900, 800, 700, 600, 500, 400
    };
    private SpreadsheetService service;
    private SpreadsheetEntry spreadsheet;
    private Map<String,WorksheetEntry> worksheetMap = new HashMap<>();

    PLSSpreadsheet(SpreadsheetService service, SpreadsheetEntry spreadsheet) throws IOException, ServiceException
    {
        this.service = service;
        this.spreadsheet = spreadsheet;
        WorksheetFeed worksheetFeed = service.getFeed(
                spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
        for (WorksheetEntry worksheet : worksheetFeed.getEntries())
        {
            worksheetMap.put(worksheet.getTitle().getPlainText(), worksheet);
        }
    }

    void fill(List<RaceEntry> competitors) throws IOException, ServiceException
    {
        GPHComparator comparator = new GPHComparator();
        List<Division> divisions = getDivisions();
        for (PLSBoat boat : competitors)
        {
            for (Division div : divisions)
            {
                if (div.match(boat.getGPH()))
                {
                    div.add(boat);
                    break;
                }
            }
        }
        List<List<PLSBoat>> all = new ArrayList<List<PLSBoat>>();
        for (Division div : divisions)
        {
            Collections.sort(div.boats, comparator);
            all.add(div.boats);
        }
        RankingPoints points = new RankingPoints(getSheet("Points"));
        int clazz = 1;
        int offset = 3;
        int row = 7;
        RandomAccessWorksheet sheet = getSheet("ORC");
        String raceName = (String) sheet.getString(A, 0);

        for (List<PLSBoat> list : all)
        {
            String className = "ORC" + clazz;
            sheet.setValueAt(className, A, offset);
            sheet.setValueAt("Start:", D, offset);
            
            sheet.setValueAt("Sail #", A, offset+2);
            sheet.setValueAt("Boat", C, offset+2);
            sheet.setValueAt("Type", D, offset+2);
            sheet.setValueAt("Owner", E, offset+2);
            sheet.setValueAt("Club", F, offset+2);
            sheet.setValueAt("GPH", G, offset+2);
            sheet.setValueAt("PLT", H, offset+2);
            sheet.setValueAt("PLD", I, offset+2);
            sheet.setValueAt("Finish", J, offset+2);
            sheet.setValueAt("Elapsed", L, offset+2);
            sheet.setValueAt("Corrected", M, offset+2);
            sheet.setValueAt("Position", N, offset+2);
            sheet.setValueAt("Points", O, offset+2);
            
            int pos = 1;
            int start = offset + 5;
            int end = list.size() + row;
            sheet.setFormula("=MIN(P" + start + ":P" + end + ")", P, offset + 4);
            for (PLSBoat boat : list)
            {
                sheet.setValueAt(new String(boat.getNationality()), A, row);
                sheet.setValueAt(new Float(boat.getNumber()), B, row);
                sheet.setValueAt(boat.getName(), C, row);
                sheet.setValueAt(boat.getType(), D, row);
                sheet.setValueAt(boat.getOwner(), E, row);
                sheet.setValueAt(boat.getClub(), F, row);
                sheet.setValueAt(new Float(boat.getGPH()), G, row);
                sheet.setValueAt(new Float(boat.getPLTOffshore()), H, row);
                sheet.setValueAt(new Float(boat.getPLDOffshore()), I, row);
                sheet.setValueAt(0, J, row);  // days
                sheet.setFormula("=IF(ISNUMBER(K%1$d);J%1$d+K%1$d-$E$%2$d+$I$3/24;\"\")", L, row+1, offset+1);
                sheet.setFormula("=IF(ISNUMBER(K%1$d);P%1$d-$P$%2$d;\"\")", M, row+1, offset + 4);
                sheet.setValueAt(pos, N, row);
                sheet.setValueAt(points.points(pos, list.size()), O, row);
                sheet.setFormula("=IF(ISNUMBER(K%1$d);(H%1$d*L%1$d*86400-I%1$d*$F$2)/86400;\"\")", P, row+1, offset+1);
                row++;
                pos++;
            }
            clazz++;
            offset += 5 + list.size();
            row += 5;
        }
        // Scratch sheets
        RandomAccessWorksheet course = getSheet("Course");
        clazz = 1;
        for (List<PLSBoat> list : all)
        {
            for (PLSBoat entry : list)
            {
                sheet = createSheet(entry.getName());
                sheet.setValueAt("Change formatting of columns F-N to Hours!", A, 0);
                sheet.setValueAt(String.format(raceName + " ORC%d Time verification table for boat %s   %s",
                        clazz, entry.getSailNumber(), entry.getName()), A, 1);
                sheet.setValueAt("Corrected time (sec/NM) for elapsed time (sec/NM)", B, 3);
                sheet.setValueAt("Sail #", A, 4);
                sheet.setValueAt("Name", B, 4);
                sheet.setValueAt("GPH", C, 4);
                sheet.setValueAt("PLT", D, 4);
                sheet.setValueAt("PLD", E, 4);
                for (int ii = 0; ii < SECONDS.length; ii++)
                {
                    sheet.setFormula("=%2$d/86400", ii + 5, 5, SECONDS[ii]);
                }
                row = 5;
                int myRow = -1;
                for (PLSBoat boat : list)
                {
                    if (entry.equals(boat))
                    {
                        myRow = row;
                    }
                    sheet.setValueAt(boat.getSailNumber(), A, row);
                    sheet.setValueAt(boat.getName(), B, row);
                    sheet.setValueAt(boat.getGPH(), C, row);
                    sheet.setValueAt(boat.getPLTOffshore(), D, row);
                    sheet.setValueAt(boat.getPLDOffshore(), E, row);
                    for (int ii = 0; ii < SECONDS.length; ii++)
                    {
                        char l = LETTER[ii + 5];
                        String seconds = "$"+l+"$5*86400";
                        sheet.setFormula("=("+seconds+"*D%1$d-E%1$d)/86400", ii + 5, row+1);
                    }
                    row++;
                }
                row++;
                sheet.setValueAt("Difference (sec/NM) to boat " + entry.getSailNumber() + " when it's elapsed time is (sec/NM)", B, row);
                row++;
                sheet.setValueAt("Sail #", A, row);
                sheet.setValueAt("Name", B, row);
                sheet.setValueAt("GPH", C, row);
                sheet.setValueAt("PLT", D, row);
                sheet.setValueAt("PLD", E, row);
                for (int ii = 0; ii < SECONDS.length; ii++)
                {
                    sheet.setFormula("=%2$d/86400", ii + 5, row+1, SECONDS[ii]);
                }
                row++;
                for (PLSBoat boat : list)
                {
                    if (!entry.equals(boat))
                    {
                        sheet.setValueAt(boat.getSailNumber(), A, row);
                        sheet.setValueAt(boat.getName(), B, row);
                        sheet.setValueAt(boat.getGPH(), C, row);
                        sheet.setValueAt(boat.getPLTOffshore(), D, row);
                        sheet.setValueAt(boat.getPLDOffshore(), E, row);
                        for (int ii = 0; ii < SECONDS.length; ii++)
                        {
                            char l = LETTER[ii + 5];
                            String seconds = "$"+l+"$5*86400";
                            String entryPLT = "$D%2$d";
                            String entryPLD = "$E%2$d";
                            String boatPLT = "$D$%1$d";
                            String boatPLD = "$E$%1$d";
                            String f = "=(((("+seconds+"*"+entryPLT+"-"+entryPLD+")+"+boatPLD+")/"+boatPLT+")-"+seconds+")/86400";
                            sheet.setFormula(f, ii + 5, row+1, myRow+1);
                        }
                        row++;
                    }
                }
                for (int r = 1; r < 100; r++)
                {
                    row++;
                    if (!course.exists(A, r))
                    {
                        break;
                    }
                    String legName = (String) course.getString(A, r);
                    if (legName.isEmpty())
                    {
                        break;
                    }
                    double miles = course.getDouble(B, r);
                    sheet.setValueAt(miles, A, row);
                    int milesRow = row;
                    sheet.setValueAt(String.format("%s Allowed time, when %s elapsed time is",
                            legName, entry.getSailNumber()), B, row);
                    row++;
                    sheet.setValueAt("Sail #", A, row);
                    sheet.setValueAt("Name", B, row);
                    sheet.setValueAt("GPH", C, row);
                    sheet.setValueAt("PLT", D, row);
                    sheet.setValueAt("PLD", E, row);
                    int start = (int) (miles / 0.05);
                    int end = (int) (miles / 0.15);
                    int step = (start - end) / 8;
                    int c = 5;
                    for (int min = start; min >= end; min -= step)
                    {
                        sheet.setFormula("=%2$d/1440", c, row+1, min);
                        c++;
                    }
                    int hourRow = row;
                    row++;
                    for (PLSBoat boat : list)
                    {
                        if (!entry.equals(boat))
                        {
                            sheet.setValueAt(boat.getSailNumber(), A, row);
                            sheet.setValueAt(boat.getName(), B, row);
                            sheet.setValueAt(boat.getGPH(), C, row);
                            sheet.setValueAt(boat.getPLTOffshore(), D, row);
                            sheet.setValueAt(boat.getPLDOffshore(), E, row);
                            c = 5;
                            for (int min = start; min >= end; min -= step)
                            {
                                double t = 60 * min;
                                double ve = t * entry.getPLTOffshore() - entry.getPLDOffshore() * miles;
                                double v = (ve + boat.getPLDOffshore() * miles) / boat.getPLTOffshore();
                                int diff = (int) ((v - t) / 60);
                                char l = LETTER[c];
                                String seconds = "($"+l+"$%3$d*86400)";
                                String entryPLT = "$D$%2$d";
                                String entryPLD = "$E$%2$d";
                                String boatPLT = "$D$%1$d";
                                String boatPLD = "$E$%1$d";
                                String legMiles = "$A$%4$d";
                                String vef = seconds+"*"+entryPLT+"-"+entryPLD+"*"+legMiles;
                                String vf = "("+vef+"+"+boatPLD+"*"+legMiles+")/"+boatPLT;
                                String f = "=("+vf+"-"+seconds+")/86400";
                                sheet.setFormula(f, c, row+1, myRow+1, hourRow+1, milesRow+1);
                                c++;
                            }
                            row++;
                        }
                    }
                }
            }
            clazz++;
        }
    }

    private List<Division> getDivisions() throws IOException, ServiceException
    {
        List<Division> list = new ArrayList<>();
        RandomAccessWorksheet sheet = getSheet("GPH");
        for (int row = 0;row<10;row++)
        {
            String cls = sheet.getString(A, row);
            if (cls.isEmpty())
            {
                break;
            }
            else
            {
                double min = sheet.getDouble(B, row);
                double max = sheet.getDouble(C, row);
                list.add(new Division(cls, min, max));
            }
        }
        return list;
    }

    private RandomAccessWorksheet getSheet(String title) throws IOException, ServiceException
    {
        WorksheetEntry sheet = worksheetMap.get(title);
        sheet.setColCount(20);
        sheet.setRowCount(100);
        sheet.update();
        return new RandomAccessWorksheet(service, spreadsheet, sheet);
    }

    private RandomAccessWorksheet createSheet(String name) throws IOException, ServiceException
    {
        WorksheetEntry sheet = new WorksheetEntry();
        sheet.setTitle(new PlainTextConstruct(name));
        sheet.setColCount(20);
        sheet.setRowCount(100);
        sheet = service.insert(spreadsheet.getWorksheetFeedUrl(), sheet);
        return new RandomAccessWorksheet(service, spreadsheet, sheet);
    }
    
    private class Division
    {
        String name;
        double min;
        double max;
        List<PLSBoat> boats = new ArrayList<>();

        public Division(String name, double min, double max)
        {
            this.name = name;
            this.min = min;
            this.max = max;
        }
        
        public void add(PLSBoat boat)
        {
            boats.add(boat);
        }

        public List<PLSBoat> getBoats()
        {
            return boats;
        }
        
        public boolean match(double gph)
        {
            return gph >= min && gph <= max;
        }
    }
}
