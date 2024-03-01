package mips;

public enum MipsOp {
    data, // .data
    asciiz,
    text, // .text
    syscall,
    moveop,
    jal, jr, j,
    li, // 特定数值加载该寄存器
    la,
    label,
    sll,
    lw,
    sw,
    add, sub, mult,div,
    addu,
    mflo, mfhi,
    addi, //符号加立即数
    slt, // 小于置 1
    sgt, // 大于置1
    sle, // 小于等于置 1
    sge, // 大于等于置1
    seq, // == 1
    sne, // != 1
    beq, // 若相等，则跳转
}
