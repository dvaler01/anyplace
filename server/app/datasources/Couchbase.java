package datasources;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;
import play.Logger;
import play.Play;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO - NOT USED ANYMORE

/**
 * The `Couchbase` class acts a simple connection manager for the `CouchbaseClient`
 * and makes sure that only one connection is alive throughout the application.
 *
 */
public final class Couchbase {

        /**
         * Holds the actual `CouchbaseClient`.
         */
        private static CouchbaseClient client = null;

        /**
         * Connects to Couchbase based on the configuration settings.
         *
         * If the database is not reachable, an error message is written and the
         * DatabaseConnectionException is thrown
         */
        public static boolean connect() throws DatabaseConnectionException {

            String hostname = Play.application().configuration().getString("couchbase.hostname");
            String port = Play.application().configuration().getString("couchbase.port");
            String bucket = Play.application().configuration().getString("couchbase.bucket");
            String password = Play.application().configuration().getString("couchbase.password");
            Logger.info("Trying to connect to: " + hostname + ":" + port + " bucket[" + bucket + "] password: " + password);

            List<URI> uris = new LinkedList<URI>();
            //uris.add(URI.create("http://" + hostname + ":" + port + "/pools"));
            uris.add(URI.create(hostname + ":" + port + "/pools"));

            try {
                CouchbaseConnectionFactoryBuilder cfb = new CouchbaseConnectionFactoryBuilder();

                cfb.setMaxReconnectDelay(10000);
                cfb.setOpQueueMaxBlockTime(5000);
                cfb.setOpTimeout(20000);
                cfb.setShouldOptimize(true);
                cfb.setViewTimeout(300000);
                //CouchbaseConnectionFactory connFactory = cfb.buildCouchbaseConnection(uris, bucket, password);

                CouchbaseConnectionFactory connFactory = new CouchbaseConnectionFactory(uris, bucket, password);
                client = new CouchbaseClient(connFactory);
                //client = new CouchbaseClient(uris, bucket, password);
            } catch (java.net.SocketTimeoutException e) {
                // thrown by the constructor on timeout
                Logger.error("Error connection to Couchbase: " + e.getMessage());
                throw new DatabaseConnectionException("Cannot connect to Anyplace Database [SocketTimeout]!");
            } catch (IOException e) {
                Logger.error("Error connection to Couchbase: " + e.getMessage());
                throw new DatabaseConnectionException("Cannot connect to Anyplace Database [IO]!");
            } catch (Exception e) {
                Logger.error("Error connection to Couchbase: " + e.getMessage());
                throw new DatabaseConnectionException("Cannot connect to Anyplace Database! [Unknown]");
            }

            return true;

        }

        /**
         * Disconnect from Couchbase.
         */
        public static boolean disconnect() {
            if(client == null) {
                return false;
            }
            boolean res = client.shutdown(3, TimeUnit.SECONDS);
            client = null;
            return res;
        }

        /**
         * Returns the actual `CouchbaseClient` connection object.
         *
         * If no connection is established yet, it tries to connect. Note that
         * this is just in place for pure convenience, make sure to connect explicitly.
         */
        public static CouchbaseClient getConnection() throws DatabaseConnectionException {
            if(client == null) {
                connect();
            }

            return client;
        }


}
