package com.yayhi.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.yayhi.tags.Tag;

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
        		"WHERE TABLE_SCHEMA = 'tagmap' " + 
        		"AND TABLE_NAME = 'map'";

        if (debug) {
        	System.out.print("TagDAO: getNextID: " + query);
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
    
    // create tag record
    public void commit(Tag t) throws SQLException, IOException {

    	dao.open();
    	Connection con = dao.getDAOConnection();
    	
    	Statement stmt = null;
	   	ResultSet rset = null;
	   	int currentID = 0;
	   	boolean exists = false;
	   	String whereStr = "";
	   	
	   	whereStr += "t.vocabulary = '" + t.getVocabulary() + "' AND t.parent_term = '" + t.getParentTerm() + "' AND t.synonym = '" + t.getSynonym() + "'";
	   	if (!t.getTerm().equals("")) {
	   		whereStr += " AND t.term = '" + t.getTerm() + "'";
	   	}
	   
	   	String query = "" + 
		"SELECT t.id as currentID " + 
		"FROM map t " + 
		"WHERE " + whereStr;
	
	   	if (debug) {
	   		System.out.print("TagDAO: check existence: " + query);
	   	}
   
	   	stmt = con.createStatement();

	   	rset = stmt.executeQuery(query);

	   	// get next id
	   	while (rset.next()) {    	
	   		currentID = rset.getInt("currentID");
	   	}
   
	   	if (currentID > 0) {
	   		exists = true;
	   	}

	   	rset.close();

	   	stmt.close();
	   
	   	//boolean exists = this.mappingExists(t);
    	
    	if (debug) {
    		System.out.println("TagDAO: commit: vocabulary: " + t.getVocabulary() + " parent term: " + t.getParentTerm() + " term: " + t.getTerm() + " exists: " + exists);
    	}
    	
    	// log the work
    	if (exists) {
    		t.getLogger().write("\tID: " + currentID + ":\tVOCABULARY: " + t.getVocabulary() + "\tPARENT TERM: " + t.getParentTerm() + "\tTERM: " + t.getTerm() + "\tSYNONYM: " + t.getSynonym() + "\t***** EXISTS");
    	} else {
        	t.setID(getNextID());
        	t.getLogger().write("\tID: " + t.getID() + ":\tVOCABULARY: " + t.getVocabulary() + "\tPARENT TERM: " + t.getParentTerm() + "\tTERM: " + t.getTerm() + "\tSYNONYM: " + t.getSynonym());
    	}
    	
        if (!exists && t.getID() > 0) {

        	PreparedStatement pst = null;

        	String insertQuery = "INSERT INTO map (vocabulary,parent_term,term,synonym) VALUES (?,?,?,?)";

        	if (debug) {
        		System.out.println("\nTag: commit: insertQuery: " + insertQuery);
        	}

        	try {

        		pst = con.prepareStatement(insertQuery);

        		pst.setString(1,t.getVocabulary());
        		pst.setString(2,t.getParentTerm());
        		pst.setString(3,t.getTerm());
        		pst.setString(4,t.getSynonym());
        		
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
