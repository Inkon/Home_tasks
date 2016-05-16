package ru.ifmo.ctddev.zernov.bank;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.rmi.RemoteException;
import java.util.Random;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BankTests {
    private int currentPerson = 2;
    private final int personCount = 10;

    @Before
    public void startClient(){
        try {
            Client.start();
        } catch (RemoteException e){
            e.printStackTrace();
        }
    }

    @Test
    public void test01_oneAccount(){
        System.err.println("Test 1 | One account");
        try{
            Client.addRemotePerson("Name 0", "Surname 0", 0);
            Assert.assertEquals("On new account must be 0", 0, Client.getAccount(0,0).getAmount());
        } catch (RemoteException e){
            e.printStackTrace();
        }
    }

    @Test
    public void test02_oneUser(){
        System.err.println("Test 2 | One user");
        try{
            Client.main(new String[]{"Gleb", "Zernov", "1", "1", "100"});
            Assert.assertEquals("On operated account must be 100", 100 , Client.getAccount(1,1).getAmount());
        } catch (RemoteException e){
            e.printStackTrace();
        }
    }

    @Test
    public void test03_multiplyAccounts(){
        System.err.println("Test 3 | Multiply accounts");
        try {
            int accs = 100;
            addNonExistingPerson(false);
            Account[] accounts = new Account[accs];
            for (int i = 0; i < accs; i++){
                accounts[i] = Client.getAccount(currentPerson, i);

            }

            int[] amount = new int[accs];
            Random random = new Random();
            for (int i = 0; i < accs; i++) {
                for (int j = 0; j < accs; j++) {
                    amount[j] = random.nextInt(Integer.MAX_VALUE);
                    accounts[j].setAmount(amount[j]);
                }
            }
            for (int j = 0; j < accs; j++){
                Assert.assertEquals("Wrong amount on account", amount[j], Client.getAccount(currentPerson, j).getAmount());
            }
        } catch (RemoteException e){
            e.printStackTrace();
        }
    }

    @Test
    public void test04_multiplyReference(){
        System.err.println("Test 4 | Multiply person reference");
        try {
            addNonExistingPerson(false);
            Client.getAccount(currentPerson, 0).setAmount(currentPerson);
            currentPerson--;
            addNonExistingPerson(false);
            Assert.assertEquals("Money zeroing", currentPerson, Client.getAccount(currentPerson, 0).getAmount());
        } catch (RemoteException e){
            e.printStackTrace();
        }
    }


    @Test
    public void test05_multiplyRemoteUsers(){
        System.err.println("Test 5 | Multiply remote persons");
        personTest(0, false, false);
    }

    @Test
    public void test06_multiplyLocalUsers(){
        System.err.println("Test 6 | Multiply local persons");
        personTest(1, true, true);
    }

    @Test
    public void test07_localRemoteUsers(){
        System.err.println("Test 7 | Send local request remote persons");
        personTest(2, true, false);
    }

    @Test
    public void test08_remoteLocalUsers(){
        System.err.println("Test 8 | Send remote request local persons");
        personTest(3, false, true);
    }

    @Test
    public void test09_wrongUserData(){
        System.err.println("Test 9 | Wrong user data");
        try {
            personTest(0, false, true);
            for (int i = currentPerson - personCount + 1; i <= currentPerson; i++) {
                Client.getAccount(i, 0).setAmount(i);
            }
            for (int i = currentPerson  - personCount + 1; i <= currentPerson + personCount; i++){
                 Client.addRemotePerson("NewName " + i, "NewSurname " + i, i);
            }
            for (int i = currentPerson - personCount + 1; i <= currentPerson; i++) {
                if (Client.getAccount(i, 0) != null) {
                    Assert.assertEquals("Wrong amount on account " + Client.findRemotePerson(i).getName(), 0, Client.getAccount(i, 0).getAmount());
                }
            }
        } catch (RemoteException e){
            e.printStackTrace();
        }

    }

    private void personTest(int shift, boolean send, boolean request){
        currentPerson += personCount * shift;
        try{
            int initPerson = this.currentPerson + 1;
            for (int i = 0; i < personCount; i++) {
                addNonExistingPerson(send);
            }
            for (int i = 0; i < personCount; i++){
                checkPerson(initPerson + i, request);
            }
        } catch (RemoteException e){
            e.printStackTrace();
        }
    }

    private void addNonExistingPerson(boolean local) throws RemoteException{
        currentPerson++;
        if (!local) {
            Client.addRemotePerson("Name " + currentPerson, "Surname " + currentPerson, currentPerson);
        } else {
            Client.sendLocalPerson(new LocalPerson("Name " + currentPerson, "Surname " + currentPerson, currentPerson));
        }
    }

    private void checkPerson(int passId, boolean local) throws RemoteException{
        Person person = local ? Client.findLocalPerson(passId) : Client.findRemotePerson(passId);
        Assert.assertNotEquals("Can't find person", null, person);
        if (person != null) {
            Assert.assertEquals("Wrong name", "Name " + (passId), person.getName());
            Assert.assertEquals("Wrong Surname", "Surname " + (passId), person.getSurname());
        }
    }
}
