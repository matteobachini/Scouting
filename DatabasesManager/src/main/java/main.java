import com.google.gson.Gson;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import org.bson.Document;
import org.neo4j.driver.*;

import static org.neo4j.driver.Values.parameters;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class main {
     
	public static String PATH_FULL_FORMAT_REPORTS = "Dataset/Reports.json";
    public static String PATH_FULL_FORMAT_PLAYERS = "Dataset/Players.json";
    public static String PATH_FULL_FORMAT_USERS = "Dataset/Users.json";
	
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection collection;
    private static MongoCollection playersCollection;
    private static MongoCollection usersCollection;
    private static Driver driver;

    public static void main (String[] arg)
    {
    	System.out.print("start");
        ConnectionString connectionString =  new ConnectionString("mongodb://172.16.5.35:27017, " +
                "172.16.5.36:27017, 172.16.5.37:27017");
        
    	
        //ConnectionString connectionString =  new ConnectionString("mongodb://localhost:27017");
        
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .retryWrites(true)
                .writeConcern(WriteConcern.W3).build();
        
        /*
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .retryWrites(true)
                .writeConcern(WriteConcern.UNACKNOWLEDGED).build();
                */
        
        mongoClient = MongoClients.create(mongoClientSettings);

        database = mongoClient.getDatabase("scouting");
        database.drop();
        database = mongoClient.getDatabase("scouting");
        
        collection = database.getCollection("reports");
        
        playersCollection = database.getCollection("players");
        
        usersCollection= database.getCollection("users");

        
        driver = GraphDatabase.driver( "bolt://localhost:7687",
                AuthTokens.basic( "neo4j", "root" ) );
                
        
        /*
        driver = GraphDatabase.driver("neo4j://172.16.5.37:7687",
                AuthTokens.basic("neo4j", "largescale"));
        */

        System.out.print("\n drop DB");
        collection.dropIndexes();

        // First of all It is useful to remove the old values (if they exists)
        collection.drop();
        deleteAllGraph();
        
        
        System.out.print("\n create constraints and indexes");
        // Create the constraint (as index) on the codReport
        IndexOptions indexOptions = new IndexOptions().unique(true).name("codReport_constraint");
        collection.createIndex(Indexes.ascending("codReport"), indexOptions);
        
        // Create the constraint (as index) on the codPlayer
        IndexOptions indexOptions2 = new IndexOptions().unique(true).name("codPlayer_constraint");
        playersCollection.createIndex(Indexes.ascending("codPlayer"), indexOptions2);
        
        // Create the constraint (as index) on the username
        IndexOptions indexOptions3 = new IndexOptions().unique(true).name("username_constraint");
        usersCollection.createIndex(Indexes.ascending("username"), indexOptions3);
        
        // Create the constraint on the username of the User
        createUsernameConstraintNeo4j();       //Node Key constraint requires Neo4j Enterprise Edition
        createCodReportConstraintNeo4j();    //Node Key constraint requires Neo4j Enterprise Edition
        createCodPlayerConstraintNeo4j();    //Node Key constraint requires Neo4j Enterprise Edition
        
        //Create the indexes
        collection.createIndex(Indexes.descending("creationTime"),
                new IndexOptions().name("creationTime_index"));
        collection.createIndex(Indexes.descending("comments.creationTime"),
                new IndexOptions().name("commentCreationTime_index").sparse(true));
        playersCollection.createIndex(Indexes.descending("fullName"),
                new IndexOptions().name("fullName_index").sparse(true));
        
        createPlayerFullnameIndexNeo4j();    
        createReportCreationTimeIndexNeo4j();
        
        System.out.print("\n add reports, players and users");
        List<ReportRaw> rawReports = new ArrayList<>();
        addReports_full_format(rawReports, PATH_FULL_FORMAT_REPORTS);
        
        List<Player> players = new ArrayList<>();
        addPlayers_full_format(players, PATH_FULL_FORMAT_PLAYERS);

        List<User> users = new ArrayList<>();
        addUsers_full_format(users, PATH_FULL_FORMAT_USERS);
        
        List<Document> playerDocuments = new ArrayList<Document>();
        for (Player player: players) 
        {
            int codPlayer = player.getCodPlayer();
            Document playerDoc = new Document("codPlayer", codPlayer);
            playerDoc.append("codPlayer", player.getCodPlayer());
            playerDoc.append("fullName", player.getFullName());
            playerDoc.append("team", player.getTeam());
            playerDoc.append("role", player.getRole());
            playerDoc.append("age", player.getAge());
            playerDoc.append("foot", player.getFoot());
            playerDoc.append("rate", player.getRate());
            playerDoc.append("photo", player.getPhoto());
            playerDocuments.add(playerDoc);
        }
        
        List<Document> userDocuments = new ArrayList<Document>();
        for (User user: users) 
        {
            int userID = user.getUserID();
            Document userDoc = new Document("userID", userID);
            userDoc.append("username", user.getUsername());
            userDoc.append("password", user.getPassword());
            userDoc.append("role", user.getRole());
            userDoc.append("email", user.getEmail());
            userDoc.append("firstName", user.getFirstName());
            userDoc.append("lastName", user.getLastName());
            userDocuments.add(userDoc);
        }
        
        //admin user
        users.add(new User(users.size()+1, "admin", "admin", "A", "admin@admin.com", "admin", "admin"));
        
        addUsers(users);
        
        addPlayers(players);
        
        playersCollection.insertMany(playerDocuments);
        
        usersCollection.insertMany(userDocuments);
        Document adminDocument = new Document("userID", users.size()+1);
        adminDocument.append("username", "admin");
        adminDocument.append("password", "admin");
        adminDocument.append("role", "A");
        adminDocument.append("email", "admin@admin.com");
        adminDocument.append("firstName", "admin");
        adminDocument.append("lastName", "admin");
        usersCollection.insertOne(adminDocument);

        System.out.print("\n insertReportsOfUsersPlayers");
        insertReportsOfUsersPlayers(rawReports, users, players);
        
        System.out.println("\n Documents loaded: " + collection.countDocuments()); //How many documents loaded
        mongoClient.close();
        driver.close();
    }

    /**
     * Add reports from full_format dataset
     * @param reports
     * @param path
     */
    public static void addReports_full_format (List<ReportRaw> reports, String path)
    {
        Gson gson = new Gson();
        ReportRaw[] reportList = null;
        StringBuilder contentBuilder = new StringBuilder();
        try {
            Files.lines(Paths.get(path)).forEach(s -> contentBuilder.append(s));
            reportList = gson.fromJson(contentBuilder.toString(), ReportRaw[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.addAll(reports, reportList);
    }

    public static void addPlayers_full_format (List<Player> players, String path)
    {
        Gson gson = new Gson();
        Player[] playerList = null;
        StringBuilder contentBuilder = new StringBuilder();
        try {
            Files.lines(Paths.get(path)).forEach(s -> contentBuilder.append(s));
            playerList = gson.fromJson(contentBuilder.toString(), Player[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.addAll(players, playerList);
    }
    
    public static void addUsers_full_format (List<User> Users, String path)
    {
        Gson gson = new Gson();
        User[] UserList = null;
        StringBuilder contentBuilder = new StringBuilder();
        try {
            Files.lines(Paths.get(path)).forEach(s -> contentBuilder.append(s));
            UserList = gson.fromJson(contentBuilder.toString(), User[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.addAll(Users, UserList);
    }
    
    
    public static void addRelation (List<User> observers, List<User> footballDirectors, List<Player> players, List<ReportRaw> reportRaws)
    {
    	System.out.print("\n addRelation");
    	Random random = new Random();
    	try ( Session session = driver.session())
		{ 
    	List<Map<String,Object>> list = new ArrayList<>();
    	for(int i=0; i<100;i++)
    	{	
    		List<Integer> o= new ArrayList();
    		List<Integer> p= new ArrayList();
    		List<Integer> r= new ArrayList();
    		final int innerI = i;

        		int relationIndex = random.nextInt(1,10);
        		
        		for(int j=0; j<relationIndex;j++)
        		{
        			//System.out.print("\n"+j);
        			int observerIndex = random.nextInt(0,observers.size()-1);
        			
        			if(!o.contains(observerIndex))
        			{	
        				Map<String,Object> props = new HashMap<>();
        	            props.put("follower", footballDirectors.get(innerI).getUserID());
        	            props.put("following", observers.get(observerIndex).getUserID());
        	            list.add(props);
        				o.add(observerIndex);
        				
        				session.writeTransaction((TransactionWork<Void>) tx -> {
        					tx.run("match (a:User) where a.userID=$following " +
                            "match (b:User) where b.userID=$follower " +
                            "merge (b)-[:FOLLOWS]->(a)",parameters("follower",footballDirectors.get(innerI).getUserID(),"following",observers.get(observerIndex).getUserID()));
        					return null;
        					});
        				
        			}
        			else
        			{
        				j--;
        				continue;
        			}
        		}
        		
        		for(int j=0; j<relationIndex;j++)
        		{
        			int playerIndex = random.nextInt(1,players.size());
        			if(!p.contains(playerIndex))
        			{	
        				p.add(playerIndex);
        				session.writeTransaction((TransactionWork<Void>) tx -> {
        					tx.run("match (a:Player) where a.codPlayer=$codPlayer " +
                                    "match (b:User) where b.userID=$userID " +
                                    "merge (b)-[:WISHES]->(a)",parameters("codPlayer",playerIndex,"userID",footballDirectors.get(innerI).getUserID()));
                            return null;
        					});
        			}
        			else
        			{
        				j--;
        				continue;
        			}
        		
        		}
        		for(int j=0; j<relationIndex;j++)
        		{
        			int reportIndex = random.nextInt(1,reportRaws.size());
        			if(!r.contains(reportIndex))
        			{	
        				r.add(reportIndex);
        				session.writeTransaction((TransactionWork<Void>) tx -> {
        					tx.run("match (a:User) where a.userID=$u " +
                                    "match (b:Report) where b.codReport=$t " +
                                    "merge (a)-[:LIKES]->(b)",parameters("u",footballDirectors.get(innerI).getUserID(),"t",reportIndex));
                            return null;
        					});
        			}
        			else
        			{
        				j--;
        				continue;
        			}
        		}
        		
       		} 
		}
    		catch (Exception ex)
    		{
    			ex.printStackTrace();
    		    System.err.println("Error in adding relations on Neo4J");
    		}
        	       
    }
    
    /**
     * Function that insert all the reports, every one is associated at one user randomly picked
     * @param reportRaws    The list of the reports
     * @param users         The list of the users
     */
    public static String generateReviewForDefaultReport (String role)
    { // ^ Role * Surname £ TeamName
    	String[] review = new String[10];
    	review[0]="One of the biggest compliments I can pay to * is: he always wants the football, and really does take care of it once he’s got it. Never seems panicked when it’s at his feet. Clever drops of the shoulder to deceive an opponent allow him seemingly endless time on the ball. His passing may not be overly-extravagant, but he just keeps everything ticking over nicely. This isn’t to say that he’s averse to a line-breaking pass or a raking, cross field switch of play. He most certainly has both in his locker. *’s capacity to recycle the ball and sustain pressure makes his game perfectly-suited to a possession-heavy team. The willingness he displays to swiftly release the ball – in just one or two touches – ensures there’s seldom a stagnation in tempo.";
    	review[1]="* is at best on role of ^, but he is also capable to play on both wings. He likes to move into wide areas in possession to create space for himself and to provide width for his team. * has great vision and game intelligence, always knowing where his teammates are and also finding his teammates in space. His excellent passing technique and extensive passing range allows him to play over the top through passes from deep but also to find his teammates in space in the final third. * is also capable of finding space in behind himself by making good runs in behind. He likes to make runs into the channels in between the central defender and full-back. When put under pressure, * understands the game well as he often switches play to the full-back who is in acres of space.";
    	review[2]="* usually plays on ^ of £. He sits very narrowly allowing space for wing-backs to overlap. As I noted in my original match report, he makes a lot of good runs off the ball. A lot more of these runs have been rewarded than in the match I saw, resulting in a steady stream of goals from within 10 yards of the goal. He has also shown a strong ability to play incisive through balls, racking up assists by playing in teammates. Now, no doubt being on one of the best teams helps him significantly. Strong playmaking by his teammates and their good off-the-ball movement allows him to be as good as he can be. But even taking that into account, his offensive production has been outstanding. He’s also quite an active and effective presser of the ball.";
    	review[3]="Not many players are as good at dribbling as *, but I think his lack of intelligence and egoism in the final third is a major concern for the future. I think that * should have been coached as an advanced #8 in £’s 4-3-3. He would have been more reliable in the defensive aspect. However, as this did not happen, he is now decent in several positions without excelling in any of them. In my opinion, the links to elite clubs such as Liverpool and Real Madrid are very strange because * is far from ready to play for clubs like that and I doubt he will ever be good enough for such clubs. He is a very enjoyable player to watch as a neutral fan, but his limitations will hold him back to become one of the world’s elite.";
    	review[4]="* has decent technical ability, even though it sometimes looks slightly unconventional. Under pressure, * does well to keep the ball close to him despite not always having full control over the ball. However, he uses his body very smartly to protect the ball giving him extra time on the ball. His receiving skills were mostly good, even though he rarely miscontrolled a pass. The ^ has the confidence to play long passes, playing some decent long passes also showing an okay passing technique. * has the potential to become better in passing, especially in the link-up play and hold-up play. He just does not get found that often, at least in this match";
    	review[5]="* is the quintessential modern-day goalkeeper. His distribution is outstanding, and he often helps his team start attacks. Against teams that press high up the pitch, he’s capable of playing good passes out to the fullbacks to beat the press of the opponent. So far this season, * has completed 74% of his passes while the others GK has only completed 64% of his passes in the League. * has also completed 40% of his long-range passes which is also better than the others, who’s completed 34% of their long-range passes.";
    	review[6]="On defense, * really proved to be a force, rarely making a mistake. He showed great awareness and positioning. His overall knowledge of the flow of the game showed in the way he reacted and moved off ball. * was timing his actions very well and picked up an interception that way. Afterward, he hit an effective right-footed clearance deep, something he did many times even with his left once. He did mishit a clearance, leading to some trouble in the box, but that was a rare mistake on the night. * was not just an intelligent presence at the back, he utilized his frame and physicality, too. * imposed himself with his frame in 1v1s and combined that with his good positioning and timing to great success.";
    	review[7]="Technically he looked solid compared to the level of the rest of the players. He was a bit quiet at the start of the game, but really showed his abilities after a while. His first touches were very good, at times he only needed one touch to beat his opponent. He also had a good day in his offensive 1v1s. After around 20 minutes it seemed like he moved to the left and here he was able to cut inside to his favoured right foot. One moment in the 24th minute stood out, where he beat one opponent in the box by cutting outside towards his left foot, with some lovely ball control. After this he had the opportunity to put in a cross or play it back to the edge of the box but instead, he cut back to his right foot and saw his shot get blocked. He showed some real quality here with his great dribbling, but also some areas where he really has to improve.";
    	review[8]="* played a great game and I definitely see him make a step up in the future. He was great defensively but seemed to have a more difficult time if he had to play in a high line. In possession, * did pretty well. He was barely pressed by his opponents but stayed calm when someone tried to take the ball off him. His passing was nearly flawless this match, but he didn’t really try anything too difficult. He played a couple of long passes which were fine and the rest of the passes were often quite safe.";
    	review[9]="* is only taking his first few steps in £ first team, with this being only his second game with the senior squad. He has been making quite the impression after having scored in both of those games. In this match, I was stunned by the amazing goal he scored, and he was awarded the Man of the match trophy. * has been making very good use of the opportunities he has been given, and I would not be surprised to see him as a regular presence in £ squad for the rest of the season. However, he still has some room for improvement, especially regarding his on-the-ball ability.";
    	Random random = new Random();
    	if(role.equals("GK"))
        	return review[5];
    	int reviewIndex = random.nextInt(0,9); //[0,9]
    	return review[reviewIndex];
    }
    
    /**
     * Function that insert all the reports, every one is associated at one user randomly picked
     * @param reportRaws    The list of the reports
     * @param users         The list of the users
     */
    public static void insertReportsOfUsersPlayers (List<ReportRaw> reportRaws, List<User> users, List<Player> players)
    {
    	List<User> observers = new ArrayList<User>();
    	List<User> footballDirectors = new ArrayList<User>();
    	for (User user: users) {
    		if(user.getRole().equals("O"))
    			observers.add(user);
    		if(user.getRole().equals("F"))
    			footballDirectors.add(user);
    	}
        List<Document> documents = new ArrayList<Document>();
        long aDay = TimeUnit.DAYS.toMillis(1);
    	long now = new Date().getTime();
    	Date fiveYearsAgo = new Date(now - aDay * 365 * 5);
    	Date oneDayAgo = new Date(now - aDay * 1);
        Map<String,Object> params = new HashMap<>();
        List<Map<String,Object>> list = new ArrayList<>();
        Random random = new Random();
        int i= 0;
        for (ReportRaw rawReport: reportRaws) // For every report
        {	   
        	Date randomDate = between(fiveYearsAgo, oneDayAgo);
            // pick-up randomly a user to associate with this report
            int userIndex = random.nextInt(observers.size()); //[0,19]
            User user = observers.get(userIndex);
            
            // pick-up randomly a player to associate with this report
            int playerIndex = random.nextInt(players.size()); //[0,19]
            Player player = players.get(playerIndex);
            String review = generateReviewForDefaultReport(player.getRole());
            review = review.replace("*", player.getFullName());
            review = review.replace("^", player.getRole());
            review = review.replace("£", player.getTeam());

            List<Document> comments = new ArrayList<Document>();
            //7 comments for each report
            for(int j=0; j<7; j++) {
            	Document comment = new Document();
            	int footballDirectorIndex = random.nextInt(footballDirectors.size());
            	User footballDirector = footballDirectors.get(footballDirectorIndex);
            	comment.append("authorUsername", footballDirector.getUsername());
            	comment.append("creationTime", new Date());
            	comment.append("text", "comment"+Integer.toString(j));
            	comments.add(comment);
            }
            
            // MongoDB part
            int codReport = rawReport.getCodReport();
            Document doc = new Document("codReport", codReport);

            if (rawReport.getReview() != null)
                doc.append("review", review /*rawReport.getReview()*/);
            
            // For the timestamp MongoDB use the "Date"
            doc.append("creationTime", randomDate);
            doc.append("userID", user.getUserID());
            doc.append("rate", rawReport.getRate());
            doc.append("codPlayer", player.getCodPlayer());
            doc.append("comments", comments);
            documents.add(doc);

            // Neo4j part
            Map<String,Object> props = new HashMap<>();
            props.put( "username", user.getUsername());
            props.put("timestamp", randomDate.getTime());
            props.put("codReport", rawReport.getCodReport());
            props.put("rate", rawReport.getRate());
            props.put("codPlayer", player.getCodPlayer());
            props.put("userID", user.getUserID());

            list.add(props);
            i++;
            
        }

        System.out.print("\ncollection.insertMany(documents)");
        // Mongo insert
        collection.insertMany(documents);

        System.out.print("\nNeo4j insert");
        // Neo4j insert
        params.put( "batch", list );
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "UNWIND $batch AS row " +
                        "MATCH (u:User {userID: row.userID}) " +
                        "CREATE (u)-[:ADDS {when: row.timestamp}]->(r:Report "
                        + "{rate: row.rate, userID: row.userID," +
                        "codPlayer: row.codPlayer, "
                        + "codReport: row.codReport, creationTime: datetime({epochmillis: row.timestamp})})",
                params);
                return null;
            });
        }
        
        System.out.print("\n ADDS relations inserted");
        try ( Session session = driver.session()){
        	session.writeTransaction((TransactionWork<Void>) tx -> {
        		tx.run( "UNWIND $batch AS row " +
        				"MATCH (p:Player {codPlayer: row.codPlayer}) " +
        				"MATCH (r:Report {codReport: row.codReport}) " +
        				"CREATE (p)-[:HAVE {when: datetime({epochmillis: row.timestamp})}]->(r)",
        				params);
        		return null;
        	});
        }
        addRelation(observers, footballDirectors, players, reportRaws);
    }

    /**
     * Function used to delete all the nodes and the edges of the graph
     */
    public static void deleteAllGraph ()
    {
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "MATCH (n) DETACH DELETE n");
                return null;
            });
        }
    }

    /**
     * This function creates the constraint on the username (that must be unique and must always exist)
     */
    private static void createUsernameConstraintNeo4j() {
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "CREATE CONSTRAINT username_constraint IF NOT EXISTS FOR (u: User) REQUIRE (u.username) IS NODE KEY");
                return null;
            });
        }
    }
    
    
    public static Date between(Date startInclusive, Date endExclusive) {
        long startMillis = startInclusive.getTime();
        long endMillis = endExclusive.getTime();
        long randomMillisSinceEpoch = ThreadLocalRandom
          .current()
          .nextLong(startMillis, endMillis);

        return new Date(randomMillisSinceEpoch);
    }
    
    private static void createCodReportConstraintNeo4j() {
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "CREATE CONSTRAINT codReport_constraint IF NOT EXISTS FOR (r: Report) REQUIRE (r.codReport) IS NODE KEY");
                return null;
            });
        }
    }
    
    private static void createCodPlayerConstraintNeo4j() {
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "CREATE CONSTRAINT codPlayer_constraint IF NOT EXISTS FOR (p: Player) REQUIRE (p.codPlayer) IS NODE KEY");
                return null;
            });
        }
    }
    
    private static void createPlayerFullnameIndexNeo4j() {
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "CREATE INDEX fullName_index IF NOT EXISTS FOR (p: Player) ON (p.fullName)");
                return null;
            });
        }
    }
    
    private static void createReportCreationTimeIndexNeo4j() {
        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "CREATE INDEX creationTime_Index IF NOT EXISTS FOR (r: Report) ON (r.creationTime)");
                return null;
            });
        }
    }

    public static void addUsers( final List<User> users)
    {
        Map<String,Object> params = new HashMap<>();
        List<Map<String,Object>> list = new ArrayList<>();
        for (User user: users)
        {
            Map<String,Object> props = new HashMap<>();
            props.put( "userID", user.getUserID());
            props.put( "username", user.getUsername());
   //         props.put( "password", user.getPassword());
            props.put("role", user.getRole());
   //         props.put("email", user.getEmail());
            props.put("firstName", user.getFirstName());
            props.put("lastName", user.getLastName());
            list.add(props);
        }
        params.put( "batch", list );

        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "UNWIND $batch AS row " +
                                "MERGE (u:User {" +
                                "userID: row.userID, username: row.username," +
                               /* "password: row.password, */" role: row.role," /* email: row.email,*/
                                + "firstName: row.firstName, lastName: row.lastName})",
                        params);
                return null;
            });
        }
    }
    
    public static void addPlayers( final List<Player> players)
    {
        Map<String,Object> params = new HashMap<>();
        List<Map<String,Object>> list = new ArrayList<>();
        for (Player player: players)
        {
            Map<String,Object> props = new HashMap<>();
            props.put( "codPlayer", player.getCodPlayer());
            props.put( "fullName", player.getFullName());
            props.put( "team", player.getTeam());
            props.put( "role", player.getRole());
            props.put( "age", player.getAge());
            list.add(props);
        }
        params.put( "batch", list );

        try ( Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "UNWIND $batch AS row " +
                                "MERGE (p:Player {" +
                                "codPlayer: row.codPlayer, fullName: row.fullName, "
                                + "role: row.role, age:row.age, team: row.team})",
                        params);
                return null;
            });
        }
    }
}
