/*
 DA METTERE CAMPI REPORT
 */

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.Date;

/**
 * Controller for the page of the report
 */
public class ReportPageController {

    @FXML private ImageView homeImg;
    @FXML private ImageView discoveryImg;
    @FXML private ImageView profileImg;
    @FXML private Text reportReview;
    @FXML private ImageView reportLikeImg;
    @FXML private ImageView reportPicture;
    @FXML private ImageView logoutPic;
    @FXML private Text reportFullName;
    @FXML private Text reportUsername;
    @FXML private Text reportRate;
    @FXML private Text reportPlayerRole;
    @FXML private Text reportPlayerAge;
    @FXML private ImageView reportEditImg;
    @FXML private Text reportCategories;
    @FXML private Text reportIngredients;
    @FXML private Text reportPlayerTeam;
    @FXML private Label reportLikes;
    @FXML private Label reportDate;
    @FXML private VBox reportVBox;
    @FXML private ImageView reportDelete;
    @FXML private TextArea commentsArea;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;

    private Report report;
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
        reportVBox.setAlignment(Pos.CENTER);
        appSession = Session.getInstance();
        neo4jDriver = Neo4jDriver.getInstance();
        mongoDBDriver = MongoDBDriver.getInstance();
        sendButton.setOnAction(actionEvent -> handleSendButtonAction());
        cancelButton.setOnAction(actionEvent -> handleCancelButtonAction());
        reportUsername.setOnMouseClicked(mouseEvent -> handleClickOnUsername(mouseEvent));
        reportFullName.setOnMouseClicked(mouseEvent -> handleClickOnPlayerName(mouseEvent));
        reportPicture.setOnMouseClicked(mouseEvent -> handleClickOnPlayerName(mouseEvent));
        if(appSession.getLoggedUser().getRole()=='O')
        reportEditImg.setOnMouseClicked(mouseEvent -> clickOnEditButton(mouseEvent));
        if(appSession.getLoggedUser().getRole()=='F')
        reportLikeImg.setOnMouseClicked(mouseEvent -> handleClickOnLike());
    }

    /**
     * Handler function for the click on the like
     */
    private void handleClickOnLike()
    {
        if(neo4jDriver.isThisReportLikedByOne(this.report.getCodReport(),appSession.getLoggedUser().getUsername()))
        {
            neo4jDriver.unlike(appSession.getLoggedUser().getUsername(),this.report.getCodReport());
            reportLikeImg.setImage(new Image("img/like.png"));
        }
        else
        {
            neo4jDriver.like(appSession.getLoggedUser().getUsername(),this.report.getCodReport());
            reportLikeImg.setImage(new Image("img/alreadyliked.png"));
        }

        reportLikes.setText(String.valueOf(neo4jDriver.howManyLikes(this.report.getCodReport())));
    }

    /**
     * Function who handle the adding comments, and upload on mongoDB
     */
    private void handleSendButtonAction(){
        if(commentsArea.getText().equals("")) {
            Utils.showErrorAlert("No Comments in the CommentsArea");
            return;
        }
        Comment comment = new Comment(appSession.getLoggedUser().getUsername(), commentsArea.getText(), new Date());
        Utils.showComment(reportVBox, comment, report);

        if(mongoDBDriver.addComment(report, comment))
        {
            Utils.showInfoAlert("Comment successfully added");
            commentsArea.setText("");
        }
    }

    /**
     * Function who handle the editButton
     * @param mouseEvent    Event that leads to the handler
     */
    private void clickOnEditButton(MouseEvent mouseEvent){
        AddReportPageController addReportPageController = (AddReportPageController)
                Utils.changeScene("/addReport.fxml", mouseEvent);
        addReportPageController.setReportToUpdate(report);
    }

    /**
     * Cancelling the comment textArea by clicking on the cancel Button
     */
    private void handleCancelButtonAction(){
        if(!commentsArea.getText().equals("")) commentsArea.setText("");
    }

    /**
     * Setters for the report, in which we also set the correct value to show
     * @param r    Report to show
     */
    public void setReport(Report r) {
        this.report = r;
        
        Player player = MongoDBDriver.getInstance().getCodPlayerFromCodPlayer(report.getCodPlayer());
        String authorUsername = neo4jDriver.getUserUsernameByUserID(report.getUserID());
        
        
        if(report.getFullName() == null)
        	reportFullName.setText(player.getFullName());
        else
        	reportFullName.setText(report.getFullName());
        reportReview.setText(report.getReview());
        if (report.getPicture() != null)
        {
            reportPicture.setImage(new Image(report.getPicture()));
        }
        else
        {
            reportPicture.setImage(new Image("/img/genericUser.png"));
        }
        
        if(report.getAuthorUsername() == null)
        	reportUsername.setText(authorUsername);
        else
        	reportUsername.setText(report.getAuthorUsername());
        
        if (report.getRate() != 0)
            reportRate.setText(Integer.toString(report.getRate()));
        else
            reportRate.setText(" -- ");
        
        if (report.getPlayerRole() == null)
        	reportPlayerRole.setText(player.getRole());
        else
        	reportPlayerRole.setText(report.getPlayerRole());
        
        if (report.getPlayerAge() != 0)
            reportPlayerAge.setText(Integer.toString(report.getPlayerAge()));
        else
        	reportPlayerAge.setText(Integer.toString(player.getAge()));
        
        if (report.getPlayerTeam() == null)
        	reportPlayerTeam.setText(player.getTeam());
        else
        	reportPlayerTeam.setText(report.getPlayerTeam());
        
        reportDate.setText("Published on: " + Utils.fromDateToString(report.getCreationTime()));
        reportLikes.setText(String.valueOf(neo4jDriver.howManyLikes(report.getCodReport())));
        if(neo4jDriver.isThisReportLikedByOne(report.getCodReport(),appSession.getLoggedUser().getUsername()))
            reportLikeImg.setImage(new Image("img/alreadyliked.png"));
        if(appSession.getLoggedUser().getRole()=='O' || appSession.getLoggedUser().getRole()=='A')
            reportLikeImg.setImage(new Image("img/alreadyliked.png"));
        if(report.getComments() != null && report.getComments().size() != 0) {
            Label commentsTitle = new Label("Comments:");
            commentsTitle.setFont(Font.font(24));
            reportVBox.getChildren().add(commentsTitle);
            Utils.showCommentsOfReport(reportVBox, report.getComments(), report);
        }
        String role= ""+appSession.getLoggedUser().getRole();
       if(role.equals("A") || appSession.getLoggedUser().getUsername().equals(report.getAuthorUsername()))
           reportDelete.setOnMouseClicked(mouseEvent -> handleDeleteButtonAction(mouseEvent));
        else
        	reportDelete.setVisible(false);
        if(!appSession.getLoggedUser().getUsername().equals(report.getAuthorUsername()))
            reportEditImg.setVisible(false);
    }

    /**
     * Handler for deleting this report
     */
    private void handleDeleteButtonAction(MouseEvent mouseEvent) {
        if(mongoDBDriver.deleteReport(report))
        {
            // if mongo operation is successfully executed then the neo4j op is performed
            if(!neo4jDriver.deleteReport(report))
            {
                // If neo4j fails I have to restore the initial condition in mongo
                mongoDBDriver.addReport(report);
                Utils.showErrorAlert("Error in delete the report");
            }
            else
            {
                Utils.showInfoAlert("Report correctly deleted");
            }
        }

        // Go to profile page
        ProfilePageController profilePageController =
                (ProfilePageController) Utils.changeScene("/profilePage.fxml", mouseEvent);
        profilePageController.setProfile(appSession.getLoggedUser());
    }

    /**
     * Function used to handle the click on the homepage icon
     * @param mouseEvent    event that represents the click on the icon
     */
    private void clickOnHomepageToChangePage(MouseEvent mouseEvent){
        Utils.changeScene("/homepage.fxml", mouseEvent);
    }

    /**
     * Function used to handle the click on the report's owner
     * @param mouseEvent    event that represents the click on the report's owner
     */
    private void handleClickOnUsername(MouseEvent mouseEvent){
        ProfilePageController profilePageController = (ProfilePageController)
                Utils.changeScene("/profilePage.fxml", mouseEvent);
        profilePageController.setProfile(neo4jDriver.getUserByUsername(reportUsername.getText()));
    }
    
    /**
     * Function used to handle the click on the report's player
     * @param mouseEvent    event that represents the click on the report's owner
     */
    private void handleClickOnPlayerName(MouseEvent mouseEvent){
        PlayerPageController playerPageController = (PlayerPageController)
                Utils.changeScene("/playerPage.fxml", mouseEvent);
        playerPageController.setPlayer(mongoDBDriver.getCodPlayerFromCodPlayer(report.getCodPlayer()));
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

