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

import it.unipi.dii.inginf.lsdb.scouting.model.Session;
import it.unipi.dii.inginf.lsdb.scouting.model.User;
import it.unipi.dii.inginf.lsdb.scouting.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.scouting.persistence.MongoDBDriver;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
/*import com.jfoenix.controls.TextField;
import com.jfoenix.controls.TextField;
import it.unipi.dii.inginf.lsdb.justrecipe.model.Session;
import it.unipi.dii.inginf.lsdb.justrecipe.model.User;
import it.unipi.dii.inginf.lsdb.justrecipe.persistence.Neo4jDriver;
import it.unipi.dii.inginf.lsdb.justrecipe.utils.Utils;*/
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.MouseEvent;

/**
 * Controller for the Welcome page
 */
public class WelcomePageController {
    @FXML private TextField usernameLoginTextField;
    @FXML private PasswordField passwordLoginTextField;
    @FXML private TextField firstNameRegistrationTextField;
    @FXML private TextField lastNameRegistrationTextField;
    @FXML private TextField usernameRegistrationTextField;
    @FXML private PasswordField passwordRegistrationTextField;
    @FXML private PasswordField confirmPasswordRegistrationTextField;
    @FXML private Button loginButton;
    @FXML private Button registrationButton;
    @FXML private ComboBox roleComboBox;
    @FXML private TextField emailTextField;
    private Neo4jDriver neo4jDriver;
    private MongoDBDriver mongoDBDriver;


    /**
     * Method called when the controller is initialized
     */
    public void initialize()
    {
        loginButton.setOnMouseClicked(mouseEvent -> handleLoginButtonAction(mouseEvent));
        registrationButton.setOnMouseClicked(mouseEvent -> handleRegisterButtonAction(mouseEvent));
        // Initializing the options of the ComboBox
        ObservableList<String> options =
                FXCollections.observableArrayList(
                        "Observer",
                        "Football Director"
                );
        emailTextField.focusedProperty().addListener((arg0, oldValue, newValue) -> {
        	if (!newValue) { //when focus lost
                if(!emailTextField.getText().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$") && !emailTextField.getText().equals("")){
                    //when it not matches the pattern 
                    //set the textField empty
                	Utils.showErrorAlert("Email not correct!");
                	emailTextField.setText("");
                }
            }
        });
        roleComboBox.setItems(options);
        neo4jDriver = Neo4jDriver.getInstance();
        mongoDBDriver = MongoDBDriver.getInstance();
    }

	/**
     * Method used to handle the Login button click event
     * @param actionEvent   The event that occurs when the user click the Login button
     */
    private void handleLoginButtonAction(MouseEvent actionEvent) {
        if (usernameLoginTextField.getText().equals("") || passwordLoginTextField.getText().equals(""))
        {
            Utils.showErrorAlert("You need to insert all the values!");
        }
        else
        {
            if (login(usernameLoginTextField.getText(), passwordLoginTextField.getText()))
            {
                HomePageController homePageController = (HomePageController)
                        Utils.changeScene("/homepage.fxml", actionEvent);
            }
            else
            {
                Utils.showErrorAlert("Login failed!");
            }
        }
    }

    /**
     * Method used to handle the Register button click event
     * @param actionEvent   The event that occurs when the user click the Register button
     */
    private void handleRegisterButtonAction(MouseEvent actionEvent) {
        if ((firstNameRegistrationTextField.getText().equals("") ||
                lastNameRegistrationTextField.getText().equals("") ||
                usernameRegistrationTextField.getText().equals("") ||
                passwordRegistrationTextField.getText().equals("") ||
                roleComboBox.getValue() == null ||
                emailTextField.getText().equals("") ||
                confirmPasswordRegistrationTextField.getText().equals(""))
            || (!passwordRegistrationTextField.getText().equals(confirmPasswordRegistrationTextField.getText())))
        {
            Utils.showErrorAlert("You need to insert all the values! Pay attention that the passwords must be equals!");
        }
        else
        {
        	char userType;
        	if(roleComboBox.getValue().toString().equals("Observer"))
        		userType = 'O';
        	else
        		userType = 'F';
            if (register(firstNameRegistrationTextField.getText(), lastNameRegistrationTextField.getText(),
                    usernameRegistrationTextField.getText(), passwordRegistrationTextField.getText(),
                    userType, emailTextField.getText()))
            {
                Session newSession = Session.getInstance();
                User registered = new User(
                		firstNameRegistrationTextField.getText(),
                        lastNameRegistrationTextField.getText(),
//                        null,
                        usernameRegistrationTextField.getText(), 
                        passwordRegistrationTextField.getText()
//                        ,0
                        , userType
                        , emailTextField.getText()
                        );
                registered.setFollowing(0);
                if (userType == 'O') 
                {
                	registered.setNumReports(0);
                }
                registered.setFollower(0);
                newSession.setLoggedUser(registered);
                Utils.changeScene("/homepage.fxml", actionEvent);
            }
            else
            {
                Utils.showErrorAlert("Registration failed! Username not available..");
            }
        }
    }

    /**
     * Function used to perform the operations needed to login a user
     * @param username  username of the user
     * @param password  password of the user
     * @return          true if the login was successful, otherwise false
     */
    private boolean login (final String username, final String password)
    {
        //User user = neo4jDriver.login(username, password);
        User user = MongoDBDriver.getInstance().login(username, password);
        if(user!=null)
        {
            Session newSession = Session.getInstance();
            newSession.setLoggedUser(user);
            System.out.print(user.getUsername());
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Function used to perform the operations needed to register a user
     * @param firsName  first name of the user
     * @param lastName  last name of the user
     * @param username  username of the user
     * @param password  password of the user
     * @return          true if the registration was successful, otherwise false
     */
    private boolean register (final String firstName, final String lastName, final String username,
                           final String password, final char role, final String email)
    {
    	if(neo4jDriver.addUser(firstName, lastName, username, role))
    	{
    		//If neo is ok, perform mongo
    		if(!mongoDBDriver.addUser(firstName, lastName, username, password, role, email))
    		{
    			// if mongo is not ok, reset the previously modified report
    			neo4jDriver.deleteUser(username);

    			Utils.showErrorAlert("Error in register");
    			return false;
    		}
    		else
    		{
    			Utils.showInfoAlert("User succesfully inserted");
    			return true;
    		}
    	}
    	return false;
    	//mongoDBDriver.addUser(firsName, lastName, username, password, role, email)
    }
   
}