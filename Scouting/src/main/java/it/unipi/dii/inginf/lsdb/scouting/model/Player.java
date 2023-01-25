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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Player {
    private String fullName;
    private String role;
    private int age;
    private String team;
    private int codPlayer;
    private String foot;
    private int rate;
    private String picture;
    private double avgReportsRate;
    private int numWishes;

    public Player(){}

    public Player(String fullName, String role, int age, String team, int codPlayer)
    {
        this.setFullName(fullName);
        this.setCodPlayer(codPlayer);
        this.setAge(age);
        this.setTeam(team);
        this.setRole(role);
    }

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
	
	public void setRate (int rate) {
		this.rate= rate;
	}
	
	public int getRate() {
		return rate;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public int getCodPlayer() {
		return codPlayer;
	}

	public void setCodPlayer(int codPlayer) {
		this.codPlayer = codPlayer;
	}

    @Override
    public String toString() {
        return fullName;
    }

	public String getFoot() {
		return foot;
	}

	public void setFoot(String foot) {
		this.foot = foot;
	}
	
	public String getPicture() {
	        return picture;
	    }
	   
	public void setPicture(String picture) {
	        this.picture = picture;
	    }

	public double getAvgReportsRate() {
		return avgReportsRate;
	}

	public void setAvgReportsRate(double avgReportsRate) {
		this.avgReportsRate = avgReportsRate;
	}

	public int getNumWishes() {
		return numWishes;
	}

	public void setNumWishes(int numWishes) {
		this.numWishes = numWishes;
	}
}   

