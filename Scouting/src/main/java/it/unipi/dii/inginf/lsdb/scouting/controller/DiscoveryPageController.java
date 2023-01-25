package it.unipi.dii.inginf.lsdb.scouting.controller;

import it.unipi.dii.inginf.lsdb.scouting.model.Report;
import it.unipi.dii.inginf.lsdb.scouting.model.Session;
import it.unipi.dii.inginf.lsdb.scouting.model.User;
import it.unipi.dii.inginf.lsdb.scouting.model.Player;
import it.unipi.dii.inginf.lsdb.scouting.persistence.MongoDBDriver;
import it.unipi.dii.inginf.lsdb.scouting.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import it.unipi.dii.inginf.lsdb.scouting.controller.ProfilePageController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.skip;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Sorts.descending;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

public class DiscoveryPageController {
    private Neo4jDriver neo4jDriver;
    private MongoDBDriver mongoDBDriver;
    private Session appSession;
    @FXML private ImageView homepageIcon;
    @FXML private ImageView profilePageIcon;
    @FXML private ImageView logoutPic;
    @FXML private Button searchButton;
    @FXML private TextField searchBarTextField;
    @FXML private TextField searchBarYear;
    @FXML private Text textYear;
    @FXML private ComboBox searchComboBox;
    @FXML private VBox discoveryVBox;
    @FXML private Button nextButton;
    @FXML private Button previousButton;

    private final int HOW_MANY_REPORT_SNAPSHOT_TO_SHOW = 20;
    private final int HOW_MANY_USER_SNAPSHOT_TO_SHOW = 20;
    private final int HOW_MANY_MOST_COMMON_CATEGORIES_TO_SHOW = 5;
    private final int HOW_MANY_SNAPSHOT_FOR_EACH_COMMON_CATEGORY = 4;
    private final int LIKES_THRESHOLD_SECOND_LEVEL_SUGGESTION = 3;
    private final int HOW_MANY_SUGGESTED_REPORTS_FIRST_LVL = 15;
    private final int HOW_MANY_SUGGESTED_REPORTS_SECOND_LVL = HOW_MANY_REPORT_SNAPSHOT_TO_SHOW-HOW_MANY_SUGGESTED_REPORTS_FIRST_LVL;
    private final int NUM_REPORTS_THRESHOLD = 5;
    private int page; // number of page (at the beginning at 0), increase with nextButton and decrease with previousButton

    /**
     * Initialization functions
     */
    public void initialize ()
    {
        appSession = Session.getInstance();
        neo4jDriver = Neo4jDriver.getInstance();
        mongoDBDriver = MongoDBDriver.getInstance();
        homepageIcon.setOnMouseClicked(mouseEvent -> clickOnHomepageToChangePage(mouseEvent));
        profilePageIcon.setOnMouseClicked(mouseEvent -> clickOnProfImgToChangePage(mouseEvent));
        logoutPic.setOnMouseClicked(mouseEvent -> clickOnLogoutImg(mouseEvent));
        page = 0;
        
    	textYear.setVisible(false);
    	searchBarYear.setVisible(false);
       
        ObservableList<String> options =
                FXCollections.observableArrayList(
                		  "Search a Report by Player's fullname", //Fatto
                		  "Search a Player by his name", //Fatto
                		  "Search a Player by his role", //Fatto
                		  "Search a Player by his age", //Fatto
                		  "Search a Player by his preferred foot"
                );
   
       //Filter that only a Football Director User can do
       if (appSession.getLoggedUser().getRole()=='F') {
    	   options.add("Search an Observer by his username");
       	   options.add("Search an Observer by his last name");
       	   options.add("Reports of the following Observers");
       	   options.add("Search Reports based on Player's role");
       	   options.add("Search Reports with rate higher than");
       }
       
       //Filter that only an Administrator or Football Director User can do
       if (appSession.getLoggedUser().getRole()=='F' || appSession.getLoggedUser().getRole()=='A') {
    	   options.add("Most liked Players"); //In lavorazione
       	   options.add("Most followed users");
       	   options.add("Most active users");
       	   options.add("Most liked Reports");
       	   
       }
       //Filter that only an Administrator User can do 
       if (appSession.getLoggedUser().getRole()=='A') {
    	   
       	   options.add("Search a User by his username");
       	   options.add("Search a User by his lastName");
       	   options.add("Top 10 best players of a role");
       	   options.add("Top 10 best players of a role based on report");
       	   options.add("Top 11 football players of a team by year");
       	   
       }
       
        searchComboBox.setItems(options);
        searchComboBox.setValue("Search a Report by Player's fullname");
        
        // if some changes happens to the combobox
        searchComboBox.setOnAction(actionEvent -> comboAction((ActionEvent) actionEvent));
        searchButton.setOnAction(actionEvent -> search(actionEvent));

        nextButton.setOnMouseClicked(mouseEvent -> clickOnNext(mouseEvent));
        previousButton.setOnMouseClicked(mouseEvent -> clickOnPrevious(mouseEvent));
        previousButton.setVisible(false); //in the first page it is not visible
    }

