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

public class Report {
    private String title;
    private int codPlayer;
    private String playerRole;
    private int playerAge;
    private Date creationTime;
    private String picture;
    private String authorUsername;
    private List<Comment> comments;
    private String fullName;
    private int rate;
    private String playerTeam;
    private String review;
    private int codReport;
    private int userID;

    public Report(){}

    public Report(String title, int codPlayer, 
                  int rate, Date creationTime, String picture,
                  String authorUsername, List<Comment> comments)
    {
        this.title = title;
        this.picture = picture;
        this.codPlayer = codPlayer;
        this.creationTime = creationTime;
        this.rate = rate;
        this.authorUsername = authorUsername;
        this.comments = comments;
    }

    public Report(String fullName, String authorUsername, int rate, Date creationTime){
    	this.fullName = fullName;
    	this.authorUsername = authorUsername;
    	this.rate = rate;
    	this.creationTime = creationTime;
    }
    
    public Report(String fullName, int codReport, int rate, String review){
    	this.fullName = fullName;
    	this.codReport = codReport;
    	this.rate = rate;
    	this.review = review;
    }
    
    public Report(int codReport, String fullName, String authorUsername, int rate, Date creationTime, String playerRole, int playerAge, String playerTeam){
    	this.codReport = codReport;
    	this.fullName = fullName;
    	this.authorUsername = authorUsername;
    	this.rate = rate;
    	this.creationTime = creationTime;
    	this.playerRole = playerRole;
    	this.playerAge = playerAge;
    	this.playerTeam = playerTeam;
    }

    public Report(int codReport, int codPlayer, int rate, String review, int userID)
    {
        this.codReport = codReport;
        this.codPlayer = codPlayer;
        this.rate = rate;
        this.review = review;
        this.userID = userID;
    }

    public String getTitle() {
        return title;
    }

    public String getPicture() {
        return picture;
    }

    public int getCodPlayer() {
        return codPlayer;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public List<Comment> getComments() {
        return comments;
    }


    public void setTitle(String title) {
        this.title = title;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setCodPlayer(int codPlayer) {
        this.codPlayer = codPlayer;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }


    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public void addComments(Comment comment) {this.comments.add(comment);}

    @Override
    public String toString() {
        return "Report{" +
                "codReport='" + codReport + '\'' +
                ", codPlayer='" + codPlayer + '\'' +
                ", rate=" + rate +
                ", creationTime=" + creationTime +
                ", picture='" + picture + '\'' +
                ", authorUsername='" + authorUsername + '\'' +
                ", review=" + review +
                '}';
    }

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public int getRate() {
		return rate;
	}

	public String getPlayerRole() {
		return playerRole;
	}

	public void setPlayerRole(String playerRole) {
		this.playerRole = playerRole;
	}

	public int getPlayerAge() {
		return playerAge;
	}

	public void setPlayerAge(int playerAge) {
		this.playerAge = playerAge;
	}

	public String getPlayerTeam() {
		return playerTeam;
	}

	public void setPlayerTeam(String playerTeam) {
		this.playerTeam = playerTeam;
	}

	public String getReview() {
		return review;
	}

	public void setReview(String review) {
		this.review = review;
	}

	public int getCodReport() {
		return codReport;
	}

	public void setCodReport(int codReport) {
		this.codReport = codReport;
	}
	
	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}
}   

