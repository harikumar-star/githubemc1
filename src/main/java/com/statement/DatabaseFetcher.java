package com.statement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class DatabaseFetcher {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFetcher.class);

    private final BlockingQueue<byte[]> queue;
    private final String url = "jdbc:mysql://localhost:3306/testdb";
    private final String username = "root";
    private final String password = "password";

    private final ConnectionPool1 conn1;

    @Autowired
    public DatabaseFetcher(BlockingQueue<byte[]> queue, ConnectionPool1 conn1) {
        this.queue = queue;
        this.conn1 = conn1;
    }

    @Scheduled(fixedDelay = 10000) // Runs every 10 seconds
    public void fetchData() {
        try (Connection conn = conn1.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, email, mailcode FROM statements_customers")) {

            Vector<String> records = new Vector<>();
            while (rs.next()) {
                String customerInfo = rs.getString("name") + " - " + rs.getString("email") + " - " + rs.getString("mailcode");
                records.add(customerInfo);
            }

            if (records.isEmpty()) {
                logger.info("No new records found in statements_customers.");
                return;
            }

            byte[] serializedData = serialize(records);

            if (queue.remainingCapacity() > 0) {
                queue.put(serializedData);
                logger.info("Fetched and added {} records to queue.", records.size());
            } else {
                logger.warn("Queue is full. Skipping data insertion.");
            }
        } catch (SQLException e) {
            logger.error("Database error while fetching data", e);
        } catch (IOException e) {
            logger.error("Serialization error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted while putting data into queue", e);
        }
    }

    private byte[] serialize(Vector<String> data) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(data);
            return bos.toByteArray();
        }
    }
}