package it.unipi.dii.inginf.lsdb.scouting.controller;

import java.util.Date;

//import org.controlsfx.control.textfield.TextFields;

import it.unipi.dii.inginf.lsdb.scouting.model.Player;
import it.unipi.dii.inginf.lsdb.scouting.model.Report;
import it.unipi.dii.inginf.lsdb.scouting.model.Session;
import it.unipi.dii.inginf.lsdb.scouting.persistence.MongoDBDriver;
import it.unipi.dii.inginf.lsdb.scouting.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;


import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class AddReportPageController {
    private Neo4jDriver neo4jDriver;
    private MongoDBDriver mongoDBDriver;
    private Session appSession;
    private Report report;
    private Player player;
    private int i;
    @FXML private ImageView homeImg;
    @FXML private ImageView profileImg;
    @FXML private ImageView discoveryImg;
    @FXML private ImageView logoutImg;
    @FXML private ComboBox addPlayer;
    @FXML private TextField addRate;
    @FXML private TextField addPlayerText;
    @FXML private TextArea addReview;
    @FXML private Button submit;
    @FXML private Button clear;
    @FXML private Text titlePage;
    @FXML private ImageView iconOfTitlePage;

    /**
     * Initialization functions
     */
    public void initialize ()
    {
        neo4jDriver = Neo4jDriver.getInstance();
        mongoDBDriver = MongoDBDriver.getInstance();
        appSession = Session.getInstance();
        homeImg.setOnMouseClicked(mouseEvent -> clickOnHomepageToChangePage(mouseEvent));
        discoveryImg.setOnMouseClicked(mouseEvent -> clickOnDiscImgtoChangePage(mouseEvent));
        logoutImg.setOnMouseClicked(mouseEvent -> clickOnLogoutImg(mouseEvent));
        profileImg.setOnMouseClicked(mouseEvent -> clickOnProfileToChangePage(mouseEvent));
        submit.setOnMouseClicked(mouseEvent -> submitNewReport());
        clear.setOnMouseClicked(mouseEvent -> clearAllFields());
        addPlayerText.setVisible(false);
    }
   
	/**
     * Using the report (argument) to fill the form in order to edit an already present report
     * @param report
     */
    public void setPlayerToReport(Player player)
    {
        titlePage.setText("New Report");
        iconOfTitlePage.setImage(new Image("img/edit.png"));
        this.player = player;
        if(player == null)
        {
        addPlayer.setDisable(false);
        addPlayer.setVisible(false);
        addPlayerText.setVisible(true);
        this.i=3;
        }
        else
        {
        addPlayer.setValue(player.getFullName());
        addPlayer.setDisable(true);
        this.i = 1;
        }
        
    }
    /**
     * Using the report (argument) to fill the form in order to edit an already present report
     * @param report
     */
    public void setReportToUpdate(Report report)
    {
        titlePage.setText("Update Report");
        iconOfTitlePage.setImage(new Image("img/edit.png"));
        this.report = report;
        addPlayer.setValue(report.getFullName());
        addPlayer.setDisable(true);
        addRate.setText(String.valueOf(report.getRate()));
        addReview.setText(report.getReview());
        this.i = 2;
    }

    /**
     * Control the fields and add new report in the DBs
     */
    private void submitNewReport()
    { 
    	int codPlayer;
        // If I write nothing in the field then getText return an empty String (not null!)
        if(addRate.getText().isEmpty() || addReview.getText().isEmpty())
        {
            Utils.showErrorAlert("Rate and Review are mandatory fields");
        }
        else
        {
            Date ts;
            if (this.i==1 || this.i==3) {
                ts = new Date();
            }
            else
                ts = report.getCreationTime();
            
            String review = addReview.getText();
            int rate=0;

            try {
                rate = Integer.parseInt(addRate.getText());
            }
            catch (NumberFormatException e){
                Utils.showErrorAlert("Error!\nThe Rate field must conteins only numbers!");
                return;
            }
            try {
                if(this.i==3)
                {
                	if(mongoDBDriver.getCodPlayerFromFullName2(addPlayerText.getText(), 20, 20)==null)
                	{
                    Utils.showErrorAlert("Error!\n No Players with that name!");
                    return;
                	}
                	codPlayer = mongoDBDriver.getCodPlayerFromFullName2(addPlayerText.getText(), 20, 20).getCodPlayer();
                }
                else
                {
                codPlayer = mongoDBDriver.getCodPlayerFromFullName2(addPlayer.getValue().toString(), 20, 20).getCodPlayer();
                }
            }
            catch (NumberFormatException e){
                Utils.showErrorAlert("Error!\n No Players with that name!");
                return;
            }
            
           
            int newCodReport = neo4jDriver.getNewCodReport();
            Report newRep = new Report(newCodReport, codPlayer, rate, review, Session.getInstance().getLoggedUser().getUserId());
            
            if(addPlayer.isEditable() || !addPlayer.isDisable()) {
                if(neo4jDriver.newReport(newRep))
                {
                    //If neo is ok, perform mongo
                    if(!mongoDBDriver.addReport(newRep))
                    {
                        // if mongo is not ok, remove the previously added report
                        neo4jDriver.deleteReport(newRep);
                        Utils.showErrorAlert("Error in adding the report");
                    }
                    else
                    {
                        Utils.showInfoAlert("Report succesfully added");
                    }
                }
                clearAllFields();
            }
            else {
            	newRep.setCodReport(this.report.getCodReport());
                Report oldRep = mongoDBDriver.getReportFromCodReport(this.report.getCodReport());
                if(oldRep!=null)
                {
                    if(mongoDBDriver.editReport(newRep))
                    {
                        //If mongo is ok, perform neo
                        if(!neo4jDriver.updateReport(newRep))
                        {
                            // if neo is not ok, reset the previously modified report
                            mongoDBDriver.editReport(oldRep);
                            
                            Utils.showErrorAlert("Error in edit the report");
                        }
                        else
                        {
                            Utils.showInfoAlert("Report succesfully edited");
                        }

                    }
                }
            }
        }
    }

    private void clearAllFields()
    {
        if (addPlayer.isEditable() || !addPlayer.isDisable())
        	addPlayer.setValue("");
        addRate.setText("");
        addReview.setText("");
    }

    /**
     * Function that let the navigation into the ui ---> homepage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnHomepageToChangePage(MouseEvent mouseEvent){
        Utils.changeScene("/homepage.fxml", mouseEvent);
    }

    /**
     * Function that let the logout action, by going into the welcome page
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnLogoutImg(MouseEvent mouseEvent){
        Utils.changeScene("/welcome.fxml", mouseEvent);
    }

    /**
     * Function that let the navigation into the ui ---> discoveryPage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnDiscImgtoChangePage(MouseEvent mouseEvent){
        Utils.changeScene("/discoveryPage.fxml", mouseEvent);
    }

    /**
     * Function used to handle the click on the profile icon
     * @param mouseEvent    event that represents the click on the icon
     */
    private void clickOnProfileToChangePage(MouseEvent mouseEvent){
        ProfilePageController profilePageController = (ProfilePageController)
                Utils.changeScene("/profilePage.fxml", mouseEvent);
        profilePageController.setProfile(Session.getInstance().getLoggedUser());
    }

}
