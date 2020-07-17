package cn.hpc;

import cn.hpc.operation.Compress;
import cn.hpc.operation.Decompress;

import java.io.File;

public class FastHRCMApplication {

    public static void main(String[] args) {
        String[] myArgs = new String[3];
        myArgs[0] = args[0];
        myArgs[1] = args[1];
        if (args.length == 2) {
            myArgs[2] = "4";
        } else {
            myArgs[2] = args[2];
        }
        String path = myArgs[0];
        File pathFile = new File(path);
        int poolSize = Integer.valueOf(myArgs[2]);

        if (myArgs[1].equals("compress")) {
            Compress.initial(pathFile);
            Long startCompressTime = System.currentTimeMillis();
            Compress.compress(pathFile.getAbsolutePath(), poolSize);
            Compress.sevenZip(path);
            Long endCompressTime = System.currentTimeMillis();
            System.out.println("Info: Compression complete. Compression time: " + (endCompressTime - startCompressTime) / 1000 + "s.");
        } else if (myArgs[1].equals("decompress")) {
            Decompress.initial(pathFile);
            Long startDecompressTime = System.currentTimeMillis();
            Decompress.decompress(pathFile.getAbsolutePath());
            Long endDecompressTime = System.currentTimeMillis();
            System.out.println("Info: Decompression complete. Decompression time: " + (endDecompressTime - startDecompressTime) / 1000 + "s.");
        } else {
            System.out.println("Error: Please specify the execution mode, compress for compression and decompress for decompression");
        }
    }
}
