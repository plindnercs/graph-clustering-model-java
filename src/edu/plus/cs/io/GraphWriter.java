package edu.plus.cs.io;


import edu.plus.cs.Main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class GraphWriter {
    public static void writeGraphToFile(HashMap<Integer, List<Integer>> adjacencyLists, int numberOfEdges, boolean onMach2) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);

        String outputPath = "draw_edges_graph_output_" + timestamp + ".txt";
        if (onMach2) {
            outputPath = Main.MACH2_DIR_PREFIX + outputPath;
        }

        List<Integer> verticesIds = adjacencyLists.keySet().stream().sorted().collect(Collectors.toList());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // write metis header: <numberOfVertices> <numberOfEdges>
            writer.write(Integer.toString(verticesIds.size()));
            writer.write(" ");
            writer.write(Integer.toString(numberOfEdges));
            writer.newLine();

            // write one line for each vertex
            for (int vertexId : verticesIds) {
                List<Integer> connectedVertices = new ArrayList<>(adjacencyLists.get(vertexId));
                for (int otherVertexId : connectedVertices) {
                    writer.write(Integer.toString(otherVertexId));

                    // only place whitespace if current member is not the last one
                    if (vertexId != verticesIds.get(verticesIds.size() - 1)) {
                        writer.write(" ");
                    }
                }
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to the output file: " + e.getMessage());
        }
    }
}
