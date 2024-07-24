package edu.plus.cs.impl;

import edu.plus.cs.io.CommunityReader;
import edu.plus.cs.io.GraphReader;
import edu.plus.cs.io.GraphWriter;
import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;
import edu.plus.cs.util.Mode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Set;

public class ExtractSubgraphImpl {

    public static void extractSubgraph(String originalInputGraphFile, String clusteredOutputGraphFile, Logger logger) {
        HashMap<Integer, Set<Integer>> inputFileAdjacencyLists = GraphReader.readGraphFromMetisFile(originalInputGraphFile, logger);
        Set<Integer> clusteredOutputGraphMembers = CommunityReader.readUniqueMembersFromFile(clusteredOutputGraphFile, logger);

        logger.log("Input files read. Start processing the graphs ...", LogLevel.INFO, originalInputGraphFile,
                clusteredOutputGraphFile);

        // write edges from the clustered output graph to the extracted subgraph to file
        // in order to restore the connections later on and rebuild the whole original graph
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);
        String outputPath = "extract_subgraph_extracted_edges_output_" + timestamp + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            for (int outputVertexId : clusteredOutputGraphMembers) {
                Set<Integer> originalNeighborsOfRemovedSubgraph = inputFileAdjacencyLists.get(outputVertexId);
                originalNeighborsOfRemovedSubgraph.removeAll(clusteredOutputGraphMembers);

                // we write: <outputVertexId>: <extractedVertexId>+
                writer.write(Integer.toString(outputVertexId));
                writer.write(": ");

                int vertexCnt = 0;
                for (int neighborVertexId : originalNeighborsOfRemovedSubgraph) {
                    writer.write(Integer.toString(neighborVertexId));

                    if (vertexCnt < originalNeighborsOfRemovedSubgraph.size() - 1) {
                        writer.write(" ");
                    }
                    vertexCnt++;
                }

                writer.write("\n");
            }
        } catch (IOException e) {
            logger.log("Error writing to output file: " + outputPath + "; " + e.getMessage(), LogLevel.ERROR);
        }

        logger.log("Exported the previous edges between the extracted subgraph and the clustered output graph.", LogLevel.INFO);

        // remove the vertices from the clustered output graph from the original input graph to obtain the extracted
        // subgraph

        // first we remove all adjacency lists of the vertices in the output graph
        for (int outputVertexId : clusteredOutputGraphMembers) {
            inputFileAdjacencyLists.remove(outputVertexId);
        }

        logger.log("Removed vertices of the output graph from the original graph. Step 1/2 of obtaining the extracted subgraph.",
                LogLevel.INFO);

        // then we remove the references to these vertices in the remaining adjacency lists
        for (int inputVertexId : inputFileAdjacencyLists.keySet()) {
            inputFileAdjacencyLists.get(inputVertexId).removeAll(clusteredOutputGraphMembers);
        }

        logger.log("Removed the edges between the extracted subgraph and the clustered output graph. Step 2/2 of obtaining the extracted subgraph.",
                LogLevel.INFO);

        int numberOfEdges = inputFileAdjacencyLists.values().stream().mapToInt(Set::size).sum();

        // as the last step we write the extracted graph to a metis file
        GraphWriter.writeGraphToFile(inputFileAdjacencyLists, numberOfEdges, Mode.EXTRACT_SUBGRAPH, false, logger);

        logger.log("Wrote extracted subgraph to metis file.", LogLevel.INFO);
    }
}
