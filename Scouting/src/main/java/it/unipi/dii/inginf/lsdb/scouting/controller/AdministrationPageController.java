package it.unipi.dii.inginf.lsdb.scouting.controller;

import it.unipi.dii.inginf.lsdb.scouting.model.Player;
import it.unipi.dii.inginf.lsdb.scouting.model.Report;
import it.unipi.dii.inginf.lsdb.scouting.model.Session;
import it.unipi.dii.inginf.lsdb.scouting.model.User;
import it.unipi.dii.inginf.lsdb.scouting.persistence.MongoDBDriver;
import it.unipi.dii.inginf.lsdb.scouting.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;

public class AdministrationPageController {
    private Neo4jDriver neo4jDriver;
    private MongoDBDriver mongoDBDriver;
    private Session appSession;
    @FXML private ImageView homepageIcon;
    @FXML private ImageView profilePageIcon;
    @FXML private ImageView logoutPic;
    @FXML private ImageView discoveryImg;
    @FXML private Button allComments;
    @FXML private Button allReports;
    @FXML private Button allUsers;
    @FXML private Button searchButton;
    @FXML private ComboBox chooseQuery;
    @FXML private TextField searchBar;
    @FXML private TextField searchBarYear;
    @FXML private Text textYear;
    @FXML private Button nextButton;
    @FXML private Button previousButton;
    @FXML private VBox adminPageBox;

    private int page; // number of page (at the beginning at 0), increase with nextButton and decrease with previousButton
    private final int HOW_MANY_REPORT_SNAPSHOT_TO_SHOW = 20;
    private final int HOW_MANY_USER_SNAPSHOT_TO_SHOW = 20;
    private final int HOW_MANY_COMMENTS_TO_SHOW = 20;

    /**
     * Initialization functions
     */
    public void initialize()
    {
        neo4jDriver = Neo4jDriver.getInstance();
        mongoDBDriver = MongoDBDriver.getInstance();
        appSession = Session.getInstance();

        // Setting the menu'
        homepageIcon.setOnMouseClicked(mouseEvent -> clickOnHomepageToChangePage(mouseEvent));
        profilePageIcon.setOnMouseClicked(mouseEvent -> clickOnProfImgToChangePage(mouseEvent));
        logoutPic.setOnMouseClicked(mouseEvent -> clickOnLogoutImg(mouseEvent));
        discoveryImg.setOnMouseClicked(mouseEvent -> clickOnDiscImgtoChangePage(mouseEvent));

        // Setting the ComboBox
        ObservableList<String> entries =
                FXCollections.observableArrayList(
                        "User username",
                        "User fullname",
                        "Report by Player fullname",
                    	"Best Football Players divided by role (GK,RWB, CF, RW, LW, RM, LB, CDM, CAM, RB, LM, CB, CM ST)", //facciamo un if nella funzione che passa la stringa?
                    	"Top 11 football players of a team by year",
                 	    "Top Commentators",
                 	    "Most active Users"
                );
        chooseQuery.setItems(entries);
        chooseQuery.setPromptText("Click to choose");
     //   chooseQuery.setOnAction(event -> clickOnComboBox());
        chooseQuery.setOnAction(actionEvent -> comboAction((ActionEvent) actionEvent));


        // Set the queries that can be called from buttons
        allComments.setOnMouseClicked(mouseEvent -> clickOnAllComments());
        allReports.setOnMouseClicked(mouseEvent -> clickOnAllReports());
        allUsers.setOnMouseClicked(mouseEvent -> clickOnAllUsers());

        // Search button
        searchButton.setOnMouseClicked(mouseEvent -> clickOnSearchButton());

        // Previous and next button behaviour
        page = 0;
        nextButton.setOnMouseClicked(mouseEvent -> clickOnNext(mouseEvent));
        previousButton.setOnMouseClicked(mouseEvent -> clickOnPrevious(mouseEvent));
        previousButton.setVisible(false); //in the first page it is not visible
       	textYear.setVisible(false);
    	searchBarYear.setVisible(false);
    }

