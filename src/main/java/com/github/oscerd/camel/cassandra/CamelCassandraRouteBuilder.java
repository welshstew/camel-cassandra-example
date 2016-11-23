package com.github.oscerd.camel.cassandra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.github.oscerd.component.cassandra.CassandraException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import com.github.oscerd.component.cassandra.CassandraConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usage example of Camel-cassandra Component
 */
public class CamelCassandraRouteBuilder extends RouteBuilder {

    Logger log = LoggerFactory.getLogger(this.getClass());

    public void configure() {

    	String CLUSTER_ADDRESS = System.getenv("CLUSTER_ADDRESS");
    	List<String> collAddr = new ArrayList<String>();
    	collAddr.add(CLUSTER_ADDRESS);

        onException(InvalidQueryException.class)
                .handled(true)
                .log("INVALID QUERY - recreating schema!")
                .to("direct:populateData");

        from("direct:populateData")
                .process(exchange -> {
                    Cluster cluster;
                    Session session;
                    cluster = Cluster.builder()
                            .addContactPoint(CLUSTER_ADDRESS)
                            .build();
                    Metadata metadata = cluster.getMetadata();
                    log.info("Connected to cluster " + metadata.getClusterName());
                    session = cluster.connect();
                    session.execute("CREATE KEYSPACE IF NOT EXISTS simplex WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};");
                    session.execute("CREATE TABLE IF NOT EXISTS simplex.songs (id uuid PRIMARY KEY, title text, album text, artist text, tags set<text>, data blob);");
                    session.execute("CREATE INDEX IF NOT EXISTS album_idx ON simplex.songs(album);");
                    session.execute("CREATE INDEX IF NOT EXISTS title_idx ON simplex.songs(title);");
                    session.execute("INSERT INTO simplex.songs (id, title, album, artist, tags) VALUES (now(), 'Intolerance', 'Undertow', 'Tool',  {'1993', 'Alternative Metal'});");
                    session.execute("INSERT INTO simplex.songs (id, title, album, artist, tags) VALUES (now(), 'Prison Sex', 'Undertow', 'Tool',  {'1993', 'Alternative Metal'});");
                    session.execute("INSERT INTO simplex.songs (id, title, album, artist, tags) VALUES (now(), 'Sober', 'Undertow', 'Tool',  {'1993', 'Alternative Metal'});");
                });

        from ("timer://first?fixedRate=true&period=1000&repeatCount=1").to("direct:populateData").to("controlbus:route?routeId=selecta&action=start");

        from ("timer://timer?fixedRate=true&period=10000").autoStartup(false).routeId("selecta")
        .setHeader(CassandraConstants.CASSANDRA_CONTACT_POINTS, constant(collAddr))
        .to("cassandra:cassandraConnection?keyspace=simplex&table=songs&operation=selectAll")
        .to("log:stuff?showAll=true");
    }

}
