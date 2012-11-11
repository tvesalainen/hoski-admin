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

package fi.hoski.remote.sync;

import com.google.appengine.api.datastore.Key;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.Event;
import fi.hoski.datastore.repository.Reservation;
import fi.hoski.datastore.repository.ResultSetFilter;
import fi.hoski.util.Day;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * @author Timo Vesalainen
 */
public class InspectionHandler extends SqlConnection
{
    private PreparedStatement isInspectedStatement;
    private PreparedStatement deleteStatement;
    private PreparedStatement insertStatement;

    public InspectionHandler(Properties properties) throws SQLException, ClassNotFoundException
    {
        super(properties);
        debug = Boolean.parseBoolean(properties.getProperty("debug"));
        String driverName = properties.getProperty("driver");
        Class.forName(driverName);
        String databaseURL = properties.getProperty("databaseURL")+properties.getProperty("dsn");
        if (debug) DriverManager.setLogWriter(new PrintWriter(System.err));
        connection = DriverManager.getConnection(databaseURL, properties);
        
        String selectSql = "select count(*) from Katsastustiedot where VeneID = ? and Katsastustyyppi = ? and Paiva >= ? and Paiva <= ?";
        isInspectedStatement = connection.prepareStatement(selectSql);
        
        String deleteSql = "delete from Katsastustiedot where VeneID = ? and Katsastustyyppi = ? and Paiva >= ? and Paiva <= ?";
        deleteStatement = connection.prepareStatement(deleteSql);
        
        String insertSql = "insert into Katsastustiedot (VeneID, Katsastusluokka, Katsastustyyppi, Katsastaja, Kaasu, Paiva) values (?,?,?,?,?,?)";
        insertStatement = connection.prepareStatement(insertSql);
    }

    public void insert(int veneID, int inspectionClass, int inspectionType, int inspector, boolean gas, Day day) throws SQLException
    {
        insertStatement.setInt(1, veneID);
        insertStatement.setInt(2, inspectionClass);
        insertStatement.setInt(3, inspectionType);
        insertStatement.setInt(4, inspector);
        insertStatement.setBoolean(5, gas);
        Date date = new Date(day.getDate().getTime());
        insertStatement.setDate(6, date);
        insertStatement.execute();
    }
    public boolean isInspected(int veneID, int inspectionType, int year) throws SQLException
    {
        isInspectedStatement.setInt(1, veneID);
        isInspectedStatement.setInt(2, inspectionType);
        Day lower = new Day(year, 1, 1);
        Date sqlLower = new Date(lower.getDate().getTime());
        isInspectedStatement.setDate(3, sqlLower);
        Day upper = new Day(year, 12, 31);
        Date sqlUpper = new Date(upper.getDate().getTime());
        isInspectedStatement.setDate(4, sqlUpper);
        ResultSet rs = isInspectedStatement.executeQuery();
        if (!rs.next())
        {
            throw new IllegalArgumentException();
        }
        int count = rs.getInt(1);
        return count > 0;
    }
    public void delete(int veneID, int inspectionType, int year) throws SQLException
    {
        deleteStatement.setInt(1, veneID);
        deleteStatement.setInt(2, inspectionType);
        Day lower = new Day(year, 1, 1);
        Date sqlLower = new Date(lower.getDate().getTime());
        deleteStatement.setDate(3, sqlLower);
        Day upper = new Day(year, 12, 31);
        Date sqlUpper = new Date(upper.getDate().getTime());
        deleteStatement.setDate(4, sqlUpper);
        deleteStatement.execute();
    }
    public void updateInspection(List<Reservation> reservations) throws SQLException
    {
        Day now = new Day();
        int yearNow = now.getYear();
        for (Reservation reservation : reservations)
        {
            Event.EventType eventType = reservation.getEventType();
            assert Event.isInspection(eventType);
            Boolean inspectedObj = (Boolean) reservation.get(Reservation.INSPECTED);
            boolean inspected = false;
            if (inspectedObj != null)
            {
                inspected = inspectedObj;
            }
            Key veneIDKey = (Key) reservation.get(Reservation.BOAT);
            int veneID = (int) veneIDKey.getId();
            Number inspectionClassNumber = (Number) reservation.get(Reservation.INSPECTION_CLASS);
            int inspectionClass = 0;
            if (inspectionClassNumber != null)
            {
                inspectionClass = inspectionClassNumber.intValue();
            }
            Boolean basic = (Boolean) reservation.get(Reservation.BASICINSPECTION);
            int inspectionType = 0;
            if (basic != null && basic)
            {
                inspectionType = 1;
            }
            else
            {
                inspectionType = 2;
            }
            Number inspectorNumber = (Number) reservation.get(Reservation.INSPECTOR);
            int inspector = 0;
            if (inspectorNumber != null)
            {
                inspector = inspectorNumber.intValue();
            }
            Boolean gasBoolean = (Boolean) reservation.get(Reservation.INSPECTION_GASS);
            boolean gas = false;
            if (gasBoolean != null)
            {
                gas = gasBoolean;
            }
            if (inspected)
            {
                boolean already = isInspected(veneID, inspectionType, yearNow);
                if (!already)
                {
                    insert(veneID, inspectionClass, inspectionType, inspector, gas, now);
                }
            }
            else
            {
                delete(veneID, inspectionType, yearNow);
            }
        }
    }
    public void inspectAllLightBoats(int maxMemberNo) throws SQLException
    {
        Day now = new Day();
        String selectSql = "select VeneID from Veneet where Tyyppi = 'KV' and Omistaja < "+maxMemberNo;
        PreparedStatement statement = connection.prepareStatement(selectSql);
        ResultSet rs = statement.executeQuery();
        while (rs.next())
        {
            int veneID = rs.getInt(1);
            boolean inspected = isInspected(veneID, 2, now.getYear());
            if (!inspected)
            {
                insert(veneID, 4, 2, 0, false, now);
            }
        }
    }
    public void getUninspectedBoats() throws SQLException
    {
        Day now = new Day();
        String selectSql = "select Veneet.VeneID, Jasenet.Etunimi, Jasenet.Sukunimi, Veneet.Nimi, Veneet.Tyyppi from Veneet, Jasenet where Veneet.Omistaja = Jasenet.JasenNo";
        PreparedStatement statement = connection.prepareStatement(selectSql);
        ResultSet rs = statement.executeQuery();
        while (rs.next())
        {
            int veneID = rs.getInt(1);
            String etunimi = rs.getString(2);
            String sukunimi = rs.getString(3);
            String vene = rs.getString(4);
            String tyyppi = rs.getString(5);
            boolean inspected = isInspected(veneID, 2, now.getYear());
            if (!inspected)
            {
                System.err.println(veneID+"\t"+etunimi+"\t"+sukunimi+"\t"+vene+"\t"+tyyppi);
            }
        }
    }
    public static void main(String[] args)
    {
        try
        {
            final Properties properties = new Properties();
            try (FileInputStream pFile = new FileInputStream("C:\\Jasenrekisteri\\replicator.properties");)
            {
                properties.load(pFile);
            }
            InspectionHandler ih = new InspectionHandler(properties);
            ih.getUninspectedBoats();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
