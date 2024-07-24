package edu.plus.cs.io;

import edu.plus.cs.util.Constants;
import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FunctionOutputWriter {

    /**
     * Writes the contents of the hGenerated matrix to a file in the specified format.
     *
     * @param hGenerated The matrix containing the overlap function data.
     * @param onMach2 Indicates if the application runs locally or on the mach2 cluster.
     */
    public static void writeToOutputFile(int[][] hGenerated, String prefix, boolean onMach2, Logger logger) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");
        String timestamp = now.format(formatter);

        String outputPath = "output_" + prefix + timestamp + ".txt";
        if (onMach2) {
            outputPath = Constants.MACH2_DIR_PREFIX + outputPath;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            for (int communitySize = 0; communitySize < hGenerated.length; communitySize++) {
                for (int overlapSize = 0; overlapSize < hGenerated[communitySize].length; overlapSize++) {
                    int value = hGenerated[communitySize][overlapSize];

                    // Only write non-zero values to reduce file size
                    if (value > 0) {
                        writer.write(overlapSize + ", " + communitySize + ", " + value);
                        writer.newLine();
                    }
                }
            }

            logger.log("Finished writing into file", LogLevel.DEBUG, outputPath);
        } catch (IOException e) {
            logger.log("Error writing to the output file: " + e.getMessage(), LogLevel.ERROR, outputPath);
        }
    }
}