    private void search(ActionEvent actionEvent) {

        Utils.removeAllFromPane(discoveryVBox);

        if (String.valueOf(searchComboBox.getValue()).equals("Search a Report by Player's fullname"))
        {
            List<Report> reports = mongoDBDriver.searchReportsFromPlayerFullName(searchBarTextField.getText(),
                    HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
            if(reports==null)
            		Utils.showErrorAlert("Name is wrong!");
            else if (reports.size()==0)
            {
                Text adv = new Text("No reports are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
            else 
            	Utils.addReportsSnap(discoveryVBox, reports);
            
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Search Reports based on Player's role"))
        { 
        	String role= new String();
        	if( searchBarTextField.getText().toUpperCase().equals("GK") || searchBarTextField.getText().toLowerCase().equals("goalkeeper") || searchBarTextField.getText().toLowerCase().equals("portiere"))
        		role= "gk";
        	else if( searchBarTextField.getText().toUpperCase().equals("CM") || searchBarTextField.getText().toLowerCase().equals("central midfielder") || searchBarTextField.getText().toLowerCase().equals("centrocampista centrale"))
                role= "cm";
        	else if( searchBarTextField.getText().toUpperCase().equals("LB") || searchBarTextField.getText().toLowerCase().equals("left back") || searchBarTextField.getText().toLowerCase().equals("terzino sinistro"))
                role= "lb";
        	else if( searchBarTextField.getText().toUpperCase().equals("ST") || searchBarTextField.getText().toLowerCase().equals("striker") || searchBarTextField.getText().toLowerCase().equals("punta"))
                role= "st";
        	else if( searchBarTextField.getText().toUpperCase().equals("LM") || searchBarTextField.getText().toLowerCase().equals("left midfielder") || searchBarTextField.getText().toLowerCase().equals("esterno sinistro"))
                role= "lm";
        	else if( searchBarTextField.getText().toUpperCase().equals("CB") || searchBarTextField.getText().toLowerCase().equals("center back") || searchBarTextField.getText().toLowerCase().equals("difensore centrale"))
                role= "cb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RB") || searchBarTextField.getText().toLowerCase().equals("right back") || searchBarTextField.getText().toLowerCase().equals("terzino destro"))
                role= "rb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RWB") || searchBarTextField.getText().toLowerCase().equals("right wing back") || searchBarTextField.getText().toLowerCase().equals("terzino fluidificante destro"))
                role= "rwb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RM") || searchBarTextField.getText().toLowerCase().equals("right midfielder") || searchBarTextField.getText().toLowerCase().equals("esterno destro"))
                role= "rm";
        	else if( searchBarTextField.getText().toUpperCase().equals("LWB") || searchBarTextField.getText().toLowerCase().equals("left wing back") || searchBarTextField.getText().toLowerCase().equals("terzino fluidificante sinistro"))
                role= "lwb";
        	else if( searchBarTextField.getText().toUpperCase().equals("LW") || searchBarTextField.getText().toLowerCase().equals("left winger") || searchBarTextField.getText().toLowerCase().equals("ala sinistra"))
                role= "lw";
        	else if( searchBarTextField.getText().toUpperCase().equals("RW") || searchBarTextField.getText().toLowerCase().equals("right winger") || searchBarTextField.getText().toLowerCase().equals("ala destra"))
                role= "rw";
        	else if( searchBarTextField.getText().toUpperCase().equals("CAM") || searchBarTextField.getText().toLowerCase().equals("central attacking Midfielder") || searchBarTextField.getText().toLowerCase().equals("trequartista centrale"))
                role= "cam";
        	else if( searchBarTextField.getText().toUpperCase().equals("CF") || searchBarTextField.getText().toLowerCase().equals("center forward") || searchBarTextField.getText().toLowerCase().equals("prima punta"))
                role= "cf";
        	if(role.equals(null))
        		Utils.showErrorAlert("Enter a valid Player position!");
        	else
        	{
	        	List<Report> reports = mongoDBDriver.searchReportsFromPlayerRole(role,
	                    HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
	            Utils.addReportsSnap(discoveryVBox, reports);
	            if(reports.size()==0)
	            {
	                Text adv = new Text("No reports are present with the requested parameters");
	                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
	                discoveryVBox.getChildren().add(adv);
	            }
        	}
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Search Reports with rate higher than"))
        {
            List<Report> reports = mongoDBDriver.getReportsFromRate(Integer.parseInt(searchBarTextField.getText()),
                    HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
            Utils.addReportsSnap(discoveryVBox, reports);
            if(reports.size()==0)
            {
                Text adv = new Text("No reports are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Search a Player by his name"))
        {
            List<Player> players = mongoDBDriver.searchPlayersFromName(searchBarTextField.getText(),
                    HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
            Utils.addPlayersSnap(discoveryVBox, players); 
            if(players.size()==0)
            {
                Text adv = new Text("No players are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Search a Player by his role"))
        {
         	String role= new String();
        	if( searchBarTextField.getText().toUpperCase().equals("GK") || searchBarTextField.getText().toLowerCase().equals("goalkeeper") || searchBarTextField.getText().toLowerCase().equals("portiere"))
        		role= "gk";
        	else if( searchBarTextField.getText().toUpperCase().equals("CM") || searchBarTextField.getText().toLowerCase().equals("central midfielder") || searchBarTextField.getText().toLowerCase().equals("centrocampista centrale"))
                role= "cm";
        	else if( searchBarTextField.getText().toUpperCase().equals("LB") || searchBarTextField.getText().toLowerCase().equals("left back") || searchBarTextField.getText().toLowerCase().equals("terzino sinistro"))
                role= "lb";
        	else if( searchBarTextField.getText().toUpperCase().equals("ST") || searchBarTextField.getText().toLowerCase().equals("striker") || searchBarTextField.getText().toLowerCase().equals("punta"))
                role= "st";
        	else if( searchBarTextField.getText().toUpperCase().equals("LM") || searchBarTextField.getText().toLowerCase().equals("left midfielder") || searchBarTextField.getText().toLowerCase().equals("esterno sinistro"))
                role= "lm";
        	else if( searchBarTextField.getText().toUpperCase().equals("CB") || searchBarTextField.getText().toLowerCase().equals("center back") || searchBarTextField.getText().toLowerCase().equals("difensore centrale"))
                role= "cb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RB") || searchBarTextField.getText().toLowerCase().equals("right back") || searchBarTextField.getText().toLowerCase().equals("terzino destro"))
                role= "rb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RWB") || searchBarTextField.getText().toLowerCase().equals("right wing back") || searchBarTextField.getText().toLowerCase().equals("terzino fluidificante destro"))
                role= "rwb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RM") || searchBarTextField.getText().toLowerCase().equals("right midfielder") || searchBarTextField.getText().toLowerCase().equals("esterno destro"))
                role= "rm";
        	else if( searchBarTextField.getText().toUpperCase().equals("LWB") || searchBarTextField.getText().toLowerCase().equals("left wing back") || searchBarTextField.getText().toLowerCase().equals("terzino fluidificante sinistro"))
                role= "lwb";
        	else if( searchBarTextField.getText().toUpperCase().equals("LW") || searchBarTextField.getText().toLowerCase().equals("left winger") || searchBarTextField.getText().toLowerCase().equals("ala sinistra"))
                role= "lw";
        	else if( searchBarTextField.getText().toUpperCase().equals("RW") || searchBarTextField.getText().toLowerCase().equals("right winger") || searchBarTextField.getText().toLowerCase().equals("ala destra"))
                role= "rw";
        	else if( searchBarTextField.getText().toUpperCase().equals("CAM") || searchBarTextField.getText().toLowerCase().equals("central attacking Midfielder") || searchBarTextField.getText().toLowerCase().equals("trequartista centrale"))
                role= "cam";
        	else if( searchBarTextField.getText().toUpperCase().equals("CF") || searchBarTextField.getText().toLowerCase().equals("center forward") || searchBarTextField.getText().toLowerCase().equals("prima punta"))
                role= "cf"; 
        	if(role.equals(null))
        		Utils.showErrorAlert("Enter a valid Player position!");
        	else
        	{
	        	List<Player> players = mongoDBDriver.searchPlayersFromRole(role,
	                    HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
	            Utils.addPlayersSnap(discoveryVBox, players);
	            if(players.size()==0)
	            {
	                Text adv = new Text("No players are present with the requested parameters");
	                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
	                discoveryVBox.getChildren().add(adv);
	            }
        	}
        }

