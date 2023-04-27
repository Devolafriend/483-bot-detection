/*
File: QueryEngine.java
Project: CSC 483 Final Project
Authors: Zachary Lopez, Jaygee Galvez, Eric Simonson
Description:
This program uses Lucene to detect if a Twitter post is from a Russian
troll. The data we use for the project is from Twitter and is comprised of
20,000 verified Russian troll tweets and 20,000 tweets from a Twitter 
repository with no attribution data (assumed to be non-trolls).
Our program prompts the user to choose between tf-idf and BM25
scoring. In it's current state, the program indexes stop words - 
we explained the reasoning for this in the accompanying pdf.
*/

package final_project;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.ConnectIOException;
import java.util.Scanner;

//Main class for the file
public class QueryEngine {
    
    private static final Path DATA_PATH = Paths.get("resources", "data");
    boolean indexExists=false;
    static Directory index;
    //list of stop words to be excluded from the index (we set this to none)
    private static final CharArraySet keepStopWords = null;
    //the analyzer will include ALL stop words in the index
    static StopwordAnalyzerBase analyzer = new StandardAnalyzer(keepStopWords);

	
    /* Constructor for QueryEngine.
    Does not accept any arguments.
   */
    public QueryEngine(){
        index = buildIndex();
    }

    /*
    This method reads the contents of "data_to_query.csv", which contains
    approximately 40,000 tweets from troll and non-troll accounts.
    Each line is read and parsed. The first column of the csv is expected to
    be the content of the tweet and the second column is expected to be the 
    troll status (1 for troll, 0 for non-troll).
    */
    private Directory buildIndex() {
        //initializes data structures for Lucene index
        Directory index = new ByteBuffersDirectory();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter w;
		try {
			w = new IndexWriter(index, config);

			//Get file from resources folder
			File myObj = new File(getClass().getClassLoader().getResource("data_to_query.csv").getFile());
	        try (Scanner inputScanner = new Scanner(new BufferedReader(new FileReader(myObj)))) {
	        	
	            while (inputScanner.hasNext()) {
	            	String input = inputScanner.nextLine();
	            	if (input.length() > 1) {
	            		String content = input.substring(0, input.length()-2);
	            		String troll = input.substring(input.length()-1);
	            		if (troll.equals("0") || troll.equals("1")) {
					//adds tweet content and troll status
					addDoc(w, content, troll);}
	            		}
	            }

	            inputScanner.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        indexExists = true;
	        w.close();
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
		return index;
    }

    /*
    This method adds a document to Lucene's index.
    The content of the tweet is stored as "content" and
    the troll status is stored as "troll"
    */
    private void addDoc(IndexWriter w, String content, String troll) throws IOException {
    	  Document doc = new Document();
    	  doc.add(new TextField("content", content, Field.Store.YES));
    	  doc.add(new StringField("troll", troll, Field.Store.YES));
    	  w.addDocument(doc);
    	}

	private File openDataFile(String filename) {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(filename).getFile());
		return file;
	}

	/*
	Main method for the class.
	Main first asks the user for an input. 1 for tf-idf scoring and 2 for BM25.
	The program then opens query.csv and calls runQuery() for each line
	of the csv.
	runQuery() will return the top result from the query and this method
	prints the result to the terminal. It will first show the queried tweet content
	and troll status and the top result's content and troll status.
	Once all lines from the csv have been queried, the total number of tweets,
	total correct matched troll status, and percentage correct is printed to
	the terminal.
	*/
	public static void main(String[] args ) {
		System.out.println("********Welcome To The Final Project!********");
		Scanner userScanner = new Scanner(System.in);

		String rankingMethodString = "";
		runAll();
		while(true){
			System.out.println("Enter the following commands to choose a ranking method -\n" + 
					"\tEnter 1 to redisplay summary of all ranking methods\n" + 
					"\tEnter 2 for tf-idf\n" + 
					"\tEnter 3 for BM25\n" +   
					"\tEnter exit to exit");
			
					rankingMethodString = userScanner.nextLine();
			
			// check to see if we exit, display all, or had invalid input
			if (rankingMethodString.equals("exit")){
				break;
			} 
			else if (rankingMethodString.equals("1")){
				runAll();
				continue;
			}
			else if (!(rankingMethodString.equals("1") | rankingMethodString.equals("2") 
					 | rankingMethodString.equals("3") | rankingMethodString.equals("exit"))){
				System.out.println("Invalid input");
				continue;
			}


			// ask about stemming
			String stemmingString = ""; 
			while (!(stemmingString.equals("1") | stemmingString.equals("2"))){
				System.out.println("Choose whether or not to include stemming - \n" + 
									"\tEnter 1 to include stemming\n" + 
									"\tEnter 2 to not include stemming");
				stemmingString = userScanner.nextLine();
			}

			// ask about stop words if BM25 was not chosen 
			String stopWordString = "";
			if (!rankingMethodString.equals("3")){
				while(!(stopWordString.equals("1") | stopWordString.equals("2"))){
					System.out.println("Include stop words? - \n" + 
						"\tEnter 1 to include\n" + 
						"\tEnter 2 to not");
						stopWordString = userScanner.nextLine();
				}
			} else {
				stopWordString = "1";
			}


			// if they chose BM25, ask which hyperparameters they would wish to see. 
			String hyperParamString = "";
			if (rankingMethodString.equals("3")){
				while ( !( hyperParamString.equals("1") | hyperParamString.equals("2") 
					     | hyperParamString.equals("3") | hyperParamString.equals("4")
					     | hyperParamString.equals("5") ) ){
					System.out.println("Since you chose BM25, choose from the following commands for the hyperparameters -\n" + 
						"\tEnter 1 to choose default parameters\n"+
						"\tEnter 2 to have k1 = 4, b = .25\n" + 
						"\tEnter 3 to have k1 = 2, b = .5\n" + 
						"\tEnter 4 to have k1 = 2, b = .25\n" + 
						"\tEnter 5 to have k1 = 2, b = .0");
					hyperParamString = userScanner.nextLine();
				}
			}

			// set up the standardAnalyzer, queryEngine, along with the parameters to pass into queryController
			int rankingMethod = Integer.parseInt(rankingMethodString); // shift down
			int stopWord = Integer.parseInt(stopWordString);
			int stemming = Integer.parseInt(stemmingString);
			int k = 0; 
			float b = 0;
			if (rankingMethod == 3){
				int hyperParam = Integer.parseInt(hyperParamString);
				if (hyperParam == 2){k = 4; b = 0.25f;}
				if (hyperParam == 3){k = 2; b = 0.5f;}
				if (hyperParam == 4){k = 2; b = 0.25f;}
				if (hyperParam == 5){k = 2;}
			}

			// set up stop words and stemming exclusion/exclusion. 
			boolean stem = false;
			boolean stop = false; 
			if (stemming == 1)
				stem = true; 
			else 
				stem = false; 
			if (stopWord == 1)
				stop = true;
			else 
				stop = false; 
			changeStandardAnalyzer(stem, stop);

			
			QueryEngine objQueryEngine = new QueryEngine();
			queryController(objQueryEngine, rankingMethod, k, b, true);
		}
    }


	/*
	 * Method: QueryController 
	 * Purpose: iterates through the query.csv file where upon each iteration takes a line/query 
	 * 			from the query.csv file and runs this line in runquery. 
	 * Parameters: objQueryEngine - QueryEngine object we are using to run queries  
	 * 			   rankingMethod - the ranking method to be used. tfidf if 2, BM25 if 3
	 * 			   k - the k1 hyperparameter for BM25
	 * 			   b - the b hyperparameter for BM25
	 * 			   printQueryInfo - boolean if we want to print each of the individual queries
	 * return: float of the amount of ( correct queries / total queries made )
	 */
	public static float queryController(QueryEngine objQueryEngine, int rankingMethod, int k, float b, boolean printQueriesInfo){
		float resultPercentage = 0;
		try{
			float total = 0;
			float correct = 0;

			File file = objQueryEngine.openDataFile("query.csv");
			Scanner myReader = new Scanner(new BufferedReader(new FileReader(file)));
			myReader.nextLine();
			while (myReader.hasNextLine()) {
				
				String data = myReader.nextLine();
				if (data.length() > 3) {
					String troll = data.substring(data.length()-1, data.length());
					String[] query = normalizeQuery((data.substring(0, data.length()-2)).split(" "));
					
					if (query.length > 1 && (troll.equals("0") || troll.equals("1"))) {
						total += 1;
						String[] answer = runQuery(query, rankingMethod, k, b);

						if (printQueriesInfo){
							System.out.println("Query " + (int) (total));
							System.out.println("Query = " + String.join(" ", query) + " (Troll Query: " + troll +
								")\nTop Result = " + answer[0] + " (Troll Result: " +answer[1]+ ")\n");
						}
						
						if (answer[1] != null) {
							if (answer[1].equals(troll))
								correct +=1;
						}      
					}
				}
			}
			resultPercentage = correct / total;
			if (printQueriesInfo){
				System.out.println("total = " + (int) total + " Correct = " + (int) correct);
				System.out.println(resultPercentage);
				System.out.println();
			}
			myReader.close();   
		}
        catch (Exception ex) {
            System.out.println(ex.getMessage());
			System.out.println("An error occurred.");
			ex.printStackTrace();
        }
		return resultPercentage;
	}

	/*
	 * Method: runAll
	 * Purpose: This method runs a set of query runs.
	 * Paremeters: None
	 * Returns: none
	 */
	public static void runAll(){

		QueryEngine objQueryEngine1 = new QueryEngine();
		changeStandardAnalyzer(true, false);
		float tfidfStopWords = queryController(objQueryEngine1, 2, 0, 0, false);
		QueryEngine objQueryEngine2 = new QueryEngine();
		float BM25DefStopWords = queryController(objQueryEngine2, 3, 0, 0, false);
		QueryEngine objQueryEngine3 = new QueryEngine();
		float BM25k4b25 = queryController(objQueryEngine3, 3, 4, 0.25f, false);
		QueryEngine objQueryEngine4 = new QueryEngine();
		float BM25k2b5 = queryController(objQueryEngine4, 3, 2, 0.5f, false);
		QueryEngine objQueryEngine5 = new QueryEngine();
		float BM25k2b25 = queryController(objQueryEngine5, 3, 2, 0.25f, false);
		QueryEngine objQueryEngine6 = new QueryEngine();
		float BM25k2b0 = queryController(objQueryEngine6, 3, 2, 0, false);

		changeStandardAnalyzer(false, false);
		QueryEngine objQueryEngine7 = new QueryEngine();
		float tfidfNoStopWords = queryController(objQueryEngine7, 2, 0, 0, false);
		QueryEngine objQueryEngine8 = new QueryEngine();
		float BM25DefNoStopWords = queryController(objQueryEngine8, 3, 0, 0, false);

		// change from standard analyzer to EnglishAnalyzer. Use KeepStopWords 
		changeStandardAnalyzer(true, true);
		QueryEngine engQueryEngine1 = new QueryEngine();
		float engtfidfStopWords = queryController(engQueryEngine1, 2, 0, 0, false);
		QueryEngine engQueryEngine2 = new QueryEngine();
		float engBM25DefStopWords = queryController(engQueryEngine2, 3, 0, 0, false);
		QueryEngine engQueryEngine3 = new QueryEngine();
		float engBM25k4b25 = queryController(engQueryEngine3, 3, 4, 0.25f, false);
		QueryEngine engQueryEngine4 = new QueryEngine();
		float engBM25k2b5 = queryController(engQueryEngine4, 3, 2, 0.5f, false);
		QueryEngine engQueryEngine5 = new QueryEngine();
		float engBM25k2b25 = queryController(engQueryEngine5, 3, 2, 0.25f, false);
		QueryEngine engQueryEngine6 = new QueryEngine();
		float engBM25k2b0 = queryController(engQueryEngine6, 3, 2, 0, false);

		changeStandardAnalyzer(false, true);
		QueryEngine engQueryEngine7 = new QueryEngine();
		float engtfidfNoStopWords = queryController(engQueryEngine7, 2, 0, 0, false);
		QueryEngine engQueryEngine8 = new QueryEngine();
		float engBM25DefNoStopWords = queryController(engQueryEngine8, 3, 0, 0, false);


		
		System.out.println("BM25(default hyperparameters) stop words indexed: \t" + BM25DefStopWords);
		System.out.println("BM25(default hyperparameters) no stop words indexed: \t" + BM25DefNoStopWords);
		System.out.println("tf-idf stop words\t" + tfidfStopWords);
		System.out.println("tf-idf no stop words\t" + tfidfNoStopWords);
		System.out.println("BM25 k1 = 4, b = .25 (stop words indexed)\t" + BM25k4b25);
		System.out.println("BM25 k1 = 2, b = .5 (stop words indexed)\t" + BM25k2b5);
		System.out.println("BM25 k1 = 2, b = .25 (stop words indexed)\t" + BM25k2b25);
		System.out.println("BM25 k1 = 2, b = .0 (stop words indexed)\t" + BM25k2b0);
		System.out.println();

		System.out.println("stemming - BM25(default hyperparameters) stop words indexed: \t" + engBM25DefStopWords);
		System.out.println("stemming - BM25(default hyperparameters) no stop words indexed: \t" + engBM25DefNoStopWords);
		System.out.println("stemming - tf-idf stop words\t" + engtfidfStopWords);
		System.out.println("stemming - tf-idf no stop words\t" + engtfidfNoStopWords);
		System.out.println("stemming - BM25 k1 = 4, b = .25 (stop words indexed)\t" + engBM25k4b25);
		System.out.println("stemming - BM25 k1 = 2, b = .5 (stop words indexed)\t" + engBM25k2b5);
		System.out.println("stemming - BM25 k1 = 2, b = .25 (stop words indexed)\t" + engBM25k2b25);
		System.out.println("stemming - BM25 k1 = 2, b = .0 (stop words indexed)\t" + engBM25k2b0);
		System.out.println();
	}

    /*
    This method runs a query for a single tweet.
    It will use either tf-idf or BM25 for scoring similarity and will return the top result.
    We changed the hyper parameters for BM25 to k1 = 2 and b = 0 as this 
    yielded the highest correct percentage
    */
    public static String[] runQuery(String[] query, int rankingMethod, int k, float b) throws java.io.FileNotFoundException,java.io.IOException {
        
        //creates query with query terms separated by a space        
        String[] top = new String[2];
        
        String querystr = String.join(" ", query);
        try {
		Query q = new QueryParser("content", analyzer).parse(QueryParser.escape(querystr));
	        int hitsPerPage = 1;
	        IndexReader reader = DirectoryReader.open(index);
	        IndexSearcher searcher = new IndexSearcher(reader);
		
			if (rankingMethod == 2){
				searcher.setSimilarity(new ClassicSimilarity());
			} 	
			else{
				if (k == 0 && b == 0) // default
			    	searcher.setSimilarity(new BM25Similarity());
				else 
					searcher.setSimilarity(new BM25Similarity(k, b));
			}
	        TopDocs docs = searcher.search(q, hitsPerPage);
	        ScoreDoc[] hits = docs.scoreDocs;
			
	        for(int i=0;i<hits.length;++i) {
	            int docId = hits[i].doc;
	            Document d = searcher.doc(docId);
                top[0] = d.get("content"); 
                top[1] = d.get("troll");
	        }
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
              
        return top;
    }

	/*
	This method is used to normalize the queries.
	Currently, out normalization includes the Lucene default
	normalization along with eliminating any tokens that
	start with a question mark, as that's an indicator the 
	tweet contains an emoji or other non-alphanumeric character.
	We did not have a way to tokenize emojis and similar.
	*/
	private static String[] normalizeQuery(String[] query) {
		List<String> lst = new ArrayList<String>();
		for (int i = 0; i < query.length; i++) {
			if (query[i].length() > 0)
				if (!(query[i].charAt(0)=='?'))
					lst.add(query[i]);
			}
		String[] returnarr = new String[lst.size()];
		for (int j = 0; j < lst.size(); j++)
			returnarr[j] = lst.get(j);
		
		return returnarr;
	}


	/*
	 * Method: changeStandardAnalyzer()
	 * Purpose: Changes the standard analyzer to either include or not include stop words
	 * Parameter: includeStopWords - boolean for if we want to include stop words. 
	 * 			  stemming - boolean for if we want to stem or not
	 * returns: void
	 */
	private static void changeStandardAnalyzer(boolean includeStopWords, boolean stemming){
		if (stemming){
			if (includeStopWords){
				analyzer = new EnglishAnalyzer(keepStopWords);
			} else {
				analyzer = new EnglishAnalyzer();
			}
		} else {
			if (includeStopWords){
				analyzer = new StandardAnalyzer(keepStopWords);
			} else {
				analyzer = new StandardAnalyzer();
			}
		}
	}
}
