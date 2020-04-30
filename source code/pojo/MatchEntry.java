package cn.hpc.pojo;

public class MatchEntry {
    private int pos;
    private int length;
    private String misStr;

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getMisStr() {
        return misStr;
    }

    public void setMisStr(String misStr) {
        this.misStr = misStr;
    }

    @Override
    public String toString() {
        return "MatchEntry{" +
                "pos=" + pos +
                ", length=" + length +
                ", misStr='" + misStr + '\'' +
                '}';
    }
}
