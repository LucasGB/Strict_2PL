/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aps_bd;

import java.util.Date;

/**
 *
 * @author lucasgb
 */
class Operation {
    private String operation_type;
    private Data data;
    private Date timestamp;
    
    public Operation(String operation_type, Data data, Date timestamp){
        this.operation_type = operation_type;
        this.data = data;
        this.timestamp = timestamp;
    }

    /**
     * @return the operation_type
     */
    public String getOperation_type() {
        return operation_type;
    }

    /**
     * @return the data
     */
    public Data getData() {
        return data;
    }

    /**
     * @return the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }
}
