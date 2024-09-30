package org.logging.repository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.logging.entity.User;

import java.util.Optional;

public class UserRepository {

    private final MongoDatabase database;
    private final MongoClient mongoClient;
    public UserRepository() {
        this.mongoClient = MongoClients.create("mongodb://localhost:27017");
        this.database = mongoClient.getDatabase("logging");
    }

    public boolean existsByEmail(String email) {
        MongoCollection<Document> collection = database.getCollection("user");
        Document query = new Document("email", email);
        return collection.find(query).first() != null;
    }

    public void save(User user) {
        MongoCollection<Document> collection = database.getCollection("user");
        Document doc = new Document("name", user.getName())
                .append("email", user.getEmail())
                .append("password", user.getPassword());
        collection.insertOne(doc);
    }
    public Optional<User> findByEmail(String email) {
        MongoCollection<Document> collection = database.getCollection("user");
        Document query = new Document("email", email);
        Document userDocument = collection.find(query).first();

        if (userDocument != null) {
            User user = new User();
            user.setName(userDocument.getString("name"));
            user.setEmail(userDocument.getString("email"));
            user.setPassword(userDocument.getString("password"));
            return Optional.of(user);
        } else {
            return Optional.empty();
        }
    }
    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}

