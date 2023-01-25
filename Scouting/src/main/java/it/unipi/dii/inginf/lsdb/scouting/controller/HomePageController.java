/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author andreasottile
 */
package it.unipi.dii.inginf.lsdb.scouting.controller;

import it.unipi.dii.inginf.lsdb.scouting.model.Report;
import it.unipi.dii.inginf.lsdb.scouting.model.Session;
import it.unipi.dii.inginf.lsdb.scouting.persistence.MongoDBDriver;
import it.unipi.dii.inginf.lsdb.scouting.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import java.util.List;

public class HomePageController {
    private Neo4jDriver neo4jDriver;
    private MongoDBDriver mongoDBDriver;
    private Session session;
    @FXML private VBox mainPage;
    @FXML private ImageView profileImg;
    @FXML private ImageView homeImg;
    @FXML private ImageView discoveryImg;
    @FXML private ImageView logoutPic;
    @FXML private Button nextButton;
    @FXML private Button previousButton;

    private final int HOW_MANY_SNAPSHOT_TO_SHOW = 10; //standard case
    private int page; // number of page (at the beginning at 0), increase with nextButton and decrease with previousButton

    /**
     * Initialization function for HomePageController
     */
    public void initialize()
    {
        neo4jDriver = Neo4jDriver.getInstance();
        mongoDBDriver = MongoDBDriver.getInstance();
        session = Session.getInstance();
        System.out.print(session.getLoggedUser().getUsername());
        List<Report> reports = neo4jDriver.getHomepageReportSnap(0,
               HOW_MANY_SNAPSHOT_TO_SHOW, session.getLoggedUser().getUsername());
        Utils.addReportsSnap(mainPage, reports);

        if(reports.size()==0)
        {
            Text adv = new Text("No users are followed - starts from Discovery");
            adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
            mainPage.getChildren().add(adv);
        }

        profileImg.setOnMouseClicked(mouseEvent -> clickOnProfImgToChangePage(mouseEvent));
        homeImg.setOnMouseClicked(mouseEvent -> clickOnHomeImg(mouseEvent));
        discoveryImg.setOnMouseClicked(mouseEvent -> clickOnDiscImgtoChangePage(mouseEvent));
        logoutPic.setOnMouseClicked(mouseEvent -> clickOnLogoutImg(mouseEvent));
        page = 0;
        nextButton.setOnMouseClicked(mouseEvent -> clickOnNext());
        previousButton.setOnMouseClicked(mouseEvent -> clickOnPrevious());
        previousButton.setVisible(false); //in the first page it is not visible
    }

    /**
     * Function that let the navigation into the ui ---> profilePage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnProfImgToChangePage(MouseEvent mouseEvent){
        ProfilePageController profilePageController = (ProfilePageController)
                Utils.changeScene("/profilePage.fxml", mouseEvent);
        profilePageController.setProfile(Session.getInstance().getLoggedUser());
    }

    /**
     * Function that let the logout action, by going into the welcome page
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnLogoutImg(MouseEvent mouseEvent){
        Utils.changeScene("/welcome.fxml", mouseEvent);
    }

    /**
     * Function that let the navigation into the ui ---> homepage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnHomeImg(MouseEvent mouseEvent){
    	Utils.changeScene("/homepage.fxml", mouseEvent);    }
    
    /**
     * Function that let the navigation into the ui ---> discoveryPage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnDiscImgtoChangePage(MouseEvent mouseEvent){
        Utils.changeScene("/discoveryPage.fxml", mouseEvent);
    }

    /**
     * Function called when the user click on the previous button
     */
    private void clickOnPrevious(){
        Utils.removeAllFromPane(mainPage);
        page--;
        if (page < 1)
            previousButton.setVisible(false);
        Utils.addReportsSnap(mainPage,
                neo4jDriver.getHomepageReportSnap(HOW_MANY_SNAPSHOT_TO_SHOW*page,
                        HOW_MANY_SNAPSHOT_TO_SHOW, session.getLoggedUser().getUsername()));
    }

    /**
     * Function called when the user click on the next button
     */
    private void clickOnNext(){
        Utils.removeAllFromPane(mainPage);
        page++;
        if (page > 0)
            previousButton.setVisible(true);
        Utils.addReportsSnap(mainPage,
                neo4jDriver.getHomepageReportSnap(HOW_MANY_SNAPSHOT_TO_SHOW*page,
                        HOW_MANY_SNAPSHOT_TO_SHOW, session.getLoggedUser().getUsername()));
    }

}