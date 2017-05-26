/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aps_bd;

import java.util.ArrayList;

/**
 *
 * @author lucasgb
 */
public class Schedule {
    private ArrayList<Transaction> transactions;
    
    public Schedule(){        
        this.transactions = new ArrayList<>();       
    }

    /**
     * @return the transactions
     */
    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    /**
     * @param transactions the transactions to set
     */
    public void setTransactions(ArrayList<Transaction> transactions) {
        this.transactions = transactions;
    }
    
    public void addTransaction(Transaction t){
        this.transactions.add(t);
    }
}