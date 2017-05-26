/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aps_bd;

import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author lucasgb
 */
public class Transaction {
    private String label;
    private Operation operation;
    
    public Transaction(String label, String operation_type, Data data, Date timestamp){
        this.label = label;
        this.operation = new Operation(operation_type, data, timestamp);        
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the operations
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * @param operation the operations to set
     */
    public void setOperation(Operation operation) {
        this.operation = operation;
    }
   
    
    public void print(){
        System.out.println(label);
        System.out.println("  |");
        System.out.println("  +--"+operation.getOperation_type());
        System.out.println("     |");
        System.out.println("     +--"+operation.getData().getLabel());        
    }
    
}
