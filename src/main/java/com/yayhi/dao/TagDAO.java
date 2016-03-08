package com.yayhi.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import com.yayhi.tags.Tag;
import com.yayhi.dao.DAO;

/**
 * -----------------------------------------------------------------------------
 * @version 1.0
 * @author  Mark Gaither
 * @date	Mar 7 2016
 * -----------------------------------------------------------------------------
 */

public class TagDAO {

    static boolean debug			= true;
    DAO dao							= null;
    
    // create the Tag Direct Access Object.
    public TagDAO() {   	
        dao = new DAO();     
    }
    
    // commit a set of inserts
    public void commit() throws SQLException { 
    	dao.getDAOConnection().commit(); 	
    }
    
    public int getNextID()  throws SQLException {
    	
    	dao.open();
    	Connection con = dao.getDAOConnection();
    	Statement stmt = null;
        ResultSet rset = null;
        int nextID = 0;
        
        String query = "" + 
        		"SELECT AUTO_INCREMENT as nextID " + 
        		"FROM information_schema.TABLES " + 
        		"WHERE TABLE_SCHEMA = 'tag_mapper'" + 
        		"AND TABLE_NAME = 'tag_map'";

        if (debug) {
        	System.out.print(query);
        }
        
        stmt = con.createStatement();

        rset = stmt.executeQuery(query);

        // get next id
        while (rset.next()) {    	
        	nextID = rset.getInt("nextID");
        }

        rset.close();

        stmt.close();
        
        con.close();
        
        dao.close();
        
        return nextID;
    	
    }
    
    // get all data about a Tag by id
    public boolean read(Tag tag, int id) throws SQLException {
    	
    	boolean returnVal = false;
    	dao.open();
    	Connection con = dao.getDAOConnection();
    	
    	if (debug) {
    		System.out.print("read Tag\n");
    		System.out.print("  id: " + id + "\n");
    		System.out.print("  concern: " + tag.getConcern() + "\n");
    	}

    	Statement stmt = null;
        ResultSet rset = null;
        String query = "" +
        		"SELECT t.concern " + 
        		"FROM tag_mapper t " +
        		"WHERE t.id = " + id;

        if (debug) {
        	System.out.print("  Query...\n");
        	System.out.print(query);
        	System.out.print("\n");
        	System.out.print("  Creating Statement...\n");
        }
        
        stmt = con.createStatement();

        if (debug) {
        	System.out.print("  Opening ResultsSet...\n");
        }
        rset = stmt.executeQuery(query);

        // set Tag data
        while (rset.next()) {
        	
        	if (debug) {
	            System.out.println("  Results...");
	            System.out.println("      id					   -> " + rset.getInt("id"));
	            System.out.println("      concern				   -> " + rset.getString("concern"));
        	}
        	
        	tag.setID(rset.getInt("id"));
        	tag.setConcern(rset.getString("concern"));
        	
        	returnVal = true;
        }

        rset.close();

        if (debug) {
        	System.out.print("  Closing Statement...\n");
        }
        stmt.close();
        
        dao.close();

    	return returnVal;
    	
    }
    
    // create tag record
    public void commit(Tag t) throws SQLException {

    	dao.open();
    	Connection con = dao.getDAOConnection();
    	t.setID(getNextID());
    	
        if (t.getID() > 0) {

        	PreparedStatement pst = null;

        	String insertQuery = "INSERT INTO tag_map (id,concern,tag,keyword) VALUES (?,?,?,?)";

        	if (debug) {
        		System.out.println("\nTag: commit: insertQuery: " + insertQuery);
        	}

        	try {

        		pst = con.prepareStatement(insertQuery);

        		pst.setInt(1,t.getID());
        		pst.setString(2,t.getConcern());
        		pst.setString(3,t.getTag());
        		pst.setString(4,t.getKeyword());
        		
    	        int affectedRows = pst.executeUpdate();
    	        if (debug) {
    	        	System.out.println("TagDAO: commit: affectedRows: " + affectedRows);
    	        }
    	        if (affectedRows == 0) {
    	            throw new SQLException("Creating user failed, no rows affected.");
    	        }

        	} catch (Exception e) {
        		e.printStackTrace();
    	    } finally {

    	    	pst.close();
    	    	con.commit();
    	    	
    	    	if(con != null) {
    	    		con.close();
    	    	}

    	    }	
        	
        }
        dao.close();
        
    }
    
}
