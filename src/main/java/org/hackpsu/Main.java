package org.hackpsu;

import com.mongodb.client.FindIterable;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.javalin.Javalin;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static void main(String[] args) {
        HttpServer httpServer = HttpServer.getInstance();
        httpServer.start();

        Twilio.init(Constants.twilioSID, Constants.twilioAuthToken);

        Timer eventCheckTimer = new Timer ();
        TimerTask checkEventsTask = new TimerTask () {
            @Override
            public void run () {
                System.out.println("Checking for events...");
                FindIterable<Document> events = MongoManager.getInstance().getTodaysEvents();
                for (Document e : events) {
                    System.out.println("Event");
                    if (e.getBoolean("notified")) continue;
                    System.out.println("Finding zip " + e.getString("zip"));
                    FindIterable<Document> users = MongoManager.getInstance().getAllUsersInZip(e.getString("zip"));

                    users.forEach((Document userDoc) -> {
                        System.out.print("Sending message to ");
                        System.out.println(userDoc.getString("phone"));
                        Message message = Message.creator(
                                        new PhoneNumber("+1" + userDoc.getString("phone")),
                                        new PhoneNumber(Constants.twilioNumber),
                                        generateEventInfo(e))
                                .create();
                    });

                    MongoManager.getInstance().updateNotifiedEvent(e);
                }
            }
        };

        eventCheckTimer.scheduleAtFixedRate(checkEventsTask , 0l, 5 * (60*1000)); // Runs every 5 mins

    }

    public static void sendBlastToZip(String zip, String message) {
        FindIterable<Document> users = MongoManager.getInstance().getAllUsersInZip(zip);
        users.forEach((Document userDoc) -> {
            System.out.print("Sending message to ");
            System.out.println(userDoc.getString("phone"));
            Message m = Message.creator(
                            new PhoneNumber("+1" + userDoc.getString("phone")),
                            new PhoneNumber(Constants.twilioNumber),
                            message)
                    .create();
        });
    }

    public static String generateEventInfo(Document doc) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("MM/dd/yyyy");

            Date date = parser.parse(doc.getString("date"));
            String dateToString = parser.format(date);

            return "New Event!\n"
                    + "Organizer: " + doc.getString("name") + "\n"
                    + "Organizer Phone #: " + doc.getString("phone") + "\n"
                    + "Address: " + doc.getString("address") + "\n"
                    + "Date: " + dateToString + "\n"
                    + "Time: " + doc.getString("time") + "\n\n"
                    + doc.getString("details");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}