        if (String.valueOf(searchComboBox.getValue()).equals("Search a Player by his Football Team"))
        {
        	List<Player> players = mongoDBDriver.searchPlayersFromFootballTeam(searchBarTextField.getText(),
                    HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
            Utils.addPlayersSnap(discoveryVBox, players);
            if(players.size()==0)
            {
                Text adv = new Text("No players are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Search a Player by his preferred foot"))
        {
        	List<Player> players = mongoDBDriver.searchPlayersFromFoot(searchBarTextField.getText(),
                    HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
            Utils.addPlayersSnap(discoveryVBox, players);
            if(players.size()==0)
            {
                Text adv = new Text("No players are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Search a Player by his age"))
        {
        	List<Player> players = mongoDBDriver.searchPlayersFromAge(searchBarTextField.getText(),
                    HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
            Utils.addPlayersSnap(discoveryVBox, players);
            if(players.size()==0)
            {
                Text adv = new Text("No players are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Search an Observer by his username"))
        {
        	List<User> user = neo4jDriver.searchObserverByUsername(HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW, searchBarTextField.getText());
            Utils.addUsersSnap(discoveryVBox, user);
            if(user.size()==0)
            {
                Text adv = new Text("No observers are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Search an Observer by his last name"))
        {
        	List<User> user = neo4jDriver.searchObserverByLastName(HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW, searchBarTextField.getText());
            Utils.addUsersSnap(discoveryVBox, user);
            if(user.size()==0)
            {
                Text adv = new Text("No observers are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Reports of the following Observers"))
        { 
        	List<Report> report = neo4jDriver.searchReportFollowedObserver(HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW, appSession.getLoggedUser().getUsername());
        	Utils.addReportsSnap(discoveryVBox, report);
            if(report.size()==0)
            {
                Text adv = new Text("No reports are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Most liked Reports"))
        { 
        	List<Report> report = neo4jDriver.searchMostLikedReports(HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
        	Utils.addReportsSnap(discoveryVBox, report);
            if(report.size()==0)
            {
                Text adv = new Text("No reports are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Most liked Players"))
        {
        	List<Player> players = neo4jDriver.searchMostLikedPlayers(HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
            Utils.addPlayersSnap(discoveryVBox, players);
            if(players.size()==0)
            {
                Text adv = new Text("No players are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        
        if (String.valueOf(searchComboBox.getValue()).equals("Search a User by his username"))
        {
            List<User> users = neo4jDriver.searchUserByUsername(HOW_MANY_USER_SNAPSHOT_TO_SHOW*page,
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW, searchBarTextField.getText());
            Utils.addUsersSnap(discoveryVBox, users);
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Search a User by his lastName"))
        {
            List<User> users = neo4jDriver.searchUserByFullName(HOW_MANY_USER_SNAPSHOT_TO_SHOW*page,
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW, searchBarTextField.getText());
            Utils.addUsersSnap(discoveryVBox, users);
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Most followed users"))
        {
            List<User> users = neo4jDriver.searchMostFollowedObservers(
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW*page,
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW);
            Utils.addUsersSnap(discoveryVBox, users);
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Most active users"))
        {
            List<User> users = neo4jDriver.searchMostActiveObservers(
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW*page,
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW);
            Utils.addUsersSnap(discoveryVBox, users);
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Most liked users"))
        {
            List<User> users = neo4jDriver.searchMostLikedUsers(
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW*page,
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW);
            Utils.addUsersSnap(discoveryVBox, users);
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Top 11 football players of a team by year"))
        {
        	int year = 0;
        	try {
        		year = Integer.parseInt(searchBarYear.getText());
        	}
        	
        	catch(Exception e) {
        		Utils.showErrorAlert("Enter a valid Year");
        	}
            
        	String team = searchBarTextField.getText();
        	if(team.isEmpty()) {
        		Utils.showErrorAlert("Enter a valid Football Team!");
        	}
        		
        	List<Player> players = neo4jDriver.searchTopPlayersOfATeamByYear(team, year, 0, 11);
            Utils.addPlayersSnap(discoveryVBox, players);
            if(players.size()==0)
            {
                Text adv = new Text("No players are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            }
        }
        
        if (String.valueOf(searchComboBox.getValue()).equals("Top 10 best players of a role"))
        {
        	String role= new String();
        	if( searchBarTextField.getText().toUpperCase().equals("GK") || searchBarTextField.getText().toLowerCase().equals("goalkeeper") || searchBarTextField.getText().toLowerCase().equals("portiere"))
        		role= "gk";
        	else if( searchBarTextField.getText().toUpperCase().equals("CM") || searchBarTextField.getText().toLowerCase().equals("central midfielder") || searchBarTextField.getText().toLowerCase().equals("centrocampista centrale"))
                role= "cm";
        	else if( searchBarTextField.getText().toUpperCase().equals("LB") || searchBarTextField.getText().toLowerCase().equals("left back") || searchBarTextField.getText().toLowerCase().equals("terzino sinistro"))
                role= "lb";
        	else if( searchBarTextField.getText().toUpperCase().equals("ST") || searchBarTextField.getText().toLowerCase().equals("striker") || searchBarTextField.getText().toLowerCase().equals("punta"))
                role= "st";
        	else if( searchBarTextField.getText().toUpperCase().equals("LM") || searchBarTextField.getText().toLowerCase().equals("left midfielder") || searchBarTextField.getText().toLowerCase().equals("esterno sinistro"))
                role= "lm";
        	else if( searchBarTextField.getText().toUpperCase().equals("CB") || searchBarTextField.getText().toLowerCase().equals("center back") || searchBarTextField.getText().toLowerCase().equals("difensore centrale"))
                role= "cb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RB") || searchBarTextField.getText().toLowerCase().equals("right back") || searchBarTextField.getText().toLowerCase().equals("terzino destro"))
                role= "rb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RWB") || searchBarTextField.getText().toLowerCase().equals("right wing back") || searchBarTextField.getText().toLowerCase().equals("terzino fluidificante destro"))
                role= "rwb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RM") || searchBarTextField.getText().toLowerCase().equals("right midfielder") || searchBarTextField.getText().toLowerCase().equals("esterno destro"))
                role= "rm";
        	else if( searchBarTextField.getText().toUpperCase().equals("LWB") || searchBarTextField.getText().toLowerCase().equals("left wing back") || searchBarTextField.getText().toLowerCase().equals("terzino fluidificante sinistro"))
                role= "lwb";
        	else if( searchBarTextField.getText().toUpperCase().equals("LW") || searchBarTextField.getText().toLowerCase().equals("left winger") || searchBarTextField.getText().toLowerCase().equals("ala sinistra"))
                role= "lw";
        	else if( searchBarTextField.getText().toUpperCase().equals("RW") || searchBarTextField.getText().toLowerCase().equals("right winger") || searchBarTextField.getText().toLowerCase().equals("ala destra"))
                role= "rw";
        	else if( searchBarTextField.getText().toUpperCase().equals("CAM") || searchBarTextField.getText().toLowerCase().equals("central attacking Midfielder") || searchBarTextField.getText().toLowerCase().equals("trequartista centrale"))
                role= "cam";
        	else if( searchBarTextField.getText().toUpperCase().equals("CF") || searchBarTextField.getText().toLowerCase().equals("center forward") || searchBarTextField.getText().toLowerCase().equals("prima punta"))
                role= "cf";
        	if(role.equals(null) || role.isEmpty())
        		Utils.showErrorAlert("Enter a valid Player position!");
        	else
        	{
        	List<Player> players = mongoDBDriver.getTopCodPlayerFromRole(role,
                    0, 10);
            Utils.addPlayersSnap(discoveryVBox, players);
            if(players.size()==0)
            	{
                Text adv = new Text("No players are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                discoveryVBox.getChildren().add(adv);
            	}
        	}
        }
        if (String.valueOf(searchComboBox.getValue()).equals("Top 10 best players of a role based on report"))
        {
        	String role= new String();
        	if( searchBarTextField.getText().toUpperCase().equals("GK") || searchBarTextField.getText().toLowerCase().equals("goalkeeper") || searchBarTextField.getText().toLowerCase().equals("portiere"))
        		role= "gk";
        	else if( searchBarTextField.getText().toUpperCase().equals("CM") || searchBarTextField.getText().toLowerCase().equals("central midfielder") || searchBarTextField.getText().toLowerCase().equals("centrocampista centrale"))
                role= "cm";
        	else if( searchBarTextField.getText().toUpperCase().equals("LB") || searchBarTextField.getText().toLowerCase().equals("left back") || searchBarTextField.getText().toLowerCase().equals("terzino sinistro"))
                role= "lb";
        	else if( searchBarTextField.getText().toUpperCase().equals("ST") || searchBarTextField.getText().toLowerCase().equals("striker") || searchBarTextField.getText().toLowerCase().equals("punta"))
                role= "st";
        	else if( searchBarTextField.getText().toUpperCase().equals("LM") || searchBarTextField.getText().toLowerCase().equals("left midfielder") || searchBarTextField.getText().toLowerCase().equals("esterno sinistro"))
                role= "lm";
        	else if( searchBarTextField.getText().toUpperCase().equals("CB") || searchBarTextField.getText().toLowerCase().equals("center back") || searchBarTextField.getText().toLowerCase().equals("difensore centrale"))
                role= "cb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RB") || searchBarTextField.getText().toLowerCase().equals("right back") || searchBarTextField.getText().toLowerCase().equals("terzino destro"))
                role= "rb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RWB") || searchBarTextField.getText().toLowerCase().equals("right wing back") || searchBarTextField.getText().toLowerCase().equals("terzino fluidificante destro"))
                role= "rwb";
        	else if( searchBarTextField.getText().toUpperCase().equals("RM") || searchBarTextField.getText().toLowerCase().equals("right midfielder") || searchBarTextField.getText().toLowerCase().equals("esterno destro"))
                role= "rm";
        	else if( searchBarTextField.getText().toUpperCase().equals("LWB") || searchBarTextField.getText().toLowerCase().equals("left wing back") || searchBarTextField.getText().toLowerCase().equals("terzino fluidificante sinistro"))
                role= "lwb";
        	else if( searchBarTextField.getText().toUpperCase().equals("LW") || searchBarTextField.getText().toLowerCase().equals("left winger") || searchBarTextField.getText().toLowerCase().equals("ala sinistra"))
                role= "lw";
        	else if( searchBarTextField.getText().toUpperCase().equals("RW") || searchBarTextField.getText().toLowerCase().equals("right winger") || searchBarTextField.getText().toLowerCase().equals("ala destra"))
                role= "rw";
        	else if( searchBarTextField.getText().toUpperCase().equals("CAM") || searchBarTextField.getText().toLowerCase().equals("central attacking Midfielder") || searchBarTextField.getText().toLowerCase().equals("trequartista centrale"))
                role= "cam";
        	else if( searchBarTextField.getText().toUpperCase().equals("CF") || searchBarTextField.getText().toLowerCase().equals("center forward") || searchBarTextField.getText().toLowerCase().equals("prima punta"))
                role= "cf";
        	if(role.equals(null) || role.isEmpty())
        		Utils.showErrorAlert("Enter a valid Player position!");
        	else
        	{
	        	List<Player> players = neo4jDriver.getTopCodPlayerFromRoleReport(role.toUpperCase(),
	                    0, 10);
	            Utils.addPlayersSnap(discoveryVBox, players);
	            if(players.size()==0)
	            	{
	                Text adv = new Text("No players are present with the requested parameters");
	                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
	                discoveryVBox.getChildren().add(adv);
	            	}
        	}
        }
        

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
     * Function that let the navigation into the ui ---> profilePage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnProfImgToChangePage(MouseEvent mouseEvent){
        ProfilePageController profilePageController = (ProfilePageController)
                Utils.changeScene("/profilePage.fxml", mouseEvent);
        profilePageController.setProfile(Session.getInstance().getLoggedUser());
    }

    /**
     * Function that handle the changes to the searchComboBox
     * @param event
     */
    private void comboAction(ActionEvent event) {
        page = 0;
        if (String.valueOf(searchComboBox.getValue()).equals("Reports of the following Observers") || (searchComboBox.getValue()).equals("Most liked Players"))
        	{
        	searchBarTextField.setDisable(true);
        	}
        else 
        	{
        	searchBarTextField.setDisable(false);
        	searchBarTextField.setEditable(true);
        	}
        	Utils.removeAllFromPane(discoveryVBox);
        	
        if(String.valueOf(searchComboBox.getValue()).equals("Top 11 football players of a team by year")) {
        	textYear.setVisible(true);
        	searchBarYear.setVisible(true);
        	searchBarTextField.setPromptText("Insert the Football Team here (case sensitive)");
        }
        else {
        	textYear.setVisible(false);
        	searchBarYear.setVisible(false);
        	searchBarTextField.setText("");
        }
        
        
        if(String.valueOf(searchComboBox.getValue()).equals("Top 11 football players of a team by year") ||
        	String.valueOf(searchComboBox.getValue()).equals("Top 10 best players of a role") ||
        	String.valueOf(searchComboBox.getValue()).equals("Top 10 best players of a role based on report")) {	
	    		nextButton.setVisible(false);
	    		previousButton.setVisible(false);
        }
        else {
    		nextButton.setVisible(true);
    		previousButton.setVisible(true);
        }
    }

    /**
     * Handler for the next button
     * @param mouseEvent    Events that leads to this function
     */
    private void clickOnNext(MouseEvent mouseEvent) {
        page++;
        if (page > 0)
            previousButton.setVisible(true);
        searchButton.fire(); // simulate the click of the button
    }

    /**
     * Handler for the previous button
     * @param mouseEvent    Events that leads to this function
     */
    private void clickOnPrevious(MouseEvent mouseEvent) {
        page--;
        if (page < 1)
            previousButton.setVisible(false);
        searchButton.fire(); // simulate the click of the button
    }
}
