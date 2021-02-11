package com.demo.hts;
import com.hedera.hashgraph.sdk.*;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

@SpringBootApplication
@RestController
public class HederaTsApplication {

	public static void main(String[] args) {
		SpringApplication.run(HederaTsApplication.class, args);
	}

    @RequestMapping("/createToken/{tokenName}")
    public String createToken(@PathVariable() String tokenName) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        AccountId myAccountId = AccountId.fromString("0.0.277196");
        PrivateKey myPrivateKey = PrivateKey.fromString("302e020100300506032b657004220420e70c2ac25277241774bd7829c8769e7acf3f2ced967fc36e812ed4b8cea27f27");

        //Create your Hedera testnet client
        Client client = Client.forTestnet();
        client.setOperator(myAccountId, myPrivateKey);

        //Creating token names - Example
//        TokenId tokenId = new TokenId(0,0,5);
//        System.out.println(tokenId);
//
//        TokenId tokenIdFromString = TokenId.fromString("0.0.3");
//        System.out.println(tokenIdFromString);

        //Creating tokens in Hedera
        TokenCreateTransaction transaction = new TokenCreateTransaction()
            .setAdminKey(myPrivateKey.getPublicKey())
            .setTokenName("Winner Token")
            .setTokenSymbol(tokenName)
            .setDecimals(18)
            .setAutoRenewAccountId(myAccountId)
            .setFreezeKey(myPrivateKey.getPublicKey())
            .setInitialSupply(10000)
            .setTreasuryAccountId(myAccountId)
            .setMaxTransactionFee(new Hbar(10));

        TransactionResponse txResponse = transaction.freezeWith(client).sign(myPrivateKey).execute(client);

        TransactionReceipt receipt = txResponse.getReceipt(client);

        TokenId tokenId = receipt.tokenId;
        System.out.println("The tokenId is: "+ tokenId);

        //Creating new accounts to transfer the tokens

        PrivateKey newAccountPrivateKey = PrivateKey.generate();
        PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();

        //Create new account and assign the public key
        TransactionResponse newAccount = new AccountCreateTransaction()
            .setKey(newAccountPublicKey)
            .setInitialBalance( Hbar.fromTinybars(1000))
            .execute(client);

        // Get the new account ID
        AccountId newAccountId = newAccount.getReceipt(client).accountId;

        List<TokenId> list = Arrays.asList(tokenId);
        //Associate the token to the account
        //Associate a token to an account
        TokenAssociateTransaction txAssociation = new TokenAssociateTransaction()
            .setAccountId(newAccountId)
            .setTokenIds(list);

        //Freeze the unsigned transaction, sign with the private key of the account that is being associated to a token, submit the transaction to a Hedera network
        TransactionResponse txResponseAssociation = txAssociation.freezeWith(client).sign(newAccountPrivateKey).execute(client);

        //Request the receipt of the transaction
        TransactionReceipt receiptTxAssociation = txResponseAssociation.getReceipt(client);

        //Get the transaction consensus status
        Status transactionStatus = receiptTxAssociation.status;

        System.out.println("The transaction consensus status for association transaction " +transactionStatus);

        //Create the transfer transaction
        TransferTransaction tokenTx = new TransferTransaction()
            .addTokenTransfer(tokenId, myAccountId, -1000)
            .addTokenTransfer(tokenId, newAccountId, 1000);

        //Sign with the client operator key and submit the transaction to a Hedera network
        TransactionResponse tokenTxResponse = tokenTx.execute(client);

        //Request the receipt of the transaction
        TransactionReceipt tokenReceipt = tokenTxResponse.getReceipt(client);

        //Get the transaction consensus status
        Status transactionStatusAssociate = tokenReceipt.status;

        System.out.println("The transaction consensus status is " +transactionStatusAssociate);

        //Check the new account's balance
        AccountBalance accountBalance = new AccountBalanceQuery()
            .setAccountId(newAccountId)
            .execute(client);

        System.out.println("The new account balance is: " + accountBalance.token);
        String explorer = "Copy paste the following URL to see the information about your newly created token! \n " +
            "https://ledger-testnet.hashlog.io/tx/"+ tokenTxResponse.transactionId.toString();
        return explorer;
    }

}
