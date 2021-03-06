package aps_bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author lucasgb
 */
public class Scheduler {

    private ArrayList<Transaction> ready_list;
    private ArrayList<Transaction> finished_list;
    private ArrayList<Transaction> wait_list;

    private ArrayList<Lock> locks;
    
    private static Connection conn;

    public Scheduler() {
        this.finished_list = new ArrayList<Transaction>();
        this.ready_list = new ArrayList<Transaction>();
        this.wait_list = new ArrayList<Transaction>();
        this.locks = new ArrayList<Lock>();
        
        conn = Singleton.getInstance();
    }

    public void startScheduling() {
        
        Transaction t = ready_list.get(0);
        Operation op = t.getOperation();
        
        switch(op.getOperation_type()){
            case "B":   Process_B(t);
                        break;
            case "E":   Process_E(t);
                        break;
            case "R":   Process_R(t, op);
                        break;
            case "W":   Process_W(t, op);
                        break;
        }

    }
    
    public void Process_B(Transaction t){
        ready_list.remove(t);
    }
    
    public void Process_E(Transaction t){
        
        ArrayList<Data> aux = new ArrayList<Data>();
        ArrayList<Transaction> aux2 = new ArrayList<Transaction>();
        ArrayList<Lock> l = new ArrayList<Lock>();
        int flag = 0;

            /*
             * Verifica se existe algum dado na fila de espera antes de comitar
            */
            for (Transaction w : getWait_list()) {
                if (w.getLabel().equals(t.getLabel())) {                    
                    flag = 1;                    
                }
            }
            
            /*
             * Verifica se existe uma operação da mesma transação que o commit.
             * Se sim, coloca o commit na lista de espera.
             */
            for(int i = 1; i < ready_list.size(); i++){
                if(ready_list.get(i).getLabel().equals(t.getLabel())){
                    flag = 1;
                    break;
                }
            }
            /*
             * Acorda transações assim que um dado foi liberado para uso
            */
            if (flag == 0) {
                aux = getDatasLockedBy(t.getLabel());
                
                for (Data d : aux) {
                    for (Transaction w : getWait_list()) {
                        if (w.getOperation().getData().getLabel().equals(d.getLabel())) {
                            aux2.add(w);
                        }
                    }                   
                    ready_list.addAll(aux2);
                    getWait_list().removeAll(aux2);
                    aux2.clear();                
                }
                l = getLocksLockedBy(t.getLabel());
                locks.removeAll(l);
                aux.clear();
                ready_list.remove(t);
                finished_list.add(t);
                writeToDB();
            }
            
            /*
             * Insere o commit no final da fila de espera caso a flag seja acionada
            */
            if (flag == 1) {
                wait_list.add(t);
                ready_list.remove(t);
                flag = 0;
            }
    }

    public void Process_R(Transaction t, Operation op){
        if (!isLocked(t.getLabel(), op.getData())) {
                locks.add(new Lock(t.getLabel(), op.getData(), Lock.TYPE.LS));
                finished_list.add(t);
                ready_list.remove(t);
                writeToDB();
        }
        /*
         * Se o dado está na lista de bloqueio compartilhado OU
         * o dado foi bloqueado exclusivamente e a transação é o dono do mesmo, 
         * escalona a operação(coloca na lista de terminados) e a retira da lista de prontos
         */
        else if (isLockedS(op.getData())
                    || isLockedXBy(t.getLabel(), op.getData())) {
            finished_list.add(t);
            ready_list.remove(t);    
            writeToDB();
        }
        /*
         * O dado foi bloqueado exclusivamente por outra transação que está executando
         */
        else {
            getWait_list().add(t);
            ready_list.remove(t);
        }
    }
    
    public void Process_W(Transaction t, Operation op){
        
        if (!isLocked(t.getLabel(), op.getData())) {
                locks.add(new Lock(t.getLabel(), op.getData(), Lock.TYPE.LX));
                finished_list.add(t);
                ready_list.remove(t);
                writeToDB();
        }
        /*
         * Se o dado foi bloqueado exclusivamente e a transação em questão é o dono,
         * escalona a operação e a tira da lista de prontos
        */
        else if (isLockedXBy(t.getLabel(), op.getData())) {
            finished_list.add(t);
            ready_list.remove(t);
            writeToDB();
        }
        /*
         * Se o dado está na lista de bloqueio compartilhado e a transação atual é a dona do mesmo,
         * armazeno o dado no bloqueio exclusivo e retiro do bloqueio compartilhado, e escalono a operação
         */
        else if (isLockedSBy(t.getLabel(), op.getData())) {
            if (getTransactionsByDataCount(op.getData()) <= 1
                    && isLockedBy(t.getLabel(), op.getData())) {
                switchLock(t.getLabel(), op.getData());
                finished_list.add(t);
                ready_list.remove(t);
                writeToDB();
            }
            else {
                getWait_list().add(t);
                ready_list.remove(t);
            }
        }
        else {
            ready_list.remove(t);
            getWait_list().add(t);
        }
    }
    
