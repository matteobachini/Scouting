
package it.unipi.dii.inginf.lsdb.scouting.controller;

import it.unipi.dii.inginf.lsdb.scouting.model.Comment;
import it.unipi.dii.inginf.lsdb.scouting.model.Player;
import it.unipi.dii.inginf.lsdb.scouting.model.Report;
import it.unipi.dii.inginf.lsdb.scouting.model.Session;
import it.unipi.dii.inginf.lsdb.scouting.model.User;
import it.unipi.dii.inginf.lsdb.scouting.persistence.MongoDBDriver;
import it.unipi.dii.inginf.lsdb.scouting.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.File;
import java.util.Date;

/**
 * Controller for the page of the report
 */
public class PlayerPageController {

    @FXML private ImageView homeImg;
    @FXML private ImageView discoveryImg;
    @FXML private ImageView profileImg;
    @FXML private ImageView playerImg;
    @FXML private ImageView playerWishList;
    @FXML private ImageView logoutPic;
    @FXML private ImageView addReportOrMyProfileImg;
    @FXML private TextField fullname;
    @FXML private TextField team;
    @FXML private TextField role;
    @FXML private TextField age;
    @FXML private TextField playerFoot;
    @FXML private TextField playerRate;
    @FXML private TextField playerNumWishes;
    @FXML private VBox playerVBox;
    @FXML private ImageView addWishlist;

    private Player player;
    private Session appSession;
    private Neo4jDriver neo4jDriver;
    private MongoDBDriver mongoDBDriver;

    /**
     * Initialization functions
     */
    public void initialize ()
    {
        homeImg.setOnMouseClicked(mouseEvent -> clickOnHomepageToChangePage(mouseEvent));
        profileImg.setOnMouseClicked(mouseEvent -> clickOnProfileToChangePage(mouseEvent));
        discoveryImg.setOnMouseClicked(mouseEvent -> clickOnDiscoveryToChangePage(mouseEvent));
        logoutPic.setOnMouseClicked(mouseEvent -> clickOnLogoutImg(mouseEvent));
        playerVBox.setAlignment(Pos.CENTER);
        appSession = Session.getInstance();
        neo4jDriver = Neo4jDriver.getInstance();
        mongoDBDriver = MongoDBDriver.getInstance();
        addReportOrMyProfileImg.setOnMouseClicked(mouseEvent -> clickOnEditButton(mouseEvent));
        if(appSession.getLoggedUser().getRole()=='F' || appSession.getLoggedUser().getRole()=='A')
        	addReportOrMyProfileImg.setVisible(false);
    }


    /**
     * Setters for the report, in which we also set the correct value to show
     * @param r    Report to show
     */
    public void setPlayer(Player p) {
        this.player = p;
        
        Player player = MongoDBDriver.getInstance().getCodPlayerFromCodPlayer(p.getCodPlayer());
        
        
        fullname.setText(player.getFullName());
        team.setText(player.getTeam());
        role.setText(player.getRole());
        age.setText(Integer.toString(player.getAge()));
        playerFoot.setText(player.getFoot());
        playerRate.setText(Integer.toString(player.getRate()));
        int codImg = player.getCodPlayer() - 1;
        File directory = new File("..\\..\\immagini/immagine" + codImg + ".png");
        if(directory==null)
        	 playerImg.setImage(new Image("/img/genericUser.png"));
        else {
               try {
              		 String s1= directory.getCanonicalPath();
              		 playerImg.setImage(new Image(s1));
              		 this.player.setPicture(s1);
              	 	}
               catch(Exception ex)
      	             	{
      	                 ex.printStackTrace();
      	             	}
              		}
        
        if(player.getNumWishes() == 0)
        	playerNumWishes.setText(Integer.toString(Neo4jDriver.getInstance().getPlayerNumWishes(player)));
        
        if(appSession.getLoggedUser().getRole() != 'F')
        	addWishlist.setVisible(false);

        if (neo4jDriver.isPlayerWishedByUser(player.getCodPlayer(), Session.getInstance().getLoggedUser().getUserId()))
        	 addWishlist.setImage(new Image("img/alreadyFollowed_profile.png"));
        else
        	addWishlist.setImage(new Image("img/follow_profile.png"));

         addWishlist.setOnMouseClicked(mouseEvent -> clickOnAddWishlist());
         
    }
    
    private void clickOnAddWishlist()
    {
        if (neo4jDriver.isPlayerWishedByUser(player.getCodPlayer(), Session.getInstance().getLoggedUser().getUserId()))
        {
            // I want to remove the player from the football director's wishlist
            neo4jDriver.removeFromWishlist(player.getCodPlayer(), Session.getInstance().getLoggedUser().getUserId());
            addWishlist.setImage(new Image("img/follow_profile.png"));
        }
        else
        {
            // I want to follow a user
            neo4jDriver.addToWishlist(player.getCodPlayer(), Session.getInstance().getLoggedUser().getUserId());
            addWishlist.setImage(new Image("img/alreadyFollowed_profile.png"));        }
    }

    /**
     * Function who handle the editButton
     * @param mouseEvent    Event that leads to the handler
     */
    private void clickOnEditButton(MouseEvent mouseEvent){
        AddReportPageController addReportPageController = (AddReportPageController)
                Utils.changeScene("/addReport.fxml", mouseEvent);
        addReportPageController.setPlayerToReport(player);
    }


    /**
     * Function used to handle the click on the homepage icon
     * @param mouseEvent    event that represents the click on the icon
     */
    private void clickOnHomepageToChangePage(MouseEvent mouseEvent){
        Utils.changeScene("/homepage.fxml", mouseEvent);
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

    /**
     * Function used to handle the click on the discovery icon
     * @param mouseEvent    event that represents the click on the icon
     */
    private void clickOnDiscoveryToChangePage(MouseEvent mouseEvent){
        Utils.changeScene("/discoveryPage.fxml", mouseEvent);
    }

    /**
     * Function that let the logout action, by going into the welcome page
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnLogoutImg(MouseEvent mouseEvent){
        Utils.changeScene("/welcome.fxml", mouseEvent);
    }
}

