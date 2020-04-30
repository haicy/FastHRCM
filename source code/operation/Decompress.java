package cn.hpc.operation;

import cn.hpc.pojo.MatchEntry;
import cn.hpc.pojo.Sequence;
import cn.hpc.util.MyException;
import cn.hpc.util.MySevenZ;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.ceil;

public class Decompress {
    public static int MAX_SEQ_NUM = 2000;      //maximum sequence number
    public static int MAX_CHA_NUM = 1 << 28;   //maximum length of a chromosome
    public static int PERCENT = 10;    //the percentage of compressed sequence number uses as reference
    public static int kMerLen = 14;    //the length of k-mer
    public static int kMer_bit_num = 2 * kMerLen;  //bit numbers of k-mer
    public static int hashTableLen = 1 << kMer_bit_num;    // length of hash table
    public static int VEC_SIZE = 1 << 20;  //length for other character arrays
    public static int min_rep_len = 15;    //minimum replace length, matched string length exceeds min_rep_len, saved as matched information

    public static int seqNumber;
    public static int secSeqNum;

    public static int refCodeLen, refLowVecLen;
    public static char[] refCode;
    public static int[] refLoc;
    public static int[] refBucket;
    public static int[] refLowVecBegin;
    public static int[] refLowVecLength;

    public static char[] integerEncoding = new char[]{'A', 'C', 'G', 'T'};
    public static List<String> seqPaths;
    public static List<Integer> lineWidthVec;
    public static List<String> identifierVec;
    public static List<List<MatchEntry>> matchResultVec;

    public static BufferedReader br1;
    public static BufferedReader br2;
    public static BufferedWriter bw;

    public static int integerCoding(char ch) {
        switch (ch) {
            case 'A':
                return 0;
            case 'C':
                return 1;
            case 'G':
                return 2;
            case 'T':
                return 3;
            default:
                return -1;
        }
    }

