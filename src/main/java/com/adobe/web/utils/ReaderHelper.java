package com.adobe.web.utils;

import java.io.*;
import java.nio.channels.FileChannel;

public class ReaderHelper {

    /**
     * This will delete the boundary from the file dumped
     * @param f File object
     * @param boundary boundary character
     * @throws IOException Exception in case of input/output failures
     */
    public static void DeleteBoundaryFromFile(File f, String boundary)
            throws IOException {
        FileChannel outChan = null;
        FileOutputStream file = null;
        try {
            file = new FileOutputStream(f, true);
            outChan = file.getChannel();
            long newSize = f.length() - boundary.length();
            outChan.truncate(newSize);
        } finally {
            if (outChan != null)
                outChan.close();
            if (file != null)
                file.close();
        }
    }
}
