package com.sharad.myapp.banks;

import com.sharad.myapp.Utils.Constants;
import com.sharad.myapp.Utils.Functions;

public class HDFC implements Bank {

    // Source of data (https://v1.hdfcbank.com/htdocs/common/account-balance/index.html?utm_campaign=acbalance&utm_medium=sms&utm_source=balancetrxnsms)
    @Override
    public String getBankBalanceSmsBody() {
        return "bal";
    }

    @Override
    public String getBankBalanceSmsAddress() {
        return "5676712";
    }

    @Override
    public String getBankBalanceCallPhoneAddress() {
        return "18002703333";
    }

    @Override
    public SmsParseResult parse(String smsBody) throws NotTransactionSMSException, FalseAlarmException {
        String merchName = "", tags = "", txnType = "", amount = "";
        boolean isBankBalanceSMS = false;

        if (smsBody.contains("EMI")) {
            // TODO: Implement this
            throw new NotTransactionSMSException("EMI Loans are not supported yet");
        } else if (smsBody.contains("debited") || smsBody.contains("Debited")) {
            txnType = Constants.TXN_TYPE_DEBITED;
        } else if (smsBody.contains("credited") || smsBody.contains("Credited") || smsBody.contains("You have received") || smsBody.contains("deposited")) {
            txnType = Constants.TXN_TYPE_CREDITED;
        } else {
            throw new NotTransactionSMSException("Not a bank transaction");
        }

        if (smsBody.contains("UPI")) {
            tags = "UPI";
            int initialIndex = smsBody.indexOf("VPA") + 4;
            int lastIndex = smsBody.indexOf("(UPI Ref No");
            merchName = smsBody.substring(initialIndex,lastIndex);
        } else if (smsBody.contains("IMPS")) {
            tags = "IMPS";
        } else if (smsBody.contains("w/d") || smsBody.contains("withdrawn")) {
            tags = "ATM";
        } else if (smsBody.contains("NEFT")) {
            tags = "NEFT";
        } else if (smsBody.contains("ATMID")) {
            tags = "ATM";
        } else if (smsBody.contains("Info:")) {
            // ******* Sample *********
            // Dear Customer, ICICI Bank Account XX705 is debited with INR 9,757.00 on 06-Sep-21.
            // Info: BIL*000202192. The Available Balance is INR 5,606.10. Call 18002662
            // for dispute or SMS BLOCK 705 to 9215676766
            Integer initialIndex = smsBody.lastIndexOf("Info:");
            Integer lastIndex = smsBody.indexOf('.', initialIndex);
            // 5 is the lenght of Info:
            String subString = smsBody.substring(initialIndex + 5, lastIndex);
            subString = subString.trim();
            subString = subString.replace("*", " ");
            subString = subString.replace("-", " ");
            String[] arr = subString.split(" ");
            String finalStr = "";
            for (Integer i = 1; i < arr.length; i++) {
                finalStr += arr[i] + " ";
            }
            merchName = finalStr;
            tags = arr[0].toUpperCase();
        }

        String amt = Functions.extractAmountFromSMS(smsBody);
        amount = amt;
        return new SmsParseResult(merchName, tags, txnType, amount, smsBody, isBankBalanceSMS);
    }
}
