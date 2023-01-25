package it.unipi.dii.inginf.lsdb.scouting.persistence;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import it.unipi.dii.inginf.lsdb.scouting.config.ConfigurationParameters;
import it.unipi.dii.inginf.lsdb.scouting.model.*;
import it.unipi.dii.inginf.lsdb.scouting.utils.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import static com.mongodb.client.model.Accumulators.sum;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Sorts.descending;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * This class is used to communicate with MongoDB
 */
public class MongoDBDriver implements DatabaseDriver{
    private static MongoDBDriver instance;

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection collection;
    private CodecRegistry pojoCodecRegistry;
    private String firstIp;
    private int firstPort;
    private String secondIp;
    private int secondPort;
    private String thirdIp;
    private int thirdPort;
    private String username;
    private String password;
    private String dbName;

    public static MongoDBDriver getInstance() {
        if (instance == null)
        {
            instance = new MongoDBDriver(Utils.readConfigurationParameters());
        }
        return instance;
    }

    /**
     * Consumer function that prints the document in json format
     */
    private Consumer<Document> printDocuments = doc -> {
        System.out.println(doc.toJson());
    };

    private MongoDBDriver (ConfigurationParameters configurationParameters)
    {
        this.firstIp = configurationParameters.getMongoFirstIp();
        this.firstPort = configurationParameters.getMongoFirstPort();
        this.secondIp = configurationParameters.getMongoSecondIp();
        this.secondPort = configurationParameters.getMongoSecondPort();
        this.thirdIp = configurationParameters.getMongoThirdIp();
        this.thirdPort = configurationParameters.getMongoThirdPort();
        this.username = configurationParameters.getMongoUsername();
        this.password = configurationParameters.getMongoPassword();
        this.dbName = configurationParameters.getMongoDbName();
    }

    /**
     * Method that inits the MongoClient and choose the correct database
     */
    @Override
    public boolean initConnection() {
        try
        {
            String string = "mongodb://";
            if (!username.equals("")) // if there are access rules
            {
                string += username + ":" + password + "@";
            }
            string += firstIp + ":" + firstPort + ", " + secondIp + ":" + secondPort + ", " + thirdIp + ":" + thirdPort;

            ConnectionString connectionString = new ConnectionString(string);
            MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .readPreference(ReadPreference.secondaryPreferred())
                    .retryWrites(true)
                    .writeConcern(WriteConcern.W3)
                    .build();
            
            //ConnectionString connectionString =  new ConnectionString("mongodb://localhost:27017");
           
          
//            MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
//                  .applyConnectionString(connectionString)
//                  .retryWrites(true)
//                  .writeConcern(WriteConcern.UNACKNOWLEDGED).build();
          
            mongoClient = MongoClients.create(mongoClientSettings);

            database = mongoClient.getDatabase(dbName);

            DBObject ping = new BasicDBObject("ping","1");

            // In order to check the connectivity
            database.runCommand((Bson) ping);

            pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));

