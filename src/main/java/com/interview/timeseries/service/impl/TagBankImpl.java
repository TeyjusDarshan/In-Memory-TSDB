package com.interview.timeseries.service.impl;

import com.interview.timeseries.service.interfaces.TagBank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class TagBankImpl implements TagBank {
    private final Map<String, Integer> tagsToId = new ConcurrentHashMap<>();
    private final AtomicInteger idx = new AtomicInteger(0);
    private final ReentrantLock fileLock = new ReentrantLock();

    @Value("${tag.filepath}")
    private String keyIdFilePath;

    private File keyIdfile;


    @PostConstruct
    public void init(){
        keyIdfile = new File(keyIdFilePath);
        loadFromFile();
    }

    @Override
    public int getIdAndStoreIfAbsent(String key) {
        return tagsToId.computeIfAbsent(key, k -> {
            int id = idx.getAndIncrement();
            persistKeyIdToFile(key, id);
            return id;
        });
    }

    @Override
    public int getId(String key) {
        return tagsToId.getOrDefault(key, -1);
    }

    private void persistKeyIdToFile(String key, int id) {
        fileLock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(keyIdfile, true))) {
            writer.write(key + "," + id);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to tag file", e);
        } finally {
            fileLock.unlock();
        }
    }

    private void loadFromFile() {
        if (!keyIdfile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(keyIdfile))) {
            String line;
            int maxId = -1;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String key = parts[0];
                    int id = Integer.parseInt(parts[1]);
                    tagsToId.put(key, id);
                    maxId = Math.max(maxId, id);
                }
            }
            idx.set(maxId + 1);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tags from file", e);
        }
    }
}
