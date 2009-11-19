package replicatorg.app;

import twitter4j.Twitter;
import twitter4j.Status;
import twitter4j.TwitterException;

/**
 * Created by IntelliJ IDEA.
 * User: Rick Pollack
 * Date: Aug 13, 2009
 * Time: 7:35:45 PM
 */

public class TwitterBot implements Tweet{

    private Twitter twitter;

    public void initialize (String init) {

        String[] params;

        try {
            params = init.split("\\,");    //no space after the comma for password!!
            twitter = new Twitter(params[0], params[1]);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void message (String message) {

        try {
            @SuppressWarnings("unused")
			Status status = twitter.updateStatus(message);
        } catch (TwitterException e) {
            e.printStackTrace(); 
        }
    }

    public void cleanUp () {
    }
}
