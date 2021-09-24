package com.example.myapp;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.example.myapp.banks.Bank;
import com.example.myapp.banks.FalseAlarmException;
import com.example.myapp.banks.ICICI;
import com.example.myapp.banks.NotTransactionSMSException;
import com.example.myapp.banks.StateBankOfIndia;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProcessSMS {
    static final String ICICI = "ICICI";
    static final String STATE_BANK_OF_INDIA = "SBI";
    static final String HDFC_BANK = "HDFC";
    static final String BANK_OF_INDIA = "BOI";

    // List of supported banks
    private final HashMap<String, String[]> supportedBanks = new HashMap<>();

    private StoreFilteredTransactionResult filteredResult;
    private final Context ctx;

    private DatabaseHelper dh;

    public ProcessSMS(Context ctx) {
        this.ctx = ctx;
        this.dh = new DatabaseHelper(ctx);
        this.supportedBanks.put(ICICI, new String[]{"ICICIB"});
//        this.supportedBanks.put(STATE_BANK_OF_INDIA, new String[]{"SBIUPI", "ATMSBI", "SBIATM", "CBSSBI", "SBIPSG", "SBIDGT"});
//        this.supportedBanks.put(HDFC_BANK, new String[]{"ICICIB"});
//        this.supportedBanks.put(BANK_OF_INDIA, new String[]{"ICICIB"});
        processAndStoreSMS();
    }

    public void filterList(String startDate, String endDate, ArrayList<String> banks, ArrayList<String> paymentType, ArrayList<String> transactionType) {
        this.filteredResult = this.dh.getFilteredTransactions(startDate, endDate, banks, paymentType, transactionType);
    }

    public float calculateIncome() {
        return this.filteredResult.getIncome();
    }

    public float calculateExpense() {
        return this.filteredResult.getExpense();
    }

    public float calculateInHandCash() {
        return this.filteredResult.getInHandCash();
    }

    public boolean addTransaction(float amount, Long date, int smsId, String smsMsg, String tt, String pt, String tp, String tags, String bankName, String notes, String imageRef, String category) {
        return this.dh.addTransactionSMS(amount, date, smsId, smsMsg, tt, pt, tp, tags, bankName, notes, imageRef, category);
    }


    public List getMessages() {
        return filteredResult.getFilteredList();
    }

    public String getDate(Integer position) {
        HashMap<String, Object> d = (HashMap<String, Object>) filteredResult.getFilteredList().get(position);
        Long date1 = (Long) d.get("date");
        SimpleDateFormat sdf1 = new SimpleDateFormat("dd MMM YYYY");
        return sdf1.format(date1);
    }

    public String getAmount(Integer position) {
        HashMap<String, Object> d = (HashMap<String, Object>) filteredResult.getFilteredList().get(position);
        return d.get("amount").toString();
    }

    public String getType(Integer position) {
        HashMap<String, Object> d = (HashMap<String, Object>) filteredResult.getFilteredList().get(position);
        return d.get("transaction_type").toString();
    }

    public String getBank(Integer position) {
        HashMap<String, Object> d = (HashMap<String, Object>) filteredResult.getFilteredList().get(position);
        return d.get("bank_name").toString();
    }

    public String getTransactionPerson(Integer position) {
        HashMap<String, Object> d = (HashMap<String, Object>) filteredResult.getFilteredList().get(position);
        return d.get("transaction_person").toString();
    }

    public String getTags(Integer position) {
        HashMap<String, Object> d = (HashMap<String, Object>) filteredResult.getFilteredList().get(position);
        return d.get("tags").toString();
    }

    private void processAndStoreSMS() {
        // For now, remove previous db & start fresh
//        this.dh.clean();
        String[] columns = new String[]{"_id", "address", "body", "date"};

        for (Map.Entry<String, String[]> bank : this.supportedBanks.entrySet()) {

            // Build where clause
            String lastProcessedDate = this.dh.getLastProcessedTxnSMSDate();
            if (lastProcessedDate.isEmpty()) {
                // This happens during first initialization
                // Use the default of past 6 months
                Long startDate = LocalDateTime.now().minusMonths(6).withDayOfMonth(1).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
                lastProcessedDate = String.valueOf(startDate);
                Log.d("Last processed date is empty: ", lastProcessedDate);
            }

            StringBuilder whereClause = new StringBuilder(String.format("(date > %s) and ", lastProcessedDate));
            for (int i = 0; i < bank.getValue().length; i++) {
                whereClause.append("address LIKE \"%").append(bank.getValue()[i]).append("\"");
                if (i != bank.getValue().length - 1) {
                    whereClause.append(" or ");
                }
            }

            Log.d("SMS where clause: ", whereClause.toString());

            Cursor cursor = ctx.getContentResolver().query(Uri.parse("content://sms"), columns, whereClause.toString(), null, null);

            Bank bankObj;
            switch (bank.getKey()) {
                case STATE_BANK_OF_INDIA:
                    bankObj = new StateBankOfIndia();
                    break;
                case ICICI:
                    bankObj = new ICICI();
                    break;
                default:
                    throw new IllegalStateException("Unexpected bank: " + bank.getKey());
            }

            while (cursor.moveToNext()) {
                HashMap<String, String> map = new HashMap<>();

                if (cursor.getColumnName(2).equals("body")) {
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        map.put(cursor.getColumnName(i), cursor.getString(i));
                    }

                    map.put("bank", bank.getKey());
                    map.put("payment_type", "online");
                    map.put("transaction_person", "N/A");
                    map.put("tags", "N/A");

                    String smsBody = cursor.getString(2);
                    HashMap<String, String> newMap;
                    try {
                        newMap = bankObj.parse(smsBody);
                        newMap.forEach((key, value) -> map.merge(key, value, (v1, v2) -> v2));

                        Log.d("Transaction Data: id", map.get("_id"));
                        int dSmsId = Integer.parseInt(Objects.requireNonNull(map.get("_id")));
                        Log.d("Transaction Data: bank", map.get("bank"));
                        String dBank = map.get("bank");
                        Log.d("Transaction Data: tp", map.get("transaction_person"));
                        String dtp = map.get("transaction_person");
                        Log.d("Transaction Data: tags", map.get("tags"));
                        String dTags = map.get("tags");
                        Log.d("Transaction Data: amount", map.get("amount"));
                        float dAmount = Float.parseFloat(Objects.requireNonNull(map.get("amount")));
                        Log.d("Transaction Data: pt", map.get("payment_type"));
                        String dpt = map.get("payment_type");
                        Log.d("Transaction Data: tt", map.get("type"));
                        String dType = map.get("type");
                        Log.d("Transaction Data: date", map.get("date"));
                        Long dDate = Long.parseLong(Objects.requireNonNull(map.get("date")));

                        if (this.dh.addTransactionSMS(dAmount, dDate, dSmsId, smsBody, dType, dpt, dtp, dTags, dBank, "", "", "")) {
                            Log.d("Transaction Data: ", "************ Successful Insert ************");
                        } else {
                            Log.d("Transaction Data: ", "************ Fail Insert ************");
                        }
                    } catch (NotTransactionSMSException | FalseAlarmException e) {
                        Log.d("User Exception", e.getMessage());
                    } catch (Exception e) {
                        Log.d("System Exception", e.getMessage());
                    }
                }
            }
            cursor.close();
        }
        // Done
        this.dh.upsertLastProcessedTxnDate();
        this.filteredResult = this.dh.getFilteredTransactions("", "", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
}
