package org.logging.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.logging.entity.AlertProfile;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class AlertProfileRepository {
    private MongoDatabase database;
    private final MongoClient mongoClient;
    public AlertProfileRepository()
    {
        this.mongoClient = MongoClients.create("mongodb://localhost:27017");
        this.database = mongoClient.getDatabase("logging");
    }
    public void save(AlertProfile alertProfile) {
        MongoCollection<Document> collection = database.getCollection("alertProfile");
        Document doc = new Document("profileName",alertProfile.getProfileName())
                .append("criteria", alertProfile.getCriteria())
                .append("notifyEmail", alertProfile.getNotifyEmail());
        collection.insertOne(doc);
    }
    public boolean existsByProfileName(String profileName) {
        MongoCollection<Document> collection = database.getCollection("alertProfile");
        Document query = new Document("profileName", profileName);
        return collection.find(query).first() != null;
    }

    public List<AlertProfile> findAll() {
        MongoCollection<Document> collection = database.getCollection("alertProfile");
        List<AlertProfile> alertProfiles = new ArrayList<>();

        collection.find().forEach(document ->
                alertProfiles.add(new AlertProfile(
                        document.getString("profileName"),
                        document.getString("criteria"),
                        document.getString("notifyEmail")
                ))
        );

        return alertProfiles;
    }
    public void updateProfile(AlertProfile updatedProfile) {
        MongoCollection<Document> collection = database.getCollection("alertProfile");

        Document query = new Document("profileName", updatedProfile.getProfileName());
        Document updateFields = new Document()
                .append("criteria", updatedProfile.getCriteria())
                .append("notifyEmail", updatedProfile.getNotifyEmail());

        Document updateOperation = new Document("$set", updateFields);

        collection.updateOne(query, updateOperation);
    }
    public void deleteByProfileName(String profileName) {
        MongoCollection<Document> collection = database.getCollection("alertProfile");
        collection.deleteOne(eq("profileName", profileName));
    }
    public void closeConnection()
    {
        if(mongoClient!= null)
        {
            mongoClient.close();
        }
    }

}
