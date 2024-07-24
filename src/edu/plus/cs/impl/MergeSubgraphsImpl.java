package edu.plus.cs.impl;

import edu.plus.cs.io.EdgeReader;
import edu.plus.cs.io.GraphReader;
import edu.plus.cs.io.GraphWriter;
import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;
import edu.plus.cs.util.Mode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MergeSubgraphsImpl {
    public static void mergeSubgraphs(String fileSubgraph1, String fileSubgraph2, String fileConnectingEdges,
                                      Logger logger) {
        HashMap<Integer, Set<Integer>> subgraph1 = GraphReader.readGraphFromMetisFile(fileSubgraph1, logger);
        HashMap<Integer, Set<Integer>> subgraph2 = GraphReader.readGraphFromMetisFile(fileSubgraph2, logger);

        HashMap<Integer, Set<Integer>> edges = EdgeReader.readEdgesFromFile(fileConnectingEdges, logger);

        HashMap<Integer, Set<Integer>> mergedSubgraphs = mergeMaps(subgraph1, subgraph2);

        int isolatedNodesAfterExtraction = 0;
        for (Integer edgeFrom : edges.keySet()) {
            Set<Integer> setOfEdgesTo = edges.get(edgeFrom);

            try {
                for (Integer edgeTo : setOfEdgesTo) {
                    if (!mergedSubgraphs.containsKey(edgeFrom)) {
                        logger.log("Found isolated node: " + edgeFrom, LogLevel.DEBUG);
                        mergedSubgraphs.put(edgeFrom, new HashSet<>());
                        isolatedNodesAfterExtraction++;
                    }
                    mergedSubgraphs.get(edgeFrom).add(edgeTo);

                    if (!mergedSubgraphs.containsKey(edgeTo)) {
                        logger.log("Found isolated node: " + edgeTo, LogLevel.DEBUG);
                        mergedSubgraphs.put(edgeTo, new HashSet<>());
                        isolatedNodesAfterExtraction++;
                    }
                    mergedSubgraphs.get(edgeTo).add(edgeFrom);
                }
            } catch (Exception ex) {
                logger.log("Error occurred while merging!", LogLevel.ERROR, ex);
            }
        }

        logger.log("Found " + isolatedNodesAfterExtraction +
                " isolated nodes that had no neighbours after the extraction of the subgraph.", LogLevel.INFO,
                fileSubgraph1, fileSubgraph2, fileConnectingEdges);

        int numberOfEdges = mergedSubgraphs.values().stream().mapToInt(Set::size).sum();

        GraphWriter.writeGraphToFile(mergedSubgraphs, numberOfEdges, Mode.MERGE_SUBGRAPHS, false, logger);
    }

    private static <K, V> HashMap<K, Set<V>> mergeMaps(Map<K, Set<V>> map1, Map<K, Set<V>> map2) {
        HashMap<K, Set<V>> mergedMap = new HashMap<>();

        for (K key : map1.keySet()) {
            if (mergedMap.containsKey(key)) {
                mergedMap.get(key).addAll(map1.get(key));
            }
            mergedMap.put(key, map1.get(key));
        }

        for (K key : map2.keySet()) {
            if (mergedMap.containsKey(key)) {
                mergedMap.get(key).addAll(map2.get(key));
            }
            mergedMap.put(key, map2.get(key));
        }

        return mergedMap;
    }
}
