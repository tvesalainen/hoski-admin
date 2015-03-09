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

import fi.hoski.util.Utils;
import fi.hoski.sailwave.Fleet;
import fi.hoski.sailwave.Competitor;
import fi.hoski.sailwave.Race;
import fi.hoski.sailwave.SailWaveFile;
import com.google.appengine.api.datastore.Blob;
import fi.hoski.remote.DataStoreService;
import fi.hoski.remote.DataStoreProxy;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Link;
import com.google.appengine.api.datastore.Text;
import fi.hoski.datastore.*;
import fi.hoski.datastore.repository.*;
import fi.hoski.datastore.repository.Attachment.Type;
import fi.hoski.datastore.repository.Event.EventType;
import fi.hoski.remote.DataStore;
import fi.hoski.datastore.repository.Event;
import fi.hoski.remote.*;
import fi.hoski.remote.sync.InspectionHandler;
import fi.hoski.remote.sync.IsNotInspectedFilter;
import fi.hoski.remote.sync.SqlConnection;
import fi.hoski.remote.ui.sms.SMSPlugin;
import fi.hoski.sms.SMSException;
import fi.hoski.util.BoatInfo;
import fi.hoski.util.CreditorReference;
import fi.hoski.util.Day;
import fi.hoski.util.Time;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import org.json.JSONException;
import org.json.JSONObject;
import org.vesalainen.parsers.sql.dsql.ui.WorkBench;

/**
 * @author Timo Vesalainen
 */
public class Admin extends WindowAdapter
{
    public static final int MAXMEMBER = 90000;
    public static URL SPREADSHEET_FEED_URL;
    private static final String SAILWAVEDIR = "SailWaveDir";
    private static final String BACKUPDIR = "BackupDir";
    private static final String ATTACHDIR = "AttachDir";
    private static final String RACEATTACHDIR = "RaceAttachDir";
    public final static Cursor busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    public final static Cursor defaultCursor = Cursor.getDefaultCursor();
    private JFrame frame;
    private JPanel panel;
    private JMenuBar menuBar;
    private JScrollPane leftPane;
    private JScrollPane rightPane;
    private EventEditor[] creators;
    private DataStoreService dss = DataStore.getDss();
    private Messages messages;
    private ServerProperties serverProperties;
    private String server;
    private String creator;
    private JMenu menuReservation;
    private Container safeContainer;
    private JSplitPane splitPane;
    private List<Reservation> reservationList;
    private List<Reservation> selectedReservations;
    private List<Reservation> unSelectedReservations;
    private boolean privileged;
    private boolean accessUser;
    private String safeTitle;
    
    private WorkBench workBench;
    private boolean isRaceAdmin;

    public Admin(ServerProperties serverProperties) throws EntityNotFoundException, IOException, SMSException
    {
        this.serverProperties = serverProperties;
        server = serverProperties.getServer();
        creator = serverProperties.getUsername();
        UIManager.getDefaults().addResourceBundle("fi.hoski.remote.ui.ui");
        creators = new EventEditor[EventType.values().length];
        int index = 0;
        for (EventType eventType : EventType.values())
        {
            creators[index++] = new EventEditor(eventType, this.getFrame());
        }
        if (serverProperties.isZonerSMSSupported())
        {
            int credits = dss.messagesLeft();
            if (credits < 100)
            {
                String smsLeftString = TextUtil.getText("SMS LEFT");
                JOptionPane.showMessageDialog(frame, smsLeftString + " " + credits);
            }
        }
        privileged = serverProperties.isSuperUser();
        isRaceAdmin = dss.isRaceAdmin(creator+"@gmail.com");
        try
        {
            new SqlConnection(serverProperties.getProperties());
            accessUser = true;
        }
        catch (ClassNotFoundException | SQLException ex)
        {
        }
        initFrame();
    }

    /**
     * Initializes frame
     */
    private void initFrame() throws EntityNotFoundException, MalformedURLException, IOException
    {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new ExceptionHandler());
        ResourceBundle applicationProperties = ResourceBundle.getBundle("application");
        frame = new JFrame(TextUtil.getText("ADMIN") + " " + creator + " / " + server + " Version: " + applicationProperties.getString("version"));
        frame.addWindowListener(this);
        panel = new JPanel(new BorderLayout());
        frame.add(panel);
        menuBar = new JMenuBar();

        menuFile();
        menuRace();
        if (serverProperties.isSuperUser())
        {
            menuEvent();
            menuReservation();
            menuSwapPatrolShift();
        }
        menuQuery();

        frame.setJMenuBar(menuBar);