    /**
     * Handle the click on the search button
     */
    private void clickOnSearchButton()
    {
        Utils.removeAllFromPane(adminPageBox);
        if (String.valueOf(chooseQuery.getValue()).equals("Report by Player fullname"))
        {
            List<Report> reports = mongoDBDriver.searchReportsFromPlayerFullName(searchBar.getText(),
                    HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
            Utils.addReportsSnap(adminPageBox, reports);
            if(reports.size()==0)
        	{
            Text adv = new Text("No reports are present with the requested parameters");
            adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
            adminPageBox.getChildren().add(adv);
        	}
        }
        else if (String.valueOf(chooseQuery.getValue()).equals("User username"))
        {
            List<User> users = neo4jDriver.searchUserByUsername(HOW_MANY_USER_SNAPSHOT_TO_SHOW*page,
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW, searchBar.getText());
            Utils.addUsersSnap(adminPageBox, users);
            if(users.size()==0)
        	{
            Text adv = new Text("No users are present with the requested parameters");
            adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
            adminPageBox.getChildren().add(adv);
        	}
        }
        else if (String.valueOf(chooseQuery.getValue()).equals("User fullname"))
        {
            List<User> users = neo4jDriver.searchUserByFullName(HOW_MANY_USER_SNAPSHOT_TO_SHOW*page,
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW, searchBar.getText());
            Utils.addUsersSnap(adminPageBox, users);
            if(users.size()==0)
        	{
            Text adv = new Text("No users are present with the requested parameters");
            adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
            adminPageBox.getChildren().add(adv);
        	}
        }
        else if (String.valueOf(chooseQuery.getValue()).equals("Best Football Players divided by role (GK,RWB, CF, RW, LW, RM, LB, CDM, CAM, RB, LM, CB, CM ST)"))
        {
        	String role= new String();
    	if( searchBar.getText().toUpperCase().equals("GK") || searchBar.getText().toLowerCase().equals("goalkeeper") || searchBar.getText().toLowerCase().equals("portiere"))
    		role= "gk";
    	else if( searchBar.getText().toUpperCase().equals("CM") || searchBar.getText().toLowerCase().equals("central midfielder") || searchBar.getText().toLowerCase().equals("centrocampista centrale"))
            role= "cm";
    	else if( searchBar.getText().toUpperCase().equals("LB") || searchBar.getText().toLowerCase().equals("left back") || searchBar.getText().toLowerCase().equals("terzino sinistro"))
            role= "lb";
    	else if( searchBar.getText().toUpperCase().equals("ST") || searchBar.getText().toLowerCase().equals("striker") || searchBar.getText().toLowerCase().equals("punta"))
            role= "st";
    	else if( searchBar.getText().toUpperCase().equals("LM") || searchBar.getText().toLowerCase().equals("left midfielder") || searchBar.getText().toLowerCase().equals("esterno sinistro"))
            role= "lm";
    	else if( searchBar.getText().toUpperCase().equals("CB") || searchBar.getText().toLowerCase().equals("center back") || searchBar.getText().toLowerCase().equals("difensore centrale"))
            role= "cb";
    	else if( searchBar.getText().toUpperCase().equals("RB") || searchBar.getText().toLowerCase().equals("right back") || searchBar.getText().toLowerCase().equals("terzino destro"))
            role= "rb";
    	else if( searchBar.getText().toUpperCase().equals("RWB") || searchBar.getText().toLowerCase().equals("right wing back") || searchBar.getText().toLowerCase().equals("terzino fluidificante destro"))
            role= "rwb";
    	else if( searchBar.getText().toUpperCase().equals("RM") || searchBar.getText().toLowerCase().equals("right midfielder") || searchBar.getText().toLowerCase().equals("esterno destro"))
            role= "rm";
    	else if( searchBar.getText().toUpperCase().equals("LWB") || searchBar.getText().toLowerCase().equals("left wing back") || searchBar.getText().toLowerCase().equals("terzino fluidificante sinistro"))
            role= "lwb";
    	else if( searchBar.getText().toUpperCase().equals("LW") || searchBar.getText().toLowerCase().equals("left winger") || searchBar.getText().toLowerCase().equals("ala sinistra"))
            role= "lw";
    	else if( searchBar.getText().toUpperCase().equals("RW") || searchBar.getText().toLowerCase().equals("right winger") || searchBar.getText().toLowerCase().equals("ala destra"))
            role= "rw";
    	else if( searchBar.getText().toUpperCase().equals("CAM") || searchBar.getText().toLowerCase().equals("central attacking Midfielder") || searchBar.getText().toLowerCase().equals("trequartista centrale"))
            role= "cam";
    	else if( searchBar.getText().toUpperCase().equals("CF") || searchBar.getText().toLowerCase().equals("center forward") || searchBar.getText().toLowerCase().equals("prima punta"))
            role= "cf";
    	if(role.equals(null) || role.isEmpty())
    		Utils.showErrorAlert("Enter a valid Player position!");
    	else
    	{
    	List<Player> players = mongoDBDriver.getTopCodPlayerFromRole(role,
                0, 10);
        Utils.addPlayersSnap(adminPageBox, players);
        if(players.size()==0)
        	{
            Text adv = new Text("No players are present with the requested parameters");
            adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
            adminPageBox.getChildren().add(adv);
        	}
    	}
    }
        
        else if (String.valueOf(chooseQuery.getValue()).equals("Top Commentators"))
        {
            List<User> users = mongoDBDriver.topCommentators(HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
            Utils.addUsersSnap(adminPageBox, users);
            if(users.size()==0)
        	{
            Text adv = new Text("No users are present with the requested parameters");
            adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
            adminPageBox.getChildren().add(adv);
        	}
        }
        
        else if (String.valueOf(chooseQuery.getValue()).equals("Most active Users"))
        {
            List<User> users = neo4jDriver.searchMostActiveObservers(
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW*page,
                    HOW_MANY_USER_SNAPSHOT_TO_SHOW);
            Utils.addUsersSnap(adminPageBox, users);
            if(users.size()==0)
        	{
            Text adv = new Text("No users are present with the requested parameters");
            adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
            adminPageBox.getChildren().add(adv);
        	}
        }
        else if (String.valueOf(chooseQuery.getValue()).equals("Top 11 football players of a team by year"))
        {
        	int year = 0;
        	try {
        		year = Integer.parseInt(searchBarYear.getText());
        	}
        	
        	catch(Exception e) {
        		Utils.showErrorAlert("Enter a valid Year");
        	}
            
        	String team = searchBar.getText();
        	if(team.isEmpty()) {
        		Utils.showErrorAlert("Enter a valid Football Team!");
        	}
        		
        	List<Player> players = neo4jDriver.searchTopPlayersOfATeamByYear(team, year, 0, 11);
            Utils.addPlayersSnap(adminPageBox, players);
            if(players.size()==0)
            {
                Text adv = new Text("No players are present with the requested parameters");
                adv.setStyle("-fx-padding: 5px; -fx-font-size: 2em");
                adminPageBox.getChildren().add(adv);
            }
        }
    }

    /**
     * Handle the click of an option of the ComboBox
     */
    private void clickOnComboBox()
    {
        page = 0;
        Utils.removeAllFromPane(adminPageBox);
    }
    
    private void comboAction(ActionEvent event) {
        page = 0;
        if (String.valueOf(chooseQuery.getValue()).equals("Top Commentators") || (chooseQuery.getValue()).equals("Most active Users"))
    	{
    	searchBar.setDisable(true);
    	}
    else 
    	{
    	searchBar.setDisable(false);
    	searchBar.setEditable(true);
    	}
    	Utils.removeAllFromPane(adminPageBox);
        if(String.valueOf(chooseQuery.getValue()).equals("Top 11 football players of a team by year")) {
        	textYear.setVisible(true);
        	searchBarYear.setVisible(true);
        	searchBar.setPromptText("Insert the Football Team here (case sensitive)");
        }
        else {
        	textYear.setVisible(false);
        	searchBarYear.setVisible(false);
        	searchBar.setText("");
        }
        
        
        if(String.valueOf(chooseQuery.getValue()).equals("Top 11 football players of a team by year")) {	
	    		nextButton.setVisible(false);
	    		previousButton.setVisible(false);
        }
        else {
    		nextButton.setVisible(true);
    		previousButton.setVisible(true);
        }
    }


    /**
     * Show all the users of the db
     */
    private void clickOnAllUsers()
    {
        Utils.removeAllFromPane(adminPageBox);
        List<User> allUsers = neo4jDriver.searchAllUsers(HOW_MANY_USER_SNAPSHOT_TO_SHOW*page, HOW_MANY_USER_SNAPSHOT_TO_SHOW);
        Utils.addUsersSnap(adminPageBox, allUsers);
    }

    /**
     * Show all the reports of the db
     */
    private void clickOnAllReports()
    {
        Utils.removeAllFromPane(adminPageBox);
        List<Report> allReports = mongoDBDriver.searchAllReports(HOW_MANY_REPORT_SNAPSHOT_TO_SHOW*page, HOW_MANY_REPORT_SNAPSHOT_TO_SHOW);
        Utils.addReportsSnap(adminPageBox, allReports);
    }

    /**
     * Show the comments sorted by creation time
     */
    private void clickOnAllComments()
    {
        Utils.removeAllFromPane(adminPageBox);
        List<List<Object>> objects = mongoDBDriver.searchAllComments(
                HOW_MANY_COMMENTS_TO_SHOW*page, HOW_MANY_COMMENTS_TO_SHOW);
        Utils.showAllComments(adminPageBox, objects);
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
     * Function that let the navigation into the ui ---> discoveryPage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnDiscImgtoChangePage(MouseEvent mouseEvent){
        Utils.changeScene("/discoveryPage.fxml", mouseEvent);
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
