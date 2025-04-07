package com.statement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component // Mark as a Spring component
public class ConnectionPool1 {
	
	//IMPROVED VERSION

    private BlockingQueue<Connection> connectionPool; // Use BlockingQueue
    private int maxPoolSize;
    private String url;
    private String username;
    private String password;

    public ConnectionPool1(
        @Value("${spring.datasource.url}") String url,
        @Value("${spring.datasource.username}") String username,
        @Value("${spring.datasource.password}") String password,
        @Value("${spring.datasource.max-pool-size}") int poolSize) {

        this.url = url;
        this.username = username;
        this.password = password;
        this.maxPoolSize = poolSize;

        initializePool();
    }

    private void initializePool() {
        connectionPool = new LinkedBlockingQueue<>(maxPoolSize); // Fixed capacity
        for (int i = 0; i < maxPoolSize; i++) {
            try {
                connectionPool.offer(createConnection()); // Use offer (non-blocking)
            } catch (SQLException e) {
                throw new RuntimeException("Error creating connection", e); // Handle connection failures
            }
        }
    }

    private Connection createConnection() throws SQLException { // Declare SQLException
        return DriverManager.getConnection(url, username, password);
    }

    public Connection getConnection() throws SQLException, InterruptedException {
        return connectionPool.take(); // Blocking take
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {  // Check for null
            try {
                connectionPool.offer(connection); // Non-blocking offer
            } catch (Exception e) {
                //Log the issue. It is not expected to happen
                e.printStackTrace();// Or use a logger
                try {
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

        }
    }

    @PreDestroy
    public void shutdown() {
        while (!connectionPool.isEmpty()) {
            try {
                Connection connection = connectionPool.poll();//Remove and return the head of this queue. Return null if this queue is empty.
                if(connection != null){
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace(); // Use a logger
            }
        }
    }

    // No longer needed with BlockingQueue
    // public int getActiveConnections() { ... }
    // public int getAvailableConnections() { ... }

}
