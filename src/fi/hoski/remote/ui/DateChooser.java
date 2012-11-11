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

import fi.hoski.util.EasterCalendar;
import fi.hoski.util.Day;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * @author Timo Vesalainen
 */
public class DateChooser extends AbstractTableModel
{
    private JDialog dialog;
    private JTable table;
    private JLabel monthLabel;
    private static ColorRenderer colorRenderer = new ColorRenderer();
    private EasterCalendar cal;
    private DateFormatSymbols symbols;
    private int firstWeekday;
    private int daysInMonth;
    private int firstDayOfWeek;

    private DateChooser(final JDialog dialog, EasterCalendar cal, DateFormatSymbols symbols)
    {
        this.dialog = dialog;
        this.cal = cal;
        this.symbols = symbols;
        
        String longest = "";
        for (String m : symbols.getMonths())
        {
            if (m.length() > longest.length())
            {
                longest = m;
            }
        }
        monthLabel = new JLabel(longest+" 9999");
        
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        dialog.add(panel, BorderLayout.NORTH);
        
        URL urlLeft = getClass().getResource("images/arrowleft.png");
        ImageIcon iconLeft = new ImageIcon(urlLeft);
        JButton backward = new JButton(iconLeft);
        backward.setPreferredSize(new Dimension(iconLeft.getIconWidth(), iconLeft.getIconHeight()));
        panel.add(backward);
        
        ActionListener backwardAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                updateMonth(monthLabel, -1);
            }

        };
        backward.addActionListener(backwardAction);
        
        panel.add(monthLabel);
        
        URL urlRight = getClass().getResource("images/arrowright.png");
        ImageIcon iconRight = new ImageIcon(urlRight);
        JButton forward = new JButton(iconRight);
        forward.setPreferredSize(new Dimension(iconRight.getIconWidth(), iconRight.getIconHeight()));
        panel.add(forward);
        
        ActionListener forwardAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                updateMonth(monthLabel, 1);
            }
        };
        forward.addActionListener(forwardAction);
        
        table = new JTable(this)
        {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column)
            {
                if (isHolyday(row, column))
                {
                    return getCellRenderer();
                }
                else
                {
                    return super.getCellRenderer(row, column);
                }
            }
        };
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int ps = table.getFont().getSize();
        Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
        while (columns.hasMoreElements())
        {
            TableColumn tc = columns.nextElement();
            tc.setPreferredWidth(ps);
        }

        TableSelector ts = new TableSelector(table, dialog) {

            @Override
            protected boolean selected(int row, int col)
            {
                return setDate(row, col);
            }
        };
        ts.setMinClickCount(1);
        table.addKeyListener(ts);
        table.addMouseListener(ts);
        
        dialog.add(table, BorderLayout.CENTER);
        
        dialog.setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.pack();
        updateMonth(monthLabel, 0);
    }

    private void initMonth()
    {
        Date date = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        firstWeekday = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        daysInMonth = cal.get(Calendar.DAY_OF_MONTH);
        firstDayOfWeek = cal.getFirstDayOfWeek();
        cal.setTime(date);
        dialog.repaint();
    }

    private boolean setDate(int row, int col)
    {
        int dayOfMonth = dayOfMonth(row, col);
        if (dayOfMonth != -1)
        {
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean isHolyday(int row, int column)
    {
        int dayOfMonth = dayOfMonth(row, column);
        if (dayOfMonth != -1)
        {
            Date date = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            boolean ret = cal.isHolyday();
            cal.setTime(date);
            return ret;
        }
        else
        {
            return false;
        }
    }

    private TableCellRenderer getCellRenderer()
    {
        return colorRenderer;
    }

    private int dayOfMonth(int rowIndex, int columnIndex)
    {
        int d = (rowIndex-1)*7+columnIndex+(firstDayOfWeek-firstWeekday)+1;
        if (d >= 1 && d <= daysInMonth)
        {
            return d;
        }
        else
        {
            return -1;
        }
    }

    private void updateMonth(JLabel monthLabel, int amount)
    {
        cal.add(Calendar.MONTH, amount);
        initMonth();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        String monthStr = symbols.getMonths()[month]+" "+year;
        monthLabel.setText(monthStr);
        dialog.repaint();
    }
    
    public static Day chooseDate(String title, Day date)
    {
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        return chooseDate(dialog, date);
    }
    public static Day chooseDate(JFrame frame, String title, Day date)
    {
        return chooseDate(new JDialog(frame, title), date);
    }
    public static Day chooseDate(Window window, String title, Day date)
    {
        return chooseDate(new JDialog(window, title), date);
    }
    public static Day chooseDate(JDialog dialog, String title, Day date)
    {
        return chooseDate(new JDialog(dialog, title), date);
    }
    private static Day chooseDate(final JDialog dialog, Day date)
    {
        EasterCalendar cal = new EasterCalendar();
        if (date != null)
        {
            cal.setTime(date.getDate());
        }
        DateFormatSymbols symbols = DateFormatSymbols.getInstance();
        DateChooser calDia = new DateChooser(dialog, cal, symbols);
        
        return calDia.chooseDate();
    }
    private Day chooseDate()
    {
        dialog.setVisible(true);
        return new Day(cal.getTime());
    }
    @Override
    public int getRowCount()
    {
        return 7;
    }

    @Override
    public int getColumnCount()
    {
        return 7;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (rowIndex == 0)
        {
            int wd = (7 + columnIndex + firstDayOfWeek - 1) % 7;
            return symbols.getShortWeekdays()[1+wd];
        }
        else
        {
            int d = dayOfMonth(rowIndex, columnIndex);
            if (d != -1)
            {
                return String.valueOf(d);
            }
            else
            {
                return "";
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        try
        {
            Day chooseDate = DateChooser.chooseDate("Testaan", new Day());
            System.err.println(chooseDate);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
