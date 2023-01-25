/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author andreasottile
 */
package it.unipi.dii.inginf.lsdb.scouting.model;

public class Session {
    private static Session instance = null; // Singleton
    private User loggedUser;

    public static Session getInstance()
    {
        if(instance==null)
            instance = new Session();
        return instance;
    }

    private Session () {}

    public static void setLoggedUser(User loggedUser) {
        instance.loggedUser = loggedUser;
    }
    public void updateLoggedUserInfo(User u) {instance.loggedUser = u;}
    public User getLoggedUser() {
        return loggedUser;
    }

}