    // Trata um problema que ocorria quando o E de uma transação colocava na lista de espera o programa encerrava a execução.
    public void senpai(){
        ready_list.add(wait_list.get(0));
        wait_list.remove(0);
        startScheduling();
    }
    
    public boolean isLocked(String transaction, Data data) {
        for (Lock lock : locks) {
            if (lock.getData().getLabel().equals(data.getLabel())) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isLockedBy(String transaction, Data data){
        for(Lock lock : locks){
            if(lock.getData().getLabel().equals(data.getLabel())
                    && lock.getTransaction().equals(transaction)){
                return true;
            }
        }
        return false;
    }

    public boolean isLockedXBy(String transaction, Data data) {
        for (Lock lock : locks) {
            if (lock.getTransaction().equals(transaction)
                    && lock.getData().getLabel().equals(data.getLabel())
                    && lock.getType() == Lock.TYPE.LX) {
                return true;
            }
        }
        return false;
    }

    public boolean isLockedS(Data data) {
        if (locks.stream().anyMatch((lock) -> (lock.getData().getLabel().equals(data.getLabel())
                && lock.getType() == Lock.TYPE.LS))) {
            return true;
        }
        return false;
    }

    public boolean isLockedSBy(String transaction, Data data) {
        for (Lock lock : locks) {
            if (lock.getTransaction().equals(transaction)
                    && lock.getData().getLabel().equals(data.getLabel())
                    && lock.getType() == Lock.TYPE.LS) {
                return true;
            }
        }
        return false;
    }

    public void switchLock(String transaction, Data data) {
        for (Lock lock : locks) {
            if (lock.getTransaction().equals(transaction)
                    && lock.getData().getLabel().equals(data.getLabel())
                    && lock.getType() == Lock.TYPE.LS) {
                lock.setType(Lock.TYPE.LX);
            }
        }
    }

    public void printLocks() {
        System.out.println("LOCKS");
        for (Lock lock : locks) {
            System.out.println("(" + lock.getTransaction() + ", " + lock.getData().getLabel() + ", " + lock.getType() + ")");
        }
    }

    public void addToReadyList(Transaction t) {
        ready_list.add(t);
    }

    private Transaction getFirst(ArrayList<Transaction> list) {
        return list.get(0);
    }

    public void printReadyList() {
        System.out.println("READY LIST");
        for (Transaction t : getReady_list()) {
            t.print();
        }
    }

    public void printFinishedList() {
        System.out.println("\n\n FINISHED LIST");
        for (Transaction t : finished_list) {
            t.print();
        }
    }

    public void printWaitList() {
        System.out.println("\n\n WAITs LIST");
        for (Transaction t : wait_list) {
            t.print();
        }
    }

    /**
     * @return the ready_list
     */
    public ArrayList<Transaction> getReady_list() {
        return ready_list;
    }

    private ArrayList<Data> getDatasLockedBy(String transaction) {
        ArrayList<Data> aux = new ArrayList<Data>();

        for (Lock lock : locks) {
            if (lock.getTransaction().equals(transaction)) {
                aux.add(lock.getData());
            }
        }
        return aux;
    }
    
    private ArrayList<Lock> getLocksLockedBy(String transaction) {
        ArrayList<Lock> aux = new ArrayList<Lock>();

        for (Lock lock : locks) {
            if (lock.getTransaction().equals(transaction)) {
                aux.add(lock);
            }
        }
        return aux;
    }

    private int getTransactionsByDataCount(Data data) {
        ArrayList<String> aux = new ArrayList<>();
        Set<String> set = new HashSet<>();       
        
        for (Lock lock : locks) {
            if (lock.getData().getLabel().equals(data.getLabel())) {
                aux.add(lock.getTransaction());
            }
        }
        return aux.stream().distinct().collect(Collectors.toList()).size();
    }        

    /**
     * @return the wait_list
     */
    public ArrayList<Transaction> getWait_list() {
        return wait_list;
    }

    /**
     * @return the finished_list
     */
    public ArrayList<Transaction> getFinishedList() {
        return finished_list;
    }
    
    public void writeToDB(){
        Transaction output = finished_list.get(finished_list.size() -1);
        
        try{
            PreparedStatement stm = conn.prepareStatement("INSERT INTO output (indiceTransacao,"
                                                                           + "operacao,"
                                                                           + "itemDado,"
                                                                           + "timestampj)"
                                                                           + "VALUES"
                                                                           + "(?, ?, ?, ?);");
            stm.setString(1, output.getLabel());
            stm.setString(2, output.getOperation().getOperation_type());
            stm.setString(3, output.getOperation().getData().getLabel());
            stm.setString(4, new SimpleDateFormat("yyyyMMdd_HHmmss").format(output.getOperation().getTimestamp()));
            stm.executeUpdate();
            stm.close();
        } catch (SQLException e){
            Logger.getLogger(Consumer.class.getName()).log(Level.SEVERE, null, e);
        }
        
    }
}