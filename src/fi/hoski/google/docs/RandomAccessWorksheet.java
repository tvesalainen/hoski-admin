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

package fi.hoski.google.docs;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Timo Vesalainen
 */
public class RandomAccessWorksheet implements A1
{
    private SpreadsheetService service;
    private SpreadsheetEntry spreadsheet;
    private WorksheetEntry worksheet;
    private CellFeed feed;
    private Map<String,CellEntry> map = new HashMap<>();
    private int maxCol;
    private int maxRow;

    public RandomAccessWorksheet(SpreadsheetService service, SpreadsheetEntry spreadsheet, WorksheetEntry worksheet) throws IOException, ServiceException
    {
        this.service = service;
        this.spreadsheet = spreadsheet;
        this.worksheet = worksheet;
        feed = service.getFeed(worksheet.getCellFeedUrl(), CellFeed.class);
        for (CellEntry cell : feed.getEntries())
        {
            int col = cell.getCell().getCol();
            int row = cell.getCell().getRow();
            maxCol = Math.max(col, maxCol);
            maxRow = Math.max(row, maxRow);
            map.put(cell.getTitle().getPlainText(), cell);
        }
    }

    public RandomAccessWorksheet copy(String name) throws IOException, ServiceException
    {
        WorksheetEntry sheet = new WorksheetEntry(worksheet);
        sheet.setTitle(new PlainTextConstruct(name));
        sheet.setColCount(20);
        sheet.setRowCount(100);
        sheet = service.insert(spreadsheet.getWorksheetFeedUrl(), sheet);
        return new RandomAccessWorksheet(service, spreadsheet, sheet);
    }
    
    public void setValueAt(Object value, int col, int row) throws IOException, ServiceException
    {
        CellEntry cell = getCellEntry(col, row);
        if (cell == null)
        {
            cell = new CellEntry(row+1, col+1, value.toString());
            cell = feed.insert(cell);
            map.put(cell.getTitle().getPlainText(), cell);
            maxCol = Math.max(col, maxCol);
            maxRow = Math.max(row, maxRow);
        }
        else
        {
            cell.changeInputValueLocal(value.toString());
            cell.update();
        }
    }
    
    public void setFormula(String formula, int col, Object... args) throws IOException, ServiceException
    {
        Integer row = (Integer) args[0];
        setValueAt(String.format(formula, args), col, row-1);
    }

    public boolean exists(int col, int row)
    {
        return getCellEntry(col, row) != null;
    }
    
    public String getString(int col, int row)
    {
        CellEntry cell = getCellEntry(col, row);
        if (cell != null)
        {
            return cell.getCell().getInputValue();
        }
        else
        {
            return "";
        }
    }

    public int getInt(int col, int row)
    {
        CellEntry cell = getCellEntry(col, row);
        return cell.getCell().getNumericValue().intValue();
    }

    public boolean isNumber(int col, int row)
    {
        CellEntry cell = getCellEntry(col, row);
        return cell.getCell().getNumericValue() != null;
    }

    public double getDouble(int col, int row)
    {
        CellEntry cell = getCellEntry(col, row);
        return cell.getCell().getDoubleValue();
    }

    private CellEntry getCellEntry(int col, int row)
    {
        row++;
        String key = ""+LETTER[col]+row;
        return map.get(key);
    }

    public void insertRows(int at, int count) throws IOException, ServiceException
    {
        for (int row=maxRow-count;row > at;row--)
        {
            for (int col=0;col<maxCol;col++)
            {
                String value = getString(col, row);
                setValueAt(value, col, row+count);
            }
        }
        for (int row=at;row <at+count;row++)
        {
            for (int col=0;col<maxCol;col++)
            {
                setValueAt("", col, row+count);
            }
        }
    }

}
