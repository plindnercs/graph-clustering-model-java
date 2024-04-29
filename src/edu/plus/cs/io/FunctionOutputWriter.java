package edu.plus.cs.io;

import edu.plus.cs.Main;

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
    public static void writeToOutputFile(int[][] hGenerated, boolean onMach2) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);

        String outputPath = "output_" + timestamp + ".txt";
        if (onMach2) {
            outputPath = Main.MACH2_DIR_PREFIX + outputPath;
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
        } catch (IOException e) {
            System.err.println("Error writing to the output file: " + e.getMessage());
        }
    }
}
