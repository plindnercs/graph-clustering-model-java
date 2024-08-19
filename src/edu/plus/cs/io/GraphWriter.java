package edu.plus.cs.io;


import edu.plus.cs.util.Constants;
import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;
import edu.plus.cs.util.Mode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Important: we assume that an empty line in a metis file can either be that the node does not exist, or that
 * the node has no neighbors. It is important to check the semantics of the input files.
 */
public class GraphWriter {
    public static void writeGraphToFile(HashMap<Integer, Set<Integer>> adjacencyLists, int numberOfEdges, Mode mode,
                                        boolean onMach2, Logger logger, boolean countEmpty) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);

        String outputPath = "";
        if (mode == Mode.DRAW_EDGES) {
            outputPath += "draw_edges_";
        } else if (mode == Mode.EXTRACT_SUBGRAPH) {
            outputPath += "extract_subgraph_";
        } else if (mode == Mode.MERGE_SUBGRAPHS) {
            outputPath += "merge_subgraphs_";
        } else {
            logger.log("Invalid mode provided, adding no mode prefix to the name of the output file!", LogLevel.ERROR, mode);
        }
        outputPath += "graph_output_" + timestamp + ".metis";

        if (onMach2) {
            outputPath = Constants.MACH2_DIR_PREFIX + outputPath;
        }

        List<Integer> verticesIds = adjacencyLists.keySet().stream().sorted().collect(Collectors.toList());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // write metis header: <numberOfVertices> <numberOfEdges>
            if (countEmpty) {
                writer.write(Integer.toString(verticesIds.get(verticesIds.size() - 1)));
            } else {
                writer.write(Long.toString(verticesIds.stream().filter(id -> !adjacencyLists.get(id).isEmpty()).count()));
            }
            writer.write(" ");
            writer.write(Integer.toString(numberOfEdges));
            writer.newLine();

            // write one line for each vertex
            for (int currentVertexId = 1; currentVertexId <= verticesIds.get(verticesIds.size() - 1); currentVertexId++) {
                if (nodeExists(currentVertexId, adjacencyLists)) {
                    List<Integer> connectedVertices = adjacencyLists.get(currentVertexId).stream().sorted().collect(Collectors.toList());
                    for (int otherVertexId : connectedVertices) {
                        writer.write(Integer.toString(otherVertexId));

                        // only place whitespace if current member is not the last one
                        if (currentVertexId != connectedVertices.get(connectedVertices.size() - 1)) {
                            writer.write(" ");
                        }
                    }
                    writer.newLine();
                } else {
                    writer.newLine();
                }
            }

            logger.log("Finished writing into file", LogLevel.DEBUG, outputPath);
        } catch (IOException e) {
            logger.log("Error writing to the output file: " + e.getMessage(), LogLevel.ERROR, outputPath);
        }
    }

    private static boolean nodeExists(int currentVertexId, HashMap<Integer, Set<Integer>> adjacencyLists) {
        return adjacencyLists.containsKey(currentVertexId);
    }
}
