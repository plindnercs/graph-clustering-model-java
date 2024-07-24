package edu.plus.cs;


import edu.plus.cs.impl.*;
import edu.plus.cs.util.Logger;
import edu.plus.cs.util.Mode;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Invalid mode provided!");
            return;
        }

        Mode mode = Mode.valueOf(args[0].toUpperCase());
        boolean onMach2 = false;
        if (args.length > 4) {
            onMach2 = Boolean.parseBoolean(args[4]);
        }

        Logger logger = new Logger(Logger.createLoggingFileName(onMach2));
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

                RandomMatchingImpl.prepareAndExecuteRandomMatching(communitiesFile, overlapFunctionFile,
                        randomMatchingFactor, onMach2, logger);

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

                MergeSubgraphsImpl.mergeSubgraphs(fileSubgraph1, fileSubgraph2, fileConnectingEdges, logger);

                break;
        }
    }
}