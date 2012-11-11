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

import fi.hoski.util.Day;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import javax.swing.JTextField;

/**
 * @author Timo Vesalainen
 */
public class DateCalendarHelper implements ActionListener
{
    private String title;
    private JTextField field;

    public DateCalendarHelper(String title, JTextField field)
    {
        this.title = title;
        this.field = field;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Day d = new Day(field.getText());
        Day date = DateChooser.chooseDate(title, d);
        field.setText(date.toString());
        field.requestFocusInWindow();
    }
}
