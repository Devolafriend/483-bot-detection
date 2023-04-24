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
import java.util.Scanner;

//Main class for the file
public class QueryEngine {
    
    private static final Path DATA_PATH = Paths.get("resources", "data");
    boolean indexExists=false;
    static Directory index;
    //list of stop words to be excluded from the index (we set this to none)
    private static final CharArraySet noStopWords = null;
    //the analyzer will include ALL stop words in the index
    static StandardAnalyzer analyzer = new StandardAnalyzer(noStopWords);

	
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

        try {
            System.out.println("********Welcome To The Final Project!");
            QueryEngine objQueryEngine = new QueryEngine();

			Scanner userScanner = new Scanner(System.in);

            String userInput = "1";
			while (userInput.equals("1") || userInput.equals("2")){
				float total = 0;
				float correct = 0;

				System.out.println("Enter the following commands to choose a ranking method -\n" + 
						"\tEnter 1 for tf-idf\n" + 
						"\tEnter 2 for BM25\n" + 
						"\tEnter anything else to exit");
				userInput = userScanner.nextLine();
				if (! (userInput.equals("1") || userInput.equals("2"))){
					System.out.println("***************** Thanks for using the program! *****************\n");
					userScanner.close();
					break;
				}


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
							String[] answer = runQuery(query, userInput);
							
							System.out.println("Query " + (int) (total));
							System.out.println("Query = " + String.join(" ", query) + " (Troll Query: " + troll +
									")\nTop Result = " + answer[0] + " (Troll Result: " +answer[1]+ ")\n");
							
							
							if (answer[1] != null) {
								if (answer[1].equals(troll))
									correct +=1;
							}      
						}
					}
				}
				System.out.println("total = " + (int) total + " Correct = " + (int) correct);
				System.out.println(correct/total);
				myReader.close();
			}    
            userScanner.close();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
			System.out.println("An error occurred.");
			ex.printStackTrace();
        }
    }

    /*
    This method runs a query for a single tweet.
    It will use either tf-idf or BM25 for scoring similarity and will return the top result.
    We changed the hyper parameters for BM25 to k1 = 2 and b = 0 as this 
    yielded the highest correct percentage
    */
    public static String[] runQuery(String[] query, String rankingMethod) throws java.io.FileNotFoundException,java.io.IOException {
        
        //creates query with query terms separated by a space        
        String[] top = new String[2];
        
        String querystr = String.join(" ", query);
        try {
		Query q = new QueryParser("content", analyzer).parse(QueryParser.escape(querystr));
	        int hitsPerPage = 1;
	        IndexReader reader = DirectoryReader.open(index);
	        IndexSearcher searcher = new IndexSearcher(reader);
		
			if (rankingMethod.equals("1")){
				searcher.setSimilarity(new ClassicSimilarity());
			}	
			else
			    searcher.setSimilarity(new BM25Similarity(2, 0));

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

}
