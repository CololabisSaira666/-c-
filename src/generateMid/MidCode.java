package generateMid;

import tree.Node;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MidCode {
    public MidCodeOp op = null;
    public String arg1 = null;
    public String arg2= null;
    public String result= null;

    public MidCode(MidCodeOp op, String r) {
        this.op = op;
        this.result = r;
    }

    public MidCode(MidCodeOp op) {
        this.op = op;
    }

    public MidCode(MidCodeOp op, String r, String a1) {
        this.op = op;
        this.result = r;
        this.arg1 = a1;
    }

    public MidCode(MidCodeOp op, String r, String a1, String a2) {
        this.op = op;
        this.result = r;
        this.arg1 = a1;
        this.arg2 = a2;
    }

    public String putAll() {
        return op + ", "+ result + ", " + arg1 + ", " + arg2;
    }

    @Override
    public String toString() {
        switch (op) {
            case JUMP:
                if (arg1 != null) {
                    return "    <Loop "+ result + " " + arg2 +">    ";
                } else {
                    return "    <JUMP "+ result +">    ";
                }
            case GOTO:
                if (arg1 != null) {
                    return "GOTO LOOP" + result + "_" + arg2;
                } else {
                    return "GOTO Jump" + result;
                }

            case EQLOP:
                return  result + " = " + arg1 + " == " + arg2;
            case NEQOP:
                return  result + " = " + arg1 + " != " + arg2;
            case LSS:
                return  result + " = " + arg1 + " < " + arg2;
            case LEQ:
                return  result + " = " + arg1 + " <= " + arg2;
            case GRE:
                return  result + " = " + arg1 + " > " + arg2;
            case GEQ:
                return  result + " = " + arg1 + " >= " + arg2;
            case BZ:
                return "if " + result + " == 0 then goto Jump" + arg1;
            case FINISH:
                return  "\n-------FINISH-------\n";
            case MAIN:
                return  "\n-------MAIN-------\n";
            case CONST:
                return "CONST " + result + " = " + arg1;
            case CONSTARRAY_1:
                return "CONSTARRAY_1 " + result + "[" + arg1 + "]";
            case CONSTARRAY_2:
                return "CONSTARRAY_2 " + result + "[" + arg1 + "][" + arg2 + "]";
            case VAR:
                if (arg1 == null) {
                    return "VAR " + result;
                } else {
                    return "VAR " + result + " = " + arg1;
                }
            case ARRAY_1:
                return "ARRAY_1 " + result + "[" + arg1 + "]";
            case ARRAY_2:
                return "ARRAY_2 " + result + "[" + arg1 + "][" + arg2 + "]";
            case PUTARRAY:
                return result + "[" + arg1 + "]" + " = " + arg2;
            case GETARRAY:
                return result + " = " + arg1 + "[" + arg2 + "]";
            case ASSIGN:
                return "retvalue " + result;
            case CALL:
                return "CALL " + result;
            case PASS:
                return "PUSH " + result;
            case ASSIGNOP:
                return result + " = " + arg1;
            case BLOCK:
                return "    <BLOCK " + result + " " + arg1 + ">";
            case INT:
                return "INT " + result + "( )";
            case VOID:
                return "VOID " + result + "( )";
            case PARA:
                if (arg1.equals("0")) {
                    return "PARA int " + result;
                } else if (arg1.equals("1")) {
                    return "PARA int " + result + "[]";
                } else {
                    return "PARA int " + result + "[][" + arg2 + "]";
                }
            case RETURN:
                return "RETURN " + result;
            case PRINT:
                return "PRINT " + result;
            case SCAN:
                return "SCAN " + result;
            case ADD:
                return result + " = " + arg1 + " + " + arg2;
            case MINU:
                return result + " = " + arg1 + " - " + arg2;
            case MULT:
                return result + " = " + arg1 + " * " + arg2;
            case DIV:
                return result + " = " + arg1 + " / " + arg2;
            case MOD:
                return result + " = " + arg1 + " % " + arg2;
            default:
                return "";
        }
    }
}
