package com.example.myapp.banks;

public class NotTransactionSMSException extends Exception{
    public NotTransactionSMSException(String s) {
        super(s);
    }
}
