public class User {
	private int userID;
    private String username;
    private String password;
    private String role;
    private String email;
    private String firstName;
    private String lastName;

    public User(){}

    public User(int userID, String username, String password, String role, String email, String firstName, String lastName) {
    	this.userID = userID;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    //Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

	public int getUserID() {
		return userID;
	}

	public String getEmail() {
		return email;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}
}

