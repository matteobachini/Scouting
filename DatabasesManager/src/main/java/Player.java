public class Player {
	private int codPlayer; 
	private String fullName; 
	private String team;
	private String role; 
	private int age;
	private String foot;
	private String photo;
	private int rate;

    public Player(){}

    public Player(int codPlayer, String fullName, String team, String role, int age, String foot, String photo, int rate) {
        this.codPlayer = codPlayer;
        this.fullName = fullName;
        this.team = team;
        this.role = role;
        this.age = age;
        this.foot = foot;
        this.photo = photo;
        this.rate = rate;
    }

    /*
    public Player (String firstName, String lastName, String Playername, String password)
    {
        this (firstName, lastName, Playername, password, 0);
    }
    */

    //Getters
	public int getCodPlayer() {
		return codPlayer;
	}

	public String getFullName() {
		return fullName;
	}

	public String getTeam() {
		return team;
	}

	public String getRole() {
		return role;
	}

	public int getAge() {
		return age;
	}

	public String getFoot() {
		return foot;
	}

	public String getPhoto() {
		return photo;
	}

	public int getRate() {
		return rate;
	}
}

