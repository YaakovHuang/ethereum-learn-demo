package io.xxoo.eth;

import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.crypto.*;
import org.bitcoinj.wallet.DeterministicSeed;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import sun.security.provider.SecureRandom;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @ Author     ：Yaakov
 * @ Date       ：Created in 11:36 上午 2019/12/31
 */
@Slf4j
@Component
public class EthService {

    private final static List<ChildNumber> BIP44_ETH_ACCOUNT_ZERO_PATH = Arrays.asList(new ChildNumber(44, true), new ChildNumber(60, true), ChildNumber.ZERO_HARDENED, ChildNumber.ZERO);

    public boolean validateAddress(String address) {
        return EthUtils.isValidAddress(address);
    }

    /**
     * 生成地址和私钥
     */
    public void createAddress() {
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] entropy = new byte[DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8];
            secureRandom.engineNextBytes(entropy);
            // 生成12位助记词
            List<String> mnemonics = MnemonicCode.INSTANCE.toMnemonic(entropy);
            // 使用助记词生成钱包种子
            byte[] seed = MnemonicCode.toSeed(mnemonics, "");
            DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
            DeterministicHierarchy deterministicHierarchy = new DeterministicHierarchy(masterPrivateKey);
            DeterministicKey deterministicKey = deterministicHierarchy.deriveChild(BIP44_ETH_ACCOUNT_ZERO_PATH, false, true, new ChildNumber(0));
            byte[] bytes = deterministicKey.getPrivKeyBytes();
            ECKeyPair keyPair = ECKeyPair.create(bytes);
            //通过公钥生成钱包地址
            String address = Keys.getAddress(keyPair.getPublicKey());
            log.info("Mnemonic：" + mnemonics);
            log.info("Address：0x" + address);
            log.info("PrivateKey：0x" + keyPair.getPrivateKey().toString(16));
            log.info("PublicKey：" + keyPair.getPublicKey().toString(16));
        } catch (Exception e) {
            log.error("create address error: ", e);
        }
    }

    /**
     * 获取指定地址ETH余额
     *
     * @param address 地址
     * @return
     */
    public void getEthBalance(String address) {
        if (!validateAddress(address)) {
            log.error("address is incorrect.");
            return;
        }
        try {
            EthGetBalance ethGetBalance = Web3jClient.instance().ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            if (ethGetBalance.hasError()) {
                log.error("【获取账户 {} ETH余额失败】", address, ethGetBalance.getError().getMessage());
                return;
            }
            String balance = Convert.fromWei(new BigDecimal(ethGetBalance.getBalance()), Convert.Unit.ETHER).toPlainString();
            log.info("balance = " + balance);
        } catch (Exception e) {
            log.error("【获取账户 {} ETH余额失败】", address, e);
        }
    }

    /**
     * 获取地址代币余额
     *
     * @param address         ETH地址
     * @param contractAddress 代币合约地址
     * @return
     */
    public void getTokenBalance(String address, String contractAddress) {
        if (!validateAddress(address) || !validateAddress(contractAddress)) {
            log.error("address is incorrect.");
            return;
        }
        try {
            Function balanceOf = new Function("balanceOf", Arrays.asList(new Address(address)), Arrays.asList(new TypeReference<Uint256>() {
            }));
            EthCall ethCall = Web3jClient.instance().ethCall(Transaction.createEthCallTransaction(address, contractAddress, FunctionEncoder.encode(balanceOf)), DefaultBlockParameterName.PENDING).send();
            if (ethCall.hasError()) {
                log.error("【获取账户 {}, 合约 {contractAddress} 余额失败】", address, contractAddress, ethCall.getError().getMessage());
                return;
            }
            String value = ethCall.getValue();
            String balance = Numeric.toBigInt(value).toString();
            int decimal = getTokenDecimal(contractAddress);
            log.info("balance = " + EthAmountFormat.format(balance, decimal));
        } catch (Exception e) {
            log.error("【获取账户 {}, 合约 {contractAddress} 余额失败】", address, contractAddress, e);
        }
    }

    /**
     * 获取代币精度
     *
     * @param contractAddress 代币合约地址
     * @return
     */
    public int getTokenDecimal(String contractAddress) throws Exception {
        Function function = new Function("decimals", Arrays.asList(), Arrays.asList(new TypeReference<Uint8>() {
        }));
        EthCall ethCall = Web3jClient.instance().ethCall(Transaction.createEthCallTransaction("0x0000000000000000000000000000000000000000", contractAddress, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST).send();
        if (ethCall.hasError()) {
            log.error("【获取合约 {} Token 精度失败】", contractAddress, ethCall.getError().getMessage());
            throw new Exception(ethCall.getError().getMessage());
        }
        List<Type> decode = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        int decimals = Integer.parseInt(decode.get(0).getValue().toString());
        log.info("decimals = " + decimals);
        return decimals;
    }

    /**
     * 获取代币符号
     *
     * @param contractAddress 代币合约地址
     * @return
     */
    public String getTokenSymbol(String contractAddress) throws Exception {
        Function function = new Function("symbol", Arrays.asList(), Arrays.asList(new TypeReference<Utf8String>() {
        }));
        EthCall ethCall = Web3jClient.instance().ethCall(Transaction.createEthCallTransaction("0x0000000000000000000000000000000000000000", contractAddress, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST).send();
        if (ethCall.hasError()) {
            throw new Exception(ethCall.getError().getMessage());
        }
        List<Type> decode = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        return decode.get(0).getValue().toString();
    }

    /**
     * 获取代币名称
     *
     * @param contractAddress 代币合约地址
     * @return
     */
    public String getTokenName(String contractAddress) throws Exception {
        Function function = new Function("name", Arrays.asList(), Arrays.asList(new TypeReference<Utf8String>() {
        }));
        EthCall ethCall = Web3jClient.instance().ethCall(Transaction.createEthCallTransaction("0x0000000000000000000000000000000000000000", contractAddress, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST).send();
        if (ethCall.hasError()) {
            throw new Exception(ethCall.getError().getMessage());
        }
        List<Type> decode = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        return decode.get(0).getValue().toString();
    }

    /**
     * 获取指定区块高度的交易信息
     *
     * @param height 区块高度
     * @return
     */
    public List<org.web3j.protocol.core.methods.response.Transaction> getTransactionByHeight(BigInteger height) throws Exception {
        List<org.web3j.protocol.core.methods.response.Transaction> transactions = new ArrayList<>();
        EthBlock ethBlock = Web3jClient.instance().ethGetBlockByNumber(DefaultBlockParameter.valueOf(height), false).send();
        if (ethBlock.hasError()) {
            throw new Exception(ethBlock.getError().getMessage());
        }
        EthBlock.Block block = ethBlock.getBlock();
        for (EthBlock.TransactionResult transactionResult : block.getTransactions()) {
            try {
                org.web3j.protocol.core.methods.response.Transaction transaction = getTransactionByTxId((String) transactionResult.get());
                transactions.add(transaction);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("【获取区块交易数据成功】 区块高度: {}, 区块哈希: {}", block.getNumber(), block.getHash());
        return transactions;
    }

    /**
     * 获取指定交易ID的交易信息
     *
     * @param txId 交易hash
     * @return
     */
    public org.web3j.protocol.core.methods.response.Transaction getTransactionByTxId(String txId) throws IOException {
        return Web3jClient.instance().ethGetTransactionByHash(txId).send().getTransaction().orElse(null);
    }

    /**
     * 获取合约交易估算gas值
     *
     * @param from        发送者
     * @param to          发送目标地址
     * @param coinAddress 代币地址
     * @param value       发送金额（单位：代币最小单位）
     * @return
     */
    public BigInteger getTransactionGasLimit(String from, String to, String coinAddress, BigInteger value) throws Exception {
        Function transfer = new Function("transfer", Arrays.asList(new org.web3j.abi.datatypes.Address(to), new org.web3j.abi.datatypes.generated.Uint256(value)), Collections.emptyList());
        String data = FunctionEncoder.encode(transfer);
        EthEstimateGas ethEstimateGas = Web3jClient.instance().ethEstimateGas(new Transaction(from, null, null, null, coinAddress, BigInteger.ZERO, data)).send();
        if (ethEstimateGas.hasError()) {
            throw new Exception(ethEstimateGas.getError().getMessage());
        }
        return ethEstimateGas.getAmountUsed();
    }

    /**
     * 广播交易
     *
     * @param signData 签名数据
     * @return
     */
    public String broadcastTransaction(String signData) throws Exception {
        EthSendTransaction transaction = Web3jClient.instance().ethSendRawTransaction(signData).send();
        if (transaction.hasError()) {
            throw new Exception(transaction.getError().getMessage());
        }
        String txId = transaction.getTransactionHash();
        log.info("【发送交易交易成功】txId: {}", txId);
        return txId;
    }

    /**
     * 验证交易是否被打包进区块
     *
     * @param txId 交易id
     * @return
     */
    public boolean validateTransaction(String txId) throws Exception {
        org.web3j.protocol.core.methods.response.Transaction transaction = getTransactionByTxId(txId);
        String blockHash = transaction.getBlockHash();
        if (StringUtils.isEmpty(blockHash) || Numeric.toBigInt(blockHash).compareTo(BigInteger.valueOf(0)) == 0) {
            return false;
        }
        EthGetTransactionReceipt receipt = Web3jClient.instance().ethGetTransactionReceipt(txId).send();
        if (receipt.hasError()) {
            throw new Exception(receipt.getError().getMessage());
        }
        TransactionReceipt transactionReceipt = receipt.getTransactionReceipt().get();
        if (Numeric.toBigInt(transactionReceipt.getStatus()).compareTo(BigInteger.valueOf(1)) == 0) {
            return true;
        }
        return false;
    }


}
