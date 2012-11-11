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

package fi.hoski.remote;

import fi.hoski.datastore.BoatNotFoundException;
import com.google.appengine.api.datastore.*;
import fi.hoski.datastore.*;
import fi.hoski.datastore.repository.*;
import fi.hoski.datastore.repository.Event.EventType;
import fi.hoski.mail.MailService;
import fi.hoski.mail.MailServiceImpl;
import fi.hoski.remote.sync.InspectionHandler;
import fi.hoski.remote.sync.Synchronizer2;
import fi.hoski.sms.IllegalCharacterException;
import fi.hoski.sms.SMSException;
import fi.hoski.sms.SMSService;
import fi.hoski.sms.SMSStatus;
import fi.hoski.sms.zoner.ZonerSMSService;
import fi.hoski.util.BankingBarcode;
import fi.hoski.util.Day;
import java.io.*;
import java.nio.charset.CharacterCodingException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * @author Timo Vesalainen
 */
public class DataStoreServiceImpl implements DataStoreService
{
    private Properties properties;
    private DatastoreService datastore;
    private DSUtils entities;
    private Events events;
    private Users users;
    private MailService mail;
    private SMSService sms;
    private Synchronizer2 synchronizer;
    private PatrolShifts patrolShifts;
    private Races races;

    public DataStoreServiceImpl(Properties properties, DatastoreService datastore) throws EntityNotFoundException, SQLException, ClassNotFoundException
    {
        this.properties = properties;
        this.datastore = datastore;
        SystemLog systemLog = new SystemLog();
        entities = new DSUtilsImpl(datastore);
        events = new EventsImpl(datastore, entities);
        users = new UsersImpl(datastore);
        mail = new MailServiceImpl();
        sms = new ZonerSMSService(datastore);
        synchronizer = new Synchronizer2(properties);
        patrolShifts = new PatrolShiftsImpl(systemLog, 5, datastore, entities, mail, sms);
        races = new RacesImpl(systemLog, datastore, entities, mail);
    }

    public void inspectAllLightBoats(int maxMemberNo) throws SQLException, ClassNotFoundException
    {
        InspectionHandler ih = new InspectionHandler(properties);
        ih.inspectAllLightBoats(maxMemberNo);
    }
    @Override
    public void inspectionFix1() throws SQLException, ClassNotFoundException
    {
        InspectionHandler ih = new InspectionHandler(properties);
        Day now = new Day();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query("Reservation");
        Key ancestor = Keys.getTypeKey(now, EventType.INSPECTION);
        query.setAncestor(ancestor);
        PreparedQuery prepared = datastore.prepare(query);
        List<Entity> list = prepared.asList(FetchOptions.Builder.withChunkSize(500));
        List<Reservation> reservations = new ArrayList<>();
        for (Entity entity : list)
        {
            Key parent = entity.getParent();
            if (parent.getId() < now.getValue())
            {
                try
                {
                    Boolean ins = (Boolean) entity.getProperty(Reservation.INSPECTED);
                    if (ins != null && ins)
                    {
                        Key veneIDKey = (Key) entity.getProperty("VeneID");
                        int veneID = (int) veneIDKey.getId();
                        boolean inspected = ih.isInspected(veneID, 2, now.getYear());
                        if (!inspected)
                        {
                            try
                            {
                                reservations.add((Reservation)entities.newInstance(entity));
                                System.err.println(entity);
                            }
                            catch (EntityNotFoundException ex)
                            {
                                throw new IllegalArgumentException(ex);
                            }
                        }
                    }
                }
                catch (SQLException ex)
                {
                    throw new IllegalArgumentException(ex);
                }
            }
        }
        ih.updateInspection(reservations);
    }

    public Entity retrieveUser(String email) throws EmailNotUniqueException
    {
        return users.retrieveUser(email);
    }

    public Entity retrieveCredentials(String email) throws EmailNotUniqueException
    {
        return users.retrieveCredentials(email);
    }

    public Map<String, Object> getUserData(Entity user)
    {
        return users.getUserData(user);
    }

    public void addSideReferences(Map<String, Entity> entityMap, Entity entity)
    {
        users.addSideReferences(entityMap, entity);
    }

    public Map<String, String[]> getMap(Entity entity)
    {
        return entities.getMap(entity);
    }

    public Entity createEntity(Map<String, String[]> parameters)
    {
        return entities.createEntity(parameters);
    }

    public boolean hasChilds(Event event)
    {
        return events.hasChilds(event);
    }

    public List<Event> getEvents(EventType type)
    {
        return events.getEvents(type);
    }

    public void deleteEvents(List<Event> events)
    {
        this.events.deleteEvents(events);
    }

    public int childCount(Event event)
    {
        return events.childCount(event);
    }

