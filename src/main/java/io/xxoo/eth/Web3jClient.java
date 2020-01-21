package io.xxoo.eth;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * @ Author     ：Yaakov
 * @ Date       ：Created in 10:30 上午 2019/12/31
 */
public class Web3jClient {

    private static volatile Web3j instance;

    public static Web3j instance() {
        if (instance == null) {
            synchronized (Web3j.class) {
                if (instance == null) {
                    instance = Web3j.build(new HttpService("https://rinkeby.infura.io/f2f5d9d6d1ca43ff8eb8395f68829236"));
                }
            }
        }
        return instance;
    }
}
