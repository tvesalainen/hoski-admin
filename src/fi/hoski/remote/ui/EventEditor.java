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

import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.DataObjectObserver;
import fi.hoski.remote.DataStoreService;
import fi.hoski.datastore.repository.Event;
import fi.hoski.datastore.repository.Event.EventType;
import fi.hoski.remote.DataStore;
import fi.hoski.util.Day;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import javax.swing.*;

/**
 * @author Timo Vesalainen
 */
public class EventEditor implements ActionListener, DataObjectObserver
{
    private DataStoreService dss = DataStore.getDss();
    
    private EventType type;
    private JFrame parent;
    private JDialog dialog;
    private Map<String,JComponent> componentMap;
    
    public EventEditor(EventType type, JFrame parent)
    {
        this.type = type;
        this.parent = parent;
    }

    public EventEditor(JFrame parent)
    {
        this.parent = parent;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert type != null;
        Event event = new Event(type);
        edit(event);
    }

    public void edit(Event event)
    {
        event.addObserver(this);
        DataObjectDialog<Event> et = new DataObjectDialog<Event>(parent, Event.MODEL, event);
        componentMap = et.getComponentMap();
        if (type == null)
        {
            // editing not creating
            JComponent comp = componentMap.get(Event.EVENTDATE);
            comp.setEnabled(false);
        }
        if (et.edit())
        {
            try
            {
                dss.put(event);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, ex.getMessage());
            }
        }
    }
    @Override
    public void changed(DataObject dataObject, String property, Object oldValue, Object newValue)
    {
        JComponent component = componentMap.get(property);
        if (component != null)
        {
            ComponentAccessor.set(component, newValue);
        }
        if (Event.EVENTDATE.equals(property))
        {
            Day closing = (Day) dataObject.get(Event.CLOSINGDATE);
            if (closing == null)
            {
                Day date = (Day) newValue;
                closing = new Day(date);
                closing.addDays(-6);
                dataObject.set(Event.CLOSINGDATE, closing);
            }
        }
    }

}
