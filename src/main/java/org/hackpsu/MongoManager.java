package org.hackpsu;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MongoManager {
    private static MongoManager instance;
    private final MongoDatabase db;

    public static MongoManager getInstance() {
        if (instance == null) {
            instance = new MongoManager();
        }

        return instance;
    }

    private MongoManager() {
        MongoClient mongo = MongoClients.create(Constants.MONGO_URI);
        db = mongo.getDatabase(Constants.MONGO_DATABASE_NAME);
    }

    public void registerUser(String phone, String zip) {
        MongoCollection<Document> usersCollection = db.getCollection(Constants.MONGO_USERS_COLLECTION_NAME);
        Document userDoc = new Document("phone", phone).append("zip", zip);
        usersCollection.insertOne(userDoc);
    }

    public FindIterable<Document> getAllUsersInZip(String zip) {
        MongoCollection<Document> usersCollection = db.getCollection(Constants.MONGO_USERS_COLLECTION_NAME);
        return usersCollection.find(Filters.eq("zip", zip));
    }

    public FindIterable<Document> getTodaysEvents() {
        MongoCollection<Document> eventsCollection = db.getCollection(Constants.MONGO_EVENTS_COLLECTION_NAME);
        DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        String t = formatter.format(new Date());

        Date today = new Date();

        Date todayWithZeroTime;
        try {
            todayWithZeroTime = formatter.parse(formatter.format(today));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return eventsCollection.find(Filters.eq("date", t));
    }

    public void updateNotifiedEvent(Document e) {
        MongoCollection<Document> eventsCollection = db.getCollection(Constants.MONGO_EVENTS_COLLECTION_NAME);
        Document updateDoc = new Document("$set", new Document("notified", true));
        eventsCollection.updateOne(Filters.eq("details", e.getString("details")), updateDoc);
    }

    public void createEvent(String organizerName, String organizerPhone, String address, String zip, String date, String time, String details) {
        MongoCollection<Document> eventsCollection = db.getCollection(Constants.MONGO_EVENTS_COLLECTION_NAME);
        Document eventDoc = new Document("name", organizerName).append("phone", organizerPhone).append("address", address).append("zip", zip).append("date", date).append("time", time).append("details", details).append("notified", false);
        eventsCollection.insertOne(eventDoc);
    }
}