        URL clubUrl = new URL("http", server, "club.ico");
        ImageIcon clubIcon = new ImageIcon(clubUrl);
        frame.setIconImage(clubIcon.getImage());
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        frame.setSize(800, 580);
    }

    private void menuFile()
    {
        JMenu fileMenu = new JMenu();
        TextUtil.populate(fileMenu, "FILE");
        menuBar.add(fileMenu);
        if (accessUser)
        {
            fileMenu.add(menuItemSync());
        }
        fileMenu.add(menuItemTextMaintenence());
        if (privileged)
        {
            fileMenu.add(menuItemTextUpload());

            fileMenu.add(menuItemTextDownload());
            fileMenu.add(menuItemAttach());
            fileMenu.add(menuItemRemoveAttachment());
        }
        if (serverProperties.isZonerSMSSupported())
        {
            fileMenu.add(menuItemSMSCredits());
        }
        fileMenu.addSeparator();
        if (privileged)
        {
            fileMenu.add(menuItemRemoveEntity());
            fileMenu.add(menuItemRemoveYear());
            fileMenu.addSeparator();
            fileMenu.add(menuItemBackupEntity());
            fileMenu.add(menuItemBackupYear());
            fileMenu.addSeparator();
            fileMenu.add(menuItemRestoreEntity());
            fileMenu.add(menuItemRestore());
            fileMenu.add(menuItemAddYear());
        }
        fileMenu.addSeparator();
        if (accessUser)
        {
            fileMenu.add(menuItemInspectAllLightBoats());
            fileMenu.add(menuItemInspectionFix1());
            fileMenu.add(menuItemSql());
            fileMenu.add(menuItemUninspectedBoats());
        }
        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem();
        TextUtil.populate(exitItem, "EXIT");
        ActionListener exitAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                confirmSaveReservations();
                System.exit(0);
            }
        };
        exitAction = createActionListener(frame, exitAction);
        exitItem.addActionListener(exitAction);
        fileMenu.add(exitItem);
    }

    private void menuRace()
    {
        JMenu raceMenu = new JMenu();
        TextUtil.populate(raceMenu, "RACES");
        menuBar.add(raceMenu);
        if (isRaceAdmin)
        {
            raceMenu.add(menuItemUploadRaceSeries());
            raceMenu.add(menuItemEditRaceSeries());
            raceMenu.add(menuItemRemoveRaceSeries());
        }
        raceMenu.add(menuItemDownloadCompetitorsForSailwave());
        raceMenu.add(menuItemInsertCompetitorsToSailwave());
        raceMenu.add(menuItemDownloadCompetitorsAsCSV());
        raceMenu.addSeparator();
        if (isRaceAdmin)
        {
            raceMenu.add(menuItemUploadRanking());
            raceMenu.add(menuItemEditRanking());
            raceMenu.add(menuItemRemoveRanking());
        }
        raceMenu.addSeparator();
        raceMenu.add(menuItemRaceEmail());
        if (serverProperties.isZonerSMSSupported())
        {
            raceMenu.add(menuItemRaceSMS());
        }
        raceMenu.addSeparator();
        raceMenu.add(menuItemAttachRaceSeries());
        raceMenu.add(menuItemRemoveRaceSeriesAttachment());
        raceMenu.addSeparator();
        raceMenu.add(menuItemAddReferencePayments());
        raceMenu.add(menuItemAddOtherPayments());
    }

    private void menuEvent()
    {
        JMenu eventMenu = new JMenu();
        TextUtil.populate(eventMenu, "EVENTS");
        menuBar.add(eventMenu);
        JMenu addEventMenu = new JMenu();
        TextUtil.populate(addEventMenu, "ADD EVENTS");
        eventMenu.add(addEventMenu);
        JMenu editEventMenu = new JMenu();
        TextUtil.populate(editEventMenu, "EDIT EVENTS");
        eventMenu.add(editEventMenu);
        JMenu deleteEventMenu = new JMenu();
        TextUtil.populate(deleteEventMenu, "DELETE EVENTS");
        eventMenu.add(deleteEventMenu);

        int index = 0;
        for (final EventType eventType : EventType.values())
        {
            // add
            JMenuItem addItem = new JMenuItem();
            TextUtil.populate(addItem, eventType.name());
            addItem.addActionListener(creators[index++]);
            addEventMenu.add(addItem);
            // edit
            ActionListener editAction = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Event selected = chooseEvent(eventType, "EDIT SELECTED");
                    if (selected != null)
                    {
                        EventEditor editor = new EventEditor(null);
                        editor.edit(selected);
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(
                                frame,
                                TextUtil.getText("NO SELECTION"),
                                TextUtil.getText("MESSAGE"),
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            };
            editAction = createActionListener(frame, editAction);
            JMenuItem editItem = new JMenuItem();
            TextUtil.populate(editItem, eventType.name());
            editItem.addActionListener(editAction);
            editEventMenu.add(editItem);
            // delete
            ActionListener deleteAction = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    List<Event> selected = chooseEvents(eventType, "DELETE SELECTED");
                    if (selected != null && selected.size() >= 1)
                    {
                        if (JOptionPane.showConfirmDialog(
                                panel,
                                TextUtil.getText("CONFIRM DELETE")) == JOptionPane.YES_OPTION)
                        {
                            dss.deleteEvents(selected);
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(
                                frame,
                                TextUtil.getText("NO SELECTION"),
                                TextUtil.getText("MESSAGE"),
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            };
            deleteAction = createActionListener(frame, deleteAction);
            JMenuItem deleteItem = new JMenuItem();
            TextUtil.populate(deleteItem, eventType.name());
            deleteItem.addActionListener(deleteAction);
            deleteEventMenu.add(deleteItem);
        }
    }

    private void menuReservation()
    {
        menuReservation = new JMenu();
        TextUtil.populate(menuReservation, "RESERVATIONS");
        menuBar.add(menuReservation);
        JMenu menuMakeReservation = new JMenu();
        TextUtil.populate(menuMakeReservation, "MAKE RESERVATION");
        menuReservation.add(menuMakeReservation);
        JMenu editReservation = new JMenu();
        TextUtil.populate(editReservation, "EDIT RESERVATION");
        menuReservation.add(editReservation);
        JMenu mailMenu = new JMenu();
        TextUtil.populate(mailMenu, "SEND EMAIL");
        menuReservation.add(mailMenu);
        JMenu smsMenu = new JMenu();
        TextUtil.populate(smsMenu, "SEND SMS");
        smsMenu.setEnabled(serverProperties.isZonerSMSSupported());
        menuReservation.add(smsMenu);
        for (final EventType eventType : EventType.values())
        {
            ActionListener action = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    reservate(eventType, null);
                }
            };
            action = createActionListener(frame, action);
            JMenuItem menuItem = new JMenuItem();
            TextUtil.populate(menuItem, eventType.name());
            menuItem.addActionListener(action);
            menuMakeReservation.add(menuItem);
            JMenuItem editItem = new JMenuItem();
            TextUtil.populate(editItem, eventType.name());
            ActionListener editAction = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    editReservations(eventType);
                }
            };
            editAction = createActionListener(frame, editAction);
            editItem.addActionListener(editAction);
            if (Event.isInspection(eventType))
            {
                editItem.setEnabled(accessUser);
            }
            editReservation.add(editItem);
            JMenuItem mailItem = new JMenuItem();
            TextUtil.populate(mailItem, eventType.name());
            ActionListener mailAction = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    List<Event> selected = chooseEvents(eventType, "CHOOSE", true);
                    if (selected != null && selected.size() == 1)
                    {
                        try
                        {
                            Event event = selected.get(0);
                            List<Reservation> reservationList = dss.getReservations(event);
                            Day date = (Day) event.get(Event.EventDate);
                            String subject = TextUtil.getText(eventType.name()) + " " + date;
                            String body = getMessage(eventType.name());
                            MailDialog md = new MailDialog(frame, subject, body, reservationList);
                            if (md.edit())
                            {
                            }
                        }
                        catch (AddressException ex)
                        {
                            throw new IllegalArgumentException(ex);
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(
                                frame,
                                TextUtil.getText("NO SELECTION"),
                                TextUtil.getText("MESSAGE"),
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            };
            mailAction = createActionListener(frame, mailAction);
            mailItem.addActionListener(mailAction);
            mailMenu.add(mailItem);
            JMenuItem smsItem = new JMenuItem();
            TextUtil.populate(smsItem, eventType.name());
            ActionListener smsAction = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    List<Event> selected = chooseEvents(eventType, "CHOOSE", true);
                    if (selected != null && selected.size() == 1)
                    {
                        try
                        {
                            Event event = selected.get(0);
                            List<Reservation> reservationList = dss.getReservations(event);

                            List<String> phoneList = new ArrayList<String>();
                            for (Reservation r : reservationList)
                            {
                                String number = (String) r.get(Repository.JASENET_MOBILE);
                                if (number != null)
                                {
                                    phoneList.add(number);
                                }
                            }
                            SMSDialog sd = new SMSDialog(
                                    frame,
                                    TextUtil.getText(eventType.name()),
                                    "",
                                    phoneList);
                            if (sd.edit())
                            {
                            }
                        }
                        catch (IOException ex)
                        {
                            JOptionPane.showMessageDialog(frame, ex.getMessage());
                        }
                        catch (SMSException ex)
                        {
                            JOptionPane.showMessageDialog(frame, ex.getMessage());
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(
                                frame,
                                TextUtil.getText("NO SELECTION"),
                                TextUtil.getText("MESSAGE"),
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            };
            smsAction = createActionListener(frame, smsAction);
            smsItem.addActionListener(smsAction);
            smsMenu.add(smsItem);
        }
    }

    private JMenuItem menuItemRemoveEntity()
    {
        final String title = TextUtil.getText("REMOVE ENTITY");
        JMenuItem removeEntityItem = new JMenuItem(title);
        ActionListener removeEntityAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                Object kind = JOptionPane.showInputDialog(frame, title, "", JOptionPane.OK_CANCEL_OPTION, null, serverProperties.getTables(), null);
                if (kind != null)
                {
                    String confirm = TextUtil.getText("CONFIRM REMOVE") + " " + kind;
                    if (JOptionPane.showConfirmDialog(frame, confirm, "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    {
                        List<String> kindList = new ArrayList<String>();
                        kindList.add(kind.toString());
                        int count = dss.remove(kindList);
                        JOptionPane.showMessageDialog(frame, TextUtil.getText("REMOVED") + " " + count);
                    }
                }
            }
        };
        removeEntityAction = createActionListener(frame, removeEntityAction);
        removeEntityItem.addActionListener(removeEntityAction);
        return removeEntityItem;
    }

    private JMenuItem menuItemRemoveYear()
    {
        final String title = TextUtil.getText("REMOVE YEAR");
        JMenuItem removeEntityItem = new JMenuItem(title);
        ActionListener removeEntityAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                Object yearString = JOptionPane.showInputDialog(frame, title, "", JOptionPane.OK_CANCEL_OPTION);
                if (yearString != null)
                {
                    String confirm = TextUtil.getText("CONFIRM REMOVE") + " " + yearString;
                    if (JOptionPane.showConfirmDialog(frame, confirm, "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    {
                        long year = Long.parseLong(yearString.toString());
                        Day now = new Day();
                        if (year < now.getYear())
                        {
                            int count = dss.remove(year);
                            JOptionPane.showMessageDialog(frame, TextUtil.getText("REMOVED") + " " + count);
                        }
                        else
                        {
                            JOptionPane.showMessageDialog(frame, TextUtil.getText("CANNOT REMOVE") + " " + year);
                        }
                    }
                }
            }
        };
        removeEntityAction = createActionListener(frame, removeEntityAction);
        removeEntityItem.addActionListener(removeEntityAction);
        return removeEntityItem;
    }
    private static final String TSFORMAT = "yyyyMMddHHmmss";

    private JMenuItem menuItemBackupEntity()
    {
        final String title = TextUtil.getText("BACKUP ENTITY");
        JMenuItem backupYearItem = new JMenuItem(title);
        ActionListener backupYearAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                Object kind = JOptionPane.showInputDialog(frame, title, "", JOptionPane.OK_CANCEL_OPTION, null, serverProperties.getTables(), null);
                if (kind != null)
                {
                    SimpleDateFormat format = new SimpleDateFormat(TSFORMAT);
                    String ts = format.format(new Date());
                    File file = saveFile(BACKUPDIR, kind + ts + ".ser", ".ser", "Backup");
                    if (file != null)
                    {
                        int count;
                        try (FileOutputStream fos = new FileOutputStream(file))
                        {
                            BufferedOutputStream bos = new BufferedOutputStream(fos);
                            ObjectOutputStream oos = new ObjectOutputStream(bos);
                            List<String> list = new ArrayList<String>();
                            list.add(kind.toString());
                            count = dss.backup(list, oos);
                            JOptionPane.showMessageDialog(frame, TextUtil.getText("STORED") + " " + count);
                        }
                        catch (IOException ex)
                        {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(frame, ex.getMessage());
                        }
                    }
                }
            }
        };
        backupYearAction = createActionListener(frame, backupYearAction);
        backupYearItem.addActionListener(backupYearAction);
        return backupYearItem;
    }

    private JMenuItem menuItemBackupYear()
    {
        final String title = TextUtil.getText("BACKUP YEAR");
        JMenuItem backupYearItem = new JMenuItem(title);
        ActionListener backupYearAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                Object yearString = JOptionPane.showInputDialog(frame, title, "", JOptionPane.OK_CANCEL_OPTION);
                if (yearString != null)
                {
                    long year = Long.parseLong(yearString.toString());
                    File file = saveFile(BACKUPDIR, year + ".ser", ".ser", "Backup");
                    if (file != null)
                    {
                        int count;
                        try (FileOutputStream fos = new FileOutputStream(file))
                        {
                            BufferedOutputStream bos = new BufferedOutputStream(fos);
                            ObjectOutputStream oos = new ObjectOutputStream(bos);
                            count = dss.backup(year, oos);
                            JOptionPane.showMessageDialog(frame, TextUtil.getText("STORED") + " " + count);
                        }
                        catch (IOException ex)
                        {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(frame, ex.getMessage());
                        }
                    }
                }
            }
        };
        backupYearAction = createActionListener(frame, backupYearAction);
        backupYearItem.addActionListener(backupYearAction);
        return backupYearItem;
    }

    private JMenuItem menuItemRestore()
    {
        final String title = TextUtil.getText("RESTORE");
        JMenuItem backupYearItem = new JMenuItem(title);
        ActionListener backupYearAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                File file = openFile(BACKUPDIR, ".ser", "Backup");
                if (file != null)
                {
                    int count;
                    try (FileInputStream fos = new FileInputStream(file))
                    {
                        BufferedInputStream bis = new BufferedInputStream(fos);
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        count = dss.restore(ois);
                        JOptionPane.showMessageDialog(frame, TextUtil.getText("RESTORED") + " " + count);
                    }
                    catch (IOException ex)
                    {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, ex.getMessage());
                    }
                }
            }
        };
        backupYearAction = createActionListener(frame, backupYearAction);
        backupYearItem.addActionListener(backupYearAction);
        return backupYearItem;
    }

    private JMenuItem menuItemInspectionFix1()
    {
        final String title = "Inspection fix1";
        JMenuItem insFix1Item = new JMenuItem(title);
        ActionListener insFix1Action = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    dss.inspectionFix1();
                }
                catch (SQLException | ClassNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        insFix1Action = createActionListener(frame, insFix1Action);
        insFix1Item.addActionListener(insFix1Action);
        return insFix1Item;
    }

    private JMenuItem menuItemSql()
    {
        final String title = TextUtil.getText("JDBC SQL");
        JMenuItem sqlItem = new JMenuItem(title);
        ActionListener sqlAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    SqlDialog dia = new SqlDialog(frame, "select ", serverProperties.getProperties());
                    dia.edit();
                }
                catch (SQLException | ClassNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        sqlAction = createActionListener(frame, sqlAction);
        sqlItem.addActionListener(sqlAction);
        return sqlItem;
    }

    private JMenuItem menuItemUninspectedBoats()
    {
        final String title = TextUtil.getText("UNINSPECTED BOATS");
        JMenuItem sqlItem = new JMenuItem(title);
        ActionListener sqlAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    InspectionHandler ih = new InspectionHandler(serverProperties.getProperties());
                    String selectSql = "select Veneet.VeneID, Jasenet.Etunimi, Jasenet.Sukunimi, Veneet.Nimi, Veneet.Tyyppi from Veneet, Jasenet where Veneet.Omistaja = Jasenet.JasenNo and Veneet.Omistaja < "+MAXMEMBER;
                    SqlConnection connection = new SqlConnection(serverProperties.getProperties());
                    ResultSet rs = connection.query(selectSql);
                    ResultSetModel model = new ResultSetModel(rs);
                    List<DataObject> list = DataObject.convert(model, rs, new IsNotInspectedFilter(ih));
                    DataObjectListDialog<DataObject> dia = new DataObjectListDialog<>(frame, "", "OK", model, list);
                    dia.setAutoCreateRowSorter(true);
                    dia.select();
                }
                catch (SQLException | ClassNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        sqlAction = createActionListener(frame, sqlAction);
        sqlItem.addActionListener(sqlAction);
        return sqlItem;
    }

    private JMenuItem menuItemAddYear()
    {
        final String title = TextUtil.getText("ADD YEAR");
        JMenuItem addYearItem = new JMenuItem(title);
        ActionListener addYearAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                String year = JOptionPane.showInputDialog(frame, TextUtil.getText("ADD YEAR"));
                int y = Integer.parseInt(year);
                if (y >= 1800 && y <= 2100)
                {
                    dss.addYear(y);
                }
            }
        };
        addYearAction = createActionListener(frame, addYearAction);
        addYearItem.addActionListener(addYearAction);
        return addYearItem;
    }
    private JMenuItem menuItemInspectAllLightBoats()
    {
        final String title = TextUtil.getText("INSPECT ALL LIGHT BOATS");
        JMenuItem inspectAllLightBoatsItem = new JMenuItem(title);
        ActionListener inspectAllLightBoatsAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    dss.inspectAllLightBoats(MAXMEMBER);
                }
                catch (SQLException | ClassNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        inspectAllLightBoatsAction = createActionListener(frame, inspectAllLightBoatsAction);
        inspectAllLightBoatsItem.addActionListener(inspectAllLightBoatsAction);
        return inspectAllLightBoatsItem;
    }

    private JMenuItem menuItemRestoreEntity()
    {
        final String title = TextUtil.getText("RESTORE ENTITY");
        JMenuItem backupYearItem = new JMenuItem(title);
        ActionListener backupYearAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                Object kind = JOptionPane.showInputDialog(frame, title, "", JOptionPane.OK_CANCEL_OPTION, null, serverProperties.getTables(), null);
                if (kind != null)
                {
                    File file = openFile(BACKUPDIR, ".ser", "Backup");
                    if (file != null)
                    {
                        int count;
                        try (FileInputStream fos = new FileInputStream(file))
                        {
                            BufferedInputStream bis = new BufferedInputStream(fos);
                            ObjectInputStream ois = new ObjectInputStream(bis);
                            List<String> list = new ArrayList<String>();
                            list.add(kind.toString());
                            count = dss.restore(list, ois);
                            JOptionPane.showMessageDialog(frame, TextUtil.getText("RESTORED") + " " + count);
                        }
                        catch (IOException ex)
                        {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(frame, ex.getMessage());
                        }
                    }
                }
            }
        };
        backupYearAction = createActionListener(frame, backupYearAction);
        backupYearItem.addActionListener(backupYearAction);
        return backupYearItem;
    }

    private JMenuItem menuItemSMSCredits()
    {
        JMenuItem creditItem = new JMenuItem();
        TextUtil.populate(creditItem, "CHECK CREDITS");
        ActionListener creditAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    int credits = dss.messagesLeft();
                    String smsLeftString = TextUtil.getText("SMS LEFT");
                    JOptionPane.showMessageDialog(frame, smsLeftString + " " + credits);
                }
                catch (IOException ex)
                {
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
                catch (SMSException ex)
                {
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        creditAction = createActionListener(frame, creditAction);
        creditItem.addActionListener(creditAction);
        return creditItem;
    }

    private JMenuItem menuItemDownloadCompetitorsForSailwave()
    {
        // download series
        JMenuItem downloadRaceCompetitorsForSailwave = new JMenuItem();
        TextUtil.populate(downloadRaceCompetitorsForSailwave, "DOWNLOAD COMPETITORS FOR SAILWAVE");
        ActionListener downloadCompetitorsForSailwaveAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    downloadCompetitorsForSailwave();
                }
                catch (EntityNotFoundException | IOException | JSONException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        downloadCompetitorsForSailwaveAction = createActionListener(frame, downloadCompetitorsForSailwaveAction);
        downloadRaceCompetitorsForSailwave.addActionListener(downloadCompetitorsForSailwaveAction);
        return downloadRaceCompetitorsForSailwave;
    }

    private JMenuItem menuItemInsertCompetitorsToSailwave()
    {
        // download series
        JMenuItem insertRaceCompetitorsToSailwave = new JMenuItem();
        TextUtil.populate(insertRaceCompetitorsToSailwave, "INSERT COMPETITORS TO SAILWAVE");
        ActionListener insertCompetitorsToSailwaveAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    insertCompetitorsToSailwave();
                }
                catch (EntityNotFoundException | IOException | JSONException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        insertCompetitorsToSailwaveAction = createActionListener(frame, insertCompetitorsToSailwaveAction);
        insertRaceCompetitorsToSailwave.addActionListener(insertCompetitorsToSailwaveAction);
        return insertRaceCompetitorsToSailwave;
    }

    private JMenuItem menuItemDownloadCompetitorsAsCSV()
    {
        // download series
        JMenuItem downloadRaceCompetitorsAsCSV = new JMenuItem();
        TextUtil.populate(downloadRaceCompetitorsAsCSV, "DOWNLOAD COMPETITORS AS CSV");
        ActionListener downloadCompetitorsAsCSVAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    downloadCompetitorsAsCSV();
                }
                catch (EntityNotFoundException | IOException | JSONException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        downloadCompetitorsAsCSVAction = createActionListener(frame, downloadCompetitorsAsCSVAction);
        downloadRaceCompetitorsAsCSV.addActionListener(downloadCompetitorsAsCSVAction);
        return downloadRaceCompetitorsAsCSV;
    }

    private JMenuItem menuItemRaceEmail()
    {
        // download series
        JMenuItem raceEmail = new JMenuItem();
        TextUtil.populate(raceEmail, "SEND EMAIL");
        ActionListener raceEmailAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    List<RaceEntry> competitors = chooseCompetitors();
                    MailDialog md = new MailDialog(frame, "", "", competitors);
                    md.edit();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        raceEmailAction = createActionListener(frame, raceEmailAction);
        raceEmail.addActionListener(raceEmailAction);
        return raceEmail;
    }

    private JMenuItem menuItemRaceSMS()
    {
        // download series
        final String title = TextUtil.getText("SEND SMS");
        JMenuItem raceSMS = new JMenuItem(title);
        ActionListener raceSMSAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    List<RaceEntry> competitors = chooseCompetitors();
                    List<String> phoneList = new ArrayList<String>();
                    for (RaceEntry r : competitors)
                    {
                        String number = (String) r.get(RaceEntry.HELMPHONE);
                        if (number != null && !number.isEmpty())
                        {
                            phoneList.add(number);
                        }
                    }
                    SMSDialog sd = new SMSDialog(
                            frame,
                            title,
                            "",
                            phoneList);
                    if (sd.edit())
                    {
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        raceSMSAction = createActionListener(frame, raceSMSAction);
        raceSMS.addActionListener(raceSMSAction);
        return raceSMS;
    }

    private JMenu menuItemAttachRaceSeries()
    {
        // download series
        JMenu attachRaceSeriesMenu = new JMenu();
        TextUtil.populate(attachRaceSeriesMenu, "ATTACH RACE");
        for (final Attachment.Type type : Attachment.Type.values())
        {
            JMenu attachmentType = new JMenu(TextUtil.getText(type.name()));
            TextUtil.populate(attachmentType, type.name());
            attachRaceSeriesMenu.add(attachmentType);

            JMenuItem fileItem = new JMenuItem();
            TextUtil.populate(fileItem, "FILE");
            ActionListener fileAction = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        attachRaceSeriesFile(type);
                    }
                    catch (EntityNotFoundException | IOException ex)
                    {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, ex.getMessage());
                    }
                }
            };
            fileAction = createActionListener(frame, fileAction);
            fileItem.addActionListener(fileAction);
            attachmentType.add(fileItem);

            JMenuItem linkItem = new JMenuItem();
            TextUtil.populate(linkItem, "LINK");
            ActionListener linkAction = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        attachRaceSeriesLink(type);
                    }
                    catch (EntityNotFoundException ex)
                    {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, ex.getMessage());
                    }
                }
            };
            linkAction = createActionListener(frame, linkAction);
            linkItem.addActionListener(linkAction);
            attachmentType.add(linkItem);
        }
        return attachRaceSeriesMenu;
    }

    private void attachRaceSeriesLink(Type type) throws EntityNotFoundException
    {
        DataObject raceSeriesOrFleet = chooseRaceSeriesOrFleet();
        if (raceSeriesOrFleet != null)
        {
            Attachment attachment = new Attachment(raceSeriesOrFleet);
            attachment.set(Attachment.TYPE, type.ordinal());
            DataObjectDialog<Attachment> dod = new DataObjectDialog<Attachment>(frame, attachment.getModel().hide(Attachment.TYPE), attachment);
            if (dod.edit())
            {
                Link link = (Link) attachment.get(Attachment.URL);
                try
                {
                    URL url = new URL(link.getValue());
                    dss.put(attachment);
                }
                catch (MalformedURLException ex)
                {
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        }
    }

    private void attachRaceSeriesFile(Type type) throws IOException, EntityNotFoundException
    {
        DataObject raceSeriesOrFleet = chooseRaceSeriesOrFleet();
        if (raceSeriesOrFleet != null)
        {
            File file = openFile(RACEATTACHDIR, null, null);
            if (file != null)
            {
                String title = JOptionPane.showInputDialog(frame, TextUtil.getText("TITLE"), TextUtil.getText(type.name()));
                if (title != null)
                {
                    dss.upload(raceSeriesOrFleet, type, title, file);
                }
            }
        }
    }

    private void removeRaceSeriesAttachment() throws EntityNotFoundException, IOException
    {
        DataObject raceSeriesOrFleet = chooseRaceSeriesOrFleet();
        if (raceSeriesOrFleet != null)
        {
            List<Attachment> attachments = chooseAttachments(raceSeriesOrFleet);
            if (attachments != null)
            {
                dss.removeAttachments(attachments);
            }
        }
    }

    private JMenuItem menuItemRemoveRaceSeriesAttachment()
    {
        // download series
        JMenuItem removeAttachmentItem = new JMenuItem();
        TextUtil.populate(removeAttachmentItem, "REMOVE ATTACHMENT");
        ActionListener removeAttachmentAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    removeRaceSeriesAttachment();
                }
                catch (IOException | EntityNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        removeAttachmentAction = createActionListener(frame, removeAttachmentAction);
        removeAttachmentItem.addActionListener(removeAttachmentAction);
        return removeAttachmentItem;
    }

    private JMenuItem menuItemUploadRanking()
    {
        // upload ranking
        JMenuItem uploadRankingItem = new JMenuItem();
        TextUtil.populate(uploadRankingItem, "UPLOAD RANKING");
        ActionListener uploadRankingAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                File file = openFile(SAILWAVEDIR, ".blw", "SailWave");
                if (file != null)
                {
                    try
                    {
                        uploadRanking(file);
                    }
                    catch (IOException ex)
                    {
                        JOptionPane.showMessageDialog(frame, ex.getMessage());
                    }
                }
            }
        };
        uploadRankingAction = createActionListener(frame, uploadRankingAction);
        uploadRankingItem.addActionListener(uploadRankingAction);
        return uploadRankingItem;
    }

    private JMenuItem menuItemAddReferencePayments()
    {
        JMenuItem addPaymentsItem = new JMenuItem();
        TextUtil.populate(addPaymentsItem, "ADD REFERENCE PAYMENTS");
        ActionListener addPaymentsAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    addReferencePayments();
                }
                catch (EntityNotFoundException ex)
                {
                    throw new IllegalArgumentException(ex);
                }
            }
        };
        addPaymentsAction = createActionListener(frame, addPaymentsAction);
        addPaymentsItem.addActionListener(addPaymentsAction);
        return addPaymentsItem;
    }

    private void addReferencePayments() throws EntityNotFoundException
    {
        while (true)
        {
            String reference = "";
            CreditorReference cr = null;
            while (true)
            {
                reference = JOptionPane.showInputDialog(frame, TextUtil.getText("REFERENCE"), reference);
                if (reference == null)
                {
                    return;
                }
                try
                {
                    cr = new CreditorReference(reference, true);
                    break;
                }
                catch (IllegalArgumentException ex)
                {
                    JOptionPane.showMessageDialog(frame, ex);
                }
            }
            int rn = Integer.parseInt(reference.substring(0, reference.length() - 1));
            RaceEntry raceEntry = dss.raceEntryForReference(rn);
            Double fee = (Double) raceEntry.get(RaceEntry.FEE);
            String fieldsAsHtmlTable = raceEntry.getFieldsAsHtmlTable(
                    RaceEntry.FLEET,
                    RaceEntry.HELMNAME,
                    RaceEntry.HELMADDRESS,
                    RaceEntry.CLUB,
                    RaceEntry.HELMEMAIL,
                    RaceEntry.HELMPHONE,
                    RaceEntry.FEE,
                    RaceEntry.PAID
                    );
            String paidStr = JOptionPane.showInputDialog(frame, "<html>" + fieldsAsHtmlTable + "</html>", fee);
            if (paidStr != null)
            {
                try
                {
                    fee = Double.parseDouble(paidStr);
                }
                catch (NumberFormatException ex)
                {
                    continue;
                }
                raceEntry.set(RaceEntry.PAID, fee);
                dss.put(raceEntry);
            }
        }
    }

    private JMenuItem menuItemAddOtherPayments()
    {
        JMenuItem addPaymentsItem = new JMenuItem();
        TextUtil.populate(addPaymentsItem, "ADD OTHER PAYMENTS");
        ActionListener addPaymentsAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    addOtherPayments();
                }
                catch (EntityNotFoundException ex)
                {
                    throw new IllegalArgumentException(ex);
                }
            }
        };
        addPaymentsAction = createActionListener(frame, addPaymentsAction);
        addPaymentsItem.addActionListener(addPaymentsAction);
        return addPaymentsItem;
    }

    private void addOtherPayments() throws EntityNotFoundException
    {
        DataObjectModel model = RaceEntry.MODEL.view(
                RaceEntry.NAT,
                RaceEntry.SAILNO,
                RaceEntry.HELMNAME,
                RaceEntry.CLUB,
                RaceEntry.FLEET,
                RaceEntry.FEE);
        List<RaceEntry> unpaidRaceEntries = dss.getUnpaidRaceEntries();
        while (true)
        {
            String totalStr = JOptionPane.showInputDialog(frame, TextUtil.getText("TOTAL"));
            if (totalStr == null)
            {
                return;
            }
            double total = Double.parseDouble(totalStr);
            Collections.sort(unpaidRaceEntries, new FeeComparator(total));

            DataObjectChooser<RaceEntry> ec = new DataObjectChooser<RaceEntry>(
                    model,
                    unpaidRaceEntries,
                    "",
                    "CHOOSE");
            ec.setSelectAlways(true);
            List<RaceEntry> entries = ec.choose();
            if (entries == null)
            {
                return;
            }
            for (RaceEntry re : entries)
            {
                re.set(RaceEntry.PAID, total);
                dss.put(re);
                unpaidRaceEntries.remove(re);
            }
        }
    }

    private String convertString(Object ob)
    {
        if (ob == null)
        {
            return null;
        }
        if (ob instanceof String)
        {
            return (String)ob;
        }
        if (ob instanceof Text)
        {
            Text text = (Text) ob;
            return text.getValue();
        }
        throw new IllegalArgumentException(ob+" not String or Text");
    }

    private class FeeComparator implements Comparator<RaceEntry>
    {

        private double target;

        public FeeComparator(double target)
        {
            this.target = target;
        }

        @Override
        public int compare(RaceEntry o1, RaceEntry o2)
        {
            Double fee1 = (Double) o1.get(RaceEntry.FEE);
            if (fee1 == null)
            {
                return -1;
            }
            Double fee2 = (Double) o2.get(RaceEntry.FEE);
            if (fee2 == null)
            {
                return 1;
            }
            Double d1 = new Double(Math.abs(target - fee1));
            Double d2 = new Double(Math.abs(target - fee2));
            return d1.compareTo(d2);
        }
    }

    private File openFile(String lastDir, String suffix, String description)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        if (suffix != null && description != null)
        {
            FileFilter ff = new SuffixFileFilter(suffix, description);
            fc.setFileFilter(ff);
        }
        File currentDirectory = LastInput.getDirectory(lastDir);
        if (currentDirectory != null)
        {
            fc.setCurrentDirectory(currentDirectory);
        }
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
        {
            File file = fc.getSelectedFile();
            LastInput.set(lastDir, file);
            return file;
        }
        return null;
    }

    private File saveFile(String lastDir, String nameCandidate, String suffix, String description)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        FileFilter ff = new SuffixFileFilter(suffix, description);
        fc.setFileFilter(ff);
        fc.setSelectedFile(new File(nameCandidate));
        File currentDirectory = LastInput.getDirectory(lastDir);
        if (currentDirectory != null)
        {
            fc.setCurrentDirectory(currentDirectory);
        }
        if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
        {
            File file = fc.getSelectedFile();
            if (file != null)
            {
                if (!file.getName().endsWith(suffix))
                {
                    file = new File(file.getPath() + suffix);
                }
                LastInput.set(lastDir, file);
            }
            return file;
        }
        return null;
    }

    private JMenuItem menuItemUploadRaceSeries()
    {
        // upload series
        JMenuItem uploadRaceSeriesItem = new JMenuItem();
        TextUtil.populate(uploadRaceSeriesItem, "UPLOAD RACE");
        ActionListener uploadRaceSeriesAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                File file = openFile(SAILWAVEDIR, ".blw", "SailWave");
                if (file != null)
                {
                    try
                    {
                        uploadSeries(file);
                    }
                    catch (IOException ex)
                    {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, ex.getMessage());
                    }
                }
            }
        };
        uploadRaceSeriesAction = createActionListener(frame, uploadRaceSeriesAction);
        uploadRaceSeriesItem.addActionListener(uploadRaceSeriesAction);
        return uploadRaceSeriesItem;
    }

    private JMenuItem menuItemEditRaceSeries()
    {
        // upload series
        JMenuItem editRaceSeriesItem = new JMenuItem(TextUtil.getText("EDIT RACE"));
        TextUtil.populate(editRaceSeriesItem, "EDIT RACE");
        ActionListener editRaceSeriesAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    editSeries();
                }
                catch (IOException | EntityNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        editRaceSeriesAction = createActionListener(frame, editRaceSeriesAction);
        editRaceSeriesItem.addActionListener(editRaceSeriesAction);
        return editRaceSeriesItem;
    }

    private JMenuItem menuItemEditRanking()
    {
        // upload series
        JMenuItem editRankingItem = new JMenuItem(TextUtil.getText("EDIT RANKING"));
        TextUtil.populate(editRankingItem, "EDIT RANKING");
        ActionListener editRankingAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    editRanking();
                }
                catch (IOException | EntityNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        editRankingAction = createActionListener(frame, editRankingAction);
        editRankingItem.addActionListener(editRankingAction);
        return editRankingItem;
    }

    private JMenuItem menuItemRemoveRaceSeries()
    {
        // upload series
        JMenuItem removeRaceSeriesItem = new JMenuItem(TextUtil.getText("REMOVE RACE"));
        TextUtil.populate(removeRaceSeriesItem, "REMOVE RACE");
        ActionListener removeRaceSeriesAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    removeSeries();
                }
                catch (IOException | EntityNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        removeRaceSeriesAction = createActionListener(frame, removeRaceSeriesAction);
        removeRaceSeriesItem.addActionListener(removeRaceSeriesAction);
        return removeRaceSeriesItem;
    }

    private JMenuItem menuItemRemoveRanking()
    {
        // upload series
        JMenuItem removeRankingItem = new JMenuItem(TextUtil.getText("REMOVE RANKING"));
        TextUtil.populate(removeRankingItem, "REMOVE RANKING");
        ActionListener removeRankingAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    removeSeries();
                }
                catch (IOException | EntityNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        removeRankingAction = createActionListener(frame, removeRankingAction);
        removeRankingItem.addActionListener(removeRankingAction);
        return removeRankingItem;
    }

    private JMenuItem menuItemSync()
    {
        JMenuItem syncItem = new JMenuItem();
        TextUtil.populate(syncItem, "SYNCHRONIZE");
        ActionListener syncAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                SwingWorker task = new SwingWorker<Void, Void>()
                {

                    @Override
                    protected Void doInBackground() throws Exception
                    {
                        Progress progress = new UIProgress(frame, TextUtil.getText("SYNCHRONIZE"));
                        progress.setNote("");
                        try
                        {
                            dss.synchronize(progress);
                        }
                        catch (Throwable ex)
                        {
                            progress.close();
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(frame, ex.getMessage());
                        }
                        return null;
                    }
                };
                task.execute();
            }
        };
        syncAction = createActionListener(frame, syncAction);
        syncItem.addActionListener(syncAction);
        return syncItem;
    }

    private JMenuItem menuItemTextMaintenence()
    {
        JMenuItem textMaintenenceItem = new JMenuItem();
        TextUtil.populate(textMaintenenceItem, "TEXT MAINTENANCE");
        ActionListener maintenanceAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                Messages messages = dss.getMessages();
                DataObjectDialog<Messages> dod = new DataObjectDialog<Messages>(frame, Messages.MODEL, messages);
                dod.setPreferredSize(new Dimension(800, 600));
                if (dod.edit())
                {
                    dss.put(messages);
                }
            }
        };
        maintenanceAction = createActionListener(frame, maintenanceAction);
        textMaintenenceItem.addActionListener(maintenanceAction);
        return textMaintenenceItem;
    }

    private JMenuItem menuItemTextUpload()
    {
        JMenuItem textUploadItem = new JMenuItem();
        TextUtil.populate(textUploadItem, "TEXT UPLOAD");
        ActionListener textUploadAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    MessagesBackup mb = new MessagesBackup(frame);
                    mb.load();
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        textUploadAction = createActionListener(frame, textUploadAction);
        textUploadItem.addActionListener(textUploadAction);
        return textUploadItem;
    }

    private JMenuItem menuItemTextDownload()
    {
        JMenuItem textDownloadItem = new JMenuItem();
        TextUtil.populate(textDownloadItem, "TEXT DOWNLOAD");
        ActionListener textDownloadAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    MessagesBackup mb = new MessagesBackup(frame);
                    mb.store();
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        textDownloadAction = createActionListener(frame, textDownloadAction);
        textDownloadItem.addActionListener(textDownloadAction);
        return textDownloadItem;
    }

    private JMenu menuItemAttach()
    {
        JMenu attachMenu = new JMenu(TextUtil.getText("ADD ATTACHMENT"));
        TextUtil.populate(attachMenu, "ADD ATTACHMENT");
        JMenuItem fileItem = new JMenuItem();
        TextUtil.populate(fileItem, "FILE");
        ActionListener fileAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    attachFile();
                }
                catch (EntityNotFoundException | IOException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        fileAction = createActionListener(frame, fileAction);
        fileItem.addActionListener(fileAction);
        attachMenu.add(fileItem);

        JMenuItem linkItem = new JMenuItem();
        TextUtil.populate(linkItem, "LINK");
        ActionListener linkAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    attachLink();
                }
                catch (EntityNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        linkAction = createActionListener(frame, linkAction);
        linkItem.addActionListener(linkAction);
        attachMenu.add(linkItem);
        return attachMenu;
    }

    private void attachLink() throws EntityNotFoundException
    {
        Title root = chooseTitle();
        Attachment attachment = new Attachment(root);
        attachment.set(Attachment.TYPE, Attachment.Type.OTHER);
        DataObjectDialog<Attachment> dod = new DataObjectDialog<Attachment>(frame, attachment.getModel().hide(Attachment.TYPE), attachment);
        if (dod.edit())
        {
            Link link = (Link) attachment.get(Attachment.URL);
            try
            {
                URL url = new URL(link.getValue());
                dss.put(attachment);
            }
            catch (MalformedURLException ex)
            {
                JOptionPane.showMessageDialog(frame, ex.getMessage());
            }
        }
    }

    private void attachFile() throws IOException, EntityNotFoundException
    {
        Title root = chooseTitle();
        File file = openFile(ATTACHDIR, null, null);
        String title = JOptionPane.showInputDialog(frame, TextUtil.getText("TITLE"));
        if (title != null)
        {
            dss.upload(root, Attachment.Type.OTHER, title, file);
        }
    }

    private void removeAttachment() throws IOException, EntityNotFoundException
    {
        Title root = chooseTitle();
        List<Attachment> attachments = chooseAttachments(root);
        if (attachments != null)
        {
            dss.removeAttachments(attachments);
        }
    }

    private JMenuItem menuItemRemoveAttachment()
    {
        // download series
        JMenuItem removeAttachmentItem = new JMenuItem(TextUtil.getText("REMOVE ATTACHMENT"));
        TextUtil.populate(removeAttachmentItem, "REMOVE ATTACHMENT");
        ActionListener removeAttachmentAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    removeAttachment();
                }
                catch (IOException | EntityNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        };
        removeAttachmentAction = createActionListener(frame, removeAttachmentAction);
        removeAttachmentItem.addActionListener(removeAttachmentAction);
        return removeAttachmentItem;
    }

    private Title chooseTitle() throws EntityNotFoundException
    {
        List<Title> titles = null;
        titles = dss.getTitles();
        // choose one of the shifts
        DataObjectChooser<Title> ec = new DataObjectChooser<Title>(
                Title.MODEL,
                titles,
                TextUtil.getText("TITLE"),
                "CHOOSE");
        ec.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        List<Title> selectedTitle = ec.choose();
        if (selectedTitle != null && selectedTitle.size() == 1)
        {
            return selectedTitle.get(0);
        }
        else
        {
            return null;
        }
    }

    /**
     * Return a text from Messages kind
     *
     * @param name
     * @return
     */
    private String getMessage(String name)
    {
        if (messages == null)
        {
            messages = dss.getMessages();
        }
        Object obj = messages.get(name);
        if (obj != null)
        {
            if (obj instanceof Text)
            {
                Text text = (Text) obj;
                return text.getValue();
            }
            else
            {
                return obj.toString();
            }
        }
        return null;
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        confirmSaveReservations();
        if (workBench != null)
        {
            workBench.close();
        }
        System.exit(0);
    }

    private void confirmSaveReservations()
    {
        if (reservationList != null)
        {
            String msg = TextUtil.getText("CONFIRM MODIFICATIONS");
            if (JOptionPane.showConfirmDialog(frame, msg, "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            {
                saveReservations();
            }
        }
    }

    private void saveReservations()
    {
        long order = 1;
        for (Reservation selectedReservation : selectedReservations)
        {
            selectedReservation.set(Reservation.ORDER, order++);
        }
        for (Reservation unSelectedReservation : unSelectedReservations)
        {
            unSelectedReservation.set(Reservation.ORDER, 0);
        }
        List<Reservation> updateList = new ArrayList<>();
        updateList.addAll(unSelectedReservations);
        reservationList.removeAll(unSelectedReservations);
        updateList.addAll(selectedReservations);
        dss.put(updateList);
        reservationList.removeAll(selectedReservations);
        dss.delete(reservationList);
        menuReservation.setEnabled(true);
        frame.setContentPane(safeContainer);
        frame.setTitle(safeTitle);
        frame.setVisible(true);
        reservationList = null;
        selectedReservations = null;
        unSelectedReservations = null;
    }

    private void setAsInspected() throws SQLException, ClassNotFoundException
    {
        for (Reservation selectedReservation : selectedReservations)
        {
            selectedReservation.set(Reservation.INSPECTED, true);
        }
        for (Reservation unSelectedReservation : unSelectedReservations)
        {
            unSelectedReservation.set(Reservation.INSPECTED, false);
        }
        InspectionHandler ih = new InspectionHandler(serverProperties.getProperties());
        if (!unSelectedReservations.isEmpty())
        {
            ih.updateInspection(unSelectedReservations);
            dss.put(unSelectedReservations);
        }
        reservationList.removeAll(unSelectedReservations);
        if (!selectedReservations.isEmpty())
        {
            ih.updateInspection(selectedReservations);
            dss.put(selectedReservations);
        }
        reservationList.removeAll(selectedReservations);
        dss.delete(reservationList);
        menuReservation.setEnabled(true);
        frame.setContentPane(safeContainer);
        frame.setVisible(true);
        reservationList = null;
        selectedReservations = null;
        unSelectedReservations = null;
    }

    private void cancel()
    {
        menuReservation.setEnabled(true);
        frame.setContentPane(safeContainer);
        frame.setTitle(safeTitle);
        frame.setVisible(true);
        reservationList = null;
        selectedReservations = null;
        unSelectedReservations = null;
    }

    public static ActionListener createActionListener(final Component component, final ActionListener actionListener)
    {
        ActionListener al = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    component.setCursor(busyCursor);
                    component.repaint();
                    actionListener.actionPerformed(e);
                }
                finally
                {
                    component.setCursor(defaultCursor);
                }
            }
        };
        return al;
    }

    public JFrame getFrame()
    {
        return frame;
    }

    public JMenuBar getMenuBar()
    {
        return menuBar;
    }

    public JPanel getPanel()
    {
        return panel;
    }

    private void editReservations(final EventType eventType)
    {
        final Event event = chooseEvent(eventType, "CHOOSE");
        if (event != null)
        {
            final String eventTitle = TextUtil.getText(event.getEventType().name()) + " " + event.get(Event.EventDate);
            safeTitle = frame.getTitle();
            frame.setTitle(eventTitle);
            reservationList = dss.getReservations(event);
            selectedReservations = new ArrayList<Reservation>();
            unSelectedReservations = new ArrayList<Reservation>();
            if (Event.isInspection(eventType))
            {
                for (Reservation reservation : reservationList)
                {
                    Boolean inspected = (Boolean) reservation.get(Reservation.INSPECTED);
                    if (inspected != null && inspected)
                    {
                        selectedReservations.add(reservation);
                    }
                    else
                    {
                        unSelectedReservations.add(reservation);
                    }
                }
            }
            else
            {
                for (Reservation reservation : reservationList)
                {
                    Long order = (Long) reservation.get(Reservation.ORDER);
                    if (order != null && order != 0)
                    {
                        selectedReservations.add(reservation);
                    }
                    else
                    {
                        unSelectedReservations.add(reservation);
                    }
                }
            }
            DataObjectModel baseModel = Reservation.getModel(eventType);
            DataObjectModel unorderedModel = null;
            DataObjectModel orderedModel = null;
            switch (eventType)
            {
                case LAUNCH:
                case LIFT:
                case HULL_INSPECTION:
                    unorderedModel = baseModel.hide(Reservation.BOAT, Reservation.INSPECTED, Reservation.CREATOR);
                    orderedModel = baseModel.hide(Reservation.BOAT, Reservation.INSPECTED, Reservation.CREATOR);
                    break;
                case INSPECTION:
                    unorderedModel = baseModel.hide(
                            Reservation.BOAT,
                            Reservation.INSPECTED,
                            Reservation.CREATOR,
                            Reservation.EMAIL,
                            Reservation.MOBILEPHONE,
                            Reservation.DOCK,
                            Reservation.DOCKNUMBER,
                            Reservation.INSPECTION_GASS,
                            Reservation.INSPECTOR);
                    orderedModel = baseModel.hide(
                            Reservation.BOAT,
                            Reservation.INSPECTED,
                            Reservation.CREATOR,
                            Reservation.EMAIL,
                            Reservation.MOBILEPHONE,
                            Reservation.DOCK,
                            Reservation.DOCKNUMBER,
                            Reservation.NOTES);
                    break;
            }

            final DataObjectListTableModel<Reservation> unorderedTableModel =
                    new DataObjectListTableModel<Reservation>(unorderedModel, unSelectedReservations);
            final JTable unorderedtable = new FitTable(unorderedTableModel);
            TableSelectionHandler tsh1 = new TableSelectionHandler(unorderedtable);
            unorderedtable.setDefaultRenderer(String.class, new StringTableCellRenderer());
            unorderedtable.setDefaultRenderer(Text.class, new TextTableCellRenderer());

            unorderedtable.addKeyListener(unorderedTableModel.getRemover(dss));
            unorderedtable.setDragEnabled(true);
            unorderedtable.setDropMode(DropMode.INSERT_ROWS);
            TransferHandler unorderedTransferHandler = new DataObjectTransferHandler<Reservation>(unorderedTableModel);
            unorderedtable.setTransferHandler(unorderedTransferHandler);

            final DataObjectListTableModel<Reservation> orderedTableModel =
                    new DataObjectListTableModel<Reservation>(orderedModel, selectedReservations);
            orderedTableModel.setEditable(Reservation.INSPECTION_CLASS, Reservation.INSPECTION_GASS, Reservation.BASICINSPECTION, Reservation.INSPECTOR);
            final JTable orderedtable = new FitTable(orderedTableModel);
            TableSelectionHandler tsh2 = new TableSelectionHandler(orderedtable);
            orderedtable.setDefaultRenderer(String.class, new StringTableCellRenderer());
            orderedtable.setDefaultRenderer(Text.class, new TextTableCellRenderer());

            orderedtable.addKeyListener(orderedTableModel.getRemover(dss));
            orderedtable.setDragEnabled(true);
            orderedtable.setDropMode(DropMode.INSERT_ROWS);
            TransferHandler orderedTransferHandler = new DataObjectTransferHandler<Reservation>(orderedTableModel);
            orderedtable.setTransferHandler(orderedTransferHandler);
            if (Event.isInspection(eventType))
            {
                unorderedtable.setAutoCreateRowSorter(true);
                orderedtable.setAutoCreateRowSorter(true);
            }
            leftPane = new JScrollPane();
            leftPane.setViewport(new InfoViewport(TextUtil.getText(eventType.name() + "-leftPane")));
            leftPane.setViewportView(unorderedtable);
            leftPane.setTransferHandler(unorderedTransferHandler);

            rightPane = new JScrollPane();
            rightPane.setViewport(new InfoViewport(TextUtil.getText(eventType.name() + "-rightPane")));
            rightPane.setViewportView(orderedtable);
            rightPane.setTransferHandler(orderedTransferHandler);

            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
            splitPane.setDividerLocation(0.5);
            menuReservation.setEnabled(false);
            safeContainer = frame.getContentPane();
            JPanel contentPane = new JPanel(new BorderLayout());
            contentPane.add(splitPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout());
            contentPane.add(buttonPanel, BorderLayout.SOUTH);

            JButton saveButton = new JButton();
            TextUtil.populate(saveButton, "SAVE");
            saveButton.setEnabled(!Event.isInspection(eventType));
            ActionListener saveAction = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    saveReservations();
                }
            };
            saveButton.addActionListener(saveAction);
            buttonPanel.add(saveButton);

            switch (eventType)
            {
                case INSPECTION:
                {
                    if (privileged)
                    {
                        JButton inspectButton = new JButton();
                        TextUtil.populate(inspectButton, "SET INSPECTED");
                        ActionListener inspectAction = new ActionListener()
                        {

                            @Override
                            public void actionPerformed(ActionEvent e)
                            {
                                TableCellEditor cellEditor = orderedtable.getCellEditor();
                                if (cellEditor != null)
                                {
                                    cellEditor.stopCellEditing();
                                }
                                try
                                {
                                    setAsInspected();
                                }
                                catch (SQLException | ClassNotFoundException ex)
                                {
                                    ex.printStackTrace();
                                    JOptionPane.showMessageDialog(null, ex.getMessage());
                                }
                            }
                        };
                        inspectButton.addActionListener(inspectAction);
                        buttonPanel.add(inspectButton);
                    }
                    
                    JButton addBoat = new JButton();
                    TextUtil.populate(addBoat, "ADD BOAT");
                    ActionListener addBoatAction = new ActionListener()
                    {

                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            Reservation reservation = reservate(eventType, event);
                            if (reservation != null)
                            {
                                reservationList.add(reservation);
                                unSelectedReservations.add(reservation);
                                unorderedTableModel.fireTableDataChanged();
                            }
                        }
                    };
                    addBoat.addActionListener(addBoatAction);
                    buttonPanel.add(addBoat);

                    JButton printTypeButton = new JButton();
                    TextUtil.populate(printTypeButton, "PRINT");
                    ActionListener printTypeAction = new ActionListener()
                    {

                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                printNameBoatTypeOrder(eventTitle);
                            }
                            catch (PrinterException ex)
                            {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(null, ex.getMessage());
                            }
                        }
                    };
                    printTypeButton.addActionListener(printTypeAction);
                    buttonPanel.add(printTypeButton);

                    JButton printDockButton = new JButton();
                    TextUtil.populate(printDockButton, "PRINT DOCK ORDER");
                    ActionListener printDockAction = new ActionListener()
                    {

                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                printDockOrder(eventTitle);
                            }
                            catch (PrinterException ex)
                            {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(null, ex.getMessage());
                            }
                        }
                    };
                    printDockButton.addActionListener(printDockAction);
                    buttonPanel.add(printDockButton);

                }
                break;
                case HULL_INSPECTION:
                {
                    JButton print = new JButton();
                    TextUtil.populate(print, "PRINT");
                    ActionListener printAction = new ActionListener()
                    {

                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                printAlphaOrder(eventTitle);
                            }
                            catch (PrinterException ex)
                            {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(null, ex.getMessage());
                            }
                        }
                    };
                    print.addActionListener(printAction);
                    buttonPanel.add(print);

                }
                break;
                case LAUNCH:
                case LIFT:
                {
                    JButton addBoat = new JButton();
                    TextUtil.populate(addBoat, "ADD BOAT");
                    ActionListener addBoatAction = new ActionListener()
                    {

                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            Reservation reservation = reservate(eventType, event);
                            if (reservation != null)
                            {
                                reservationList.add(reservation);
                                unSelectedReservations.add(reservation);
                                unorderedTableModel.fireTableDataChanged();
                            }
                        }
                    };
                    addBoat.addActionListener(addBoatAction);
                    buttonPanel.add(addBoat);

                    JButton printBrief = new JButton();
                    TextUtil.populate(printBrief, "BRIEF PRINT");
                    ActionListener printBriefAction = new ActionListener()
                    {

                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                printOrderBrief(eventTitle);
                            }
                            catch (PrinterException ex)
                            {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(null, ex.getMessage());
                            }
                        }
                    };
                    printBrief.addActionListener(printBriefAction);
                    buttonPanel.add(printBrief);

                    JButton print = new JButton();
                    TextUtil.populate(print, "PRINT");
                    ActionListener printAction = new ActionListener()
                    {

                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                if (eventType == EventType.LAUNCH)
                                {
                                    printLaunchOrder(eventTitle);
                                }
                                else
                                {
                                    printLiftOrder(eventTitle);
                                }
                            }
                            catch (PrinterException ex)
                            {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(null, ex.getMessage());
                            }
                        }
                    };
                    print.addActionListener(printAction);
                    buttonPanel.add(print);
                }
                break;
            }

            JButton cancelButton = new JButton();
            TextUtil.populate(cancelButton, "CANCEL");
            ActionListener cancelAction = new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    cancel();
                }
            };
            cancelButton.addActionListener(cancelAction);
            buttonPanel.add(cancelButton);

            frame.setContentPane(contentPane);
            frame.pack();
            frame.setVisible(true);
            KeyListener keyListener = new KeyAdapter()
            {

                @Override
                public void keyPressed(KeyEvent e)
                {
                    if (e.getKeyCode() == 10)
                    {
                        int selectedRow = unorderedtable.getSelectedRow();
                        if (selectedRow != -1)
                        {
                            RowSorter<? extends TableModel> rowSorter = unorderedtable.getRowSorter();
                            if (rowSorter != null)
                            {
                                selectedRow = rowSorter.convertRowIndexToModel(selectedRow);
                            }
                            Reservation reservation = unorderedTableModel.getObject(selectedRow);
                            orderedTableModel.add(reservation);
                            unorderedTableModel.remove(reservation);
                            e.consume();
                            unorderedtable.changeSelection(selectedRow, 0, false, false);
                        }
                    }
                }
            };
            unorderedtable.addKeyListener(keyListener);
            unorderedtable.requestFocusInWindow();
        }
    }

    private void printNameBoatTypeOrder(String eventTitle) throws PrinterException
    {
        DataObjectModel model = Reservation.getModel(EventType.INSPECTION);
        model = model.view(
                Reservation.ORDER,
                Reservation.LASTNAME,
                Reservation.FIRSTNAME,
                Reservation.BOATNAME,
                Reservation.BOATTYPE,
                Reservation.DOCK,
                Reservation.DOCKNUMBER,
                Reservation.BASICINSPECTION);
        List<Reservation> list = new ArrayList<Reservation>();
        for (Reservation r : reservationList)
        {
            list.add(r.clone());
        }
        Collections.sort(list, new DataObjectComparator(Reservation.LASTNAME, Reservation.FIRSTNAME, Reservation.BOATNAME));
        int order = 1;
        for (Reservation r : list)
        {
            r.set(Reservation.ORDER, order);
            order++;
        }
        DataObjectListDialog<Reservation> dold = new DataObjectListDialog<Reservation>(frame, eventTitle, "OK", model, list);
        dold.print();
    }

    private void printDockOrder(String eventTitle) throws PrinterException
    {
        DataObjectModel model = Reservation.getModel(EventType.INSPECTION);
        model = model.view(
                Reservation.ORDER,
                Reservation.LASTNAME,
                Reservation.FIRSTNAME,
                Reservation.BOATNAME,
                Reservation.DOCK,
                Reservation.DOCKNUMBER,
                Reservation.BASICINSPECTION);
        List<Reservation> list = new ArrayList<Reservation>();
        for (Reservation r : reservationList)
        {
            list.add(r.clone());
        }
        Collections.sort(list, new DataObjectComparator(Reservation.DOCK, Reservation.DOCKNUMBER));
        int order = 1;
        for (Reservation r : list)
        {
            r.set(Reservation.ORDER, order);
            order++;
        }
        DataObjectListDialog<Reservation> dold = new DataObjectListDialog<Reservation>(frame, eventTitle, "OK", model, list);
        dold.print();
    }

    private void printAlphaOrder(String eventTitle) throws PrinterException
    {
        DataObjectModel model = Reservation.getModel(EventType.HULL_INSPECTION);
        model = model.view(
                Reservation.ORDER,
                Reservation.LASTNAME,
                Reservation.FIRSTNAME,
                Reservation.BOATTYPE,
                Reservation.BOATNAME,
                Reservation.DOCKYARDPLACE);
        List<Reservation> list = new ArrayList<Reservation>();
        for (Reservation r : reservationList)
        {
            list.add(r.clone());
        }
        Collections.sort(list, new DataObjectComparator(Reservation.LASTNAME));
        int order = 1;
        for (Reservation r : list)
        {
            r.set(Reservation.ORDER, order);
            order++;
        }
        DataObjectListDialog<Reservation> dold = new DataObjectListDialog<Reservation>(frame, eventTitle, "OK", model, list);
        dold.print();
    }

    private void printOrderBrief(String eventTitle) throws PrinterException
    {
        DataObjectModel model = Reservation.getModel(EventType.LAUNCH);
        model = model.view(
                Reservation.ORDER,
                Reservation.LASTNAME,
                Reservation.FIRSTNAME,
                Reservation.BOATNAME);
        List<Reservation> list = new ArrayList<Reservation>();
        for (Reservation r : reservationList)
        {
            list.add(r.clone());
        }
        Collections.sort(list, new DataObjectComparator(Reservation.ORDER));
        int order = 1;
        for (Reservation r : list)
        {
            r.set(Reservation.ORDER, order);
            order++;
        }
        DataObjectListDialog<Reservation> dold = new DataObjectListDialog<Reservation>(frame, eventTitle, "OK", model, list);
        dold.print();
    }

    private void printLaunchOrder(String eventTitle) throws PrinterException
    {
        DataObjectModel model = Reservation.getModel(EventType.LAUNCH);
        model = model.view(
                Reservation.ORDER,
                Reservation.LASTNAME,
                Reservation.FIRSTNAME,
                Reservation.BOATNAME,
                Reservation.DOCKYARDPLACE,
                Reservation.MOBILEPHONE,
                Reservation.WEIGHT);
        List<Reservation> list = new ArrayList<Reservation>();
        for (Reservation r : reservationList)
        {
            list.add(r.clone());
        }
        Collections.sort(list, new DataObjectComparator(Reservation.ORDER));
        int order = 1;
        for (Reservation r : list)
        {
            r.set(Reservation.ORDER, order);
            order++;
        }
        DataObjectListDialog<Reservation> dold = new DataObjectListDialog<Reservation>(frame, eventTitle, "OK", model, list);
        dold.print();
    }

    private void printLiftOrder(String eventTitle) throws PrinterException
    {
        DataObjectModel model = Reservation.getModel(EventType.LIFT);
        model = model.view(
                Reservation.ORDER,
                Reservation.LASTNAME,
                Reservation.FIRSTNAME,
                Reservation.BOATNAME,
                Reservation.DOCKYARDPLACE,
                Reservation.MOBILEPHONE,
                Reservation.WEIGHT,
                Reservation.LENGTH);
        List<Reservation> list = new ArrayList<Reservation>();
        for (Reservation r : reservationList)
        {
            list.add(r.clone());
        }
        Collections.sort(list, new DataObjectComparator(Reservation.ORDER));
        int order = 1;
        for (Reservation r : list)
        {
            r.set(Reservation.ORDER, order);
            order++;
        }
        DataObjectListDialog<Reservation> dold = new DataObjectListDialog<Reservation>(frame, eventTitle, "OK", model, list);
        dold.print();
    }

    private Event chooseEvent(EventType eventType, String action)
    {
        List<Event> eventList = chooseEvents(eventType, action, true);
        if (eventList != null)
        {
            return eventList.get(0);
        }
        else
        {
            return null;
        }
    }

    private List<Event> chooseEvents(EventType eventType, String action)
    {
        return chooseEvents(eventType, action, false);
    }

    private List<Event> chooseEvents(EventType eventType, String action, boolean singleSelection)
    {
        List<Event> eventList = dss.getEvents(eventType);
        DataObjectChooser<Event> ec = new DataObjectChooser<Event>(
                Event.MODEL,
                eventList,
                TextUtil.getText(eventType.name()),
                action);
        ec.setSelectAlways(true);
        if (singleSelection)
        {
            ec.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        return ec.choose();
    }

    private Reservation reservate(EventType eventType, Event event)
    {
        // search a member
        String[] columns = Member.MODEL.getProperties();
        String lastName = JOptionPane.showInputDialog(panel, TextUtil.getText(Member.SUKUNIMI) + "?");
        if (lastName == null)
        {
            return null;
        }
        lastName = Utils.convertName(lastName);
        List<AnyDataObject> memberList = null;
        memberList = dss.retrieve(Member.KIND, Member.SUKUNIMI, lastName, columns);
        DataObjectChooser<AnyDataObject> ec = new DataObjectChooser<AnyDataObject>(
                Member.MODEL,
                memberList,
                TextUtil.getText(eventType.name()),
                "CHOOSE");
        ec.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        List<AnyDataObject> selected = ec.choose();
        if (selected != null && selected.size() >= 1)
        {
            try
            {
                return reservate(eventType, selected.get(0), event);
            }
            catch (ParseException ex)
            {
                Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
        {
            JOptionPane.showMessageDialog(
                    frame,
                    TextUtil.getText("NO SELECTION"),
                    TextUtil.getText("MESSAGE"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        return null;
    }

    private Reservation reservate(EventType eventType, AnyDataObject user, Event event) throws ParseException
    {
        // search boats for member
        String[] columns = Boat.MODEL.getProperties();
        List<AnyDataObject> boatList = null;
        boatList = dss.retrieve(Boat.KIND, Boat.OMISTAJA, user.createKey(), columns);
        // choose one of the boats
        DataObjectChooser<AnyDataObject> ec = new DataObjectChooser<AnyDataObject>(
                Boat.MODEL,
                boatList,
                TextUtil.getText(eventType.name()),
                "CHOOSE");
        ec.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        List<AnyDataObject> selectedBoats = ec.choose();
        if (selectedBoats.size() == 1)
        {
            DataObject selectedBoat = selectedBoats.get(0);
            // get available member data
            Map<String, Object> userData = null;
            userData = dss.getUserData(user.getEntity());   //, selectedBoat.createKeyString(), null);
            if (event == null)
            {
                event = chooseEvent(eventType, "CHOOSE");
                if (event == null)
                {
                    return null;
                }
            }
            Reservation reservation = new Reservation(event);
            DataObjectModel model = reservation.getModel();
            model = model.hide(Reservation.ORDER, Reservation.INSPECTED, Reservation.BOAT);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> boatLst = (List<Map<String, Object>>) userData.get(Repository.BOATS);
            for (Map<String, Object> m : boatLst)
            {
                if (selectedBoat.createKeyString().equals(m.get(Repository.VENEET_KEY)))
                {
                    reservation.setAll(m);
                    reservation.set(Reservation.BOAT, m.get(Repository.VENEET_KEY));
                }
            }
            reservation.setAll(userData);
            reservation.set(Reservation.CREATOR, creator);
            String title = TextUtil.getText(eventType.name());
            DataObjectDialog<Reservation> et = new DataObjectDialog<Reservation>(frame, title, model, reservation);
            if (et.edit())
            {
                try
                {
                    dss.createReservation(event, reservation, true);
                    return reservation;
                }
                catch (EntityNotFoundException | EventFullException | DoubleBookingException | BoatNotFoundException | MandatoryPropertyMissingException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, ex.getMessage(), ex.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        else
        {
            JOptionPane.showMessageDialog(
                    frame,
                    TextUtil.getText("NO SELECTION"),
                    TextUtil.getText("MESSAGE"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        return null;
    }

    private void menuSwapPatrolShift()
    {
        if (privileged)
        {
            JMenu patrolMenu = new JMenu();
            TextUtil.populate(patrolMenu, "PATROL SHIFT");
            menuBar.add(patrolMenu);
            patrolMenu.add(menuItemSwapPatrolShift());
            patrolMenu.add(menuItemChangePatrolShift());
            patrolMenu.add(menuItemSwapLog());
        }
    }

    private void menuQuery()
    {
        JMenu queryMenu = new JMenu();
        TextUtil.populate(queryMenu, "QUERIES");
        menuBar.add(queryMenu);
        queryMenu.add(menuItemQuery());
        queryMenu.add(menuItemEditQuery());
    }

    private JMenuItem menuItemQuery()
    {
        ActionListener queryAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                startQuery(true, false);
            }

        };
        queryAction = createActionListener(frame, queryAction);
        JMenuItem queryItem = new JMenuItem();
        TextUtil.populate(queryItem, "START QUERY");
        queryItem.addActionListener(queryAction);
        return queryItem;
    }

    private JMenuItem menuItemEditQuery()
    {
        ActionListener queryAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                startQuery(true, true);
            }

        };
        queryAction = createActionListener(frame, queryAction);
        JMenuItem queryItem = new JMenuItem();
        TextUtil.populate(queryItem, "EDIT QUERY");
        queryItem.addActionListener(queryAction);
        return queryItem;
    }

    private void startQuery(boolean readonly, boolean edit)
    {
        try
        {
            if (workBench == null)
            {
                workBench = new WorkBench(serverProperties.getProperties(), true, readonly);
                Messages texts = dss.getMessages();
                workBench.addFetchResultPlugin(
                        new SMSPlugin(
                        texts.getString(Messages.SMSUSERNAME),
                        texts.getString(Messages.SMSPASSWORD)
                        ));
            }
            if (edit)
            {
                workBench.setVisible(true);
            }
            else
            {
                workBench.open();
            }
        }
        catch (IOException | InterruptedException ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }
    
    private AnyDataObject chooseMember(String title)
    {
        // search a member
        String[] columns = Member.MODEL.getProperties();
        String lastName = JOptionPane.showInputDialog(panel, TextUtil.getText(Member.SUKUNIMI) + "?", title, JOptionPane.QUESTION_MESSAGE);
        if (lastName == null)
        {
            return null;
        }
        lastName = Utils.convertName(lastName);
        List<AnyDataObject> memberList = dss.retrieve(Member.KIND, Member.SUKUNIMI, lastName, columns);
        DataObjectChooser<AnyDataObject> ec = new DataObjectChooser<AnyDataObject>(
                Member.MODEL,
                memberList,
                title,
                "CHOOSE");
        ec.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        List<AnyDataObject> selected = ec.choose();
        if (selected != null && selected.size() == 1)
        {
            return selected.get(0);
        }
        else
        {
            return null;
        }
    }

    private JMenuItem menuItemSwapPatrolShift()
    {
        ActionListener swapAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                AnyDataObject member = chooseMember(TextUtil.getText("CHOOSE MEMBER"));
                if (member != null)
                {
                    swapPatrolShift(member);
                }
                else
                {
                    JOptionPane.showMessageDialog(
                            frame,
                            TextUtil.getText("NO SELECTION"),
                            TextUtil.getText("MESSAGE"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };
        swapAction = createActionListener(frame, swapAction);
        JMenuItem swapItem = new JMenuItem();
        TextUtil.populate(swapItem, "SWAP PATROL SHIFT");
        swapItem.addActionListener(swapAction);
        return swapItem;
    }

    private JMenuItem menuItemChangePatrolShift()
    {
        ActionListener swapAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                AnyDataObject member = chooseMember(TextUtil.getText("CHOOSE PATROL SHIFT LOOSER"));
                if (member != null)
                {
                    changePatrolShift(member);
                }
                else
                {
                    JOptionPane.showMessageDialog(
                            frame,
                            TextUtil.getText("NO SELECTION"),
                            TextUtil.getText("MESSAGE"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };
        swapAction = createActionListener(frame, swapAction);
        JMenuItem swapItem = new JMenuItem();
        TextUtil.populate(swapItem, "CHANGE PATROL SHIFT");
        swapItem.addActionListener(swapAction);
        return swapItem;
    }

    private JMenuItem menuItemSwapLog()
    {
        ActionListener swapLogAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    TextDialog dia = new TextDialog(frame);
                    dss.swapLog(dia);
                    dia.edit();
                }
                catch (IOException | EntityNotFoundException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, ex.getMessage());
                }
            }
        };
        swapLogAction = createActionListener(frame, swapLogAction);
        JMenuItem swapLogItem = new JMenuItem();
        TextUtil.populate(swapLogItem, "SWAP PATROL LOG");
        swapLogItem.addActionListener(swapLogAction);
        return swapLogItem;
    }

    private PatrolShift choosePatrolShift(AnyDataObject user)
    {
        List<PatrolShift> shiftList = null;
        shiftList = dss.getShifts(user.createKey());
        // choose one of the shifts
        DataObjectChooser<PatrolShift> ec = new DataObjectChooser<PatrolShift>(
                PatrolShift.MODEL.view(Repository.PAIVA),
                shiftList,
                TextUtil.getText("PATROL SHIFT"),
                "CHOOSE");
        ec.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        List<PatrolShift> selectedShifts = ec.choose();
        if (selectedShifts != null && selectedShifts.size() == 1)
        {
            return selectedShifts.get(0);
        }
        else
        {
            return null;
        }
    }

    private void swapPatrolShift(AnyDataObject user)
    {
        Long memberNumber = (Long) user.get(Member.JASENNO);
        PatrolShift selectedShift = choosePatrolShift(user);
        if (selectedShift != null)
        {
            SwapRequest req = new SwapRequest();
            req.set(Repository.JASENNO, user.createKey());
            req.set(Repository.VUOROID, selectedShift.createKey());
            req.set(Repository.PAIVA, selectedShift.get(Repository.PAIVA));
            List<Long> excluded = new ArrayList<Long>();
            Day day = (Day) selectedShift.get(Repository.PAIVA);
            excluded.add(day.getValue());
            req.set(SwapRequest.EXCLUDE, excluded);
            req.set(Repository.CREATOR, creator);
            Object[] options =
            {
                TextUtil.getText("SWAP SHIFT"),
                TextUtil.getText("EXCLUDE DATE"),
                TextUtil.getText("DELETE PREVIOUS SWAP"),
                TextUtil.getText("CANCEL")
            };
            String msg = TextUtil.getText("SWAP OPTIONS");
            try
            {
                msg = dss.getShiftString(selectedShift.getEntity(), msg);
            }
            catch (EntityNotFoundException ex)
            {
                Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
            }
            while (true)
            {
                int choise = JOptionPane.showOptionDialog(
                        frame,
                        msg + dateList(excluded),
                        "",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);
                switch (choise)
                {
                    case 0:
                        try
                        {
                            msg = TextUtil.getText("SWAP CONFIRMATION");
                            msg = dss.getShiftString(selectedShift.getEntity(), msg) + dateList(excluded);
                            int confirm = JOptionPane.showConfirmDialog(frame, msg, TextUtil.getText("SWAP SHIFT"), JOptionPane.OK_CANCEL_OPTION);
                            if (JOptionPane.YES_OPTION == confirm)
                            {
                                boolean ok = dss.swapShift(req);
                                if (ok)
                                {
                                    JOptionPane.showMessageDialog(frame, TextUtil.getText("SwapOk"));
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(frame, TextUtil.getText("SwapPending"));
                                }
                            }
                        }
                        catch (EntityNotFoundException | IOException | SMSException | AddressException ex)
                        {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, ex.getMessage());
                        }
                        return;
                    case 1:
                        Day d = DateChooser.chooseDate(TextUtil.getText("EXCLUDE DATE"), new Day(excluded.get(excluded.size() - 1)));
                        excluded.add(d.getValue());
                        break;
                    case 2:
                        dss.deleteSwaps(memberNumber.intValue());
                        return;
                    case 3:
                        return;
                }
            }
        }
        else
        {
            JOptionPane.showMessageDialog(
                    frame,
                    TextUtil.getText("NO SELECTION"),
                    TextUtil.getText("MESSAGE"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void changePatrolShift(AnyDataObject releasedUser)
    {
        Long releasedMemberNumber = (Long) releasedUser.get(Member.JASENNO);
        PatrolShift selectedShift = choosePatrolShift(releasedUser);
        if (selectedShift != null)
        {
            AnyDataObject executingUser = chooseMember(TextUtil.getText("CHOOSE PATROL SHIFT EXECUTER"));
            if (executingUser != null)
            {
                dss.changeShiftExecutor(selectedShift, executingUser);
                return;
            }
        }
        JOptionPane.showMessageDialog(
                frame,
                TextUtil.getText("NO SELECTION"),
                TextUtil.getText("MESSAGE"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String dateList(List<Long> list)
    {
        StringBuilder sb = new StringBuilder();
        Collections.sort(list);
        for (Long l : list)
        {
            Day date = new Day(l);
            sb.append('\n');
            sb.append(date.toString());
        }
        return sb.toString();
    }

    private void uploadSeries(File file) throws IOException
    {
        SailWaveFile swf = new SailWaveFile(file);
        Race firstRace = swf.getFirstRace();
        RaceSeries rs = new RaceSeries();
        rs.set(RaceSeries.ID, swf.getEventId());
        rs.set(RaceSeries.EVENT, swf.getEvent());
        rs.set(RaceSeries.RACE_AREA, swf.getVenue());
        rs.set(RaceSeries.NOTES, swf.getNotes());
        if (firstRace != null)
        {
            rs.set(RaceSeries.EventDate, firstRace.getDate());
            rs.set(RaceSeries.STARTTIME, firstRace.getTime());
        }
        List<RaceFleet> fleetList = new ArrayList<>();
        for (Fleet fleet : swf.getFleets())
        {
            RaceFleet rc = new RaceFleet(rs);
            rc.set(RaceFleet.Fleet, fleet.getFleet());
            rc.set(RaceFleet.Class, fleet.getClassname());
            rc.set(RaceFleet.RatingSystem, fleet.getRatingSystem());
            rc.set(RaceFleet.SailWaveId, fleet.getNumber());
            if (firstRace != null)
            {
                rc.set(RaceFleet.EventDate, firstRace.getDate());
                rc.set(RaceFleet.StartTime, firstRace.getTime());
            }
            fleetList.add(rc);
        }
        DataObjectModel model = RaceSeries.MODEL.hide(
                RaceSeries.ID,
                RaceSeries.SAILWAVEFILE);
        DataObjectModel listModel = RaceFleet.Model.hide(RaceFleet.RatingSystem, RaceFleet.ClosingDate, RaceFleet.ClosingDate2, RaceFleet.Ranking, RaceFleet.SailWaveId);
        RaceDialog rc = new RaceDialog(frame, swf.getEvent(), dss, model, rs, listModel, fleetList, swf, false);
        rc.setEditable(listModel.getProperties());
        if (rc.edit())
        {
            swf.setEvent((String) rs.get(RaceSeries.EVENT));
            swf.setVenue((String) rs.get(RaceSeries.RACE_AREA));
            String notes = convertString(rs.get(RaceSeries.NOTES));
            if (notes != null)
            {
                swf.setNotes(notes);
            }
            if (firstRace != null)
            {
                Day date = (Day) rs.get(RaceSeries.EventDate);
                if (date != null)
                {
                    firstRace.setDate(date.toString());
                }
                Time time = (Time) rs.get(RaceSeries.STARTTIME);
                if (time != null)
                {
                    firstRace.setTime(time.toString());
                }
            }
            swf.updateFleets(fleetList);
            swf.saveAs(file);
            rs.set(RaceSeries.SAILWAVEFILE, swf.getBytes());
            while (true)
            {
                long id = 1;
                RaceSeries existingRace = dss.getExistingRace(rs);
                if (existingRace != null)
                {
                    String eventName = (String) existingRace.get(RaceSeries.EVENT);
                    if (JOptionPane.showConfirmDialog(
                            panel,
                            TextUtil.getText("CONFIRM REPLACE") + " " + eventName) == JOptionPane.YES_OPTION)
                    {
                        dss.putRace(rs, fleetList);
                        break;
                    }
                    else
                    {
                        swf.setId(id);
                        swf.saveAs(file);
                        rs.set(RaceSeries.ID, id);
                    }
                }
                else
                {
                    dss.putRace(rs, fleetList);
                    break;
                }
                id++;
            }
        }
    }

    private void editSeries() throws IOException, EntityNotFoundException
    {
        RaceSeries raceSeries = chooseRace();
        if (raceSeries != null)
        {
            Blob swb = (Blob) raceSeries.get(RaceSeries.SAILWAVEFILE);
            SailWaveFile swf = new SailWaveFile(swb.getBytes());
            List<RaceFleet> fleetList = dss.getFleets(raceSeries);
            DataObjectModel model = RaceSeries.MODEL.hide(
                    RaceSeries.ID,
                    RaceSeries.EventDate,
                    RaceSeries.SAILWAVEFILE);
            String event = (String) raceSeries.get(RaceSeries.EVENT);
            DataObjectModel listModel = RaceFleet.Model.hide(RaceFleet.RatingSystem, RaceFleet.ClosingDate, RaceFleet.Ranking, RaceFleet.SailWaveId);
            RaceDialog rd = new RaceDialog(frame, event, dss, model, raceSeries, listModel, fleetList, swf, false);
            rd.setEditable(listModel.getProperties());
            if (rd.edit())
            {
                swf.setEvent((String) raceSeries.get(RaceSeries.EVENT));
                swf.setVenue((String) raceSeries.get(RaceSeries.RACE_AREA));
                String notes = convertString(raceSeries.get(RaceSeries.NOTES));
                if (notes != null)
                {
                    swf.setNotes(notes);
                }
                swf.updateFleets(fleetList);
                raceSeries.set(RaceSeries.SAILWAVEFILE, swf.getBytes());
                dss.putRace(raceSeries, fleetList);
            }
        }
    }

    private void editRanking() throws IOException, EntityNotFoundException
    {
        RaceSeries rs = chooseRace();
        if (rs != null)
        {
            Blob swb = (Blob) rs.get(RaceSeries.SAILWAVEFILE);
            SailWaveFile swf = new SailWaveFile(swb.getBytes());
            List<Fleet> fleets = swf.getFleets();
            Fleet defFleet = swf.getDefaultFleet();
            if (defFleet == null)
            {
                JOptionPane.showMessageDialog(frame, TextUtil.getText("SAILWAVEFILE PROBLEM"));
                return;
            }
            String ratingSystem = fleets.get(0).getRatingSystem();
            List<RaceFleet> startList = new ArrayList<>();
            Map<RaceFleet, Race> startMap = new HashMap<>();
            for (Race start : swf.getRaces())
            {
                RaceFleet st = new RaceFleet(rs);
                st.set(RaceFleet.Ranking, true);
                st.set(RaceFleet.Fleet, defFleet.getRatingSystem());
                startMap.put(st, start);
                String startDate = start.getDate();
                String startTime = start.getTime();
                if (startTime == null)
                {
                    JOptionPane.showMessageDialog(frame, TextUtil.getText("SAILWAVEFILE PROBLEM"));
                    return;
                }
                if (startDate != null && !startDate.isEmpty())
                {
                    Day sd = new Day(startDate);
                    st.set(RaceFleet.EventDate, sd);
                    st.set(RaceFleet.ClosingDate, sd);
                }
                if (startTime != null && !startTime.isEmpty())
                {
                    st.set(RaceFleet.StartTime, new Time(startTime));
                }
                st.set(RaceFleet.RatingSystem, ratingSystem);
                startList.add(st);
            }
            DataObjectModel model = RaceSeries.MODEL.hide(
                    RaceSeries.ID,
                    RaceSeries.EventDate,
                    RaceSeries.TO,
                    RaceSeries.ClosingDate,
                    RaceSeries.STARTTIME,
                    RaceSeries.SAILWAVEFILE);
            DataObjectModel listModel = RaceFleet.Model.hide(RaceFleet.Ranking, RaceFleet.SailWaveId);
            RaceDialog rc = new RaceDialog(frame, swf.getEvent(), dss, model, rs, listModel, startList, swf, true);
            rc.setEditable(RaceFleet.EventDate, RaceFleet.StartTime, RaceFleet.ClosingDate);
            if (rc.edit())
            {
                Day from = null;
                Day to = null;
                for (RaceFleet start : startList)
                {
                    if (from == null)
                    {
                        from = (Day) start.get(RaceFleet.EventDate);
                    }
                    else
                    {
                        Day d = (Day) start.get(RaceFleet.EventDate);
                        if (from.after(d))
                        {
                            from = d;
                        }
                    }
                    if (to == null)
                    {
                        to = (Day) start.get(RaceFleet.EventDate);
                    }
                    else
                    {
                        Day d = (Day) start.get(RaceFleet.EventDate);
                        if (to.before(d))
                        {
                            to = d;
                        }
                    }
                    Race r = startMap.get(start);
                    r.setDate(start.get(RaceFleet.EventDate).toString());
                    r.setTime(start.get(RaceFleet.StartTime).toString());
                }
                rs.set(RaceSeries.EventDate, from);
                rs.set(RaceSeries.TO, to);
                swf.setEvent((String) rs.get(RaceSeries.EVENT));
                swf.setVenue((String) rs.get(RaceSeries.RACE_AREA));
                String notes = convertString(rs.get(RaceSeries.NOTES));
                swf.setNotes(notes);
                rs.set(RaceSeries.SAILWAVEFILE, swf.getBytes());
                dss.putRace(rs, startList);
            }
        }
    }

    private void removeSeries() throws IOException, EntityNotFoundException
    {
        RaceSeries raceSeries = chooseRace();
        if (raceSeries != null)
        {
            String event = (String) raceSeries.get(RaceSeries.EVENT);
            if (JOptionPane.showConfirmDialog(
                    panel,
                    TextUtil.getText("CONFIRM DELETE")) == JOptionPane.YES_OPTION)
            {
                int numberOfRaceEntriesFor = dss.getNumberOfRaceEntriesFor(raceSeries);
                if (numberOfRaceEntriesFor > 0)
                {
                    if (JOptionPane.showConfirmDialog(
                            panel,
                            TextUtil.getText("CONFIRM WHOLE RACE DELETE"),
                            TextUtil.getText("FLEET HAS ENTRIES"),
                            JOptionPane.WARNING_MESSAGE
                            ) == JOptionPane.YES_OPTION)
                    {
                        dss.deleteWithChilds(raceSeries, "RaceFleet");
                    }
                }
                else
                {
                    dss.deleteWithChilds(raceSeries, "RaceFleet");
                }
            }
        }
    }

    private void uploadRanking(File file) throws IOException
    {
        SailWaveFile swf = new SailWaveFile(file);
        RaceSeries rs = new RaceSeries();
        rs.set(RaceSeries.ID, swf.getEventId());
        rs.set(RaceSeries.EVENT, swf.getEvent());
        rs.set(RaceSeries.RACE_AREA, swf.getVenue());
        rs.set(RaceSeries.NOTES, swf.getNotes());
        List<Fleet> fleets = swf.getFleets();
        if (fleets.isEmpty())
        {
            JOptionPane.showMessageDialog(frame, TextUtil.getText("SAILWAVEFILE PROBLEM"));
            return;
        }
        if (fleets.size() > 1)
        {
            JOptionPane.showMessageDialog(frame, TextUtil.getText("FLEETS IN RANKING"));
            return;
        }
        Fleet defFleet = swf.getDefaultFleet();
        if (defFleet == null)
        {
            JOptionPane.showMessageDialog(frame, TextUtil.getText("SAILWAVEFILE PROBLEM"));
            return;
        }
        String ratingSystem = fleets.get(0).getRatingSystem();
        List<RaceFleet> startList = new ArrayList<>();
        Map<RaceFleet, Race> startMap = new HashMap<>();
        for (Race start : swf.getRaces())
        {
            RaceFleet st = new RaceFleet(rs);
            st.set(RaceFleet.Ranking, true);
            st.set(RaceFleet.Fleet, defFleet.getRatingSystem());
            startMap.put(st, start);
            String startDate = start.getDate();
            String startTime = start.getTime();
            if (startTime == null)
            {
                JOptionPane.showMessageDialog(frame, TextUtil.getText("SAILWAVEFILE PROBLEM"));
                return;
            }
            if (startDate != null && !startDate.isEmpty())
            {
                Day sd = new Day(startDate);
                st.set(RaceFleet.EventDate, sd);
                st.set(RaceFleet.ClosingDate, sd);
            }
            if (startTime != null && !startTime.isEmpty())
            {
                st.set(RaceFleet.StartTime, new Time(startTime));
            }
            st.set(RaceFleet.RatingSystem, ratingSystem);
            startList.add(st);
        }
        DataObjectModel model = RaceSeries.MODEL.hide(
                RaceSeries.ID,
                RaceSeries.EventDate,
                RaceSeries.TO,
                RaceSeries.ClosingDate,
                RaceSeries.STARTTIME,
                RaceSeries.SAILWAVEFILE);
        DataObjectModel listModel = RaceFleet.Model.hide(RaceFleet.Ranking, RaceFleet.SailWaveId);
        RaceDialog rc = new RaceDialog(frame, swf.getEvent(), dss, model, rs, listModel, startList, swf, true);
        rc.setEditable(RaceFleet.EventDate, RaceFleet.StartTime, RaceFleet.ClosingDate);
        if (rc.edit())
        {
            Day from = null;
            Day to = null;
            for (RaceFleet start : startList)
            {
                if (from == null)
                {
                    from = (Day) start.get(RaceFleet.EventDate);
                }
                else
                {
                    Day d = (Day) start.get(RaceFleet.EventDate);
                    if (from.after(d))
                    {
                        from = d;
                    }
                }
                if (to == null)
                {
                    to = (Day) start.get(RaceFleet.EventDate);
                }
                else
                {
                    Day d = (Day) start.get(RaceFleet.EventDate);
                    if (to.before(d))
                    {
                        to = d;
                    }
                }
                Race r = startMap.get(start);
                r.setDate(start.get(RaceFleet.EventDate).toString());
                r.setTime(start.get(RaceFleet.StartTime).toString());
            }
            rs.set(RaceSeries.EventDate, from);
            rs.set(RaceSeries.TO, to);
            swf.setEvent((String) rs.get(RaceSeries.EVENT));
            swf.setVenue((String) rs.get(RaceSeries.RACE_AREA));
            String notes = convertString(rs.get(RaceSeries.NOTES));
            swf.setNotes(notes);
            swf.saveAs(file);
            rs.set(RaceSeries.SAILWAVEFILE, swf.getBytes());
            while (true)
            {
                long id = 1;
                RaceSeries existingRace = dss.getExistingRace(rs);
                if (existingRace != null)
                {
                    String eventName = (String) existingRace.get(RaceSeries.EVENT);
                    if (JOptionPane.showConfirmDialog(
                            panel,
                            TextUtil.getText("CONFIRM REPLACE") + " " + eventName) == JOptionPane.YES_OPTION)
                    {
                        dss.putRace(rs, startList);
                        break;
                    }
                    else
                    {
                        swf.setId(id);
                        swf.saveAs(file);
                        rs.set(RaceSeries.ID, id);
                    }
                }
                else
                {
                    dss.putRace(rs, startList);
                    break;
                }
                id++;
            }
        }
    }

    private RaceSeries chooseRace() throws EntityNotFoundException
    {
        List<RaceSeries> raceList = dss.getRaces();
        DataObjectModel model = RaceSeries.MODEL.view(RaceSeries.EVENT);
        // choose one of the shifts
        DataObjectChooser<RaceSeries> ec = new DataObjectChooser<RaceSeries>(
                model,
                raceList,
                TextUtil.getText("RACE SERIES"),
                "CHOOSE");
        ec.setSelectAlways(true);
        ec.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        List<RaceSeries> selectedRaceSeries = ec.choose();
        if (selectedRaceSeries != null && selectedRaceSeries.size() == 1)
        {
            return selectedRaceSeries.get(0);
        }
        else
        {
            return null;
        }
    }

    private List<RaceEntry> chooseCompetitors() throws EntityNotFoundException
    {
        KeyTreeChooser chooser = new KeyTreeChooser(frame, TextUtil.getText("CHOOSE COMPETITORS"), dss, dss.getYearKey(), "Year", "RaceSeries", "RaceFleet");
        DataObject selected = chooser.select();
        if (selected != null)
        {
            return dss.getRaceEntriesFor(selected);
        }
        return null;
    }

    private DataObject chooseRaceSeriesOrFleet() throws EntityNotFoundException
    {
        KeyTreeChooser chooser = new KeyTreeChooser(frame, TextUtil.getText("CHOOSE RACE"), dss, dss.getRootKey(), "Root", "Year", "RaceSeries", "RaceFleet");
        return chooser.select();
    }

    private List<Attachment> chooseAttachments(DataObject parent)
    {
        List<Attachment> attachments = dss.getAttachmentsFor(parent);
        // choose one of the shifts
        DataObjectChooser<Attachment> ec = new DataObjectChooser<Attachment>(
                Attachment.MODEL.view(Attachment.TITLE, Attachment.Filename),
                attachments,
                TextUtil.getText("ATTACHMENTS"),
                "CHOOSE");
        ec.setSelectAlways(true);
        return ec.choose();
    }

    private void downloadCompetitorsForSailwave() throws IOException, EntityNotFoundException, JSONException
    {
        List<RaceEntry> entryList = chooseCompetitors();
        if (entryList == null || entryList.isEmpty())
        {
            return;
        }
        RaceSeries raceSeries = (RaceSeries) entryList.get(0).getRaceSeries();
        Blob swb = (Blob) raceSeries.get(RaceSeries.SAILWAVEFILE);
        SailWaveFile swf = new SailWaveFile(swb.getBytes());
        for (RaceEntry entry : entryList)
        {
            Competitor competitor = new Competitor();
            competitor.setAll(entry.getAll());
            swf.addCompetitor(competitor);
        }
        swf.deleteNotNeededFleets(entryList);
        File selectedFile = saveFile(SAILWAVEDIR, swf.getEvent() + ".blw", ".blw", "SailWave");
        if (selectedFile != null)
        {
            swf.saveAs(selectedFile);
        }
    }

    private void insertCompetitorsToSailwave() throws IOException, EntityNotFoundException, JSONException
    {
        List<RaceEntry> entryList = chooseCompetitors();
        if (entryList == null || entryList.isEmpty())
        {
            return;
        }
        File selectedFile = openFile(SAILWAVEDIR, ".blw", "SailWave");
        if (selectedFile == null)
        {
            return;
        }
        SailWaveFile swf = new SailWaveFile(selectedFile);
        for (RaceEntry entry : entryList)
        {
            Competitor competitor = new Competitor();
            competitor.setAll(checkRating(entry.getAll()));
            swf.addCompetitor(competitor);
        }
        swf.saveAs(selectedFile);
    }

    private void downloadCompetitorsAsCSV() throws IOException, EntityNotFoundException, JSONException
    {
        List<RaceEntry> entryList = chooseCompetitors();
        if (entryList == null || entryList.isEmpty())
        {
            return;
        }
        // TODO check ratings
        RaceSeries raceSeries = (RaceSeries) entryList.get(0).getRaceSeries();
        File selectedFile = saveFile(SAILWAVEDIR, raceSeries.getString(RaceSeries.EVENT) + ".csv", ".csv", "CSV");
        if (selectedFile != null)
        {
            try (FileOutputStream fos = new FileOutputStream(selectedFile))
            {
                DataObjectModel model = RaceEntry.MODEL.view(entryList);
                DataObject.writeCSV(model, entryList, fos);
            }
        }
    }

    private Map<String, Object> checkRating(Map<String, Object> map) throws IOException, JSONException
    {
        String fleet = (String) map.get(RaceEntry.FLEET);
        String nat = (String) map.get(RaceEntry.NAT);
        nat = nat.toUpperCase();
        Number sailNo = (Number) map.get(RaceEntry.SAILNO);
        String clazz = (String) map.get(RaceEntry.CLASS);
        String entryRatingStr = (String) map.get(RaceEntry.RATING);
        if (entryRatingStr != null)
        {
            entryRatingStr = entryRatingStr.replace(',', '.');
        }
        else
        {
            entryRatingStr = "0.0";
        }
        int sn = 0;
        if (sailNo != null)
        {
            sn = sailNo.intValue();
        }
        JSONObject json = getRating(fleet, nat, sn, clazz);
        String ratingSystem = json.optString(BoatInfo.RATINGSYSTEM);
        if ("UNKNOWN".equals(ratingSystem))
        {
            return map;
        }
        Map<String, Object> m2 = new HashMap<>();
        m2.putAll(map);
        String listedRatingStr = json.optString(RaceEntry.RATING);
        if (listedRatingStr != null && !listedRatingStr.isEmpty())
        {
            try
            {
                BigDecimal listedRating = new BigDecimal(listedRatingStr.replace(',', '.'));
                m2.put(RaceEntry.RATING, listedRating);
                BigDecimal entryRating = new BigDecimal(entryRatingStr.replace(',', '.'));
                if (!entryRating.equals(listedRating))
                {
                    String privateNotes = TextUtil.getText("RATING DIFFERS");
                    privateNotes = String.format(privateNotes, entryRating, listedRating);
                    m2.put(RaceEntry.PRIVATENOTES, privateNotes);
                }
                else
                {
                    String privateNotes = TextUtil.getText("RATING OK");
                    m2.put(RaceEntry.PRIVATENOTES, privateNotes);
                }
            }
            catch (NumberFormatException ex)
            {
                String privateNotes = TextUtil.getText("RATING DIFFERS");
                privateNotes = String.format(privateNotes, entryRatingStr, listedRatingStr);
                m2.put(RaceEntry.PRIVATENOTES, privateNotes);
            }
        }
        else
        {
            String privateNotes = TextUtil.getText("NO RATING");
            m2.put(RaceEntry.PRIVATENOTES, privateNotes);
        }
        return m2;
    }

    private JSONObject getRating(String ratingSystem, String nat, int sailNo, String boatClass) throws IOException, JSONException
    {
        try
        {
            String path = "/race?RatingSystem=" + ratingSystem + "&Nat=" + nat + "&SailNo=" + sailNo + "&Class=" + boatClass;
            URL url = new URL("http", server, path.replace(' ', '+'));
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is, "ASCII");
            StringBuilder sb = new StringBuilder();
            char[] cb = new char[256];
            int rc = isr.read(cb);
            while (rc != -1)
            {
                sb.append(cb, 0, rc);
                rc = isr.read(cb);
            }
            return new JSONObject(sb.toString());
        }
        catch (MalformedURLException ex)
        {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            Properties properties = new Properties();
            if (args.length != 0)
            {
                try (InputStream pFile = new FileInputStream(args[0]))
                {
                    properties.load(pFile);
                }
                catch (FileNotFoundException ex)
                {
                    
                }
            }
            ServerProperties sp = new ServerProperties(properties);
            DataObjectDialog<ServerProperties> dod = new DataObjectDialog<>(
                    null, 
                    sp.getModel().hide(ServerProperties.Tables, ServerProperties.SupportsZonerSMS, ServerProperties.SuperUser), 
                    sp);
            if (dod.edit())
            {
                String[] server = sp.getServer().split(",");
                RemoteAppEngine.init(server[0], sp.getUsername(), sp.getPassword());
                DataStoreProxy dsp = new DataStoreProxy(properties);
                dsp.start();
                Admin r = new Admin(sp);
            }
            else
            {
                System.exit(0);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
    }
}
