import net.PlayerClient;
import net.PlayerFactory;
import player.AIPlayerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * The PlayerService creates a new service that connects to the
 * server. This should be launched after the judge has been started.
 * To launch the PlayerService, you should pass in a number of arguments:
 * <pre>
 *   java PlayerService localhost 8122 ab1234 cd5678 ef4321 gh6543 ab1234 cd5678
 * </pre>
 * This will start a new PlayerService that will connect to
 * the server with localhost as its ip (which is your current computer),
 * on port 8122, and will be playing with students whose ids are
 * things such as  ab1234 and cd5678. Notice that the same ID might be
 * playing more than once. You should replace these with your actual
 * University of Bristol student IDs.
 */
public class PlayerService {
    public static void main(String[] args) throws IOException {
        System.out.println(args);
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        List<String> studentIds = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));

        // TODO: This factory should be replaced with a clever AI.
        PlayerFactory factory = new AIPlayerFactory();

        PlayerClient client = new PlayerClient(host, port, studentIds, factory);
        client.run();
    }
}
