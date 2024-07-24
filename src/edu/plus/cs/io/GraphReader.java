package edu.plus.cs.io;

import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Important: we assume that an empty line in a metis file can either be that the node does not exist, or that
 * the node has no neighbors. It is important to check the semantics of the input files.
 */
public class GraphReader {

    public static HashMap<Integer, Set<Integer>> readGraphFromMetisFile(String graphInputFile, Logger logger) {
        HashMap<Integer, Set<Integer>> adjacencyLists = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(graphInputFile))) {
            String line;
            String[] splitLine;

            // handle header line
            line = br.readLine();

            splitLine = line.split(" ");
            int numberOfVertices = Integer.parseInt(splitLine[0]);
            int numberOfEdges = Integer.parseInt(splitLine[1]);

            int vertexId = 1;
            while ((line = br.readLine()) != null) {
                HashSet<Integer> adjacencyList = new HashSet<>();

                String[] splitNeighbors = line.split(" ");
                if (splitNeighbors.length > 0 && !(splitNeighbors.length == 1 && "".equals(splitNeighbors[0]))) {
                    for (String neighborVertex : splitNeighbors) {
                        adjacencyList.add(Integer.parseInt(neighborVertex));
                    }
                }
                adjacencyLists.put(vertexId, adjacencyList);

                vertexId++;
            }

            logger.log("Processed input file with " + numberOfVertices + " vertices and " + numberOfEdges +
                            " edges!", LogLevel.DEBUG, graphInputFile);
        } catch (IOException e) {
            logger.log("Error reading from file: " + graphInputFile + "; " + e.getMessage(), LogLevel.ERROR);
            return null;
        }

        return adjacencyLists;
    }
}
