package generateMid;

public enum MidCodeOp {
    MAIN, FINISH,// 标记主函数 程序结束
    PRINT, SCAN,
    PUTARRAY, // 数组赋值
    ASSIGNOP,  //=
    CONST,  CONSTARRAY_1, CONSTARRAY_2, //常量
    VAR, ARRAY_1, ARRAY_2, // 普通变量
    INT, VOID, // 自定义函数的类型
    PARA, // 标记函数的参数
    BLOCK, // 不同的区块
    RETURN, // 返回值
    ADD, MINU, MULT, DIV, MOD, // 运算符 + - * / %
    CALL, // 调用函数
    ASSIGN, // 有返回值的函数，赋值给t
    GETARRAY, // 获取数组的值
    PASS, // 函数调用时候传参
    NOT, //逻辑非

    JUMP, // 跳转的标签
    GOTO, // 跳转动作
    EQLOP,  //==
    NEQOP,  //!=
    LSS, // <
    LEQ, // <=
    GRE, // >
    GEQ, // >=
    BZ, // 不满足条件跳转
}
