/**
 * Created by IntelliJ IDEA.
 * User: admin
 * Date: Aug 13, 2009
 * Time: 7:35:29 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Tweet {

    public void initialize (String init);
    public void message (String message);
    void cleanUp ();         
}
