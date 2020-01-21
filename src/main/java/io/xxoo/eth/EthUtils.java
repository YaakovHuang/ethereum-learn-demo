package io.xxoo.eth;

import org.web3j.utils.Numeric;

/**
 * @ Author     ：Yaakov
 * @ Date       ：Created in 9:47 上午 2020/1/3
 */
public class EthUtils {

    private static final int ADDRESS_LENGTH_IN_HEX = 40;

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isValidAddress(String input) {
        if (isEmpty(input) || !input.startsWith("0x")) {
            return false;
        }
        String cleanInput = Numeric.cleanHexPrefix(input);
        try {
            Numeric.toBigIntNoPrefix(cleanInput);
        } catch (NumberFormatException e) {
            return false;
        }
        return cleanInput.length() == ADDRESS_LENGTH_IN_HEX;
    }
}
