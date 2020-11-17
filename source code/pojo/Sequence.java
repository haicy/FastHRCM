package pojo;

import java.util.ArrayList;
import java.util.List;

public class Sequence {
    private String identifier;
    private int lineWidth;
    private int codeLen;
    private int lowVecLen;
    private int nVecLen;
    private int speChaLen;
    private int diffLowVecLen;
    private char[] code;
    private int[] lowVecBegin;
    private int[] lowVecLength;
    private int[] nVecBegin;
    private int[] nVecLength;
    private int[] speChaPos;
    private int[] speChaCh;
    private int[] lowVecMatched;
    private int[] diffLowVecBegin;
    private int[] diffLowVecLength;
    private List<MatchEntry> matchResult;

    public Sequence() {
        int MAX_CHA_NUM = 1 << 28;
        int VEC_SIZE = 1 << 20;

        this.identifier = "";   //SequenceExtraction
        this.lineWidth = 0; //SequenceExtraction
        this.codeLen = 0;   //SequenceExtraction
        this.lowVecLen = 0;    //SequenceExtraction
        this.diffLowVecLen = 0;    //seqLowercaseMatching
        this.nVecLen = 0;   //SequenceExtraction
        this.speChaLen = 0; //SequenceExtraction
        this.code = new char[MAX_CHA_NUM];  //SequenceExtraction
        this.lowVecMatched = new int[VEC_SIZE];    //seqLowercaseMatching
        this.lowVecBegin = new int[VEC_SIZE];  //SequenceExtraction
        this.lowVecLength = new int[VEC_SIZE]; //SequenceExtraction
        this.diffLowVecBegin = new int[VEC_SIZE];  //seqLowercaseMatching
        this.diffLowVecLength = new int[VEC_SIZE]; //seqLowercaseMatching
        this.nVecBegin = new int[VEC_SIZE]; //SequenceExtraction
        this.nVecLength = new int[VEC_SIZE];    //SequenceExtraction
        this.speChaPos = new int[VEC_SIZE]; //SequenceExtraction
        this.speChaCh = new int[VEC_SIZE];  //SequenceExtraction
        this.matchResult = new ArrayList<>(VEC_SIZE);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getCodeLen() {
        return codeLen;
    }

    public void setCodeLen(int codeLen) {
        this.codeLen = codeLen;
    }

    public int getLowVecLen() {
        return lowVecLen;
    }

    public void setLowVecLen(int lowVecLen) {
        this.lowVecLen = lowVecLen;
    }

    public int getDiffLowVecLen() {
        return diffLowVecLen;
    }

    public void setDiffLowVecLen(int diffLowVecLen) {
        this.diffLowVecLen = diffLowVecLen;
    }

    public int getNVecLen() {
        return nVecLen;
    }

    public void setNVecLen(int nVecLen) {
        this.nVecLen = nVecLen;
    }

    public int getSpeChaLen() {
        return speChaLen;
    }

    public void setSpeChaLen(int speChaLen) {
        this.speChaLen = speChaLen;
    }

    public char[] getCode() {
        return code;
    }

    public void setCode(char[] code) {
        this.code = code;
    }

    public int[] getLowVecMatched() {
        return lowVecMatched;
    }

    public void setLowVecMatched(int[] lowVecMatched) {
        this.lowVecMatched = lowVecMatched;
    }

    public int[] getLowVecBegin() {
        return lowVecBegin;
    }

    public void setLowVecBegin(int[] lowVecBegin) {
        this.lowVecBegin = lowVecBegin;
    }

    public int[] getLowVecLength() {
        return lowVecLength;
    }

    public void setLowVecLength(int[] lowVecLength) {
        this.lowVecLength = lowVecLength;
    }

    public int[] getNVecBegin() {
        return nVecBegin;
    }

    public void setNVecBegin(int[] nVecBegin) {
        this.nVecBegin = nVecBegin;
    }

    public int[] getNVecLength() {
        return nVecLength;
    }

    public void setNVecLength(int[] nVecLength) {
        this.nVecLength = nVecLength;
    }

    public int[] getSpeChaPos() {
        return speChaPos;
    }

    public void setSpeChaPos(int[] speChaPos) {
        this.speChaPos = speChaPos;
    }

    public int[] getSpeChaCh() {
        return speChaCh;
    }

    public void setSpeChaCh(int[] speChaCh) {
        this.speChaCh = speChaCh;
    }

    public int[] getDiffLowVecBegin() {
        return diffLowVecBegin;
    }

    public void setDiffLowVecBegin(int[] diffLowVecBegin) {
        this.diffLowVecBegin = diffLowVecBegin;
    }

    public int[] getDiffLowVecLength() {
        return diffLowVecLength;
    }

    public void setDiffLowVecLength(int[] diffLowVecLength) {
        this.diffLowVecLength = diffLowVecLength;
    }

    public List<MatchEntry> getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(List<MatchEntry> matchResult) {
        this.matchResult = matchResult;
    }
}
