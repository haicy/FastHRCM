package operation;

import pojo.MatchEntry;
import pojo.Sequence;
import util.ExtractThread;
import util.MyException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Compress {
    public static int MAX_SEQ_NUM = 2000;                   //maximum sequence number
    public static int MAX_CHA_NUM = 1 << 28;                //maximum length of a chromosome
    public static int PERCENT = 25;                         //the percentage of compressed sequence number uses as reference
    public static int kMerLen = 14;                         //the length of k-mer
    public static int kMer_bit_num = 2 * kMerLen;           //bit numbers of k-mer
    public static int hashTableLen = 1 << kMer_bit_num;     // length of hash table
    public static int VEC_SIZE = 1 << 20;                   //length for other character arrays
    public static int min_rep_len = 15;                     //minimum replace length, matched string length exceeds min_rep_len, saved as matched information

    public static int seqNumber, seqBucketLen;
    public static int secSeqNum;                          //the referenced sequence number used for second compress

    public static int refCodeLen, refLowVecLen;
    public static char[] refCode;
    public static int[] refLoc;
    public static int[] refBucket;
    public static int[] refLowVecBegin;
    public static int[] refLowVecLength;

    public static List<String> seqPaths;
    public static String[] identifierVec;
    public static int[] lineWidthVec;
    public static List<List<MatchEntry>> matchResultVec;
    public static List<int[]> seqBucketVec;
    public static List<List<Integer>> seqLocVec;

    public static void initial(File pathFile) {
        if (!pathFile.exists()) {
            throw new MyException("fail to open the file that records the path of all genome files");
        }

        seqNumber = readFile(pathFile);
        secSeqNum = (int) Math.ceil((double) (PERCENT * seqNumber) / 100);
        System.out.println("Info: PERCENT is " + PERCENT + ", secSeqNum is " + secSeqNum);
        seqBucketLen = getNextPrime(VEC_SIZE);

        identifierVec = new String[seqNumber];
        lineWidthVec = new int[seqNumber];
        seqBucketVec = new ArrayList<>(seqNumber);
        seqLocVec = new ArrayList<>(seqNumber);
        matchResultVec = new ArrayList<>(secSeqNum);
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

    public static int getNextPrime(int number) {
        int cur = number + 1;
        Boolean prime = false;
        while (!prime) {
            prime = true;
            for (int i = 2; i < Math.sqrt(number) + 1; i++) {
                if (cur % i == 0) {
                    prime = false;
                    break;
                }
            }

            if (!prime) {
                cur++;
            }
        }

        return cur;
    }

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

    private static String getTmpDirectoryPath(String path) {
        String result = "";

        String[] info = path.split("/");
        for (int i = 0; i < info.length - 1; i++) {
            result += info[i];
            result += "/";
        }

        File tmpDirectory = new File(result + "tmp/");
        if (!tmpDirectory.exists()) {
            tmpDirectory.mkdir();
        }

        return result + "tmp/";
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

    public static void targetSequenceExtraction(int seqNum, Sequence sequence) {
        String path = seqPaths.get(seqNum);
        File file = new File(path);
        if (!file.exists()) {
            throw new MyException("fail to open the file and the name is " + file.getAbsolutePath());
        }

        int seqCodeLen = 0;
        int seqLowVecLen = 0;
        int seqNVecLen = 0;
        int speChaLen = 0;
        char[] seqCode = new char[MAX_CHA_NUM];
        int[] seqLowVecBegin = new int[VEC_SIZE];
        int[] seqLowVecLength = new int[VEC_SIZE];
        int[] seqNVecBegin = new int[VEC_SIZE];
        int[] seqNVecLength = new int[VEC_SIZE];
        int[] speChaPos = new int[VEC_SIZE];
        int[] speChaCh = new int[VEC_SIZE];

        String info;
        char ch;
        int lettersLen = 0, nLettersLen = 0;
        Boolean flag = true, nFlag = false;

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            identifierVec[seqNum] = br.readLine();

            br.mark(1000);
            lineWidthVec[seqNum] = br.readLine().length();
            br.reset();

            while ((info = br.readLine()) != null) {
                for (int i = 0; i < info.length(); i++) {
                    ch = info.charAt(i);

                    if (Character.isLowerCase(ch)) {
                        if (flag) {
                            flag = false;
                            seqLowVecBegin[seqLowVecLen] = lettersLen;
                            lettersLen = 0;
                        }
                        ch = Character.toUpperCase(ch);
                    } else {
                        if (!flag) {
                            flag = true;
                            seqLowVecLength[seqLowVecLen++] = lettersLen;
                            lettersLen = 0;
                        }
                    }
                    lettersLen++;

                    if (ch == 'A' || ch == 'C' || ch == 'G' || ch == 'T') {
                        seqCode[seqCodeLen++] = ch;
                    } else if (ch != 'N') {
                        speChaPos[speChaLen] = seqCodeLen;
                        speChaCh[speChaLen++] = ch - 'A';
                    }

                    if (!nFlag) {
                        if (ch == 'N') {
                            seqNVecBegin[seqNVecLen] = nLettersLen;
                            nLettersLen = 0;
                            nFlag = true;
                        }
                    } else {
                        if (ch != 'N') {
                            seqNVecLength[seqNVecLen++] = nLettersLen;
                            nLettersLen = 0;
                            nFlag = false;
                        }
                    }
                    nLettersLen++;
                }
            }

            if (!flag) {
                seqLowVecLength[seqLowVecLen++] = lettersLen;
            }

            if (nFlag) {
                seqNVecLength[seqNVecLen++] = nLettersLen;
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

        sequence.setCode(seqCode);
        sequence.setLowVecBegin(seqLowVecBegin);
        sequence.setLowVecLength(seqLowVecLength);
        sequence.setNVecBegin(seqNVecBegin);
        sequence.setNVecLength(seqNVecLength);
        sequence.setSpeChaPos(speChaPos);
        sequence.setSpeChaCh(speChaCh);
        sequence.setCodeLen(seqCodeLen);
        sequence.setLowVecLen(seqLowVecLen);
        sequence.setNVecLen(seqNVecLen);
        sequence.setSpeChaLen(speChaLen);
    }

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

    public static int getHashValue(MatchEntry me) {
        int result = 0;
        for (int i = 0; i < me.getMisStr().length(); i++) {
            result += me.getMisStr().charAt(i) * 92083;
        }
        result += me.getPos() * 69061 + me.getLength() * 51787;
        result %= seqBucketLen;
        return result;
    }

    public static void matchResultHashConstruct(List<MatchEntry> matchResult) {
        int hashValue1, hashValue2, hashValue;
        List<Integer> seqLoc = new ArrayList<>(VEC_SIZE);
        int[] seqBucket = new int[seqBucketLen];
        for (int i = 0; i < seqBucketLen; i++) {
            seqBucket[i] = -1;
        }

        hashValue1 = getHashValue(matchResult.get(0));
        if (matchResult.size() < 2) {
            hashValue2 = 0;
        } else {
            hashValue2 = getHashValue(matchResult.get(1));
        }
        hashValue = Math.abs(hashValue1 + hashValue2) % seqBucketLen;
        seqLoc.add(seqBucket[hashValue]);
        seqBucket[hashValue] = 0;

        for (int i = 1; i < matchResult.size() - 1; i++) {
            hashValue1 = hashValue2;
            hashValue2 = getHashValue(matchResult.get(i + 1));
            hashValue = Math.abs(hashValue1 + hashValue2) % seqBucketLen;
            seqLoc.add(seqBucket[hashValue]);
            seqBucket[hashValue] = i;
        }
        seqLocVec.add(seqLoc);
        seqBucketVec.add(seqBucket);
    }

    public static void seqLowercaseMatching(Sequence sequence) {
        int[] seqLowVecMatched = new int[VEC_SIZE];
        int seqDiffLowVecLen = 0;
        int[] seqDiffLowVecBegin = new int[VEC_SIZE];
        int[] seqDiffLowVecLength = new int[VEC_SIZE];

        int startPosition = 1;
        int[] seqLowVecBegin = sequence.getLowVecBegin();
        int[] seqLowVecLength = sequence.getLowVecLength();

        for (int i = 0; i < sequence.getLowVecLen(); i++) {
            for (int j = startPosition; j < refLowVecLen; j++) {
                if ((seqLowVecBegin[i] == refLowVecBegin[j]) && (seqLowVecLength[i] == refLowVecLength[j])) {
                    seqLowVecMatched[i] = j;
                    startPosition = j + 1;
                    break;
                }
            }
            if (seqLowVecMatched[i] == 0) {
                for (int j = startPosition - 1; j > 0; j--) {
                    if ((seqLowVecBegin[i] == refLowVecBegin[j]) && (seqLowVecLength[i] == refLowVecLength[j])) {
                        seqLowVecMatched[i] = j;
                        startPosition = j + 1;
                        break;
                    }
                }
            }

            if (seqLowVecMatched[i] == 0) {
                seqDiffLowVecBegin[seqDiffLowVecLen] = sequence.getLowVecBegin()[i];
                seqDiffLowVecLength[seqDiffLowVecLen++] = sequence.getLowVecLength()[i];
            }
        }

        sequence.setLowVecMatched(seqLowVecMatched);
        sequence.setDiffLowVecBegin(seqDiffLowVecBegin);
        sequence.setDiffLowVecLength(seqDiffLowVecLength);
        sequence.setDiffLowVecLen(seqDiffLowVecLen);
    }

    private static void codeMatch(String tmpDirectoryPath, int poolSize) {
        ExecutorService executorServiceFirst = Executors.newSingleThreadExecutor();
        long startTime = System.currentTimeMillis();
        for (int i = 1; i <= secSeqNum; i++) {
            executorServiceFirst.execute(new ExtractThread(i, tmpDirectoryPath));
        }
        executorServiceFirst.shutdown();
        while (!executorServiceFirst.isTerminated()) {

        }
//        System.out.println("The time that read and write secSeqNum genome files is: " + (System.currentTimeMillis() - startTime) + "ms\n");

        ExecutorService executorServiceLater = Executors.newFixedThreadPool(poolSize);
        for (int i = secSeqNum + 1; i < seqNumber; i++) {
            executorServiceLater.execute(new ExtractThread(i, tmpDirectoryPath));
        }
        executorServiceLater.shutdown();
        while (!executorServiceLater.isTerminated()) {

        }
    }

    public static void codeFirstMatch(Sequence sequence) {
        char[] seqCode = sequence.getCode();
        int seqCodeLen = sequence.getCodeLen();

        int prePos = 0;
        int step_len = seqCodeLen - kMerLen + 1;
        int i, j, id, k, refIdx, tarIdx, length, tarValue, maxLength, maxK;
        MatchEntry matchEntry = new MatchEntry();
        List<MatchEntry> matchResult = new ArrayList<>(VEC_SIZE);

        String mismatchedStr = String.valueOf(integerCoding(seqCode[0]));
        for (i = 1; i < step_len; i++) {
            tarValue = 0;
            for (j = kMerLen - 1; j >= 0; j--) {
                tarValue <<= 2;
                tarValue += integerCoding(seqCode[i + j]);
            }
            id = refBucket[tarValue];

            if (id > -1) {
                maxLength = -1;
                maxK = -1;

                for (k = id; k != -1; k = refLoc[k]) {
                    refIdx = k + kMerLen;
                    tarIdx = i + kMerLen;
                    length = kMerLen;

                    while (refIdx < refCodeLen && tarIdx < seqCodeLen && refCode[refIdx++] == seqCode[tarIdx++]) {
                        length++;
                    }

                    if (length >= min_rep_len && length > maxLength) {
                        maxLength = length;
                        maxK = k;
                    }
                }

                if (maxLength > -1) {
                    matchEntry.setMisStr(mismatchedStr);
                    matchEntry.setPos(maxK - prePos);
                    matchEntry.setLength(maxLength - min_rep_len);
                    matchResult.add(matchEntry);
                    matchEntry = new MatchEntry();

                    i += maxLength;
                    prePos = maxK + maxLength;
                    mismatchedStr = "";
                    if (i < seqCodeLen) {
                        mismatchedStr += String.valueOf(integerCoding(seqCode[i]));
                    }
                    continue;
                }
            }
            mismatchedStr += String.valueOf(integerCoding(seqCode[i]));
        }

        if (i < seqCodeLen) {
            for (; i < seqCodeLen; i++) {
                mismatchedStr += String.valueOf(integerCoding(seqCode[i]));
            }
            matchEntry.setPos(0);
            matchEntry.setLength(-min_rep_len);
            matchEntry.setMisStr(mismatchedStr);
            matchResult.add(matchEntry);
        }

        sequence.setMatchResult(matchResult);

        sequence.setCodeLen(0);
        sequence.setCode(null);
    }

    public static void saveOtherData(BufferedWriter bw, Sequence sequence) {
        int seqLowVecLen = sequence.getLowVecLen();
        int seqNVecLen = sequence.getNVecLen();
        int seqSpeChaLen = sequence.getSpeChaLen();

        int flag = 0;
        try {
            if (seqLowVecLen > 0 && refLowVecLen > 0) {
                if ((2 * sequence.getDiffLowVecLen()) < seqLowVecLen) {
                    flag = 1;
                    bw.write(flag + " ");
                    runLengthCodingForLowVecMatched(bw, sequence.getLowVecMatched(), seqLowVecLen);
                    savePositionRangeData(bw, sequence.getDiffLowVecLen(), sequence.getDiffLowVecBegin(), sequence.getDiffLowVecLength());
                }
            }
            if (flag == 0) {
                bw.write(flag + " ");
                savePositionRangeData(bw, seqLowVecLen, sequence.getLowVecBegin(), sequence.getLowVecLength());
            }

            sequence.setLowVecLen(0);
            sequence.setLowVecBegin(null);
            sequence.setLowVecLength(null);
            sequence.setLowVecMatched(null);
            sequence.setDiffLowVecLen(0);
            sequence.setDiffLowVecBegin(null);
            sequence.setDiffLowVecLength(null);

            savePositionRangeData(bw, seqNVecLen, sequence.getNVecBegin(), sequence.getNVecLength());

            sequence.setNVecLen(0);
            sequence.setNVecBegin(null);
            sequence.setNVecLength(null);

            savePositionRangeData(bw, seqSpeChaLen, sequence.getSpeChaPos(), sequence.getSpeChaCh());

            sequence.setSpeChaLen(0);
            sequence.setSpeChaPos(null);
            sequence.setSpeChaCh(null);

            bw.write("\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void runLengthCodingForLowVecMatched(BufferedWriter bw, int[] vec, int length) {
        List<Integer> code = new ArrayList<>(VEC_SIZE);

        if (length > 0) {
            code.add(vec[0]);
            int cnt = 1;
            for (int i = 1; i < length; i++) {
                if (vec[i] - vec[i - 1] == 1) {
                    cnt++;
                } else {
                    code.add(cnt);
                    code.add(vec[i]);
                    cnt = 1;
                }
            }
            code.add(cnt);
        }

        int code_len = code.size();
        try {
            bw.write(code_len + " ");
            for (int i = 0; i < code_len; i++) {
                bw.write(code.get(i) + " ");
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void runLengthCodingForLineWidth(BufferedWriter bw, int[] vec, int length) {
        List<Integer> code = new ArrayList<>(VEC_SIZE);

        if (length > 0) {
            code.add(vec[1]);
            int cnt = 1;
            for (int i = 2; i < length; i++) {
                if (vec[i] == vec[i - 1]) {
                    cnt++;
                } else {
                    code.add(cnt);
                    code.add(vec[i]);
                    cnt = 1;
                }
            }
            code.add(cnt);
        }

        int code_len = code.size();
        try {
            bw.write(code_len + " ");
            for (int i = 0; i < code_len; i++) {
                bw.write(code.get(i) + " ");
            }
            bw.write("\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveIdentifierData(BufferedWriter bw, String[] identifierVec) {
        try {
            for (int i = 1; i < identifierVec.length; i++) {
                bw.write(identifierVec[i] + "\n");
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void savePositionRangeData(BufferedWriter bw, int vecLen, int[] vecBegin, int[] vecLength) {
        try {
            bw.write(vecLen + " ");
            for (int i = 0; i < vecLen; i++) {
                bw.write(vecBegin[i] + " " + vecLength[i] + " ");
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveMatchEntry(BufferedWriter bw, MatchEntry matchEntry) {
        try {
            bw.write(matchEntry.getMisStr() + "\n" + matchEntry.getPos() + " " + matchEntry.getLength() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getMatchLength(List<MatchEntry> ref_matchResult, int ref_idx, List<MatchEntry> tar_matchResult, int tar_idx) {
        int length = 0;
        while (ref_idx < ref_matchResult.size() && tar_idx < tar_matchResult.size() && compareMatchEntry(ref_matchResult.get(ref_idx++), tar_matchResult.get(tar_idx++))) {
            length++;
        }
        return length;
    }

    public static Boolean compareMatchEntry(MatchEntry ref, MatchEntry tar) {
        if (ref.getPos() == tar.getPos() && ref.getLength() == tar.getLength() && ref.getMisStr().equals(tar.getMisStr())) {
            return true;
        } else {
            return false;
        }
    }

    public static void saveFirstMatchResult(BufferedWriter bw, List<MatchEntry> mes) {
        try {
            for (int i = 0; i < mes.size(); i++) {
                saveMatchEntry(bw, mes.get(i));
            }

            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void codeSecondMatch(BufferedWriter bw, List<MatchEntry> matchResult, int seqNum) {
        int hashValue;
        int preSeqId = 1;
        int maxPos = 0, prePos = 0, deltaPos, length, maxLength, deltaLength, seqId = 0, deltaSeqId;
        int id, pos;
        int i;

        List<MatchEntry> misMatchEntry = new ArrayList<>();

        try {
            misMatchEntry.add(matchResult.get(0));
            for (i = 1; i < matchResult.size() - 1; i++) {
                hashValue = Math.abs(getHashValue(matchResult.get(i)) + getHashValue(matchResult.get(i + 1))) % seqBucketLen;
                maxLength = 0;
                for (int j = 0; j < Math.min(seqNum - 1, secSeqNum); j++) {
                    id = seqBucketVec.get(j)[hashValue];
                    if (id != -1) {
                        for (pos = id; pos != -1; pos = seqLocVec.get(j).get(pos)) {
                            length = getMatchLength(matchResultVec.get(j), pos, matchResult, i);
                            if (length > 1 && length > maxLength) {
                                seqId = j + 1;
                                maxPos = pos;
                                maxLength = length;
                            }
                        }
                    }
                }

                if (maxLength != 0) {
                    deltaSeqId = seqId - preSeqId;
                    deltaLength = maxLength - 2;
                    deltaPos = maxPos - prePos;
                    preSeqId = seqId;
                    prePos = maxPos + maxLength;

                    for (int j = 0; j < misMatchEntry.size(); j++) {
                        saveMatchEntry(bw, misMatchEntry.get(j));
                    }
                    bw.flush();
                    misMatchEntry = new ArrayList<>();
                    bw.write(deltaSeqId + " " + deltaPos + " " + deltaLength + "\n");

                    i += maxLength - 1;
                } else {
                    misMatchEntry.add(matchResult.get(i));
                }
            }

            if (i == matchResult.size() - 1) {
                misMatchEntry.add(matchResult.get(i));
            }
            for (int j = 0; j < misMatchEntry.size(); j++) {
                saveMatchEntry(bw, misMatchEntry.get(j));
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void compress(String path, int poolSize) {
        String tmpDirectoryPath = getTmpDirectoryPath(path);
        if (seqNumber > 1) {
            File hrcmFile = new File(path.replaceAll(".txt", "") + ".hrcm");
            if (hrcmFile.exists()) hrcmFile.delete();
            File descFile = new File(path.replaceAll(".txt", "") + ".desc");
            if (descFile.exists()) descFile.delete();

            referenceSequenceExtraction(seqPaths.get(0));
            kMerHashingConstruct();

            BufferedReader br = null;
            BufferedWriter bw1 = null;
            BufferedWriter bw2 = null;
            try {
                codeMatch(tmpDirectoryPath, poolSize);

                File file;
                String info;
                bw1 = new BufferedWriter(new FileWriter(hrcmFile, true));
                for (int i = 1; i < seqNumber; i++) {
                    file = new File(tmpDirectoryPath + "result-" + i);
                    br = new BufferedReader(new FileReader(file));
                    while ((info = br.readLine()) != null) {
                        bw1.write(info + "\n");
                    }
                    bw1.write("\n");
                }

                bw2 = new BufferedWriter(new FileWriter(descFile, true));
                runLengthCodingForLineWidth(bw2, lineWidthVec, seqNumber);   //save lineWidth data
                saveIdentifierData(bw2, identifierVec);    //save identifier data

                delFile(new File(tmpDirectoryPath));


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bw1 != null) bw1.close();
                    if (bw2 != null) bw2.close();
                    if (br != null) br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            throw new MyException("Error: There is not any to-be-compressed sequence, nothing to be done.\n");
        }
    }

    /****************************
    BSC compression for *.hrcm and *.desc
    tar and compression command is：
        tar -cf *.tar *.hrcm *.desc
        ./bsc e *.tar *.bsc -e2
    untar and decompression command is：
        ./bsc d *.bsc
        tar -xf *.tar
     *****************************/
    public static void bscCompress(String path) {
        try {
            String fileName = path.replaceAll(".txt", "");
            File hrcmFile = new File(path.replaceAll("txt", "hrcm"));
            File descFile = new File(path.replaceAll("txt", "desc"));
            File tarFile = new File(path.replaceAll("txt", "tar"));
            String tarCommand = "tar -cf " + fileName + ".tar "+ fileName + ".hrcm " + fileName + ".desc";
            Process p1 = Runtime.getRuntime().exec(tarCommand);
            p1.waitFor();
            String bscCommand = "./bsc e " + fileName + ".tar " + fileName + ".bsc -e2";
            Process p2 = Runtime.getRuntime().exec(bscCommand);
            p2.waitFor();

            delFile(hrcmFile);
            delFile(descFile);
            delFile(tarFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
