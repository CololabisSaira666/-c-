package generateMid;

public class MidCodeEle {
    private EleType eleType;
    private int pos;
    private int num = 0; // 类型为 NUM 时所存储的值 ？

    public MidCodeEle() {}

    public MidCodeEle(EleType eleType) {
        this.eleType = eleType;
    }
    public void setEleType(EleType eleType) {
        this.eleType = eleType;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public int getNum() {
        return this.num;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }
}
