package it.unipi.dii.inginf.lsdb.scouting.controller;

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
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

public class UserSnapshotController {
    private Neo4jDriver neo4jDriver;
    private Session appSession;
    private MongoDBDriver mongoDBDriver;
    @FXML private ImageView userSnapImg;
    @FXML private Label userSnapUsername;
    @FXML private Label userSnapFollower;
    @FXML private Label userSnapFirstName;
    @FXML private Label userSnapLastName;
    @FXML private Text labelComments;
    @FXML private Label userSnapComments;
    @FXML private AnchorPane userSnapMain;

    private User user;

    /**
     * Initialization functions
     */
    public void initialize()
    {
        mongoDBDriver = MongoDBDriver.getInstance();
        neo4jDriver = Neo4jDriver.getInstance();
        appSession = Session.getInstance();
        userSnapMain.setOnMouseClicked(mouseEvent -> handleClickOnSnap(mouseEvent));
    }

    /**
     * Set the snapshot with the given user
     * @param u  Object user with the given information necessary to the snap
     */
    public void setUserSnap(User u)
    {
        this.user = u;
        String type= "" + u.getRole();
        if(type.equals("O"))
        	userSnapImg.setImage(new Image("/img/obsIcon4.png"));
        else if(type.equals("F"))
        	userSnapImg.setImage(new Image("/img/fdIcon4.png"));
        else if(type.equals("A"))
        	userSnapImg.setImage(new Image("/img/admIcon4.png"));
        else 
        	userSnapImg.setImage(new Image("/img/genericUser.png"));
        userSnapUsername.setText(u.getUsername());
        userSnapFirstName.setText(u.getFirstName());
        userSnapLastName.setText(u.getLastName());
        
        if(u.getNumComments() > 0) {
        	labelComments.setVisible(true);
        	userSnapComments.setText(Integer.toString(u.getNumComments()));
        }
    }

    /**
     * Handle the click on the snap and it changes page going in the profile page of the user of the clicked snap
     * @param mouseEvent    Event that leads to the handler
     */
    private void handleClickOnSnap(MouseEvent mouseEvent)
    {
        ProfilePageController profilePageController = (ProfilePageController)
                Utils.changeScene("/profilePage.fxml", mouseEvent);
        profilePageController.setProfile(user);
    }
}
