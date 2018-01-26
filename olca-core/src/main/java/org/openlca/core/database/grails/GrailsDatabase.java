package org.openlca.core.database.grails;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;


/*import org.eclipse.persistence.jpa.PersistenceProvider;*/
//import org.openlca.core.database.BaseDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.Notifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import javax.persistence.Persistence;


/**
 * IDatabase implementation for MySQL database. The URL schema is
 * "jdbc:mysql://" [host] ":" [port] "/" [database]
 */
public class GrailsDatabase extends Notifiable implements IDatabase {



	private Logger log = LoggerFactory.getLogger(this.getClass());
	private EntityManagerFactory entityFactory;
	private String url;
	private String user;
	private String password;
	private java.sql.Connection connection;
	
	public GrailsDatabase(Connection connection) {
		super();
		this.connection = connection;
		//this.entityFactory=Persistence.createEntityManagerFactory("openLCA");
	}

	public GrailsDatabase(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;
		connect();
	}

	private void connect() {
        log.trace("Connect to database mysql: {} @ {}", user, url);
        Map<Object, Object> map = new HashMap();
        if (this.entityFactory != null  && this.entityFactory.isOpen()) {
               this.entityFactory.close();
        }
        
      //  this.entityFactory=Persistence.createEntityManagerFactory("openLCA");
        //initConnectionPool();
 }


	private void initConnectionPool() {
		
	}
	@Override
	public EntityManagerFactory getEntityFactory() {
		return entityFactory;
	}

	public void setEntityFactory(javax.persistence.EntityManagerFactory entityFactory ) {
		this.entityFactory = entityFactory;
	}
	
	@Override
	public Connection createConnection() {
		log.trace("create connection mysql: {} @ {}", user, url);
		return this.connection;
			
	}

	@Override
	public void close() {
		log.trace("close database mysql: {} @ {}", user, url);
		try {
			if (entityFactory != null && entityFactory.isOpen())
				entityFactory.close();
		} catch (Exception e) {
			log.error("failed to close database", e);
		} finally {
			entityFactory = null;
		}
	}


	public String getName() {
		if (url == null)
			return null;
		String[] parts = url.split("/");
		if (parts.length < 2)
			return null;
		return parts[parts.length - 1].trim();
	}

	@Override
	public int getVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public File getFileStorageLocation() {
		// TODO Auto-generated method stub
		return null;
	}
}
