/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aps_bd;

/**
 *
 * @author lucasgb
 */


public class Lock {
    private String transaction;
    private Data data;    
    private TYPE type;
    
    public Lock(String transaction, Data data, TYPE type){
        this.transaction = transaction;
        this.data = data;        
        this.type = type;                
    }    

    /**
     * @return the transaction
     */
    public String getTransaction() {
        return transaction;
    }

    /**
     * @return the data
     */
    public Data getData() {
        return data;
    }

    /**
     * @return the type
     */
    public TYPE getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(TYPE type) {
        this.type = type;
    }    
    
    public enum TYPE{ LS, LX; }
}
