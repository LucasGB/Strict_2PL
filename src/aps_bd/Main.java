/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aps_bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lucasgb
 */
public class Main {
    
    private static int id_op = 0;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {       
        Connection conn = new ConnectionHelper().getConnection();
        Schedule schedule = new Schedule();              
        Scheduler controller = new Scheduler();
        
        try {
            ResultSet rs;
            do{                
                // Seleciona todos as operações maior que a anteriormente lida. Permite com que o programa itere sobre a tabela sem repetir leituras
                PreparedStatement stm = conn.prepareStatement("Select * from schedule where idoperacao > ?");
                stm.setInt(1, id_op);
                rs = stm.executeQuery();

                // Salva informações
                rs.next();                
                id_op =  rs.getInt("idoperacao");                
                String oldstring = rs.getString("timestamp");
                Date date = new SimpleDateFormat("y_H").parse(oldstring);                
                String l = rs.getString("indicetransacao");
                              
                schedule.addTransaction(new Transaction(rs.getString("indicetransacao"),
                                            rs.getString("operacao"),
                                                new Data(rs.getString("itemdado"), rs.getString("indicetransacao")), date));                                
                
                controller.addToReadyList(schedule.getTransactions().get(schedule.getTransactions().size() - 1));
                controller.startScheduling();                
            } while(rs.next());          
            
            while(!controller.getReady_list().isEmpty()){
                controller.startScheduling();
            }
            
            // Trata o fenomeno Senpai
            while(!controller.getWait_list().isEmpty()){
                controller.senpai();
            }
               
            controller.printFinishedList();
            controller.printWaitList();
            controller.printReadyList();
            controller.printLocks();
            
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);                        
        } catch (ParseException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}