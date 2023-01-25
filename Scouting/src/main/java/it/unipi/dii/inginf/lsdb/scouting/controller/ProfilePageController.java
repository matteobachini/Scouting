package it.unipi.dii.inginf.lsdb.scouting.controller;
import it.unipi.dii.inginf.lsdb.scouting.model.Player;
import it.unipi.dii.inginf.lsdb.scouting.model.Report;
import it.unipi.dii.inginf.lsdb.scouting.model.Session;
import it.unipi.dii.inginf.lsdb.scouting.model.User;
import it.unipi.dii.inginf.lsdb.scouting.persistence.MongoDBDriver;
import it.unipi.dii.inginf.lsdb.scouting.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProfilePageController {
    private Neo4jDriver neo4jDriver;
    private MongoDBDriver mongoDBDriver;
    private Session appSession;
    private final int HOW_MANY_SNAPSHOT_TO_SHOW = 10;
    private int page; // number of page (at the beginning at 0), increase with nextButton and decrease with previousButton
    private User user;
    @FXML private ImageView homepageIcon;
    @FXML private ImageView discoveryImg;
    @FXML private ImageView logoutPic;
    @FXML private ImageView addReportOrMyProfileImg;
    @FXML private ImageView profileDeleteUser;
    @FXML private ImageView profileEditUser;
    @FXML private ImageView profileImg;
    @FXML private ImageView profileGoAdminPage;
    @FXML private VBox reportVbox;
    @FXML private Text userName;
    @FXML private Text followerNumber;
    @FXML private Text followingNumber;
    @FXML private Label follower;
    @FXML private Label following;
    @FXML private Label reports;
    @FXML private Text reportsNumber;
    @FXML private Button nextButton;
    @FXML private Button previousButton;
    @FXML private ImageView addFollow;
    @FXML private LineChart<String, Number> lineChart;

    /**
     * Initialization functions
     */
    public void initialize ()
    {
        appSession = Session.getInstance();
        neo4jDriver = Neo4jDriver.getInstance();
        mongoDBDriver = MongoDBDriver.getInstance();
        homepageIcon.setOnMouseClicked(mouseEvent -> clickOnHomepageToChangePage(mouseEvent));
        discoveryImg.setOnMouseClicked(mouseEvent -> clickOnDiscImgtoChangePage(mouseEvent));
        logoutPic.setOnMouseClicked(mouseEvent -> clickOnLogoutImg(mouseEvent));
        profileEditUser.setOnMouseClicked(mouseEvent ->clickOnEditProfile(mouseEvent,this.user));
        nextButton.setOnMouseClicked(mouseEvent -> clickOnNext(mouseEvent));
        previousButton.setOnMouseClicked(mouseEvent -> clickOnPrevious(mouseEvent));
        profileGoAdminPage.setOnMouseClicked(mouseEvent -> clickOnAdminPage(mouseEvent));

    }

    /**
     * Set the profile page for the user U
     * @param user  User who owns the profile page
     */
    public void setProfile(User user)
    {
        this.user = user;

        userName.setText(user.getUsername());
        if(user.getUsername().equals(appSession.getLoggedUser().getUsername()))
        {
            // Update Forced
            user = neo4jDriver.getUserByUsername(appSession.getLoggedUser().getUsername());
            appSession.updateLoggedUserInfo(user);
        }
        final User u = user;
        /*
        if(user.getFollower() == 0 || user.getFollowing() == 0 || user.getNumReports() == 0) {
        	User u2 = neo4jDriver.getUserSocialInformations(u.getUsername());
        	u.setFollower(u2.getFollower());
        	u.setFollowing(u2.getFollowing());
        	u.setNumReports(u2.getNumReports());
        }
        */
        String type= "" + user.getRole();
        if(type.equals("O"))
        	profileImg.setImage(new Image("/img/obsIcon4.png"));
        else if(type.equals("F"))
        	profileImg.setImage(new Image("/img/fdIcon4.png"));
        else if(type.equals("A"))
        	profileImg.setImage(new Image("/img/admIcon4.png"));
        else 
        	profileImg.setImage(new Image("/img/genericUser.png"));

        if(user.getRole()=='F')
        {	follower.setVisible(false);
	        followerNumber.setVisible(false);
	        reports.setVisible(false);
	        reportsNumber.setVisible(false);
        	List<Player> players = neo4jDriver.getWishedSnaps(0, 100, appSession.getLoggedUser().getUsername());
        	Utils.addPlayersSnap(reportVbox, players);
        }
        else
        {
	        followerNumber.setText(String.valueOf(u.getFollower()));
	        reportsNumber.setText(String.valueOf(u.getNumReports()));
        }
       if(user.getRole()=='O') {
    	   followingNumber.setVisible(false);
       	   following.setVisible(false);
       }
       else
    	   followingNumber.setText(String.valueOf(u.getFollowing()));
       if(userName.getText().equals(appSession.getLoggedUser().getUsername()) || appSession.getLoggedUser().getRole()=='A')
            addFollow.setVisible(false);
       else
       {
            if(neo4jDriver.isUserOneFollowedByUserTwo(userName.getText(),appSession.getLoggedUser().getUsername()))
                addFollow.setImage(new Image("img/alreadyFollowed_profile.png"));

            addFollow.setOnMouseClicked(mouseEvent -> clickOnFollow());
        }

        //The admin page must be showed only in the admin profile page
        if(appSession.getLoggedUser().getRole()!='A')
            profileGoAdminPage.setVisible(false);

        page = 0;
        previousButton.setVisible(false); //in the first page it is not visible

        if(appSession.getLoggedUser().getRole()!='A' && !appSession.getLoggedUser().getUsername().equals(u.getUsername()))
            profileDeleteUser.setVisible(false);
        else
            profileDeleteUser.setOnMouseClicked(mouseEvent -> handleDeleteUserEvent(mouseEvent));

        if(!appSession.getLoggedUser().getUsername().equals(u.getUsername()) && appSession.getLoggedUser().getRole()!='A')
            profileEditUser.setVisible(false);
        profileEditUser.setOnMouseClicked(mouseEvent -> clickOnEditProfile(mouseEvent,u));

        if(appSession.getLoggedUser().getUsername().equals(u.getUsername()) && appSession.getLoggedUser().getRole() == 'O')
            addReportOrMyProfileImg.setOnMouseClicked(mouseEvent -> clickOnAddReportImg(mouseEvent));
        else
        {
            addReportOrMyProfileImg.setImage(new Image("img/user.png"));
            addReportOrMyProfileImg.setOnMouseClicked(mouseEvent -> clickOnMyProfile(mouseEvent));
        }
    }

    /**
     * Function used to delete the user
     * @param mouseEvent
     */
    private void handleDeleteUserEvent(MouseEvent mouseEvent) {
        // Delete all his/her report
        boolean restore = false;
        int howManyReports = neo4jDriver.howManyReportsAdded(user.getUserId());

        List<Report> reportsOfOldUser = new ArrayList<>();
        if(howManyReports!=0)
            reportsOfOldUser = neo4jDriver.getReportSnaps(0,howManyReports,user.getUsername());
        if(howManyReports == 0)
        {
        	// Delete the user from DB
        	if(!neo4jDriver.deleteUser(user.getUsername()))
        		Utils.showErrorAlert("Error in deleting the user in neo4j");
        	else {
        		if(!mongoDBDriver.deleteUser(user.getUsername())) {
        			neo4jDriver.addUser(user.getFirstName(), user.getLastName(), user.getUsername(), user.getRole());
        			Utils.showErrorAlert("Error in deleting the user in mongoDB");
        		}
        		else
        		{
        			Utils.showInfoAlert("User correctly deleted");
        		}
        	}
        }
        if(neo4jDriver.deleteAllReportsOfUser(user.getUsername()))
        {
            // if neo is ok then perform mongo
            if(!mongoDBDriver.deleteAllReportsOfUser(user.getUserId()))
            {
                // if mongo is not ok then restore the previous state
                for(Report r:reportsOfOldUser) {
                    neo4jDriver.newReport(r);
                }
                Utils.showErrorAlert("Error in deleting reports in neo4j");
            }
            else
            {
                // if the user's reports are deleted then
                // Delete the user from DBs
                if(!neo4jDriver.deleteUser(user.getUsername()))
                {
                	for(Report r:reportsOfOldUser) {
                        neo4jDriver.newReport(r);
                        mongoDBDriver.addReport(r);
                    }
                    Utils.showErrorAlert("Error in deleting the user in neo4j");
                }
                else
                {
                	if(!mongoDBDriver.deleteUser(user.getUsername())) {
                		neo4jDriver.addUser(user.getFirstName(), user.getLastName(), user.getUsername(), user.getRole());
                		User u = neo4jDriver.getUserByUsername(user.getUsername());
                		for(Report r:reportsOfOldUser) {
                			r.setUserID(u.getUserId());
                            neo4jDriver.newReport(r);
                            mongoDBDriver.addReport(r);
                        }
                		Utils.showErrorAlert("Error in deleting the user in MongoDB");
                	}
                	else
                		Utils.showInfoAlert("User and his reports correctly deleted");
                }
            }
        }
        else
        {
            Utils.showErrorAlert("Error in deleting reports in neo4j");
        }

        // If i am the user, go to welcome page
        if (user.getUsername().equals(appSession.getLoggedUser().getUsername()))
        {
            Utils.changeScene("/welcome.fxml", mouseEvent);
        }
        else // If i am an administrator and i have deleted another account, go to Administration Page
        {
            Utils.changeScene("/adminPage.fxml", mouseEvent);
        }
    }
    
    /**
     * Handle the click on the edit profile icon
     * @param mouseEvent
     */
    private void clickOnEditProfile(MouseEvent mouseEvent, User u)
    {
        EditProfilePageController editProfilePageController = (EditProfilePageController)
                Utils.changeScene("/editProfile.fxml", mouseEvent);
        editProfilePageController.setEditProfilePage(u);
    }

    /**
     * Handle the click on the administration page icon
     * @param mouseEvent
     */
    private void clickOnAdminPage(MouseEvent mouseEvent)
    {
        Utils.changeScene("/adminPage.fxml", mouseEvent);
    }

    /**
     * Function used to handle the click on the my profile icon
     * @param mouseEvent    event that represents the click on the icon
     */
    private void clickOnMyProfile(MouseEvent mouseEvent){
        ProfilePageController profilePageController = (ProfilePageController)
                Utils.changeScene("/profilePage.fxml", mouseEvent);
        profilePageController.setProfile(appSession.getLoggedUser());
    }
    /**
     * Function that let the navigation into the ui ---> homepage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnHomepageToChangePage(MouseEvent mouseEvent){
        Utils.changeScene("/homepage.fxml", mouseEvent);
    }

    /**
     * Handle the click on the follow/unfollow button. If the logged user follow the user's profile
     * then the click means unfollow, otherwise means follow. The image changes depending on this.
     */
    private void clickOnFollow()
    {
        if(neo4jDriver.isUserOneFollowedByUserTwo(userName.getText(),appSession.getLoggedUser().getUsername()))
        {
            // I want to unfollow an user
            neo4jDriver.unfollow(appSession.getLoggedUser().getUsername(),userName.getText());
            addFollow.setImage(new Image("img/follow_profile.png"));
            followerNumber.setText(String.valueOf(Integer.parseInt(followerNumber.getText()) - 1));
        }
        else
        {
            // I want to follow a user
            neo4jDriver.follow(appSession.getLoggedUser().getUsername(),userName.getText());
            addFollow.setImage(new Image("img/alreadyFollowed_profile.png"));
            followerNumber.setText(String.valueOf(Integer.parseInt(followerNumber.getText()) + 1));
        }
    }

    /**
     * Function that let the navigation into the ui ---> addReport
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnAddReportImg(MouseEvent mouseEvent){
    	 AddReportPageController addReportPageController = (AddReportPageController)
    			 Utils.changeScene("/addReport.fxml", mouseEvent);
        addReportPageController.setPlayerToReport(null);
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
     * Function that is called when the user click on the previous button
     * @param mouseEvent    Event that leads to the handler
     */
    private void clickOnPrevious(MouseEvent mouseEvent){
        Utils.removeAllFromPane(reportVbox);
        page--;
        if (page < 1)
            previousButton.setVisible(false);
        Utils.addReportsSnap(reportVbox,
                mongoDBDriver.getReportsFromAuthorUsername(HOW_MANY_SNAPSHOT_TO_SHOW*page,
                        HOW_MANY_SNAPSHOT_TO_SHOW, userName.getText()));
    }

    /**
     * Function that is called when the user click on the next button
     * @param mouseEvent    Event that leads to the handler
     */
    private void clickOnNext(MouseEvent mouseEvent){
        Utils.removeAllFromPane(reportVbox);
        page++;
        if (page > 0)
            previousButton.setVisible(true);
        Utils.addReportsSnap(reportVbox,
                mongoDBDriver.getReportsFromAuthorUsername(HOW_MANY_SNAPSHOT_TO_SHOW*page,
                        HOW_MANY_SNAPSHOT_TO_SHOW, userName.getText()));
    }
}
