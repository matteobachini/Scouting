package it.unipi.dii.inginf.lsdb.scouting.persistence;

import it.unipi.dii.inginf.lsdb.scouting.config.ConfigurationParameters;
import it.unipi.dii.inginf.lsdb.scouting.model.Player;
import it.unipi.dii.inginf.lsdb.scouting.model.Report;
import it.unipi.dii.inginf.lsdb.scouting.model.User;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.neo4j.driver.Values.NULL;
import static org.neo4j.driver.Values.parameters;

/**
 * This class is used to communicate with Neo4j
 */
public class Neo4jDriver implements DatabaseDriver{
    private static Neo4jDriver instance = null; // Singleton Instance

    private Driver driver;
    private String ip;
    private int port;
    private String username;
    private String password;

    private Neo4jDriver(ConfigurationParameters configurationParameters)
    {
        this.ip = configurationParameters.getNeo4jIp();
        this.port = configurationParameters.getNeo4jPort();
        this.username = configurationParameters.getNeo4jUsername();
        this.password = configurationParameters.getNeo4jPassword();
    }

    public static Neo4jDriver getInstance()
    {
        if (instance == null)
        {
            instance = new Neo4jDriver(Utils.readConfigurationParameters());
        }
        return instance;
    }

    /**
     * Method that inits the Driver
     */
    @Override
    public boolean initConnection()
    {
        try
        {
            //driver = GraphDatabase.driver("neo4j://" + ip + ":" + port, AuthTokens.basic(username, password));
            driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "root" ) );
            driver.verifyConnectivity();
        } catch (Exception e)
        {
            System.out.println("Neo4J is not available");
            return false;
        }
            return true;
    }

    /**
     * Method for closing the connection of the Driver
     */
    @Override
    public void closeConnection ()
    {
        if (driver != null)
            driver.close();
    }


    /**
     * Add a report in Neo4j databases, used the match in order to have no code replications for the editReport
     * @param r  Object report that will be added
     * @return true if operation is successfully executed, false otherwise
     */
    public boolean newReport(Report r)
    {
    	Map<String,Object> params = new HashMap<>();
    	List<Map<String,Object>> list = new ArrayList<>();
    	Map<String,Object> props = new HashMap<>();
    	Date date = new Date();
        props.put("codReport", r.getCodReport());
        props.put("rate", r.getRate());
        props.put("codPlayer", r.getCodPlayer());
        props.put("userID", r.getUserID());
        props.put("timestamp", new Date(date.getTime()+(1000)).getTime());
        list.add(props);
        params.put( "batch", list);
        
		try ( Session session = driver.session())
		{

		   session.writeTransaction((TransactionWork<Void>) tx -> {
		        tx.run( "UNWIND $batch AS row "
		        		+ "MATCH (u:User {userID: row.userID}) " +
		        		"CREATE (u)-[:ADDS]->(r:Report " +
		        		"{rate: row.rate, userID: row.userID, " +
		        		"codPlayer: row.codPlayer, " +
		        		"codReport: row.codReport, creationTime: datetime() })",
		        params);
		        return null;
		   });
		        
		   session.writeTransaction((TransactionWork<Void>) tx -> {
		        tx.run( "UNWIND $batch AS row " +
		        		"MATCH (p:Player {codPlayer: row.codPlayer}) " +
		        		"MATCH (r:Report {codReport: row.codReport}) " +
		        		"CREATE (p)-[:HAVE]->(r)",
		        params);     
		        return null;
		    });
		   
		    return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		    System.err.println("Error in adding report on Neo4J");
		    return false;
		}
    }

    public int getNewCodReport() {
	
    	try ( Session session = driver.session())
        {       
    		int maxCodReport =
            session.writeTransaction((TransactionWork<Integer>) tx -> {
			Result result = tx.run("MATCH (n:Report) RETURN MAX(n.codReport) AS MaxCodReport");
			return result.single().get("MaxCodReport").asInt() + 1;
            });
    		return maxCodReport;
        }
        catch (Exception ex)
        {
            System.err.println("Error calculating maxCodReport on Neo4J");
            ex.printStackTrace();
            return 0;
        }
	}

	/**
     * Update a report given as parameter
     * @param r
     * @return  true if operation is successfully executed, false otherwise
     */
    public boolean updateReport(Report r)
    {
        try ( Session session = driver.session())
        {
           session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "MATCH (r:Report WHERE r.codReport= $codReport) " +
                        "SET r.rate = $rate",
                parameters( "codReport", r.getCodReport(),"rate", r.getRate() ) );
                return null;
            });
            return true;
        }
        catch (Exception ex)
        {
            System.err.println("Error in updating report on Neo4J");
            return false;
        }
    }

    /**
     * Method that creates a new node in the graphDB with the information of the new user
     * @param firstName     first name of the new user
     * @param lastName      last name of the new user
     * @param username      username of the new user
     * @param password      password of the new user
     */
    public boolean addUser( final String firstName, final String lastName, final String username,
                         final char role)
    {
        try ( Session session = driver.session())
        {        	
            session.writeTransaction((TransactionWork<Void>) tx -> {
            	Session session2 = driver.session();
				Result result = session2.run("MATCH (n:User) RETURN COUNT(*) AS MaxUserID");
        		int maxUserID = result.single().get("MaxUserID").asInt();
        		maxUserID++;
                tx.run( "CREATE (u:User {firstName: $firstName, lastName: $lastName, username: $username," +
                                 "role: $role, userID: $userID})",
                        parameters( "firstName", firstName, "lastName", lastName, "username",
                                username, "role", role, "userID", maxUserID ) );
                return null;
            });
            return true;
        }
        catch (Exception e)
        {
            //e.printStackTrace();
            return false;
        }
    }

    /**
     * It performs the login with the given username and password
     * @param username  Username of the target user
     * @param password  Password of the target user
     * @return The object user if the login is done successfully, otherwise null
     */
    public User login(final String username, final String password)
    {
        User u = null;
        try ( Session session = driver.session())
        {
            u = session.readTransaction((TransactionWork<User>) tx -> {
                Result result = tx.run( "MATCH (u:User) " +
                                "WHERE u.username = $username " +
                                "AND u.password = $password " +
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN u.firstName AS firstName, u.lastName AS lastName, u.picture AS picture, " +
                                "u.username AS username, u.password AS password, u.role AS role, u.email AS email, u.userID AS userID, " +
                                "COUNT(DISTINCT f1) AS follower, COUNT (DISTINCT f2) AS following, " +
                                "COUNT (DISTINCT a) AS numReports ",
                        parameters( "username", username, "password", password));
                User user = null;
                try
                {
                    Record r = result.next();
                    String firstName = r.get("firstName").asString();
                    String lastName = r.get("lastName").asString();
                    String email = r.get("email").asString();
                    int userID = r.get("userID").asInt();
                    char role = r.get("role").asString().charAt(0);
                    user = new User(firstName, lastName, username, password, role, email);
                    user.setFollower(r.get("follower").asInt());
                    user.setFollowing(r.get("following").asInt());
                    user.setNumReports(r.get("numReports").asInt());
                    user.setUserId(userID);
                }
                catch (NoSuchElementException ex)
                {
                    user = null;
                }
                return user;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return u;
    }
    
    public User getUserSocialInformations(final String username)
    {
        User u = null;
        try ( Session session = driver.session())
        {
            u = session.readTransaction((TransactionWork<User>) tx -> {
                Result result = tx.run( "MATCH (u:User) " +
                                "WHERE u.username = $username " +
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN " +
                                "COUNT(DISTINCT f1) AS follower, "
                                + "COUNT (DISTINCT f2) AS following, " +
                                "COUNT (DISTINCT a) AS numReports ",
                        parameters( "username", username));
                User user = new User();
                try
                {
                    Record r = result.next();
                    user.setFollower(r.get("follower").asInt());
                    user.setFollowing(r.get("following").asInt());
                    user.setNumReports(r.get("numReports").asInt());
                }
                catch (NoSuchElementException ex)
                {
                    user = null;
                }
                return user;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return u;
    }

    /**
     * It controls if a user with username @one is followed by the user with username @two
     * @param one  Username of user one
     * @param two  Username of user two
     * @return  true if one is followed by two, false otherwise
     */
    public Boolean isUserOneFollowedByUserTwo(String one, String two)
    {
        Boolean relation = false;
        try(Session session = driver.session())
        {
            relation = session.readTransaction((TransactionWork<Boolean>) tx -> {
                    Result r = tx.run("match (a:User{username:$two})-[r:FOLLOWS]->(b:User{username:$one}) " +
                            "return count(*)",parameters("one",one,"two",two));
                    Record rec = r.next();
                    if(rec.get(0).asInt()==0)
                        return false;
                    else
                        return true;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return relation;
    }
    
    public Boolean isPlayerWishedByUser(int codPlayer, int userID)
    {
        Boolean relation = false;
        try(Session session = driver.session())
        {
            relation = session.readTransaction((TransactionWork<Boolean>) tx -> {
                    Result r = tx.run("match (a:User{userID:$userID})-[r:WISHES]->(b:Player{codPlayer:$codPlayer}) " +
                            "return count(*)",parameters("userID",userID,"codPlayer",codPlayer));
                    Record rec = r.next();
                    if(rec.get(0).asInt()==0)
                        return false;
                    else
                        return true;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return relation;
    }

    /**
     * It creates the relation follower-[:Follow]->following
     * @param follower  The user who starts to follow
     * @param following  The user who is followed by follower
     */
    public void follow(String follower, String following)
    {
        try(Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Integer>) tx -> {
                    tx.run("match (a:User) where a.username=$following " +
                            "match (b:User) where b.username=$follower " +
                            "merge (b)-[:FOLLOWS]->(a)",parameters("follower",follower,"following",following));
                    return 1;
                });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void addToWishlist(int codPlayer, int userID)
    {
        try(Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Integer>) tx -> {
                    tx.run("match (a:Player) where a.codPlayer=$codPlayer " +
                            "match (b:User) where b.userID=$userID " +
                            "merge (b)-[:WISHES]->(a)",parameters("codPlayer",codPlayer,"userID",userID));
                    return 1;
                });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * It deletes the relation oldFollower-[:Follow]->oldFollowing
     * @param oldFollower  The user who decide to unfollow
     * @param oldFollowing  The user unfollowed
     */
    public void unfollow(String oldFollower, String oldFollowing)
    {
        try(Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Integer>) tx -> {
                    tx.run("match (u:User{username:$oldFollower})-[r:FOLLOWS]->(u2:User{username:$oldFollowing})" +
                            " delete r",parameters("oldFollower",oldFollower,"oldFollowing",oldFollowing));
                    return 1;
                });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void removeFromWishlist(int oldCodPlayer, int oldUserID)
    {
        try(Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Integer>) tx -> {
                    tx.run("match (u:User{userID:$oldUserID})-[r:WISHES]->(u2:Player{codPlayer:$oldCodPlayer})" +
                            " delete r",parameters("oldUserID",oldUserID,"oldCodPlayer",oldCodPlayer));
                    return 1;
                });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * It counts the number of follower of a given user
     * @param user  Username of the target user
     * @return  the number of follower
     */
    public int howManyFollower(int codUser)
    {
        return howMany( "MATCH (a:User)-[r:FOLLOWS]->(b:User{codUser:$placeholder}) RETURN count(a)",codUser);
    }

    /**
     * It counts the number of following of a given user
     * @param user  Username of the target user
     * @return  The number of following
     */
    public int howManyFollowing(int user)
    {
        return howMany("match (a:User)<-[r:FOLLOWS]-(b:User{codUser:$placeholder}) return count(a)",user);
    }

    /**
     * It counts the number of reports added from the given user
     * @param user  Username of the given user
     * @return  The number of reports added from the user
     */
    public int howManyReportsAdded(int codUser)
    {
        return howMany("match (p:Report)<-[r:ADDS]-(b:User{userID:$placeholder}) return count(p)",codUser);
    }

    /**
     * It counts the number of likes of a given report
     * @param reportTitle  Title of the given report
     * @return  The number of likes
     */
    public int howManyLikes(int codReport)
    {
        return howMany("match (a:User)-[r:LIKES]->(b:Report{codReport:$placeholder}) return count(a)",codReport);
    }
    /**
     * Private function which execute a given query that count how many relation enter or go out from a node
     * @param query  query text
     * @param userOrReport  username of the given user or title of the given report
     * @return  the number of incoming or outgoing relation
     */
    private int howMany(String query, int codUserOrCodReport)
    {
        int howMany = 0;

        try(Session session = driver.session())
        {
            howMany = session.readTransaction((TransactionWork<Integer>) tx -> {
                Result r = tx.run(query,parameters("placeholder",codUserOrCodReport));
                Record rec = r.next();
                return rec.get(0).asInt();
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return howMany;
    }

    /**
     * It controls if the given report is liked by the given user
     * @param reportTitle  title of the given report
     * @param one  username of the given user
     * @return  true if the given report is liked by the given user, false otherwise
     */
    public Boolean isThisReportLikedByOne(int codReport, String one)
    {
        Boolean relation = false;
        try(Session session = driver.session())
        {
            relation = session.readTransaction((TransactionWork<Boolean>) tx -> {
                Result r = tx.run("match (a:User{username:$one})-[r:LIKES]->(b:Report{codReport:$t}) " +
                        "return count(*)",parameters("one",one,"t",codReport));
                Record rec = r.next();
                if(rec.get(0).asInt()==0)
                    return false;
                else
                    return true;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return relation;
    }

    /**
     * It creates the relation user-[:LIKES]->report
     * @param user  Username of the target user
     * @param reportTitle  Title of the target report
     */
    public void like(String user, int codReport)
    {
        try(Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Integer>) tx -> {
                    tx.run("match (a:User) where a.username=$u " +
                            "match (b:Report) where b.codReport=$t " +
                            "merge (a)-[:LIKES]->(b)",parameters("u",user,"t",codReport));
                    return 1;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * It deletes the relation user-[:LIKES]->report
     * @param user  Username of the target user
     * @param reportTitle  Title of the target report
     */
    public void unlike(String user, int codReport)
    {
        try(Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Integer>) tx -> {
                    tx.run("match (u:User{username:$u})-[r:LIKES]->(p:Report{codReport:$t})" +
                            " delete r",parameters("u",user,"t",codReport));
                    return 1;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Update the information about the user, given the username (username cannot be changed)
     * @param username  username that identifies the target user
     * @param newFirst  new first name
     * @param newLast  new last name
     * @param newPw  new password
     * @return 
     */
    public boolean updateUser(String username, String newFirst, String newLast, String newPic)
    {
        try(Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Boolean>) tx -> {
                tx.run("MATCH (u:User{username:$u}) " +
                        "SET u.firstName = $f, " +
                                " u.lastName = $l, " +
                                " u.picture = $pic",
                        parameters("u", username, "f", newFirst, "l", newLast, "pic", newPic));
                return true;
            } );
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }



    /**
     * It deletes the user with the given username
     * @param username username of the user that I want to delete
     */
    public boolean deleteUser(String username)
    {
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "MATCH (u:User) WHERE u.username = $username DETACH DELETE u",
                        parameters( "username", username) );
                return null;
            });
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    /**
     * It deletes a report given the title
     * @param report  Report to delete
     * @return  true if operation is successfully executed, false otherwise
     */
    public boolean deleteReport(Report report)
    {
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "MATCH (r:Report) WHERE r.codReport = $codReport DETACH DELETE r",
                        parameters( "codReport", report.getCodPlayer()) );
                return null;
            });
            return true;
        }
        catch (Exception ex)
        {
            System.err.println("Error in delete report");
            return false;
        }
    }

    /**
     * Function that delete all the reports of a user
     * @param username  Username of the user to delete
     * @return  true if operation is successfully executed, false otherwise
     */
    public boolean deleteAllReportsOfUser (String username)
    {
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "MATCH (u:User)-[:ADDS]->(r:Report) WHERE u.username = $username DETACH DELETE r",
                        parameters( "username", username) );
                return null;
            });
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    /**
     * Function that returns the report snapshots of one user
     * @param howManySkip   How many to skip
     * @param howMany       How many to obtain
     * @param username      Username of the user
     * @return              List of the reports
     */
    public List<Report> getReportSnaps(int howManySkip, int howMany, String username){
        List <Report> reports = new ArrayList<>();
        try(Session session = driver.session()) {
            session.readTransaction((TransactionWork<List<Report>>) tx -> {
                Result result = tx.run("MATCH (u:User{username:$username})-[a:ADDS]->(r:Report) " +
                                "RETURN u.username as authorUsername, r.codPlayer AS codPlayer, " +
                                "r.codReport AS codReport, r.userID AS userID, r.rate AS rate,"
                                + "r.review AS review " +
                                "ORDER BY a.when DESC " +
                                "SKIP $skip LIMIT $limit",
                        parameters("username", username, "skip", howManySkip, "limit", howMany));

                while(result.hasNext()){
                    Record r = result.next();
                    String review = null;
                    int rate;
                    int codReport = r.get("codReport").asInt();
                    int codPlayer = r.get("codPlayer").asInt();
                    int userID = r.get("userID").asInt();
                    if(r.get("rate") != NULL)
                        rate = r.get("rate").asInt();
                    else
                    	rate = 0;
                    
                    Report report = new Report(codReport, codPlayer, rate, review, userID);
                    reports.add(report);
                }
                return reports;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return reports;
    }

    
    
    
    /**
     * Function that returns the report snapshots of one user
     * @param howManySkip   How many to skip
     * @param howMany       How many to obtain
     * @param username      Username of the user
     * @return              List of the reports
     */
    public List<Player> getWishedSnaps(int howManySkip, int howMany, String username){
    	List<Player> listOfPlayers = new ArrayList<>();
    	try(Session session = driver.session()) {
            session.readTransaction((TransactionWork<List<Player>>) tx -> {
                Result result = tx.run("MATCH p=(n:User WHERE n.username= $username)-[r:WISHES]->(pl)"
                		+ "RETURN  pl.role AS role, pl.codPlayer AS codPlayer, pl.age AS age, pl.fullName AS fullName,"
                		+ "pl.team AS team SKIP $skip  LIMIT $limit",
                        parameters("username", username, "skip", howManySkip, "limit", howMany));

                while(result.hasNext()){
                    Record r = result.next();
                    String fullName = r.get("fullName").asString();
                    String role = r.get("role").asString();
                    int age = r.get("age").asInt();
                    String team = r.get("team").asString();
                    int codPlayer = r.get("codPlayer").asInt();
                    Player player = new Player (fullName, role, age, team, codPlayer);
                    //report.setAuthorUsername(authorUsername);
                    listOfPlayers.add(player);
                    
                }
                return listOfPlayers;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return listOfPlayers;
    }
    

    
    /**
     * Function that returns the report snapshots of one user
     * @param howManySkip   How many to skip
     * @param howMany       How many to obtain
     * @param username      Username of the user
     * @return              List of the reports
     */
    public List<Player> getTopCodPlayerFromRoleReport(String role, int howManySkip, int howMany){
    	List<Player> listOfPlayers = new ArrayList<>();
    	try(Session session = driver.session()) {
            session.readTransaction((TransactionWork<List<Player>>) tx -> {
                Result result = tx.run("MATCH q=(p:Player where p.role=$role)-[h:HAVE]->(r:Report) "
                		+ "RETURN p.role AS role, p.codPlayer AS codPlayer, p.age AS age, p.fullName AS fullName,"
                		+ "p.team AS team, avg (distinct r.rate) as rate "
                		+ "ORDER BY rate DESC SKIP $skip  LIMIT $limit",
                        parameters("role", role, "skip", howManySkip, "limit", howMany));

                while(result.hasNext()){
                    Record r = result.next();
                    String fullName = r.get("fullName").asString();
                    int age = r.get("age").asInt();
                    String team = r.get("team").asString();
                    int codPlayer = r.get("codPlayer").asInt();
                    Player player = new Player (fullName, role, age, team, codPlayer);
                    player.setAvgReportsRate(r.get("rate").asDouble());
                    listOfPlayers.add(player);
                    
                }
                return listOfPlayers;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return listOfPlayers;
    }
    
    public List<Player> searchTopPlayersOfATeamByYear(String team, int year, int howManySkip, int howMany){
    	List<Player> listOfPlayers = new ArrayList<>();
    	String team2 = ".*" + team.toLowerCase() + ".*";
    	try(Session session = driver.session()) {
            session.readTransaction((TransactionWork<List<Player>>) tx -> {
                Result result = tx.run("MATCH q=(p:Player where toLower(p.team)=~$team)-[h:HAVE]->(r:Report where r.creationTime.year=$year) " 
                		+ "RETURN p.role AS role, p.codPlayer AS codPlayer, p.age AS age, p.fullName AS fullName, "
                		+ "p.team AS team, avg (distinct r.rate) as rate "
                		+ "ORDER BY rate DESC SKIP $skip  LIMIT $limit",
                        parameters("team", team2, "year", year, "skip", howManySkip, "limit", howMany));

                while(result.hasNext()){
                    Record r = result.next();
                    String fullName = r.get("fullName").asString();
                    int age = r.get("age").asInt();
                    String playerteam = r.get("team").asString();
                    String role = r.get("role").asString();
                    int codPlayer = r.get("codPlayer").asInt();
                    Player player = new Player (fullName, role, age, playerteam, codPlayer);
                    player.setAvgReportsRate(r.get("rate").asDouble());
                    listOfPlayers.add(player);
                    
                }
                return listOfPlayers;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return listOfPlayers;
    }

    /**
     * Function that returns the information for creating the snapshot in the homepage
     * The reports to show are the ones that are added by the other users that I follow
     * @param howManySkip       How many reports to skip from the results
     * @param howMany           How many reports to return
     * @param username          The username of the user
     * @param userType          The type of the user (Admin/Observer/Football Director)
     * @return                  The list of reports to show
     */
    public List<Report> getHomepageReportSnap(int howManySkip, int howMany, String username)
    {
    	User user = getUserByUsername(username);
    	char userType = user.getRole();
    	int userID = user.getUserId();
        List<Report> reports = new ArrayList<>();
        String query;
        if(userType=='O') {
        	query =" MATCH (rr:Report)-[:HAVE]-(reports)"
        			+"WHERE rr.userID = $userID  RETURN reports.fullName as fullName , rr.rate as rate, rr.userID as userID, "
        			+ "reports.role AS role, reports.codPlayer AS codPlayer, reports.age AS age, rr.codReport AS codReport, reports.team AS team, "
        			+ "rr.creationTime as creationTime, toString(rr.creationTime) as creationTimeString,  "
        			+ "toString(apoc.temporal.format(rr.creationTime, \"dd MMMM yyyy HH:mm\")) AS creationTimeFormatString "
        			+ "ORDER BY rr.creationTime DESC SKIP $skip LIMIT $limit";
        }
        //Football Director or Admin
        else{
        	query = "MATCH (rr:Report)-[:HAVE]-(reports)"
        			+"RETURN reports.fullName as fullName , rr.rate as rate, rr.userID as userID, "
        			+ "reports.role AS role,reports.codPlayer AS codPlayer, reports.age AS age, rr.codReport AS codReport, reports.team AS team, "
        			+ "rr.creationTime as creationTime, toString(rr.creationTime) as creationTimeString,  "
        			+ "toString(apoc.temporal.format(rr.creationTime, \"dd MMMM yyyy HH:mm\")) AS creationTimeFormatString "
        			+ "ORDER BY rr.creationTime DESC SKIP $skip LIMIT $limit";
        }

        try(Session session = driver.session()) {
            session.readTransaction((TransactionWork<Void>) tx -> {
                Result result = tx.run(query,
                        parameters("userID", userID, "skip", howManySkip, "limit", howMany));

                while(result.hasNext()){
                    Record r = result.next();
                    String fullName = r.get("fullName").asString();
                    String playerRole = r.get("role").asString();
                    String playerTeam = r.get("team").asString();
                    int reportUserID = r.get("userID").asInt();
                    int playerAge = r.get("age").asInt();
                    int codReport = r.get("codReport").asInt();
                    int rate = 0;
                    String authorUsername;
                    if(userType == 'O') {
                    	authorUsername = username;
                    }
                    else {
                    	authorUsername = getUserUsernameByUserID(reportUserID);
                    }
                    if(r.get("rate") != NULL)
                        rate = r.get("rate").asInt();
                    DateFormat formatter = new SimpleDateFormat("dd MMMMMMMM yyyy HH:mm");
                    Date creationTime = null;
					try {
						creationTime = formatter.parse(r.get("creationTimeFormatString").asString());
					} catch (ParseException e) {
						e.printStackTrace();
					}
                    Report report = new Report(codReport, fullName, authorUsername, rate, creationTime, playerRole, playerAge, playerTeam);
                    report.setCodPlayer(r.get("codPlayer").asInt());
                    reports.add(report);
                }
                return null;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return reports;
    }

    public String getUserUsernameByUserID(int userID) {
    	String username = null;
        try (Session session = driver.session()) {
           username = session.readTransaction((TransactionWork<String>) tx -> {
                Result result = tx.run("MATCH (u:User) " +
                                "WHERE u.userID = $userID " +
                                "RETURN u.username AS username",
                        parameters("userID", userID));

                String u = null;
                if(result.hasNext()){
                    Record r = result.next();
                    u = r.get("username").asString();
                }
                return u;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return username;
	}

    /**
     * Function that returns a list of the users that contains in their username the word passed
     * @param howManySkip           How many to skip
     * @param howMany               How many to return
     * @param usernameWritten       Username passed by the user
     * @return                      The list of users
     */
    public List<User> searchObserverByUsername (int howManySkip, int howMany, String usernameWritten)
    {
        List<User> users = new ArrayList<>();
        try(Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (u:User WHERE u.role='O') " +
                                "WHERE toLower(u.username) CONTAINS toLower($username) " + //case insensitive search
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN u.firstName AS firstName, u.lastName AS lastName, u.email AS email," +
                                "u.username AS username, u.password AS password, u.role AS role, u.userID as userID," +
                                "COUNT(DISTINCT f1) AS follower, COUNT (DISTINCT f2) AS following, COUNT (DISTINCT a) AS numReports " +
                                "SKIP $skip LIMIT $limit",
                        parameters("username",usernameWritten, "skip", howManySkip, "limit", howMany));

                while(result.hasNext()){
                    Record r = result.next();
                    String firstName = r.get("firstName").asString();
                    String lastName = r.get("lastName").asString();
                    String username = r.get("username").asString();
                    String password = r.get("password").asString();
                    char userType = r.get("role").asString().charAt(0);
                    User user = new User(firstName, lastName, username, password);
                    user.setEmail(r.get("email").asString());
                    user.setFollower(r.get("follower").asInt());
                    user.setFollowing(r.get("following").asInt());
                    user.setNumReports(r.get("numReports").asInt());
                    users.add(user);
                }
                return null;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return users;
    }
    
    public List<User> searchUserByUsername (int howManySkip, int howMany, String usernameWritten)
    {
        List<User> users = new ArrayList<>();
        try(Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (u:User) " +
                                "WHERE toLower(u.username) CONTAINS toLower($username) " + //case insensitive search
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN u.firstName AS firstName, u.lastName AS lastName, u.email AS email," +
                                "u.username AS username, u.password AS password, u.role AS role, u.userID as userID, " +
                                "COUNT(DISTINCT f1) AS follower, COUNT (DISTINCT f2) AS following, COUNT (DISTINCT a) AS numReports " +
                                "SKIP $skip LIMIT $limit",
                        parameters("username",usernameWritten, "skip", howManySkip, "limit", howMany));

                while(result.hasNext()){
                    Record r = result.next();
                    String firstName = r.get("firstName").asString();
                    String lastName = r.get("lastName").asString();
                    String username = r.get("username").asString();
                    String password = r.get("password").asString();
                    char userType = r.get("role").asString().charAt(0);
                    User user = new User(firstName, lastName, username, password);
                    user.setRole(userType);
                    user.setEmail(r.get("email").asString());
                    //user.setNumReports(r.get("numReports").asInt());
                    users.add(user);
                }
                return null;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Function that returns the list of users whom contains in their full name the string passed
     * @param howManySkip       How many to skip
     * @param howMany           How many to retrieve
     * @param fullName          Part of the full name
     * @return                  The list of users
     */
    public List<User> searchObserverByLastName (int howManySkip, int howMany, String lastNameSearch)
    {
        List<User> users = new ArrayList<>();
        try(Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (u:User WHERE u.role='O') " +
                                "WHERE toLower(u.lastName) CONTAINS toLower($lastName) " +
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN u.firstName AS firstName, u.lastName AS lastName, " +
                                "u.username AS username, u.password AS password, u.role AS role " +
                                "SKIP $skip LIMIT $limit",
                        parameters("lastName",lastNameSearch, "skip", howManySkip, "limit", howMany));

                while(result.hasNext()){
                    Record r = result.next();
                    String firstName = r.get("firstName").asString();
                    String lastName = r.get("lastName").asString();
                    String username = r.get("username").asString();
                    String password = r.get("password").asString();
                    char userType = r.get("role").asString().charAt(0);
                    User user = new User(firstName, lastName, username, password);
                    user.setEmail(r.get("email").asString());
                    user.setNumReports(r.get("numReports").asInt());
                    users.add(user);
                }
                return null;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Function that returns the list of users whom contains in their full name the string passed
     * @param howManySkip       How many to skip
     * @param howMany           How many to retrieve
     * @param fullName          Part of the full name
     * @return                  The list of users
     */
    public List<User> searchUserByFullName (int howManySkip, int howMany, String fullName)
    {
        List<User> users = new ArrayList<>();
        try(Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (u:User) " +
                                // consider firstName-lastName and lastName-firstName
                                "WHERE toLower(u.firstName + ' ' + u.lastName) CONTAINS toLower($fullName) " +
                                "OR toLower(u.lastName + ' ' + u.firstName) CONTAINS toLower($fullName) " +
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN u.firstName AS firstName, u.lastName AS lastName, u.picture AS picture, " +
                                "u.username AS username, u.password AS password, u.role AS role, " +
                                "COUNT(DISTINCT f1) AS follower, COUNT (DISTINCT f2) AS following, COUNT (DISTINCT a) AS numReports " +
                                "SKIP $skip LIMIT $limit",
                        parameters("fullName",fullName, "skip", howManySkip, "limit", howMany));

                while(result.hasNext()){
                    Record r = result.next();
                    String firstName = r.get("firstName").asString();
                    String lastName = r.get("lastName").asString();
                    String picture = null;
                    char userType = r.get("role").asString().charAt(0);
                    String username = r.get("username").asString();
                    String password = r.get("password").asString();
                    User user = new User(firstName, lastName, username, password);
                    user.setRole(userType);
                    user.setFollower(r.get("follower").asInt());
                    user.setFollowing(r.get("following").asInt());
                    user.setNumReports(r.get("numReports").asInt());
                    users.add(user);
                }
                return null;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return users;
    }
    
    /**
     * Function that returns the list of users whom contains in their full name the string passed
     * @param howManySkip       How many to skip
     * @param howMany           How many to retrieve
     * @param fullName          Part of the full name
     * @return                  The list of users
     */
    public List<Report> searchReportFollowedObserver (int howManySkip, int howMany, String usernameSession)
    {	 
    	List <String> userIDObs = new ArrayList<>();
        List<Report> reports = new ArrayList<>();
        try(Session session = driver.session()) {
        	userIDObs = session.readTransaction((TransactionWork<List<String>>)  tx -> {
        		List <String> u = new ArrayList<>();
                Result result = tx.run("MATCH p=(f:User where f.username= $FbUsername)-[r:FOLLOWS]->(o) " + 
                		 "RETURN toString(o.username) as Username SKIP $skip LIMIT $limit",
                         parameters("FbUsername",usernameSession, "skip", howManySkip, "limit", howMany));
                		
                while(result.hasNext()){
                    Record r = result.next();
                   
                    u.add(r.get("Username").asString());
                }
                return u;
            });
            
            
            for(String user: userIDObs) {
                    List<Report> reportsUser = new ArrayList<>();
                    reportsUser = getHomepageReportSnap(howManySkip, howMany, user);
                    reports.addAll(reportsUser);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Function that returns an ordered list of the most followed and active users
     * Most followed -> depends on the number of follower
     * Active user -> depends on the number of report added
     * @param howManySkip       How many users to skip
     * @param howMany           How many users to get
     * @return                  A list of the most followed and active users
     */
    public List<User> searchMostFollowedObservers (int howManySkip, int howMany)
    {
        List<User> users = new ArrayList<>();
        try(Session session = driver.session())
        {
            users = session.readTransaction((TransactionWork<List<User>>)  tx -> {
                Result result = tx.run("MATCH (u:User WHERE u.role='O') " +
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN u.firstName, u.lastName, u.username, u.firstName AS firstName, " +
                                "u.lastName AS lastName, " +
                                "u.username AS username, u.password AS password, u.role AS role, " +
                                "COUNT(DISTINCT f1) AS follower, " +
                                "COUNT(DISTINCT f2) AS following, " +
                                "COUNT(DISTINCT a) AS numReports " +
                                "ORDER BY follower DESC " +
                                "SKIP $howManySkip LIMIT $howMany",
                        parameters("howManySkip", howManySkip, "howMany", howMany));
                List<User> listOfUsers = new ArrayList<>();
                while(result.hasNext()){
                    Record r = result.next();
                    String firstName = r.get("firstName").asString();
                    String lastName = r.get("lastName").asString();
                    String picture = null;
                    String username = r.get("username").asString();
                    String password = r.get("password").asString();
                    User user = new User(firstName, lastName, username, password);
                    user.setFollower(r.get("follower").asInt());
                    user.setFollowing(r.get("following").asInt());
                    user.setNumReports(r.get("numReports").asInt());
                    user.setRole(r.get("role").asString().charAt(0));
                    listOfUsers.add(user);
                }
                return listOfUsers;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return users;
    }
    
    public List<User> searchMostActiveObservers (int howManySkip, int howMany)
    {
        List<User> users = new ArrayList<>();
        try(Session session = driver.session())
        {
            users = session.readTransaction((TransactionWork<List<User>>)  tx -> {
                Result result = tx.run("MATCH (u:User WHERE u.role='O') " +
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN u.firstName, u.lastName, u.username, u.firstName AS firstName, " +
                                "u.lastName AS lastName, " +
                                "u.username AS username, u.password AS password, u.role AS role, " +
                                "COUNT(DISTINCT f1) AS follower, " +
                                "COUNT(DISTINCT f2) AS following, " +
                                "COUNT(DISTINCT a) AS numReports " +
                                "ORDER BY numReports DESC " +
                                "SKIP $howManySkip LIMIT $howMany",
                        parameters("howManySkip", howManySkip, "howMany", howMany));
                List<User> listOfUsers = new ArrayList<>();
                while(result.hasNext()){
                    Record r = result.next();
                    String firstName = r.get("firstName").asString();
                    String lastName = r.get("lastName").asString();
                    String picture = null;
                    String username = r.get("username").asString();
                    String password = r.get("password").asString();
                    char userType = r.get("role").asString().charAt(0);
                    User user = new User(firstName, lastName, username, password);
                    user.setRole(userType);
                    user.setFollower(r.get("follower").asInt());
                    user.setFollowing(r.get("following").asInt());
                    user.setNumReports(r.get("numReports").asInt());
                    listOfUsers.add(user);
                }
                return listOfUsers;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return users;
    }
    
    
    /**
     * Function that returns the list of the most liked players, ordered by the sum of likes received in their reports
     * @param howManySkip       How many users to skip
     * @param howMany           How many users to get
     * @return                  The list of users
	*/

public List<Report> searchMostLikedReports(final int howManySkip, final int howMany)
{  
	List<Report> reports = new ArrayList<>();
	try(Session session = driver.session())
	{
		session.readTransaction((TransactionWork<List<Report>>)  tx -> {
			Result result = tx.run("MATCH p=(u:User)-[r:LIKES]->(re:Report) "
					+ "RETURN re.codPlayer AS codPlayer, " +
					"re.codReport AS codReport,toString(apoc.temporal.format(re.creationTime, \"dd MMMM yyyy HH:mm\")) AS creationTimeFormatString, re.userID AS userID, re.rate AS rate,"
					+ "COUNT(*) as numLikes " +
					"ORDER BY numLikes DESC " +
					"SKIP $skip LIMIT $limit",
					parameters("skip", howManySkip, "limit", howMany));

			while(result.hasNext()){

				Record r = result.next();
				String review = null;
				int rate;
				int codReport = r.get("codReport").asInt();
				int codPlayer = r.get("codPlayer").asInt();
				int userID = r.get("userID").asInt();
				if(r.get("rate") != NULL)
					rate = r.get("rate").asInt();
				else
					rate = 0;
				DateFormat formatter = new SimpleDateFormat("dd MMMMMMMM yyyy HH:mm");
				Date creationTime = null;
				try {
					creationTime = formatter.parse(r.get("creationTimeFormatString").asString());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				Report report = new Report(codReport, codPlayer, rate, review, userID);
				report.setAuthorUsername(getUserUsernameByUserID(userID));
				report.setCreationTime(creationTime);
				reports.add(report);
			}
			return reports;
		});
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	return reports;
}

    
    /**
     * Function that returns the list of the most liked players, ordered by the sum of likes received in their reports
     * @param howManySkip       How many users to skip
     * @param howMany           How many users to get
     * @return                  The list of users
	*/

public List<Player> searchMostLikedPlayers(final int howManySkip, final int howMany)
    {
        List<Player> players = new ArrayList<>();
        try(Session session = driver.session())
        {
            players = session.readTransaction((TransactionWork<List<Player>>)  tx -> {
                Result result = tx.run("MATCH p=(u:User)-[r:WISHES]->(pl:Player)"
                		+ "RETURN  pl.role AS role, pl.codPlayer AS codPlayer, pl.age AS age, pl.fullName AS fullName,"
                		+ "pl.team AS team, COUNT(*) as numWishes "
                		+ "ORDER BY numWishes DESC "
                		+ "SKIP $howManySkip LIMIT $howMany",
                        parameters("howManySkip", howManySkip, "howMany", howMany));
                List<Player> listOfPlayers = new ArrayList<>();
                while(result.hasNext()){
                    Record r = result.next();
                    String fullName = r.get("fullName").asString();
                    String role = r.get("role").asString();
                    int age = r.get("age").asInt();
                    String team = r.get("team").asString();
                    int codPlayer = r.get("codPlayer").asInt();
                    Player player = new Player (fullName, role, age, team, codPlayer);
                    listOfPlayers.add(player);
                }
                return listOfPlayers;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return players;
    }

    

    /**
     * Function that returns the list of the most liked users, ordered by the sum of likes received in their reports
     * @param howManySkip       How many users to skip
     * @param howMany           How many users to get
     * @return                  The list of users
     */
    public List<User> searchMostLikedUsers (final int howManySkip, final int howMany)
    {
        List<User> users = new ArrayList<>();
        try(Session session = driver.session())
        {
            users = session.readTransaction((TransactionWork<List<User>>)  tx -> {
                Result result = tx.run("MATCH (u:User) " +
                                "OPTIONAL MATCH (u)-[:ADDS]->(:Report)<-[l:LIKES]-(:User) " +
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN u.firstName, u.lastName, u.username, u.firstName AS firstName, " +
                                "u.lastName AS lastName, u.picture AS picture, " +
                                "u.username AS username, u.password AS password, u.role AS role, " +
                                "COUNT(DISTINCT f1) AS follower, " +
                                "COUNT(DISTINCT f2) AS following, " +
                                "COUNT(DISTINCT a) AS numReports, " +
                                "COUNT(DISTINCT l) AS totLikes " +
                                "ORDER BY totLikes DESC " +
                                "SKIP $howManySkip LIMIT $howMany",
                        parameters("howManySkip", howManySkip, "howMany", howMany));
                List<User> listOfUsers = new ArrayList<>();
                while(result.hasNext()){
                    Record r = result.next();
                    String firstName = r.get("firstName").asString();
                    String lastName = r.get("lastName").asString();
                    String picture = null;
                    if (r.get("picture") != NULL)
                    {
                        picture = r.get("picture").asString();
                    }
                    String username = r.get("username").asString();
                    String password = r.get("password").asString();
                    int role = r.get("role").asInt();
                    User user = new User(firstName, lastName, username, password);
                    user.setFollower(r.get("follower").asInt());
                    user.setFollowing(r.get("following").asInt());
                    user.setNumReports(r.get("numReports").asInt());
                    listOfUsers.add(user);
                }
                return listOfUsers;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Find the users of the application
     * @param howManyToSkip
     * @param howManyToGet
     * @return  The List of the users
     */
    public List<User> searchAllUsers(int howManyToSkip, int howManyToGet)
    {
        List<User> users = new ArrayList<>();

        try(Session session = driver.session())
        {
            users = session.readTransaction((TransactionWork<List<User>>)  tx -> {
                Result r = tx.run("MATCH (u:User) " +
                        "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                        "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                        "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                        "RETURN u.firstName, u.lastName, u.username, u.role AS role," +
                        " COUNT(DISTINCT f1) AS follower, COUNT(DISTINCT f2) AS following, COUNT(DISTINCT a) AS added " +
                        "SKIP $howManyToSkip LIMIT $howManyToGet",
                        parameters("howManyToSkip", howManyToSkip, "howManyToGet", howManyToGet));
                List<User> listOfUsers = new ArrayList<>();
                while(r.hasNext())
                {
                    Record rec = r.next();
                    listOfUsers.add(new User(
                            rec.get(0).asString(), rec.get(1).asString(), rec.get(2).asString(),
                            rec.get("follower").asInt(), rec.get("following").asInt(),rec.get("added").asInt(),rec.get("role").asString().charAt(0))
                    );
                }
                return listOfUsers;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return users;
    }


    /**
     * Function that returns all the info about an User, given the username
     * @param username      Username of the user
     * @return              User instance
     */
    public User getUserByUsername (String username)
    {
        User user = null;
        try (Session session = driver.session()) {
           user = session.readTransaction((TransactionWork<User>) tx -> {
                Result result = tx.run("MATCH (u:User) " +
                                "WHERE u.username = $username " +
                                "OPTIONAL MATCH (u)<-[f1:FOLLOWS]-(:User) " +
                                "OPTIONAL MATCH (u)-[f2:FOLLOWS]->(:User) " +
                                "OPTIONAL MATCH (u)-[a:ADDS]->(:Report) " +
                                "RETURN u.firstName AS firstName, u.lastName AS lastName, u.picture AS picture, " +
                                "u.username AS username, u.password AS password, u.role AS role, u.userID AS userID, u.email AS email, " +
                                "COUNT(DISTINCT f1) AS follower, COUNT (DISTINCT f2) AS following, COUNT (DISTINCT a) AS numReports ",
                        parameters("username", username));

                User u = null;
                if(result.hasNext()){
                    Record r = result.next();
                    String firstName = r.get("firstName").asString();
                    String lastName = r.get("lastName").asString();
                    String email = r.get("email").asString();
                    String picture = null;
                    if (r.get("picture") != NULL)
                    {
                        picture = r.get("picture").asString();
                    }
                    String password = r.get("password").asString();
                    char role = r.get("role").asString().charAt(0);
                    //u = new User(firstName, lastName, picture, username, password, role);
                    u = new User(firstName, lastName, username, password, role, email);
                    u.setFollower(r.get("follower").asInt());
                    u.setFollowing(r.get("following").asInt());
                    u.setNumReports(r.get("numReports").asInt());
                    u.setUserId(r.get("userID").asInt());
                }
                return u;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return user;
    }
    
    public int getPlayerNumWishes(Player player)
    {
        try(Session session = driver.session())
        {
            int numWishes = session.readTransaction((TransactionWork<Integer>)  tx -> {
                Result result = tx.run("MATCH p=(u:User)-[r:WISHES]->(pl:Player WHERE pl.codPlayer = $codPlayer)"
                		+ "RETURN COUNT(*) as numWishes",
                        parameters("codPlayer", player.getCodPlayer()));
                

                int num = 0;
                if(result.hasNext()){
                    Record r = result.next();
                    num = r.get("numWishes").asInt();
                }
                return num;
            });
            return numWishes;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }
}