    public static boolean delFile(File file) {
        if (!file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                delFile(f);
            }
        }
        return file.delete();
    }

    public static void mkResultDir(String filePath) {
        File resultDir = new File(getResultPath(filePath));
        if (resultDir.exists()) {
            delFile(resultDir);
        }
        resultDir.mkdir();
    }

    public static String getResultPath(String filePath) {
        String resultDirPath = "";
        String[] info = filePath.split("/");
        for (int i = 0; i < info.length - 1; i++) {
            resultDirPath += info[i];
            resultDirPath += "/";
        }
        resultDirPath += "result";
        resultDirPath += "/";

        return resultDirPath;
    }

    public static int readFile(File pathFile) {
        if (!pathFile.exists()) {
            throw new MyException("fail to open the file and the name is " + pathFile.getAbsolutePath());
        }

        seqPaths = new ArrayList<>(MAX_SEQ_NUM);

        String filePath;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(pathFile));
            while ((filePath = br.readLine()) != null) {
                seqPaths.add(filePath);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return seqPaths.size();
    }

    public static void runLengthDecodingForLineWidth(BufferedReader br) {
        int length = 0;
        String str;
        try {
            str = br.readLine();
            String[] info = str.split("\\s+");

            int[] width = new int[Integer.valueOf(info[0])];
            for (int i = 0; i < width.length; i++) {
                width[i] = Integer.valueOf(info[i + 1]);
            }

            for (int i = 1; i < Integer.valueOf(info[0]); i += 2) {
                length += width[i];
            }
            if (length > 0) {
                lineWidthVec = new ArrayList<>(length);
                for (int i = 0; i < width.length; i += 2) {
                    for (int j = 0; j < width[i + 1]; j++) {
                        lineWidthVec.add(width[i]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getIdentifier(BufferedReader br) {
        String info;
        try {
            while ((info = br.readLine()) != null) {
                identifierVec.add(info);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //构建hash表
    public static void kMerHashingConstruct() {
        refLoc = new int[MAX_CHA_NUM];
        refBucket = new int[hashTableLen];
        for (int i = 0; i < hashTableLen; i++) {
            refBucket[i] = -1;
        }

        int value = 0;
        int stepLen = refCodeLen - kMerLen + 1;

        for (int i = kMerLen - 1; i >= 0; i--) {
            value <<= 2;
            value += integerCoding(refCode[i]);
        }
        refLoc[0] = refBucket[value];
        refBucket[value] = 0;

        int shiftBitNum = (kMerLen * 2 - 2);
        for (int i = 1; i < stepLen; i++) {
            value >>= 2;
            value += (integerCoding(refCode[i + kMerLen - 1])) << shiftBitNum;
            refLoc[i] = refBucket[value];
            refBucket[value] = i;
        }
    }

    public static void referenceSequenceExtraction(String referencePath) {
        File file = new File(referencePath);
        if (!file.exists()) {
            throw new MyException("fail to open the file and the name is " + file.getAbsolutePath());
        }

        refCode = new char[MAX_CHA_NUM];
        refCodeLen = 0;
        refLowVecBegin = new int[VEC_SIZE];
        refLowVecLength = new int[VEC_SIZE];
        refLowVecLen = 1;

        int lettersLen = 0;
        String info;
        char ch;
        Boolean flag = true;

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            br.readLine();

            while ((info = br.readLine()) != null) {
                for (int i = 0; i < info.length(); i++) {
                    ch = info.charAt(i);

                    if (Character.isLowerCase(ch)) {
                        if (flag) {
                            flag = false;
                            refLowVecBegin[refLowVecLen] = lettersLen;
                            lettersLen = 0;
                        }
                        ch = Character.toUpperCase(ch);
                    } else {
                        if (!flag) {
                            flag = true;
                            refLowVecLength[refLowVecLen++] = lettersLen;
                            lettersLen = 0;
                        }
                    }

                    if (ch == 'A' || ch == 'C' || ch == 'G' || ch == 'T') {
                        refCode[refCodeLen++] = ch;
                    }

                    lettersLen++;
                }
            }

            if (!flag) {
                refLowVecLength[refLowVecLen++] = lettersLen;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void readOtherData(Sequence sequence) {
        try {
            String[] info = br1.readLine().split("\\s+");
            int flag = Integer.valueOf(info[0]);
            if (flag == 0) {
                int seqLowVecLen = Integer.valueOf(info[1]);
                int k = 0;
                int[] seqLowVecBegin = new int[seqLowVecLen];
                int[] seqLowVecLength = new int[seqLowVecLen];
                for (int i = 2; i < 2 + seqLowVecLen * 2; i++) {
                    if (i % 2 == 0) {
                        seqLowVecBegin[k] = Integer.valueOf(info[i]);
                    } else {
                        seqLowVecLength[k++] = Integer.valueOf(info[i]);
                    }
                }
                sequence.setLowVecLen(seqLowVecLen);
                sequence.setLowVecBegin(seqLowVecBegin);
                sequence.setLowVecLength(seqLowVecLength);

                int seqNVecLen = Integer.valueOf(info[2 + seqLowVecLen * 2]);
                k = 0;
                int[] seqNVecBegin = new int[seqNVecLen];
                int[] seqNVecLength = new int[seqNVecLen];
                for (int i = 3 + seqLowVecLen * 2; i < 3 + seqLowVecLen * 2 + seqNVecLen * 2; i++) {
                    if (i % 2 == 1) {
                        seqNVecBegin[k] = Integer.valueOf(info[i]);
                    } else {
                        seqNVecLength[k++] = Integer.valueOf(info[i]);
                    }
                }
                sequence.setNVecLen(seqNVecLen);
                sequence.setNVecBegin(seqNVecBegin);
                sequence.setNVecLength(seqNVecLength);

                int seqSpeChaLen = Integer.valueOf(info[3 + seqLowVecLen * 2 + seqNVecLen * 2]);
                k = 0;
                if (seqSpeChaLen > 0) {
                    int[] seqSpeChaPos = new int[seqSpeChaLen];
                    int[] seqSpeChaCh = new int[seqSpeChaLen];
                    for (int i = 4 + seqLowVecLen * 2 + seqNVecLen * 2; i < info.length; i++) {
                        if (i % 2 == 0) {
                            seqSpeChaPos[k] = Integer.valueOf(info[i]);
                        } else {
                            seqSpeChaCh[k++] = Integer.valueOf(info[i]);
                        }
                    }
                    sequence.setSpeChaPos(seqSpeChaPos);
                    sequence.setSpeChaCh(seqSpeChaCh);
                }
                sequence.setSpeChaLen(seqSpeChaLen);
            } else {
                int len = Integer.valueOf(info[1]);
                List<Integer> vec = new ArrayList<>(VEC_SIZE);
                for (int i = 2; i < 2 + len; i++) {
                    vec.add(Integer.valueOf(info[i]));
                }
                int seqLowVecLen = 0;
                for (int i = 1; i < vec.size(); i += 2) {
                    seqLowVecLen += vec.get(i);
                }
                int[] seqLowVecMatched = new int[seqLowVecLen];
                int k = 0;
                for (int i = 0; i < vec.size(); i += 2) {
                    for (int j = 0; j < vec.get(i + 1); j++) {
                        seqLowVecMatched[k++] = vec.get(i) + j;
                    }
                }
                int seqDiffLowVecLen = Integer.valueOf(info[2 + len]);
                k = 0;
                int[] seqDiffLowVecBegin = new int[seqDiffLowVecLen];
                int[] seqDiffLowVecLength = new int[seqDiffLowVecLen];
                for (int i = 3 + len; i < 3 + len + seqDiffLowVecLen * 2; i++) {
                    if (i % 2 == 1) {
                        seqDiffLowVecBegin[k] = Integer.valueOf(info[i]);
                    } else {
                        seqDiffLowVecLength[k++] = Integer.valueOf(info[i]);
                    }
                }
                k = 0;
                int[] seqLowVecBegin = new int[VEC_SIZE];
                int[] seqLowVecLength = new int[VEC_SIZE];
                for (int i = 0, j = 0; i < seqLowVecLen; i++) {
                    k = seqLowVecMatched[i];
                    if (k == 0) {
                        seqLowVecBegin[i] = seqDiffLowVecBegin[j];
                        seqLowVecLength[i] = seqDiffLowVecLength[j++];
                    } else {
                        seqLowVecBegin[i] = refLowVecBegin[k];
                        seqLowVecLength[i] = refLowVecLength[k];
                    }
                }
                sequence.setLowVecLen(seqLowVecLen);
                sequence.setLowVecBegin(seqLowVecBegin);
                sequence.setLowVecLength(seqLowVecLength);

                int seqNVecLen = Integer.valueOf(info[3 + len + seqDiffLowVecLen * 2]);
                k = 0;
                int[] seqNVecBegin = new int[seqNVecLen];
                int[] seqNVecLength = new int[seqNVecLen];
                for (int i = 4 + len + seqDiffLowVecLen * 2; i < 4 + len + seqDiffLowVecLen * 2 + seqNVecLen * 2; i++) {
                    if (i % 2 == 0) {
                        seqNVecBegin[k] = Integer.valueOf(info[i]);
                    } else {
                        seqNVecLength[k++] = Integer.valueOf(info[i]);
                    }
                }
                sequence.setNVecLen(seqNVecLen);
                sequence.setNVecBegin(seqNVecBegin);
                sequence.setNVecLength(seqNVecLength);

                int seqSpeChaLen = Integer.valueOf(info[4 + len + seqDiffLowVecLen * 2 + seqNVecLen * 2]);
                k = 0;
                if (seqSpeChaLen > 0) {
                    int[] seqSpeChaPos = new int[seqSpeChaLen];
                    int[] seqSpeChaCh = new int[seqSpeChaLen];
                    for (int i = 5 + len + seqDiffLowVecLen * 2 + seqNVecLen * 2; i < info.length; i++) {
                        if (i % 2 == 1) {
                            seqSpeChaPos[k] = Integer.valueOf(info[i]);
                        } else {
                            seqSpeChaCh[k++] = Integer.valueOf(info[i]);
                        }
                    }
                    sequence.setSpeChaPos(seqSpeChaPos);
                    sequence.setSpeChaCh(seqSpeChaCh);
                }
                sequence.setSpeChaLen(seqSpeChaLen);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getMatchResult(int seqId, int pos, int length, List<MatchEntry> matchResult) {
        for (int i = 0; i < length; i++) {
            matchResult.add(matchResultVec.get(seqId).get(pos++));
        }
    }

    public static void readMatchResult(Sequence sequence) {
        List<MatchEntry> matchResult = new ArrayList<>(VEC_SIZE);
        MatchEntry me = new MatchEntry();
        String str;
        String[] info;
        int seqId, pos, length, preSeqId = 0, prePos = 0;
        try {
            while (!(str = br1.readLine()).equals("")) {
                info = str.split("\\s+");
                if (info.length == 3) {
                    seqId = Integer.valueOf(info[0]);
                    pos = Integer.valueOf(info[1]);
                    length = Integer.valueOf(info[2]);
                    seqId += preSeqId;
                    preSeqId = seqId;
                    pos += prePos;
                    length += 2;
                    prePos = pos + length;
                    getMatchResult(seqId, pos, length, matchResult);
                } else if (info.length == 2) {
                    pos = Integer.valueOf(info[0]);
                    length = Integer.valueOf(info[1]);
                    me.setPos(pos);
                    me.setLength(length);
                    matchResult.add(me);
                    me = new MatchEntry();
                } else if (info.length == 1) {
                    me.setMisStr(str);
                }
            }
            sequence.setMatchResult(matchResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readTargetSequenceCode(Sequence sequence) {
        List<MatchEntry> matchResult = sequence.getMatchResult();
        int pos, prePos = 0, curPos, length, seqCodeLen = 0, strLen;
        String misStr;
        char[] code = new char[MAX_CHA_NUM];
        int codeLen = 0;
        for (int i = 0; i < matchResult.size(); i++) {
            pos = matchResult.get(i).getPos();
            curPos = pos + prePos;
            length = matchResult.get(i).getLength() + min_rep_len;
            prePos = curPos + length;
            misStr = matchResult.get(i).getMisStr();
            for (int j = 0; j < misStr.length(); j++) {
                code[codeLen++] = integerEncoding[Integer.valueOf(misStr.charAt(j)) - 48];
            }
            for (int m = curPos, n = 0; n < length; m++, n++) {
                code[codeLen++] = refCode[m];
            }
        }
        sequence.setCodeLen(codeLen);
        sequence.setCode(code);
    }

    private static void saveSequenceFile(Sequence sequence, int seqNum, String filePath) {
        String[] split = seqPaths.get(seqNum).split("/");
        File file = new File(getResultPath(filePath) + split[split.length - 1]);
        if (file.exists()) {
            file.delete();
        }
        int codeLen = sequence.getCodeLen();
        int lowVecLen = sequence.getLowVecLen();
        int nVecLen = sequence.getNVecLen();
        int speChaLen = sequence.getSpeChaLen();
        int lineWidth = sequence.getLineWidth();

        int oLen = 0, nLen = 0;
        char[] tempSeq = new char[MAX_CHA_NUM];
        char[] seq = sequence.getCode();
        for (int i = 0; i < speChaLen; i++) {
            while (oLen < sequence.getSpeChaPos()[i] && oLen < codeLen) {
                tempSeq[nLen++] = seq[oLen++];
            }
            tempSeq[nLen++] = (char) (sequence.getSpeChaCh()[i] + 'A');
        }
        while (oLen < codeLen) {
            tempSeq[nLen++] = seq[oLen++];
        }

        int len = 0, r = 0;
        char[] codeWithN = new char[MAX_CHA_NUM];
        for (int i = 0; i < nVecLen; i++) {
            for (int j = 0; j < sequence.getNVecBegin()[i]; j++) {
                codeWithN[len++] = tempSeq[r++];
            }
            for (int j = 0; j < sequence.getNVecLength()[i]; j++) {
                codeWithN[len++] = 'N';
            }
        }
        while (r < nLen) {
            codeWithN[len++] = tempSeq[r++];
        }

        int k = 0;
        for (int i = 0; i < lowVecLen; i++) {
            k += sequence.getLowVecBegin()[i];
            int temp = sequence.getLowVecLength()[i];
            for (int j = 0; j < temp; j++) {
                codeWithN[k] = Character.toLowerCase(codeWithN[k]);
                k++;
            }
        }

        try {
            bw = new BufferedWriter(new FileWriter(file, true));
            bw.write(sequence.getIdentifier() + "\n");
            for (int i = 0; i < len; i += lineWidth) {
                for (int j = i; j < i + lineWidth; j++) {
                    if (j < len) {
                        bw.write(codeWithN[j]);
                    }
                }
                bw.write("\n");
                bw.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void decompress(String filePath) {
        if (seqNumber > 1) {
            MySevenZ.decompress(filePath.replaceAll("txt", "7z"), new File(filePath).getParent());
            String hrcmPath = filePath.replaceAll("txt", "") + "hrcm";    //stored the base of genomes
            String descPath = filePath.replaceAll("txt", "") + "desc";    //stored the lineWidth and identifier
            File hrcmFile = new File(hrcmPath);
            if (!hrcmFile.exists()) {
                throw new MyException("hrcm file does not exists");
            }
            File descFile = new File(descPath);
            if (!descFile.exists()) {
                throw new MyException("desc file does not exists");
            }

            try {
                br1 = new BufferedReader(new FileReader(hrcmPath));
                br2 = new BufferedReader(new FileReader(descPath));

                runLengthDecodingForLineWidth(br2);
                getIdentifier(br2);

                referenceSequenceExtraction(seqPaths.get(0));
                //kMerHashingConstruct();

                Sequence sequence = new Sequence();
                mkResultDir(filePath);
                for (int i = 1; i < seqNumber; i++) {
                    sequence.setIdentifier(identifierVec.get(i - 1));
                    sequence.setLineWidth(lineWidthVec.get(i - 1));
                    readOtherData(sequence);

                    readMatchResult(sequence);
                    if (i <= secSeqNum && i != seqNumber - 1) {
                        matchResultVec.add(sequence.getMatchResult());
                    }
                    readTargetSequenceCode(sequence);
                    saveSequenceFile(sequence, i, filePath);
                    sequence = new Sequence();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (br2 != null) {
                        br2.close();
                    }
                    if (br1 != null) {
                        br1.close();
                    }
                    if (bw != null) {
                        bw.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            delFile(hrcmFile);
            delFile(descFile);
        } else {
            throw new MyException("Error: There is not any to-be-decompressed sequence, nothing to be done.\n");
        }
    }

    public static void initial(File pathFile) {
        if (!pathFile.exists()) {
            throw new MyException("fail to open the file that records the path of all stream files");
        }

        seqNumber = readFile(pathFile);
        secSeqNum = (int) ceil((double) (PERCENT * seqNumber) / 100);

        identifierVec = new ArrayList<>(seqNumber);
        matchResultVec = new ArrayList<>(secSeqNum);
    }
}
