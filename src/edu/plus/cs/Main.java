package edu.plus.cs;


import edu.plus.cs.impl.*;
import edu.plus.cs.util.Logger;
import edu.plus.cs.util.Mode;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Invalid mode provided!");
            return;
        }

        Mode mode = Mode.valueOf(args[0].toUpperCase());
        boolean onMach2 = false;

        Logger logger;
        switch (mode) {
            case RANDOM_MATCHING:
                if (args.length < 4) {
                    System.err.println("Invalid number of arguments for mode 'random_matching'!");
                    System.err.println("Use: random_matching <communitiesFile> <overlapFunctionFile> <randomMatchingFactor> (<onMach2>)");
                    return;
                }

                String communitiesFile = args[1];
                String overlapFunctionFile = args[2];
                double randomMatchingFactor = Double.parseDouble(args[3]);

                if (args.length > 4) {
                    onMach2 = Boolean.parseBoolean(args[4]);
                }

                logger = new Logger(Logger.createLoggingFileName(onMach2));

                RandomMatchingImpl.prepareAndExecuteRandomMatching(communitiesFile, overlapFunctionFile,
                        randomMatchingFactor, onMach2, logger);

                displayPeakMemoryUsage();

                break;
            case DRAW_EDGES:
                if (args.length < 4) {
                    System.err.println("Invalid number of arguments for mode 'draw_edges'!");
                    System.err.println("Use: draw_edges <matchedCommunitiesFile> <targetNumberOfEdges> <deviationFactor> (<onMach2>)");
                    return;
                }

                String matchedCommunitiesFile = args[1];
                int targetNumberOfEdges = Integer.parseInt(args[2]);
                double deviationFactor = Double.parseDouble(args[3]);

                if (args.length > 4) {
                    onMach2 = Boolean.parseBoolean(args[4]);
                }

                logger = new Logger(Logger.createLoggingFileName(onMach2));

                DrawEdgesImpl.drawEdges(matchedCommunitiesFile, targetNumberOfEdges, deviationFactor, onMach2, logger);

                break;
            case EXTRACT_SUBGRAPH:
                if (args.length < 3) {
                    System.err.println("Invalid number of arguments for mode 'extract_subgraph'!");
                    System.err.println("Use: extract_subgraph <originalInputGraph> <clusteredOutputGraph>");
                    return;
                }

                String originalInputGraphFile = args[1];
                String clusteredOutputGraphFile = args[2];

                logger = new Logger(Logger.createLoggingFileName(onMach2));

                ExtractSubgraphImpl.extractSubgraph(originalInputGraphFile, clusteredOutputGraphFile, logger);

                break;
            case MERGE_SUBGRAPHS:
                if (args.length < 4) {
                    System.err.println("Invalid number of arguments for mode 'extract_subgraph'!");
                    System.err.println("Use: merge_subgraphs <fileSubgraph1> <fileSubgraph2> <fileConnectingEdges>");
                    return;
                }

                String fileSubgraph1 = args[1];
                String fileSubgraph2 = args[2];
                String fileConnectingEdges = args[3];

                logger = new Logger(Logger.createLoggingFileName(onMach2));

                MergeSubgraphsImpl.mergeSubgraphs(fileSubgraph1, fileSubgraph2, fileConnectingEdges, logger);

                break;
        }
    }

    private static void displayPeakMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

        long peakUsedHeapMemory = 0;
        long peakUsedNonHeapMemory = 0;

        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            MemoryUsage peakUsage = pool.getPeakUsage();
            if (pool.getType() == java.lang.management.MemoryType.HEAP) {
                peakUsedHeapMemory += peakUsage.getUsed();
            } else if (pool.getType() == java.lang.management.MemoryType.NON_HEAP) {
                peakUsedNonHeapMemory += peakUsage.getUsed();
            }
        }

        System.out.println("Peak used heap memory: " + peakUsedHeapMemory + " bytes");
        System.out.println("Peak used non-heap memory: " + peakUsedNonHeapMemory + " bytes");

        // Optionally, you can also display the current memory usage
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        System.out.println("Current heap memory usage: " + heapMemoryUsage.getUsed() + " bytes");
        System.out.println("Current non-heap memory usage: " + nonHeapMemoryUsage.getUsed() + " bytes");
    }
}