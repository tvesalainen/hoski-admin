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

import com.google.appengine.api.datastore.EntityNotFoundException;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.DataObjectModel;
import fi.hoski.datastore.repository.DataObjectObserver;
import fi.hoski.datastore.repository.RaceFleet;
import fi.hoski.datastore.repository.RaceSeries;
import fi.hoski.remote.DataStoreService;
import fi.hoski.sailwave.Fleet;
import fi.hoski.sailwave.SailWaveFile;
import fi.hoski.util.Day;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.TableCellEditor;

/**
 * @author Timo Vesalainen
 */
public class RaceDialog extends JDialog implements DataObjectObserver
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");
    
    private DataStoreService dss;
    private RaceSeries raceSeries;
    private List<RaceFleet> raceFleetList;
    private Map<String, JComponent> componentMap;
    private DataObjectListTableModel<RaceFleet> etm;
    private boolean accepted;
    private JTable table;

    RaceDialog(
            JFrame frame, 
            String event, 
            final DataStoreService dss, 
            final DataObjectModel model, 
            final RaceSeries raceSeries,
            final DataObjectModel listModel, 
            final List<RaceFleet> raceFleetList,
            final SailWaveFile swf
            )
    {
        super(frame, event);
        this.dss = dss;
        this.raceSeries = raceSeries;
        this.raceFleetList = raceFleetList;
        componentMap = DialogUtil.createEditPane(this, model, raceSeries, BorderLayout.NORTH);
        
        etm = new DataObjectListTableModel<>(listModel, raceFleetList);
        table = new FitTable(etm);
        TableSelectionHandler tsh = new TableSelectionHandler(table);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        // buttons
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        add(panel, BorderLayout.SOUTH);
        JButton ok = new JButton(uiBundle.getString("OK"));
        ActionListener okAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                TableCellEditor cellEditor = table.getCellEditor();
                if (cellEditor != null)
                {
                    cellEditor.stopCellEditing();
                }
                String unsetProperty = null;
                unsetProperty = raceSeries.firstMandatoryNullProperty(model.getPropertyList());
                if (unsetProperty == null)
                {
                    for (DataObject d : raceFleetList)
                    {
                        unsetProperty = d.firstMandatoryNullProperty(listModel.getPropertyList());
                        if (unsetProperty != null)
                        {
                            break;
                        }
                    }
                }
                if (unsetProperty != null)
                {
                    JOptionPane.showMessageDialog(rootPane, raceSeries.getPropertyString(unsetProperty), uiBundle.getString("UNSET FIELD"), JOptionPane.ERROR_MESSAGE);
                }
                else
                {
                    accepted = true;
                    setVisible(false);
                }
            }
        };
        ok.addActionListener(okAction);
        panel.add(ok);
        
        JButton cancel = new JButton(uiBundle.getString("CANCEL"));
        ActionListener cancelAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
            }
        };
        cancel.addActionListener(cancelAction);
        panel.add(cancel);

        JButton addFleet = new JButton(uiBundle.getString("ADD FLEET"));
        ActionListener addFleetAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int row = table.getSelectedRow();
                if (row != -1)
                {
                    TableCellEditor cellEditor = table.getCellEditor();
                    if (cellEditor != null)
                    {
                        cellEditor.stopCellEditing();
                    }
                    RaceFleet rf = etm.getObject(row);
                    int swid = rf.getSailWaveId();
                    Fleet fleet = swf.getFleet(swid);
                    Fleet copyFleet = swf.copyFleet(fleet);
                    etm.add(rf.makeCopy(copyFleet.getNumber()));
                }
            }
        };
        addFleet.addActionListener(addFleetAction);
        panel.add(addFleet);

        JButton deleteFleet = new JButton(uiBundle.getString("DELETE FLEET"));
        ActionListener deleteFleetAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int row = table.getSelectedRow();
                if (row != -1)
                {
                    TableCellEditor cellEditor = table.getCellEditor();
                    if (cellEditor != null)
                    {
                        cellEditor.stopCellEditing();
                    }
                    RaceFleet rf = etm.getObject(row);
                    try
                    {
                        int numberOfRaceEntriesFor = dss.getNumberOfRaceEntriesFor(rf);
                        if (numberOfRaceEntriesFor > 0)
                        {
                            JOptionPane.showMessageDialog(rootPane, uiBundle.getString("FLEET HAS ENTRIES"), uiBundle.getString("UNABLE TO DELETE"), JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    catch (EntityNotFoundException ex)
                    {
                        throw new IllegalArgumentException(ex);
                    }
                    etm.remove(rf);
                }
            }
        };
        deleteFleet.addActionListener(deleteFleetAction);
        panel.add(deleteFleet);

        setLocationByPlatform(true);
        setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        pack();
        setSize(700, 520);
    }

    public void setEditable(String... properties)
    {
        etm.setEditable(properties);
    }
    public boolean edit()
    {
        raceSeries.addObserver(this);
        accepted = false;
        setVisible(true);
        raceSeries.removeObserver(this);
        if (accepted)
        {
            for (RaceFleet fleet : raceFleetList)
            {
                Boolean ranking = (Boolean) fleet.get(RaceFleet.Ranking);
                Day eventDay = (Day) fleet.get(RaceFleet.EventDate);
                if (ranking != null && ranking)
                {
                    fleet.set(RaceFleet.ClosingDate, eventDay);
                }
            }
        }
        return accepted;
    }

    @Override
    public void changed(DataObject dataObject, String property, Object oldValue, Object newValue)
    {
        if (oldValue != null && oldValue.equals(newValue))
        {
            return;
        }
        JComponent component = componentMap.get(property);
        if (component != null)
        {
            ComponentAccessor.set(component, newValue);
        }
        if (RaceSeries.EventDate.equals(property))
        {
            dataObject.set(RaceSeries.TO, newValue);
            for (RaceFleet fleet : raceFleetList)
            {
                fleet.set(property, newValue);
            }
        }
        if (RaceSeries.TO.equals(property))
        {
            dataObject.set(RaceSeries.ClosingDate, newValue);
        }
        if (RaceSeries.ClosingDate.equals(property))
        {
            for (RaceFleet fleet : raceFleetList)
            {
                Boolean ranking = (Boolean) fleet.get(RaceFleet.Ranking);
                if (ranking == null || !ranking)
                {
                    fleet.set(property, newValue);
                }
            }
        }
        if (RaceSeries.CLOSINGDATE2.equals(property))
        {
            for (RaceFleet fleet : raceFleetList)
            {
                Boolean ranking = (Boolean) fleet.get(RaceFleet.Ranking);
                if (ranking == null || !ranking)
                {
                    fleet.set(property, newValue);
                }
            }
        }
        if (RaceSeries.STARTTIME.equals(property))
        {
            for (RaceFleet fleet : raceFleetList)
            {
                fleet.set(property, newValue);
            }
        }
        repaint();
        table.repaint();
    }
}