            chooseCollection("reports");
        }
        catch (Exception ex)
        {
            System.out.println("MongoDB is not available");
            return false;
        }
        return true;
    }

    /**
     * Method used to close the connection
     */
    @Override
    public void closeConnection() {
        if (mongoClient != null)
            mongoClient.close();
    }

    /**
     * Add a new report in MongoDB
     * @param r The object Report which contains all the necessary information about it
     * @return  true if operation is successfully executed, false otherwise
     */
    public boolean addReport(Report r)
    {
        try {  
        	Date date = new Date();
        	long aDay = TimeUnit.DAYS.toMillis(1);
        	long now = new Date().getTime();
        	Date hundredYearsAgo = new Date(now - aDay * 365 * 5);
        	Date tenDaysAgo = new Date(now - aDay * 1);
        	Date random = Utils.between(hundredYearsAgo, tenDaysAgo);
            Document doc = new Document("codReport", r.getCodReport())
                    .append("codPlayer", r.getCodPlayer())
                    .append("userID", r.getUserID())
                    .append("rate", r.getRate())
                    .append("review", r.getReview())
                    .append("creationTime", random);

            collection.insertOne(doc);
            return true;
        }
        catch (Exception ex)
        {
            System.err.println("Error in adding a new report");
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Add a new user in MongoDB
     * @param r The object Report which contains all the necessary information about it
     * @return  true if operation is successfully executed, false otherwise
     */
    public boolean addUser(String firstName,  String lastName,  String username,
             String password,  char role,  String email)
    {
        try { 
        	chooseCollection("users"); 
            Gson gson = new Gson();
        	Arrays.asList(new Document("$count", "userID"));
        	Document doc = (Document) collection.aggregate(Arrays.asList(new Document("$count", "userID"))).first();
        	int max= doc.getInteger("userID", 0);
        	//(List<Document>) doc = (List<Document>) collection.aggregate(Arrays.asList(new Document("$count", "userID")).into(new ArrayList<>()).get(1));
        	//String max = gson.fromJson(gson.toJson(doc), String.class);
        	System.out.print(max);
            Document doc1 = new Document("userID",max)
                    .append("firstName", firstName)
                    .append("lastName", lastName)
                    .append("username", username)
                    .append("password", password)
                    .append("email", email)                    
                    .append("role", role);
            collection.insertOne(doc1);
            chooseCollection("reports"); 
            return true;
        }
        catch (Exception ex)
        {
            System.err.println("Error in adding a new user");
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Edit an already present report
     * @param r the new report to replace the old one
     * @return  true if operation is successfully executed, false otherwise
     */
    public boolean editReport(Report r){
        try {
            Document doc = new Document("codReport", r.getCodReport())
                    .append("rate", r.getRate())
                    .append("review", r.getReview());


            Bson updateOperation = new Document("$set", doc);

            collection.updateOne(new Document("codReport", r.getCodReport()), updateOperation);
            return true;
        }
        catch (Exception ex)
        {
            System.err.println("Error in updating report on MongoDB");
            return false;
        }
    }
    
    /**
     * Edit an already present report
     * @param r the new report to replace the old one
     * @return  true if operation is successfully executed, false otherwise
     */
    public boolean editUser(User u){
        try {
        	chooseCollection("users"); 
            Document doc = new Document("userID", u.getUserId())
            		.append("firstName", u.getFirstName())
                    .append("lastName", u.getLastName())
            		.append("password", u.getPassword());
            Bson updateOperation = new Document("$set", doc);

            collection.updateOne(new Document("userID", u.getUserId()), updateOperation);
            chooseCollection("reports"); 
            return true;
        }
        catch (Exception ex)
        {
            System.err.println("Error in updating user on MongoDB");
            return false;
        }
    }
    
    

    /**
     * Function that deletes the report from the database
     * @param report    Report to delete
     * @return true if operation is successfully executed, false otherwise
     */
    public boolean deleteReport (Report report)
    {
        try {
            collection.deleteOne(eq("codReport", report.getCodReport()));
            return true;
        }
        catch (Exception ex)
        {
            System.err.println("Error in delete report");
            return false;
        }
    }

    /**
     * Function that deletes all the report of on user
     * @param username  Username of the user
     * @return true if operation is successfully executed, false otherwise
     */
    public boolean deleteAllReportsOfUser (int userID)
    {
        try{
            collection.deleteMany(eq("userID", userID));
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    /**
     * Method used to change the collection
     * @param name  name of the new collection
     */
    public void chooseCollection(String name)
    {
        collection = database.getCollection(name);
    }

    public List<Report> getReportsFromAuthorUsername(int howManySkip, int howMany, String username){
        List<Report> reports = new ArrayList<>();
        Gson gson = new Gson();
        List<Document> results = new ArrayList<>();
        Bson sort = sort(descending("creationTime"));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        Bson match = match(eq("authorUsername", username));
        results = (List<Document>) collection.aggregate(Arrays.asList(match, sort, skip, limit))
                .into(new ArrayList<>());
        Type reportListType = new TypeToken<ArrayList<Report>>(){}.getType();
        reports = gson.fromJson(gson.toJson(results), reportListType);
        return reports;
    }

    /**
     * Function that return the report given the title
     * @param title     Title of the report
     * @return          The report or null if there is no report with the given title or an error occurs
     */
    public Report getReportFromCodReport(int codReport){
        try {
            Report report = null;
            Gson gson = new Gson();
            Document myDoc = (Document) collection.find(eq("codReport", codReport)).first();
            report = gson.fromJson(gson.toJson(myDoc), Report.class);
            return report;
        }
        catch (Exception ex)
        {
            return null;
        }
    }
    
    public List<Player> getCodPlayerFromFullName (String fullName, int howManySkip, int howMany){
        try {
        	chooseCollection("players");
            List<Player> players = new ArrayList<>();
            Gson gson = new Gson();
            Pattern pattern = Pattern.compile("^.*" + fullName + ".*$", Pattern.CASE_INSENSITIVE);
            Bson match = Aggregates.match(Filters.regex("fullName", pattern));
            Bson skip = skip(howManySkip);
            Bson limit = limit(howMany);
            List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                    .into(new ArrayList<>());
            Type playerListType = new TypeToken<ArrayList<Player>>(){}.getType();
            players = gson.fromJson(gson.toJson(results), playerListType);
            chooseCollection("reports");
            return players;
        }

        catch (Exception ex)
        {
        	ex.printStackTrace();
            return null;
        }
    }
    
    public Player getCodPlayerFromFullName2(String fullName, int howManySkip, int howMany){
        try {
        	chooseCollection("players");
            Player player = null;
            Gson gson = new Gson();
            Bson match = Aggregates.match(Filters.eq("fullName", fullName));
            Bson sort = sort(descending("creationTime"));
            Bson skip = skip(howManySkip);
            Bson limit = limit(howMany);
            Document doc = (Document) collection.find(eq("fullName", fullName)).first();
            player = gson.fromJson(gson.toJson(doc), Player.class);
            if(player == null)
            	System.out.print("errore nella getCodPlayerFromFullName2");
            chooseCollection("reports");
            return player;
        }
        catch (Exception ex)
        {
        	ex.printStackTrace();
            return null;
        }
    }
    
    public Player getCodPlayerFromRole(String role, int howManySkip, int howMany){
        try {
        	chooseCollection("players");
            Player player = null;
            Gson gson = new Gson();
            Pattern pattern = Pattern.compile("^.*" + role + ".*$", Pattern.CASE_INSENSITIVE);
            Bson match = Aggregates.match(Filters.regex("role", pattern));
            Bson sort = sort(descending("creationTime"));
            Bson skip = skip(howManySkip);
            Bson limit = limit(howMany);
            Document myDoc = (Document) collection.aggregate(Arrays.asList(match, sort, skip, limit)).first();
            player = gson.fromJson(gson.toJson(myDoc), Player.class);
            if(player == null)
            	System.out.print("errore nella getCodPlayerFromRole");
            chooseCollection("reports");
            return player;
        }
        catch (Exception ex)
        {
        	ex.printStackTrace();
            return null;
        }
    }


    /**
     * Function that returns "howMany" reports that contains in their title the title inserted by the user
     * @param title         Title to check
     * @param howManySkip   How many to skip
     * @param howMany       How many report we want obtain
     * @return              The list of the reports that match the condition
     */
    public List<Report> searchReportsFromPlayerFullName (String fullName, int howManySkip, int howMany)
    {	//com'è adesso trova i giocatori solo se scrivi fullname per bene senza verificare in base a regex...manteniamo così?
    	Player player = getCodPlayerFromFullName2(fullName, 0, 999999999);
    	if(player==null)
    		return null;
        List<Report> reports = new ArrayList<>();
        Gson gson = new Gson();
        Bson match = null;
        if(Session.getInstance().getLoggedUser().getRole() == 'O') {
        	match = match(and(Filters.eq("codPlayer", player.getCodPlayer()),
                    Filters.eq("userID", Session.getInstance().getLoggedUser().getUserId())));
        }
        else {
        	match = Aggregates.match(Filters.eq("codPlayer", player.getCodPlayer()));
        }
        
        Bson sort = sort(descending("creationTime"));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, sort, skip, limit))
                .into(new ArrayList<>());
        Type reportListType = new TypeToken<ArrayList<Report>>(){}.getType();
        reports = gson.fromJson(gson.toJson(results), reportListType);
        for(Report r: reports) {
        	r.setFullName(player.getFullName());
        	r.setPlayerAge(player.getAge());
        	r.setPlayerRole(player.getRole());
        }
        return reports;
    }
    
    public List<Report> searchReportsFromPlayerRole (String role, int howManySkip, int howMany)
    {	
    	List<Player> players = searchPlayersFromRole(role,  howManySkip, howMany);
    	List<Report> reports = new ArrayList<>(); 
    	
    	for(Player player: players) {
    		List<Report> r = searchReportsFromPlayerFullName(player.getFullName(), 0, howMany);
    		if(r != null)
    			reports.addAll(r);
    	}
    	
        return reports;
    }
    
    public List<Report> getReportsFromRate(int rate, int howManySkip, int howMany){
        List<Report> reports = new ArrayList<>();
        Gson gson = new Gson();
        List<Document> results = new ArrayList<>();
        Bson sort = sort(descending("creationTime"));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        Bson match = match(gte("rate", rate));
        results = (List<Document>) collection.aggregate(Arrays.asList(match, sort, skip, limit))
                .into(new ArrayList<>());
        Type reportListType = new TypeToken<ArrayList<Report>>(){}.getType();
        reports = gson.fromJson(gson.toJson(results), reportListType);
        
        for(Report r: reports) {
        	Player player = getCodPlayerFromCodPlayer(r.getCodPlayer());
        	r.setFullName(player.getFullName());
        	r.setPlayerAge(player.getAge());
        	r.setPlayerRole(player.getRole());
        }
        return reports;
    }
        
    public Player getCodPlayerFromCodPlayer(int codPlayer) {
    	chooseCollection("players");
        try {
            Player player = null;
            Gson gson = new Gson();
            Document myDoc = (Document) collection.find(eq("codPlayer", codPlayer)).first();
            player = gson.fromJson(gson.toJson(myDoc), Player.class);
            chooseCollection("reports");
            return player;
        }
        catch (Exception ex)
        {
        	ex.printStackTrace();
            return null;
        }
    }

    /**
     * Function that returns a list of Players that have the name given as argument (or one part of that) [Andrea]
     * @param name         		Name to search
     * @param howManySkip       How many to skip
     * @param howMany           How many to obtain
     * @return                  The list of reports
     */
    public List<Player> searchPlayersFromName (String name, int howManySkip, int howMany) 
    {
    	chooseCollection("players");
        List<Player> players = new ArrayList<>();
        Gson gson = new Gson();
        Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
        Bson match = Aggregates.match(Filters.regex("fullName", pattern));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type playerListType = new TypeToken<ArrayList<Player>>(){}.getType();
        players = gson.fromJson(gson.toJson(results), playerListType);
        chooseCollection("reports");
        return players;
    }
    
    /**
     * Function that returns a list of Players that have the role given as argument (or one part of that) [Andrea]
     * @param role         		Role to search
     * @param howManySkip       How many to skip
     * @param howMany           How many to obtain
     * @return                  The list of Players
     */
    public List<Player> searchPlayersFromRole (String role, int howManySkip, int howMany) //da Creare Player
    {
    	chooseCollection("players");
        List<Player> players = new ArrayList<>();
        Gson gson = new Gson();
        Pattern pattern = Pattern.compile("^.*" + role + ".*$", Pattern.CASE_INSENSITIVE);
        Bson match = Aggregates.match(Filters.regex("role", pattern));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type playerListType = new TypeToken<ArrayList<Player>>(){}.getType();
        players = gson.fromJson(gson.toJson(results), playerListType);
        chooseCollection("reports");
        return players;
    }
    
    
    
    /**
     * Function that returns a list of Players that have the role given as argument (or one part of that) [Andrea]
     * @param role         		Role to search
     * @param howManySkip       How many to skip
     * @param howMany           How many to obtain
     * @return                  The list of Players
     */ 
    public List<Player> getTopCodPlayerFromRole(String role, int howManySkip, int howMany){
        try {
        	chooseCollection("players");
        	List<Player> players = new ArrayList<>();
            Gson gson = new Gson();
            Pattern pattern = Pattern.compile("^.*" + role + ".*$", Pattern.CASE_INSENSITIVE);
            Bson match = Aggregates.match(Filters.regex("role", pattern));
            Bson sort = sort(descending("rate"));
            Bson skip = skip(howManySkip);
            Bson limit = limit(howMany);
            List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                    .into(new ArrayList<>());
            Type playerListType = new TypeToken<ArrayList<Player>>(){}.getType();
            players = gson.fromJson(gson.toJson(results), playerListType);
            chooseCollection("reports");
            return players;
        }
        catch (Exception ex)
        {
        	ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * Function that returns a list of reports that contains the category passed (or one piece of that)
     * @param team         		Football Team to search
     * @param howManySkip       How many to skip
     * @param howMany           How many to obtain
     * @return                  The list of reports
     */
    public List<Player> searchPlayersFromFootballTeam (String team, int howManySkip, int howMany)
    {
    	chooseCollection("players");
        List<Player> players = new ArrayList<>();
        Gson gson = new Gson();
        Pattern pattern = Pattern.compile("^.*" + team + ".*$", Pattern.CASE_INSENSITIVE);
        Bson match = Aggregates.match(Filters.regex("team", pattern));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type playerListType = new TypeToken<ArrayList<Player>>(){}.getType();
        players = gson.fromJson(gson.toJson(results), playerListType);
        chooseCollection("reports");
        return players;
    }
    
    /**
     * Function that returns a list of reports that contains the category passed (or one piece of that)
     * @param team         		Football Team to search
     * @param howManySkip       How many to skip
     * @param howMany           How many to obtain
     * @return                  The list of reports
     */
    public List<Player> searchPlayersFromFoot (String foot, int howManySkip, int howMany)
    {
    	chooseCollection("players");
        List<Player> players = new ArrayList<>();
        Gson gson = new Gson();
        Pattern pattern = Pattern.compile("^.*" + foot + ".*$", Pattern.CASE_INSENSITIVE);
        Bson match = Aggregates.match(Filters.regex("foot", pattern));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type playerListType = new TypeToken<ArrayList<Player>>(){}.getType();
        players = gson.fromJson(gson.toJson(results), playerListType);
        chooseCollection("reports");
        return players;
    }
    
    /**
     * Function that returns a list of reports that contains the category passed (or one piece of that)
     * @param age         		Age to search
     * @param howManySkip       How many to skip
     * @param howMany           How many to obtain
     * @return                  The list of reports
     */
    public List<Player> searchPlayersFromAge (String age, int howManySkip, int howMany) //da Creare Player
    {
    	chooseCollection("players");
        List<Player> players = new ArrayList<>();
        Gson gson = new Gson();
        Bson match = Aggregates.match(Filters.eq("age", Integer.parseInt(age)));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type playerListType = new TypeToken<ArrayList<Player>>(){}.getType();
        players = gson.fromJson(gson.toJson(results), playerListType);
        chooseCollection("reports");
        return players;
    }
    
    /**
     * Give the reports in the db in the interval [howManyToSkip, howManyToGet+howManyToSkip]
     * @param howManyToSkip
     * @param howManyToGet
     * @return  The list of reports
     */
    public List<Report> searchAllReports(int howManyToSkip, int howManyToGet)
    {
        List<Report> listOfReports = new ArrayList<>();
        Bson sort = sort(descending("creationTime"));
        Bson skip = skip(howManyToSkip);
        Bson limit = limit(howManyToGet);
        Bson match = match(gte("codReport", 0));

        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type reportListType = new TypeToken<ArrayList<Report>>(){}.getType();
        Gson gson = new Gson();
        listOfReports = gson.fromJson(gson.toJson(results), reportListType);
        return listOfReports;
    }

    /**
     * Function that returns a list of lists, each one composed by two object, the comment and the report which it is related on
     * @param howManySkip       How many comments to skip
     * @param howMany           How many comments to get
     * @return                  List of lists of object
     */
    public List<List<Object>> searchAllComments (int howManySkip, int howMany)
    {
        List<List<Object>> objects = new ArrayList<>();
        Gson gson = new Gson();
        Bson unwind = unwind("$comments");
        Bson sort = sort(descending("comments.creationTime"));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);

        Document documentHint = new Document("comments.creationTime", -1);

        MongoCursor<Document> iterator = (MongoCursor<Document>)
                collection.aggregate(Arrays.asList(unwind, sort, skip, limit)).hint(documentHint).iterator();
        while (iterator.hasNext())
        {
            Document document = iterator.next();
            Document commentDocument = (Document) document.get("comments");
            Comment comment = gson.fromJson(gson.toJson(commentDocument), Comment.class);
            // I need to re-obtain the full report
            Report report = getReportFromCodReport(document.getInteger("codReport"));

            List<Object> objectList = new ArrayList<>();
            objectList.add(comment);
            objectList.add(report);

            objects.add(objectList);
        }
        return objects;
    }
    
    public List <User> topCommentators(int howManySkip, int howMany){
    	List <User> users = new ArrayList<>();
    	
        Gson gson = new Gson();
        Bson unwind = unwind("$comments");
             
        Bson group = group("$comments.authorUsername", Accumulators.sum("count", 1), Accumulators.first("authorUsername", "$comments.authorUsername"));
        
        Bson project = project(fields(include("comments.authorUsername"), include("count")));
        //Bson project = project(fields(computed("comments.authorUsername", "_id"), excludeId(), include("count")));
        Bson sort = sort(descending("count"));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(unwind, group, project, sort, skip, limit)).into(new ArrayList());

        for(Document doc: results) {
        	User u = Neo4jDriver.getInstance().getUserByUsername(doc.getString("_id"));
        	u.setNumComments(doc.getInteger("count"));
        	users.add(u);
        }
    	return users;
    }

    /**
     * Function who updates the comments field in report
     * @param title     report Title
     * @param comments  list of comments who will updates the report fields
     */
    public void updateComments(int codReport, List<Comment> comments){
        collection = collection.withCodecRegistry(pojoCodecRegistry);
        Bson update = new Document("comments", comments);
        Bson updateOperation = new Document("$set", update);
        collection.updateOne(new Document("codReport", codReport), updateOperation);
    }

    /**
     * Function who removes a comment element from a comment view and calls the updateComments to update
     * the report into mongo
     * @param report     report name to modify
     * @param comment   comment to delete
     */
    public void deleteComment(Report report, Comment comment){
        List<Comment> comments = report.getComments();
        int i=0;
        int k=0;
        for (Comment c: comments) {
            if(c.getCreationTime().equals(comment.getCreationTime()) &&
                    c.getAuthorUsername().equals(comment.getAuthorUsername())){
                k=i;
                break;
            }
            i++;
        }
        comments.remove(k);
        updateComments(report.getCodReport(), comments);
    }

    /**
     * Gets all the comments of a report, modify the one who has to be changed and make an upload
     * @param report
     * @param comment
     */
    public void modifyComment(Report report, Comment comment){
        List<Comment> comments = report.getComments();
        int i=0;
        for (Comment c: comments
             ) {
            if(c.getAuthorUsername().equals(comment.getAuthorUsername()) && c.getCreationTime().equals(
                    comment.getCreationTime())){
                comments.set(i, comment);
                break;
            }
            i++;
        }
        updateComments(report.getCodReport(), comments);
    }

    /**
     * Function who adds a comment element to a list, with all the others comments for the report then updates the list
     * calling the updateComments
     * @param report     report  to modify
     * @param comment   comment to add
     * @return  true if the operation is successfully updated, false otherwise
     */
    public boolean addComment(Report report, Comment comment){
        try {
            if (report.getComments() == null)
                report.setComments(new ArrayList<>());
            List<Comment> comments = report.getComments();
            comments.add(comment);
            updateComments(report.getCodReport(), comments);
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }
    
    public ObservableList<Player> getAllPlayers(){
    	chooseCollection("players");
    	ObservableList<Player> players;
        Gson gson = new Gson();
        //Bson match = match(Filters.in("categories", category));
        Bson sort = sort(descending("creationTime"));
        //Bson limit = limit(howMany);
//        List<Document> results = (List<Document>)
//                collection.aggregate(Arrays.asList(match, sort, limit)).into(new ArrayList());
        List<Document> results = (List<Document>)
                collection.aggregate(Arrays.asList()).into(new ArrayList());
        Type playerListType = new TypeToken<ArrayList<Player>>(){}.getType();
        players = FXCollections.observableList(gson.fromJson(gson.toJson(results), playerListType));
        chooseCollection("reports");
        return players;
    }
    
    public List<User> searchObserverByUsername (String username, int howManySkip, int howMany) 
    {
    	chooseCollection("users");
        List<User> users = new ArrayList<>();
        Gson gson = new Gson();
        Pattern pattern = Pattern.compile("^.*" + username + ".*$", Pattern.CASE_INSENSITIVE);
        Bson match = Aggregates.match(and(Filters.regex("username", pattern),
        								Filters.eq("role", "O")));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type userListType = new TypeToken<ArrayList<User>>(){}.getType();
        users = gson.fromJson(gson.toJson(results), userListType);
        chooseCollection("reports");
        return users;
    }
    
    public List<User> searchObserverByLastName (String lastName, int howManySkip, int howMany) 
    {
    	chooseCollection("users");
        List<User> users = new ArrayList<>();
        Gson gson = new Gson();
        Pattern pattern = Pattern.compile("^.*" + lastName + ".*$", Pattern.CASE_INSENSITIVE);
        Bson match = Aggregates.match(and(Filters.regex("username", pattern),
				Filters.eq("role", "O")));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type userListType = new TypeToken<ArrayList<User>>(){}.getType();
        users = gson.fromJson(gson.toJson(results), userListType);
        chooseCollection("reports");
        return users;
    }
    
    public List<User> searchUserByUsername (String username, int howManySkip, int howMany) 
    {
    	chooseCollection("users");
        List<User> users = new ArrayList<>();
        Gson gson = new Gson();
        Pattern pattern = Pattern.compile("^.*" + username + ".*$", Pattern.CASE_INSENSITIVE);
        Bson match = Aggregates.match(and(Filters.regex("username", pattern)));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type userListType = new TypeToken<ArrayList<User>>(){}.getType();
        users = gson.fromJson(gson.toJson(results), userListType);
        chooseCollection("reports");
        return users;
    }
    
    public List<User> searchUserByLastName (String lastName, int howManySkip, int howMany) 
    {
    	chooseCollection("users");
        List<User> users = new ArrayList<>();
        Gson gson = new Gson();
        Pattern pattern = Pattern.compile("^.*" + lastName + ".*$", Pattern.CASE_INSENSITIVE);
        Bson match = Aggregates.match(and(Filters.regex("username", pattern)));
        Bson skip = skip(howManySkip);
        Bson limit = limit(howMany);
        List<Document> results = (List<Document>) collection.aggregate(Arrays.asList(match, skip, limit))
                .into(new ArrayList<>());
        Type userListType = new TypeToken<ArrayList<User>>(){}.getType();
        users = gson.fromJson(gson.toJson(results), userListType);
        chooseCollection("reports");
        return users;
    }
    
    public User login(final String username, final String password) 
    {        
        chooseCollection("users");
        User user = null;
        Gson gson = new Gson();
        BasicDBObject criteria = new BasicDBObject();
        criteria.append("username", username);
        criteria.append("password", password);
        Document myDoc = (Document) collection.find(criteria).first();
        user = gson.fromJson(gson.toJson(myDoc), User.class);
        chooseCollection("reports");
        return user;
    }

	public boolean deleteUser(String username) {
		chooseCollection("users");
        try {
            collection.deleteOne(eq("username", username));
            chooseCollection("reports");
            return true;
        }
        catch (Exception ex)
        {
            System.err.println("Error in delete user");
            chooseCollection("reports");
        }
		chooseCollection("reports");
		return false;
	}
}
