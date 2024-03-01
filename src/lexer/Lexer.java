package lexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

import static lexer.Tag.*;

public class Lexer {
    private BufferedReader in;
    public static int lineNum = 1; //当前行号
    public static int column = 1; //当前列数
    private String line; // 当前行
    private Token curToken;
    public char ch; // 当前读到的字符
    private boolean singleLine; // 是否处于单行注释中
    private boolean multiLine = false; // 是否处于多行注释中
    public HashMap<String, Tag> code = new HashMap<>();

    public Lexer(BufferedReader in) throws IOException {
        this.in = in;
        line = in.readLine();
        column = 0;
        initCode();
    }

    private void initCode() {
        code.put("main", MAINTK);
        code.put("const", CONSTTK);
        code.put("int", INTTK);
        code.put("break", BREAKTK);
        code.put("continue", CONTINUETK);
        code.put("if", IFTK);
        code.put("else", ELSETK);
        code.put("&&", AND);
        code.put("||", OR);
        code.put("for", FORTK);
        code.put("getint", GETINTTK);
        code.put("printf", PRINTFTK);
        code.put("return", RETURNTK);
        code.put("void", VOIDTK);
        code.put("<=", LEQ);
        code.put(">=", GEQ);
        code.put("==", EQL);
        code.put("!=", NEQ);
        code.put("!", NOT);
        code.put("+", PLUS);
        code.put("-", MINU);
        code.put("*", MULT);
        code.put("/", DIV);
        code.put("%", MOD);
        code.put("<", LSS);
        code.put(">", GRE);
        code.put("=", ASSIGN);
        code.put(";", SEMICN);
        code.put(",", COMMA);
        code.put("(", LPARENT);
        code.put(")", RPARENT);
        code.put("[", LBRACK);
        code.put("]", RBRACK);
        code.put("{", LBRACE);
        code.put("}", RBRACE);
    }

    private void nextLine() throws IOException {
        column = 0;
        lineNum++;
        singleLine = false;
        line = in.readLine();
    }

    public boolean digit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public boolean nondigit(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
    }

    public Token getToken() throws IOException{
        if (line == null) {
            return curToken;
        } else {
            while (column < line.length()) {
                if (singleLine == true) {
                    nextLine();
                    return getToken();
                } else if (multiLine == true) {
                    int pos;
                    if ((pos = line.indexOf("*/", column)) == -1) { // 未查找到 */
                        nextLine();
                        return getToken();
                    } else {
                        multiLine = false;
                        column = pos + 2;
                        if(column >= line.length()) {
                            nextLine();
                            return getToken();
                        }
                    }
                }
                ch = line.charAt(column);
                if (ch == ' ' || ch == '\r' || ch == '\t' || ch == '\n') {
                    column++;
                } else if (nondigit(ch)) {
                    parseIdent();
                    return curToken;
                } else if (digit(ch)) {
                    parseInt();
                    return curToken;
                } else if (ch == '\"') {
                    parseFormatString();
                    return curToken;
                } else if(parseSymbol()) {
                    return curToken;
                }
            }
            nextLine();
            return getToken();
        }
    }

    private void parseIdent() throws IOException {
        StringBuffer str = new StringBuffer("");
        str.append(ch);
        column++;
        while (column < line.length()) {
            ch = line.charAt(column);
            if(digit(ch) || nondigit(ch)) {
                str.append(ch);
                column++;
            } else {
                break;
            }
        }
        curToken = new Token(str.toString(),code.getOrDefault(str.toString(), IDENFR), lineNum);
    }

    private void parseInt() throws IOException {
        int intNum = ch - '0';
        column++;
        while (column < line.length()) {
            ch = line.charAt(column);
            if(digit(ch)) {
                column++;
                intNum = intNum * 10 + ch - '0';
            } else {
                //column--;
                break;
            }
        }
        curToken = new Token(intNum, lineNum);
    }

    boolean isFormatString(char ch) {
        char ch1 = line.charAt(column); //注意，这里在进入之前已经加过了
        if (ch == 32 || ch == 33 || (ch >= 40 && ch <= 126)) {
            if (ch == 92 && ch1 != 110) {
                return false;
            }
            return true;
        } else if (ch == 37 && ch1 == 100){ // %d
            return true;
        } else {
            return false;
        }
    }

    private void parseFormatString() throws IOException {
        boolean err = false;
        StringBuffer str = new StringBuffer("");
        str.append(ch);
        column++;
        while (column < line.length()) {
            ch = line.charAt(column);
            str.append(ch);
            column++;
            if (ch != '\"' && isFormatString(ch) == false) {
                err = true;
            }
            if(ch == '\"') {
                //column--;
                break;
            }
        }
        curToken = new Token(str.toString(), err, lineNum);
    }

    private boolean parseSymbol() throws IOException {
        StringBuffer str = new StringBuffer("");
        str.append(ch);
        switch (ch) {
            case '&':
            case '|':
                if (column + 1 < line.length() && line.charAt(column+1) == ch) {
                    column = column + 2;
                    str.append(ch);
                    curToken = new Token(str.toString(), code.get(str.toString()), lineNum);
                } else {
                    curToken = new Token(str.toString(), code.get(str.toString()), lineNum);
                    column++;
                }
                return true;
            case '<':
            case '>':
            case '=':
            case '!':
                if(column + 1 < line.length() && line.charAt(column+1) == '=') {
                    column= column + 2;
                    str.append('=');
                    curToken = new Token(str.toString(), code.get(str.toString()), lineNum);
                } else {
                    curToken = new Token(str.toString(), code.get(str.toString()), lineNum);
                    column++;
                }
                return true;
            case '/':
                if(column + 1 < line.length() && line.charAt(column+1) == ch) {
                    column= column + 2;
                    singleLine = true;
                    return false;
                } else if (line.charAt(column+1) == '*'){
                    column= column + 2;
                    multiLine = true;
                    return false;
                } else {
                    curToken = new Token(str.toString(), code.get(str.toString()), lineNum);
                    column++;
                    return true;
                }
            default:
                curToken = new Token(str.toString(), code.get(str.toString()), lineNum);
                column++;
                return true;
        }
    }
}