    public Options<Event> getEventSelection(EventType type)
    {
        return events.getEventSelection(type);
    }

    public void createReservation(Event event, Reservation reservation, boolean replace) throws EventFullException, DoubleBookingException, BoatNotFoundException, EntityNotFoundException, MandatoryPropertyMissingException
    {
        events.createReservation(event, reservation, replace);
    }

    public void createReservation(Reservation reservation, boolean replace) throws EventFullException, DoubleBookingException, BoatNotFoundException, EntityNotFoundException, MandatoryPropertyMissingException
    {
        events.createReservation(reservation, replace);
    }

    public List<Reservation> getReservations(Event event)
    {
        return events.getReservations(event);
    }

    public void put(List<? extends DataObject> dataObjectList)
    {
        entities.put(dataObjectList);
    }

    public Entity put(DataObject dataObject)
    {
        return entities.put(dataObject);
    }

    public void upload(DataObject attachTo, Attachment.Type type, String title, File... files) throws IOException
    {
        entities.upload(attachTo, type, title, files);
    }

    @Override
    public void removeAttachments(List<Attachment> attachments) throws IOException
    {
        entities.removeAttachments(attachments);
    }

    public Entity get(Key key) throws EntityNotFoundException
    {
        return entities.get(key);
    }

    public List<Entity> getChilds(Key parent)
    {
        return entities.getChilds(parent);
    }

    public List<Entity> getChilds(Key parent, String kind)
    {
        return entities.getChilds(parent, kind);
    }

    public void delete(List<? extends DataObject> dataObjectList)
    {
        entities.delete(dataObjectList);
    }

    public void delete(DataObject dataObject)
    {
        entities.delete(dataObject);
    }

    public Entity get(String kind, String name) throws EntityNotFoundException
    {
        return entities.get(kind, name);
    }

    public List<Entity> convert(List<? extends DataObject> dataObjectList)
    {
        return entities.convert(dataObjectList);
    }

    public List<AnyDataObject> retrieve(String kind, String queryProperty, Object queryValue, String... properties)
    {
        return entities.retrieve(kind, queryProperty, queryValue, properties);
    }

    public void store(Iterator<Entity> entities)
    {
        this.entities.store(entities);
    }

    public int remove(List<String> kindList)
    {
        return entities.remove(kindList);
    }

    public int restore(Collection<String> kindList, ObjectInputStream in) throws IOException
    {
        return entities.restore(kindList, in);
    }

    public int restore(ObjectInputStream in) throws IOException
    {
        return entities.restore(in);
    }

    public int remove(long year)
    {
        return entities.remove(year);
    }

    public int backup(List<String> kindList, ObjectOutputStream out) throws IOException
    {
        return entities.backup(kindList, out);
    }

    public int backup(long year, ObjectOutputStream out) throws IOException
    {
        return entities.backup(year, out);
    }

    public void putMessages(Messages messages)
    {
        entities.putMessages(messages);
    }

    public Iterator<Entity> load(List<String> kindList)
    {
        return entities.load(kindList);
    }

    public List<String> kindList()
    {
        return entities.kindList();
    }

    public String getMessage(String key)
    {
        return entities.getMessage(key);
    }

    public Messages getMessages()
    {
        return entities.getMessages();
    }

    public void sendMail(InternetAddress from, String subject, String body, String htmlBody, List<InternetAddress> addresses)
    {
        mail.sendMail(from, subject, body, htmlBody, addresses);
    }

    public void sendMail(InternetAddress from, String subject, String body, String htmlBody, InternetAddress... addresses)
    {
        mail.sendMail(from, subject, body, htmlBody, addresses);
    }

    public void sendMail(String subject, String body, String htmlBody, InternetAddress... addresses) throws UnsupportedEncodingException
    {
        mail.sendMail(subject, body, htmlBody, addresses);
    }

    public void sendMail(String subject, String body, String htmlBody, List<InternetAddress> addresses) throws UnsupportedEncodingException
    {
        mail.sendMail(subject, body, htmlBody, addresses);
    }

    public InternetAddress createInternetAddress(DataObject dataObject) throws UnsupportedEncodingException
    {
        return mail.createInternetAddress(dataObject);
    }

    public List<SMSStatus> send(String numberFrom, List<String> numberTo, String message) throws IOException, SMSException
    {
        return sms.send(numberFrom, numberTo, message);
    }

    public SMSStatus send(String numberFrom, String numberTo, String message) throws IOException, SMSException
    {
        return sms.send(numberFrom, numberTo, message);
    }

    public int messageSize(String message) throws CharacterCodingException, IllegalCharacterException
    {
        return sms.messageSize(message);
    }

