package mips;

public class MipsCode {
    MipsOp op;
    String r = null;
    String a1 = null;
    String a2 = null;
    int num;

    public MipsCode(MipsOp op, String r) {
        this.op = op;
        this.r = r;
    }

    public MipsCode(MipsOp op, String r, String a1) {
        this.op = op;
        this.r = r;
        this.a1 = a1;
    }

    public MipsCode(MipsOp op, String r, String a1, String a2) {
        this.op = op;
        this.r = r;
        this.a1 = a1;
        this.a2 = a2;
    }

    public MipsCode(MipsOp op, String r, String a1, String a2, int num) {
        this.op = op;
        this.r = r;
        this.a1 = a1;
        this.a2 = a2;
        this.num = num;
    }
}
