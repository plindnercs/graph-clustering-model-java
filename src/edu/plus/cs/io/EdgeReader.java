package edu.plus.cs.io;

import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class EdgeReader {
    public static HashMap<Integer, Set<Integer>> readEdgesFromFile(String fileName, Logger logger) {
        HashMap<Integer, Set<Integer>> adjacencyList = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length < 2) {
                    continue; // skip empty lines
                }

                int vertex = Integer.parseInt(parts[0].trim());
                String[] edgesString = parts[1].trim().split("\\s+");

                Set<Integer> edges = new HashSet<>();
                for (String edge : edgesString) {
                    if (!edge.isEmpty()) {
                        edges.add(Integer.parseInt(edge));
                    }
                }

                adjacencyList.put(vertex, edges);
            }

            logger.log("Read edges from file", LogLevel.DEBUG, fileName);
        } catch (IOException e) {
            logger.log("Error while reading edges from file '" + fileName + "':" + e, LogLevel.ERROR);
        }

        return adjacencyList;
    }

}
