package io.xxoo.eth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

@SpringBootTest
class ApplicationTests {

    public static final String address = "0x2C104AB32BEA7eCff8f37987AB1930bdF9FDb0ac";

    public static final String contractAddress = "0x064E6aC4deE25a101d535FcD91b35b9FcbA6ff31";

    @Autowired
    EthService ethService;

    @Test
    void contextLoads() {
    }

    @Test
    void testCreateAccount() {
        ethService.createAddress();
    }

    @Test
    void getEthBalance() {
        ethService.getEthBalance(address);
    }

    @Test
    void getTokenBalance() {
        ethService.getTokenBalance(address, contractAddress);
    }

    @Test
    void getTokenDecimal() throws Exception {
        ethService.getTokenDecimal(contractAddress);
    }

    @Test
    void getTokenName() throws Exception {
        ethService.getTokenName(contractAddress);
    }

    @Test
    void getTokenSymbol() throws Exception {
        ethService.getTokenSymbol(contractAddress);
    }

    @Test
    void sendEthTransaction() throws Exception {
        String from = address;
        String to = "0xe6a974a4c020ba29a9acb6c2290175a4d8846760";
        String privateKey = "0x928a3a791ad7cb6d30aa9b50a37bdfd3b52fa1515ec5d6fbea83c2dafb03af39";
        BigDecimal amount = Convert.toWei("1", Convert.Unit.WEI);
        BigInteger nonce = Web3jClient.instance().ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send().getTransactionCount();
        BigInteger amountWei = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
        BigInteger gasPrice = Web3jClient.instance().ethGasPrice().send().getGasPrice();
        //BigInteger gasLimit = Web3jClient.instance().ethEstimateGas(new Transaction(from, null, null, null, to, amount.toBigInteger(), null)).send().getAmountUsed();
        BigInteger gasLimit = BigInteger.valueOf(21000L);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, amountWei, "");
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, Credentials.create(privateKey));
        ethService.broadcastTransaction(Numeric.toHexString(signMessage));
    }

    @Test
    void sendTokenTransaction() throws Exception {
        String from = address;
        String to = "0xe6a974a4c020ba29a9acb6c2290175a4d8846760";
        String privateKey = "0x928a3a791ad7cb6d30aa9b50a37bdfd3b52fa1515ec5d6fbea83c2dafb03af39";
        String contractAddress = this.contractAddress;
        BigInteger amount = new BigDecimal("1").multiply(BigDecimal.valueOf(Math.pow(10, ethService.getTokenDecimal(contractAddress)))).toBigInteger();
        BigInteger gasPrice = Web3jClient.instance().ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = ethService.getTransactionGasLimit(from, to, contractAddress, amount);
        BigInteger nonce = Web3jClient.instance().ethGetTransactionCount(from, DefaultBlockParameterName.PENDING).send().getTransactionCount();
        Function function = new Function("transfer", Arrays.asList(new Address(to), new Uint256(amount)), Collections.emptyList());
        String data = FunctionEncoder.encode(function);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contractAddress, data);
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, Credentials.create(privateKey));
        ethService.broadcastTransaction(Numeric.toHexString(signMessage));
    }

    @Test
    void validateTransaction() throws Exception {
        boolean isValidate = ethService.validateTransaction("0x44d7365de92d8ae1bd9362f733ebca8bae0b911779ca11417de81f270bad9cb8");
        System.out.println("交易是否确认 " + isValidate);
    }

}
