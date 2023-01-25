/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author andreasottile
 */
//bug su pressione tasto home inizia a fare in loop ciclo for degli snap

package it.unipi.dii.inginf.lsdb.scouting.model;

import java.util.Date;


public class User {
    private int userId;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String password;
    private char role; // O: Observer , F: Football Director, A: Admin
    private int follower;
    private int following;
    private Integer numReports;
    private int numComments;
    private String picture;

    public User(){}

    public User(String firstName, String lastName, String username, String password, char role, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
    }
    
    public User(String firstName, String lastName, String username, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
    }

    public User(String firstName, String lastName, String username, int follower, int following, int added, char role)
    {
    	this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.follower = follower;
        this.following = following;
        this.numReports = added;
        this.role = role;
    }

	public int getUserId() {
        return userId;
    }
    
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
    
    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
    public char getRole() {
        return role;
    }
    
    public int getFollower() {
        return follower;
    }

    public int getFollowing() {
        return following;
    }

    public int getNumReports() {
       if (this.numReports == null)
    	   this.setNumReports(0);
    	return numReports;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setRole(char role) {
    	this.role = role;
    }
    
    public void setFollower(int follower) {
        this.follower = follower;
    }

    public void setFollowing(int following) {
        this.following = following;
    }

    public void setNumReports (int numReports) {
        this.numReports = numReports;
    }

	public int getNumComments() {
		return numComments;
	}

	public void setNumComments(int numComments) {
		this.numComments = numComments;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

}
