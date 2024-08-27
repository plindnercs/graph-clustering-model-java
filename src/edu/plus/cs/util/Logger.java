package edu.plus.cs.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    String path;

    File loggingFile;

    public Logger(String path)  {
        this.path = path;
        this.loggingFile = new File(path);
    }

    /**
     * Writes a log message to file and output
     * @param message Message to be logged
     * @param logLevel The type of log message
     * @param objects Objects which will be written to log file
     */
    public void log(String message, LogLevel logLevel, Object... objects) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getCurrentTimestamp());
        stringBuilder.append(" (");
        stringBuilder.append(logLevel.toString());
        stringBuilder.append("): ");
        stringBuilder.append(message);

        if (objects.length > 0) {
            stringBuilder.append(" [");

            for (int i = 0; i < objects.length; i++) {
                stringBuilder.append(objects[i].toString());

                if (i < objects.length - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append("]");
        }

        writeToFile(stringBuilder.toString());
        if (LogLevel.ERROR.equals(logLevel)) {
            System.err.println(stringBuilder.toString());
        } else {
            System.out.println(stringBuilder.toString());
        }
    }

    private void writeToFile(String logString) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(loggingFile, true);
            fileWriter.write(logString);
            fileWriter.write(System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Could not log to file: " + loggingFile.getName());
            System.err.println("Should hava logged: " + logString);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    System.err.println("Could not close fileWriter on file: " + loggingFile.getName());
                }
            }
        }
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy:HH:mm:ss.SSS");
        Date now = new Date();
        return formatter.format(now);
    }

    public static String createLoggingFileName(boolean onMach2) {
        StringBuilder stringBuilder = new StringBuilder();

        if (onMach2) {
            stringBuilder.append(Constants.MACH2_DIR_PREFIX);
        }
        stringBuilder.append("graph_clustering_model_log_");

        // add timestamp to logging file name
        SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss_SSS");
        Date now = new Date();
        stringBuilder.append(formatter.format(now));

        stringBuilder.append(".log");

        return stringBuilder.toString();
    }
}
