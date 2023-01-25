/*
 DA METTERE CAMPI REPORT PER FUNZIONIIIIIIIIIIIIIIII
 */

package it.unipi.dii.inginf.lsdb.scouting.controller;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import it.unipi.dii.inginf.lsdb.scouting.model.Player;
import it.unipi.dii.inginf.lsdb.scouting.model.Report;
import it.unipi.dii.inginf.lsdb.scouting.model.Session;
import it.unipi.dii.inginf.lsdb.scouting.model.User;
import it.unipi.dii.inginf.lsdb.scouting.persistence.MongoDBDriver;
import it.unipi.dii.inginf.lsdb.scouting.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class ReportSnapshotController {

    @FXML private Pane snapPane;
    @FXML private Label snapTitle;
    @FXML private Label snapUser;
    @FXML private Label snapRate;
    @FXML private Label snapCreationTime;
    @FXML private Label snapPlayerRole;
    @FXML private Label snapPlayerAge;
    @FXML private Label snapProtein;
    @FXML private ImageView snapImg;

    private Report report; // report shows in this snapshot
    private Neo4jDriver neo4jDriver;
    private Session appSession;

    /**
     * Initialization functions
     */
    public void initialize ()
    {
        appSession = Session.getInstance();
        neo4jDriver = Neo4jDriver.getInstance();
        snapPane.setOnMouseClicked(mouseEvent -> showMoreInformation(mouseEvent));
    }

    /**
     * This function is used to show the complete information of the report in a new page
     * @param mouseEvent    The event that leads to show the report completely (click of the mouse in the pane)
     */
    private void showMoreInformation(MouseEvent mouseEvent) {
        if(report.getReview() == null) {             //DEBUG
            Report reportMongoDB = MongoDBDriver.getInstance().getReportFromCodReport(report.getCodReport());
            this.report.setReview(reportMongoDB.getReview());
        }
        
        if(report.getCodPlayer() == 0) {             //DEBUG
            Report reportMongoDB = MongoDBDriver.getInstance().getReportFromCodReport(report.getCodReport());
            this.report.setCodPlayer(reportMongoDB.getCodPlayer());
        }
        
        if(report.getComments() == null) {             //DEBUG
            Report reportMongoDB = MongoDBDriver.getInstance().getReportFromCodReport(report.getCodReport());
            if(reportMongoDB.getComments() != null && reportMongoDB.getComments().size() > 0)
            	this.report.setComments(reportMongoDB.getComments());
        }
        
        ReportPageController reportPageController =
                (ReportPageController) Utils.changeScene("/reportPage.fxml", mouseEvent);
        reportPageController.setReport(report);
    }

    public void setReport (Report report)
    {
        this.report = report;
        int codImg = report.getCodPlayer() - 1;
        File directory = new File("..\\..\\immagini/immagine" + codImg + ".png");
            
        if(directory==null)
            snapImg.setImage(new Image("/img/genericUser.png"));
        else {
        	try {String s1= directory.getCanonicalPath();
        	snapImg.setImage(new Image(s1));
        	this.report.setPicture(s1);}
        	catch(Exception ex)
        	{
        		ex.printStackTrace();
        	}
        }
        Player player = MongoDBDriver.getInstance().getCodPlayerFromCodPlayer(report.getCodPlayer());
        
        if(report.getFullName() == null)
        	snapTitle.setText(player.getFullName());
        else
        	snapTitle.setText(report.getFullName());
        try {
        	if(!report.getAuthorUsername().equals("") && report.getAuthorUsername() != null) 
        		snapUser.setText(report.getAuthorUsername());
        }
        catch(Exception e) {
        	snapUser.setText(neo4jDriver.getUserUsernameByUserID(report.getUserID()));
        }
        
        if (report.getRate() != 0)
        	snapRate.setText(Integer.toString(report.getRate()));
        
        if (report.getPlayerAge() != 0)
        	snapPlayerAge.setText(Integer.toString(report.getPlayerAge()));
        else
        	snapPlayerAge.setText(Integer.toString(player.getAge()));
        
        if(report.getPlayerRole() == null)
        	snapPlayerRole.setText(player.getRole());
        else
        	snapPlayerRole.setText(report.getPlayerRole());
        
		DateFormat a = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		String dateTime = a.format(report.getCreationTime());
        snapCreationTime.setText(dateTime);
    }
}
