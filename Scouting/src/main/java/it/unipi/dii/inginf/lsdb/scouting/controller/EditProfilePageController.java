package it.unipi.dii.inginf.lsdb.scouting.controller;

import it.unipi.dii.inginf.lsdb.scouting.model.Session;
import it.unipi.dii.inginf.lsdb.scouting.model.User;
import it.unipi.dii.inginf.lsdb.scouting.persistence.MongoDBDriver;
import it.unipi.dii.inginf.lsdb.scouting.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

public class EditProfilePageController {
    private Neo4jDriver neo4jDriver;
    private MongoDBDriver mongoDBDriver;
    private Session appSession;
    @FXML private ImageView homeImg;
    @FXML private ImageView logoutImg;
    @FXML private ImageView profileImg;
    @FXML private ImageView profileImgA;
    @FXML private ImageView discoveryImg;
    @FXML private TextField editFirstname;
    @FXML private TextField editLastname;
    @FXML private Button submit;
    @FXML private Button back;
    @FXML private TextField editPw;
    @FXML private TextField confirmEditPw;
    @FXML private Label role;
    @FXML private Label username;

    /**
     * Initialization functions
     */
    public void initialize ()
    {
        neo4jDriver = Neo4jDriver.getInstance();
        mongoDBDriver = MongoDBDriver.getInstance();
        appSession = Session.getInstance();
        homeImg.setOnMouseClicked(mouseEvent -> clickOnHomepageToChangePage(mouseEvent));
        profileImg.setOnMouseClicked(mouseEvent -> clickOnProfImgToChangePage(mouseEvent));
        discoveryImg.setOnMouseClicked(mouseEvent -> clickOnDiscImgtoChangePage(mouseEvent));
        logoutImg.setOnMouseClicked(mouseEvent -> clickOnLogoutImg(mouseEvent));
        back.setOnMouseClicked(mouseEvent -> clickOnBackButton(mouseEvent));
        submit.setOnMouseClicked(mouseEvent -> clickOnSubmit());
    }

    /**
     * Function to set the page with the information of the user
     * @param u  user's info
     */
    public void setEditProfilePage(User u)
    {
        username.setText(u.getUsername());

        editFirstname.setText(u.getFirstName());
        editLastname.setText(u.getLastName());
        switch (u.getRole())
        {
            case 'A':
                role.setText("Administrator");
            	profileImgA.setImage(new Image("/img/admIcon4.png"));
                break;
            case 'F':
                role.setText("Football Director");
            	profileImgA.setImage(new Image("/img/fdIcon4.png"));
                break;
            default:
                role.setText("Observer");
            	profileImgA.setImage(new Image("/img/obsIcon4.png"));

        }
    }

    /**
     * It handles the click on the submit button
     */
    private void clickOnSubmit()
    {
        String newPw = new String();
        // If I don't add a new pic then the correspondent field in neo4j will not be added because this String is null
        String newPic = null;

        if(editPw.getText().isEmpty() && confirmEditPw.getText().isEmpty()) // I don't wanna change the password
            newPw = neo4jDriver.getUserByUsername(username.getText()).getPassword();
        else if(!editPw.getText().isEmpty() && !confirmEditPw.getText().isEmpty()
                && editPw.getText().equals(confirmEditPw.getText()))
            newPw = editPw.getText();  // I wanna change the pw
        else
            Utils.showErrorAlert("The passwords don't match!");
        User u = new User (editFirstname.getText(), editLastname.getText(), username.getText(), newPw);
        System.out.print(newPw + u.getPassword());
        if(!newPw.isEmpty())
        {
        	if(neo4jDriver.updateUser(username.getText(), editFirstname.getText(), editLastname.getText(), newPic))
        	{
        		//If neo is ok, perform mongo
        		if(!mongoDBDriver.editUser(u))
        		{
        			// if mongo is not ok, reset the previously modified report
        			neo4jDriver.deleteUser(username.getText());

        			Utils.showErrorAlert("Error in update");
        		}
        		else
        		{
        			Utils.showInfoAlert("User succesfully inserted");
        		}
        	}
            Utils.showInfoAlert("Changes applied!");
        }

        editPw.setText("");
        confirmEditPw.setText("");
    }

    /**
     * Allow to return to the profile page of the owner of the visualized edit profile page
     * @param mouseEvent
     */
    private void clickOnBackButton(MouseEvent mouseEvent)
    {
        ProfilePageController profilePageController = (ProfilePageController)
                Utils.changeScene("/profilePage.fxml", mouseEvent);
        profilePageController.setProfile(neo4jDriver.getUserByUsername(username.getText()));
    }

    /**
     * Function that let the navigation into the ui ---> homepage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnHomepageToChangePage(MouseEvent mouseEvent){
        Utils.changeScene("/homepage.fxml", mouseEvent);
    }

    /**
     * Function that let the navigation into the ui ---> profilePage
     * @param mouseEvent event that represents the click on the icon
     */
    private void clickOnProfImgToChangePage(MouseEvent mouseEvent){
    	System.out.print("entrato");
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