    public int messageCount(String message) throws CharacterCodingException, IllegalCharacterException
    {
        return sms.messageCount(message);
    }

    public int messagesLeft() throws IOException, SMSException
    {
        return sms.messagesLeft();
    }

    public void updateInspection(List<Reservation> reservations)
    {
        events.updateInspection(reservations);
    }

    public Event getEvent(EventType type, long id) throws EntityNotFoundException
    {
        return events.getEvent(type, id);
    }

    public List<Event> getEvents(EventType type, Day from, Integer count)
    {
        return events.getEvents(type, from, count);
    }

    public Event getEvent(String eventKey) throws EntityNotFoundException
    {
        return events.getEvent(eventKey);
    }

    public String getEventLabel(Event event)
    {
        return events.getEventLabel(event);
    }

    public List<SMSStatus> send(List<String> numberTo, String message) throws IOException, SMSException
    {
        return sms.send(numberTo, message);
    }

    public SMSStatus send(String numberTo, String message) throws IOException, SMSException
    {
        return sms.send(numberTo, message);
    }

    public String status(String trackingId) throws IOException, SMSException
    {
        return sms.status(trackingId);
    }

    @Override
    public void synchronize(Progress progress) throws IOException
    {
        try
        {
            synchronizer.synchronize(progress);
        }
        catch (ClassNotFoundException ex)
        {
            throw new IOException(ex);
        }
        catch (SQLException ex)
        {
            throw new IOException(ex);
        }
        catch (InterruptedException ex)
        {
            throw new IOException(ex);
        }
        catch (ExecutionException ex)
        {
            throw new IOException(ex);
        }
    }

    public DataObject newInstance(Key key) throws EntityNotFoundException
    {
        return entities.newInstance(key);
    }

    public DataObject newInstance(Entity entity) throws EntityNotFoundException
    {
        return entities.newInstance(entity);
    }

    public DataObjectModel getModel(String kind)
    {
        return entities.getModel(kind);
    }

    public Key getRootKey()
    {
        return entities.getRootKey();
    }

    public Key getYearKey()
    {
        return entities.getYearKey();
    }

    public Options<String> getShiftOptions(String memberKeyString)
    {
        return patrolShifts.getShiftOptions(memberKeyString);
    }

    public List<PatrolShift> getShifts(Key memberKey)
    {
        return patrolShifts.getShifts(memberKey);
    }

    public boolean swapShift(Map<String, Object> user, String shift, String... excl) throws EntityNotFoundException, IOException, SMSException, AddressException
    {
        return patrolShifts.swapShift(user, shift, excl);
    }

    public boolean swapShift(SwapRequest req) throws EntityNotFoundException, IOException, SMSException, AddressException
    {
        return patrolShifts.swapShift(req);
    }

    public String getShiftString(Entity patrolShift, String format) throws EntityNotFoundException
    {
        return patrolShifts.getShiftString(patrolShift, format);
    }

    public List<DataObject> getSwapLog() throws EntityNotFoundException
    {
        return patrolShifts.getSwapLog();
    }

    public void deleteSwaps(int memberNumber)
    {
        patrolShifts.deleteSwaps(memberNumber);
    }

    public Day[] firstAndLastShift() throws EntityNotFoundException
    {
        return patrolShifts.firstAndLastShift();
    }

    public void removeSwapShift(Map<String, Object> user, String shift) throws EntityNotFoundException, IOException
    {
        patrolShifts.removeSwapShift(user, shift);
    }

    public List<SwapRequest> pendingSwapRequests(Map<String, Object> user) throws EntityNotFoundException, IOException
    {
        return patrolShifts.pendingSwapRequests(user);
    }

    public void changeShiftExecutor(PatrolShift shift, DataObject member)
    {
        patrolShifts.changeShiftExecutor(shift, member);
    }

    public void handleExpiredRequests(int margin) throws EntityNotFoundException
    {
        patrolShifts.handleExpiredRequests(margin);
    }

    public RaceSeries getExistingRace(RaceSeries raceSeries)
    {
        return races.getExistingRace(raceSeries);
    }

    public void putRace(RaceSeries raceSeries, List<RaceFleet> classList)
    {
        races.putRace(raceSeries, classList);
    }

    public List<RaceSeries> getRaces() throws EntityNotFoundException
    {
        return races.getRaces();
    }

    public List<RaceFleet> getFleets(RaceSeries raceSeries) throws EntityNotFoundException
    {
        return races.getFleets(raceSeries);
    }

    public List<RaceEntry> getUnpaidRaceEntries() throws EntityNotFoundException
    {
        return races.getUnpaidRaceEntries();
    }

    public List<RaceEntry> getRaceEntriesFor(DataObject race) throws EntityNotFoundException
    {
        return races.getRaceEntriesFor(race);
    }

