package util;

import pojo.Sequence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static operation.Compress.*;

public class ExtractThread extends Thread {
    private int seqNum;
    private String tmpDirectoryPath;

    public ExtractThread(int seqNum, String tmpDirectoryPath) {
        this.seqNum = seqNum;
        this.tmpDirectoryPath = tmpDirectoryPath;
    }

    @Override
    public void run() {
        Sequence sequence = new Sequence();
        System.out.println("Info: Compressing the sequence ID: "+ seqNum);
        targetSequenceExtraction(seqNum, sequence);

        if (sequence.getLowVecLen() > 0 && refLowVecLen > 0) {
            seqLowercaseMatching(sequence);
        }
        codeFirstMatch(sequence);

        if (seqNum <= secSeqNum) {
            matchResultVec.add(sequence.getMatchResult());
            matchResultHashConstruct(sequence.getMatchResult());
        }

        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter(tmpDirectoryPath + "result-" + seqNum));
            saveOtherData(bw, sequence);
            if (seqNum != 1) {
                if (sequence.getMatchResult().size() < 3) {
                    saveFirstMatchResult(bw, sequence.getMatchResult());
                }
                codeSecondMatch(bw, sequence.getMatchResult(), seqNum);
            } else {
                saveFirstMatchResult(bw, sequence.getMatchResult());
            }

            sequence.setMatchResult(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
