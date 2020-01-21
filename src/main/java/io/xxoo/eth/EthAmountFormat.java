package io.xxoo.eth;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @ Author     ：Yaakov
 * @ Date       ：Created in 4:20 下午 2020/1/3
 */
public class EthAmountFormat {
    private static final BigDecimal unit = BigDecimal.valueOf(1000000000000000000L);
    private static final BigDecimal unitWei = BigDecimal.valueOf(1000000000L);

    public EthAmountFormat() {
    }

    public static String toEther(BigInteger amount) {
        BigDecimal a = new BigDecimal(amount);
        a = a.divide(unit);
        return a.toPlainString();
    }

    public static BigDecimal fromEther(String amountStr) {
        BigDecimal amount = new BigDecimal(amountStr);
        amount = amount.multiply(unit);
        return amount;
    }

    public static long toWei(String amountStr) {
        BigDecimal amount = new BigDecimal(amountStr);
        amount = amount.multiply(unitWei);
        return amount.longValue();
    }

    public static String fromWei(long ammountWei) {
        BigDecimal amount = new BigDecimal(ammountWei);
        amount = amount.divide(unitWei);
        return amount.toPlainString();
    }

    public static String format(String amount, int decimal) {
        BigDecimal unit = new BigDecimal(Math.pow(10.0D, decimal));
        return (new BigDecimal(amount)).divide(unit, decimal, BigDecimal.ROUND_DOWN).stripTrailingZeros().toPlainString();
    }

    public static BigDecimal parse(String amountStr, int decimal) {
        BigDecimal unit = new BigDecimal(Math.pow(10.0D, decimal));
        return (new BigDecimal(amountStr)).multiply(unit);
    }
}