    public RaceEntry raceEntryForReference(long reference) throws EntityNotFoundException
    {
        return races.raceEntryForReference(reference);
    }

    public List<Attachment> getAttachmentsFor(DataObject parent)
    {
        return entities.getAttachmentsFor(parent);
    }

    public List<Attachment> getAttachmentsFor(Key parent)
    {
        return entities.getAttachmentsFor(parent);
    }

    public BankingBarcode getBarcode(RaceEntry raceEntry) throws EntityNotFoundException
    {
        return races.getBarcode(raceEntry);
    }

    public BankingBarcode getBarcode(Key raceEntryKey) throws EntityNotFoundException
    {
        return races.getBarcode(raceEntryKey);
    }

    @Override
    public List<Title> getTitles() throws EntityNotFoundException
    {
        return entities.getTitles();
    }

    @Override
    public void swapLog(Appendable a) throws IOException, EntityNotFoundException
    {
        int queued=0;
        int expired=0;
        int removed=0;
        int success=0;
        int changed=0;
        DateFormat df = DateFormat.getDateTimeInstance();
        Query query1 = new Query("SwapLog");
        PreparedQuery pq = datastore.prepare(query1);
        for (Entity e : pq.asIterable())
        {
            String status = (String) e.getProperty("Status");
            a.append(status);
            a.append('\t');
            Date timestamp = (Date) e.getProperty("Timestamp");
            a.append(df.format(timestamp));
            a.append('\t');
            String creator = (String) e.getProperty("Creator");
            a.append(creator);
            a.append('\t');
            switch (status)
            {
                case "Queued":
                case "Remove expired":
                case "Removed":
                {
                    Key jk = (Key) e.getProperty("JasenNo");
                    Day p = Day.getDay(e.getProperty("Paiva"));
                    Entity jke = datastore.get(jk);
                    String en = (String) jke.getProperty("Etunimi");
                    a.append(en);
                    a.append('\t');
                    String sn = (String) jke.getProperty("Sukunimi");
                    a.append(sn);
                    a.append('\t');
                    a.append(p.toString());
                    a.append('\t');
                }
                    break;
                case "Success":
                {
                    Key ark = (Key) e.getProperty("ActiveRequestor");
                    Entity are = datastore.get(ark);
                    String aen = (String) are.getProperty("Etunimi");
                    String asn = (String) are.getProperty("Sukunimi");
                    Key arsk = (Key) e.getProperty("ActiveRequestorShift");
                    Entity arse = datastore.get(arsk);
                    Key qrk = (Key) e.getProperty("QueuedRequestor");
                    Entity qre = datastore.get(qrk);
                    String qen = (String) qre.getProperty("Etunimi");
                    String qsn = (String) qre.getProperty("Sukunimi");
                    Key qrsk = (Key) e.getProperty("QueuedRequestorShift");
                    Entity qrse = datastore.get(qrsk);
                    a.append(aen+" "+asn+" <-> "+qen+" "+qsn);
                    a.append('\t');
                }
                    break;
                case "Change":
                {
                    Key nek = (Key) e.getProperty("NewExecutor");
                    Entity nee = datastore.get(nek);
                    String en = (String) nee.getProperty("Etunimi");
                    a.append(en);
                    a.append('\t');
                    String sn = (String) nee.getProperty("Sukunimi");
                    a.append(sn);
                    a.append('\t');
                }
                    break;
                default:
                    a.append("unknown status");
                    break;
            }
            switch (status)
            {
                case "Queued":
                    queued++;
                    break;
                case "Remove expired":
                    expired++;
                    break;
                case "Removed":
                    removed++;
                    break;
                case "Success":
                    success++;
                    break;
                case "Change":
                    changed++;
                    break;
            }
            a.append('\n');
        }
        a.append("Queued\n");
        Query query2 = new Query("SwapRequest");
        PreparedQuery pq2 = datastore.prepare(query2);
        for (Entity e : pq2.asIterable())
        {
            Key jk = (Key) e.getProperty("JasenNo");
            Day p = Day.getDay(e.getProperty("Paiva"));
            Entity jke = datastore.get(jk);
            String en = (String) jke.getProperty("Etunimi");
            a.append(en);
            a.append('\t');
            String sn = (String) jke.getProperty("Sukunimi");
            a.append(sn);
            a.append('\t');
            a.append(p.toString());
            a.append('\t');
            a.append('\n');
        }
        queued += success;
        success *= 2;
        a.append("queued="+queued+'\n');
        a.append("success="+success+'\n');
        a.append("expired="+expired+'\n');
        a.append("removed="+removed+'\n');
        a.append("changed="+changed+'\n');
    }

}
