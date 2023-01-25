package it.unipi.dii.inginf.lsdb.scouting.controller;

import it.unipi.dii.inginf.lsdb.scouting.model.Session;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import it.unipi.dii.inginf.lsdb.scouting.model.Player;
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


public class PlayerSnapshotController {
	private Neo4jDriver neo4jDriver;
    private Session appSession;
    private MongoDBDriver mongoDBDriver;
    @FXML private ImageView userSnapImg;
    @FXML private Label playerSnapFullName;
    @FXML private Label playerSnapRole;
    @FXML private Label playerSnapTeam;
    @FXML private Label playerSnapAge;
    @FXML private Label playerSnapFoot;
    @FXML private Text textFoot;
    @FXML private Label playerSnapLike;
    @FXML private AnchorPane playerSnapMain;
    @FXML private ImageView playerSnapImg;
    private Player player;

    /**
     * Initialization functions
     */
    public void initialize()
    {
        neo4jDriver = Neo4jDriver.getInstance();
        appSession = Session.getInstance();
        playerSnapMain.setOnMouseClicked(mouseEvent -> handleClickOnSnap(mouseEvent));
    }

    /**
     * Set the snapshot with the given player
     * @param u  Object player with the given information necessary to the snap
     */
    public void setPlayerSnap(Player p)
    {
    	this.player = p;
    	if(p.getPicture()==null)
    	{
    		int codImg = player.getCodPlayer() - 1;
    		File directory = new File("..\\..\\immagini/immagine" + codImg + ".png");
    		if(directory==null)
    			playerSnapImg.setImage(new Image("/img/genericUser.png"));
    		else {
    			try {
    				String s1= directory.getCanonicalPath();
    				playerSnapImg.setImage(new Image(s1));
    				this.player.setPicture(s1);
    			}
    			catch(Exception ex)
    			{
    				ex.printStackTrace();
    			}
    		}
    	}
        playerSnapFullName.setText(p.getFullName());
        playerSnapAge.setText(Integer.toString(p.getAge()));
        playerSnapTeam.setText(p.getTeam());
        playerSnapRole.setText(p.getRole());
        
        if(p.getFoot() != null) {
        	textFoot.setVisible(true);
        	playerSnapFoot.setText(p.getFoot());
        }
        
        if(p.getAvgReportsRate() > 0) {
        	textFoot.setText("Rate");
        	textFoot.setVisible(true);
        	playerSnapFoot.setText(Double.toString(p.getAvgReportsRate()));
        }
        
        else {
        	textFoot.setVisible(false);
        	playerSnapFoot.setText("");
        }

    }

    /**
     * Handle the click on the snap and it changes page going in the profile page of the player of the clicked snap
     * @param mouseEvent    Event that leads to the handler
     */
    private void handleClickOnSnap(MouseEvent mouseEvent)
    {
        PlayerPageController playerPageController = (PlayerPageController)
                Utils.changeScene("/playerPage.fxml", mouseEvent);
        playerPageController.setPlayer(this.player);
    }
}
