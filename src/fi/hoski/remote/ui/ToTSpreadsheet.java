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
import fi.hoski.datastore.repository.DataObjectComparator;
import fi.hoski.datastore.repository.RaceEntry;
import fi.hoski.google.docs.A1;
import fi.hoski.google.docs.RandomAccessWorksheet;
import fi.hoski.util.orc.GPHComparator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Timo Vesalainen
 */
public class ToTSpreadsheet implements A1
{
    private SpreadsheetService service;
    private SpreadsheetEntry spreadsheet;
    private Map<String,WorksheetEntry> worksheetMap = new HashMap<>();

    public ToTSpreadsheet(SpreadsheetService service, SpreadsheetEntry spreadsheet) throws IOException, ServiceException
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

    public void fill(List<RaceEntry> competitors) throws IOException, ServiceException
    {
        DataObjectComparator comparator = new DataObjectComparator(RaceEntry.RATING);
        List<Division> divisions = getDivisions();
        for (RaceEntry boat : competitors)
        {
            for (Division div : divisions)
            {
                String ratingStr = (String) boat.get(RaceEntry.RATING);
                double rating = Double.parseDouble(ratingStr.replace(',', '.'));
                if (div.match(rating))
                {
                    div.add(boat);
                    break;
                }
            }
        }
        List<List<RaceEntry>> all = new ArrayList<>();
        for (Division div : divisions)
        {
            Collections.sort(div.boats, comparator);
            all.add(div.boats);
        }
        int clazz = 1;
        int offset = 3;
        int row = 7;
        RandomAccessWorksheet sheet = getSheet("Scoring");

        for (List<RaceEntry> list : all)
        {
            if (!list.isEmpty())
            {
                RaceEntry sample = list.get(0);
                String fleet = (String) sample.get(RaceEntry.FLEET);
                String className = fleet + clazz;
                sheet.setValueAt(className, A, offset);
                sheet.setValueAt("Start:", D, offset);

                sheet.setValueAt("Position", A, offset+2);
                sheet.setValueAt("Sail #", B, offset+2);
                sheet.setValueAt("Boat", D, offset+2);
                sheet.setValueAt("Type", E, offset+2);
                sheet.setValueAt("Owner", F, offset+2);
                sheet.setValueAt("Club", G, offset+2);
                sheet.setValueAt("ToT", H, offset+2);
                sheet.setValueAt("Finish", I, offset+2);
                sheet.setValueAt("Elapsed", J, offset+2);
                sheet.setValueAt("Corrected", K, offset+2);

                int pos = 1;
                int start = offset + 5;
                int end = list.size() + row;
                for (RaceEntry boat : list)
                {
                    String ratingStr = (String) boat.get(RaceEntry.RATING);
                    float rating = Float.parseFloat(ratingStr.replace(',', '.'));
                    sheet.setValueAt(pos, A, row);
                    sheet.setValueAt(new String(boat.getNationality()), B, row);
                    sheet.setValueAt(new Float(boat.getNumber()), C, row);
                    String boatName = boat.getName();
                    if (boatName != null)
                    {
                        sheet.setValueAt(boat.getName(), D, row);
                    }
                    sheet.setValueAt(boat.getType(), E, row);
                    sheet.setValueAt(boat.getOwner(), F, row);
                    sheet.setValueAt(boat.getClub(), G, row);
                    sheet.setValueAt(rating, H, row);
                    sheet.setFormula("=IF(ISNUMBER(I%1$d);I%1$d-$E$%2$d;\"\")", J, row+1, offset+1);
                    sheet.setFormula("=IF(ISNUMBER(I%1$d);H%1$d*J%1$d;\"\")", K, row+1, offset + 4);
                    row++;
                    pos++;
                }
                offset += 5 + list.size();
                row += 5;
            }
            clazz++;
        }
    }

    private List<Division> getDivisions() throws IOException, ServiceException
    {
        List<Division> list = new ArrayList<>();
        RandomAccessWorksheet sheet = getSheet("DIV");
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
        List<RaceEntry> boats = new ArrayList<>();

        public Division(String name, double min, double max)
        {
            this.name = name;
            this.min = min;
            this.max = max;
        }
        
        public void add(RaceEntry boat)
        {
            boats.add(boat);
        }

        public List<RaceEntry> getBoats()
        {
            return boats;
        }
        
        public boolean match(double gph)
        {
            return gph >= min && gph <= max;
        }
    }
}
