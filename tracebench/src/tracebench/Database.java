//	Copyright (c) 2009, E. van Baaren (erikjan@gmail.com)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package tracebench;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
	Connection conn = null;
	PreparedStatement stmt = null; 
	ResultSet rs = null; 
	
	public Database(String databaseUri) {
		/* first of all try to make a connection to the database */
		try { 
			// The newInstance() call is a work around for some 
			// broken Java implementations 
			Class.forName("com.mysql.jdbc.Driver").newInstance(); 
		} catch (Exception ex) { 
			// this is bad
			ex.printStackTrace();
			System.exit(1);
		}
		
		try { 
			conn = DriverManager.getConnection(databaseUri); 
		} catch (SQLException ex) { 
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	public synchronized WikiPage getPage(String pageTitle) {
		WikiPage p = new WikiPage();
		
		String query = "SELECT * FROM page, revision, text WHERE " +
				"page.page_title = (?) AND page.page_latest = revision.rev_id " +
				"AND text.old_id = page.page_latest LIMIT 1";
		try { 
			stmt = conn.prepareStatement(query);
			stmt.setString(1, pageTitle);
			rs = stmt.executeQuery();  
			//rs = stmt.getResultSet(); 
			if (rs.next()) {
				/* Get the content from the binary blob, convert it to a String
				 * using UTF-8 encoding. The rest of the fields are easy */
				java.sql.Blob b = rs.getBlob("text.old_text");
				p.setPageContent(
					new String(
						b.getBytes(1, (int)b.length()), "UTF-8"
					)
				);
				p.setPageTitle(rs.getString("page.page_title"));
				p.setPageId(rs.getInt("page.page_id"));
				p.setRevisionId(rs.getInt("revision.rev_id"));
				p.setTimestamp(
						new String(
								rs.getBytes("revision.rev_timestamp"), "UTF-8")
						);
				
				return p;
			}
			stmt.close();
		} catch(SQLException e) {
			e.printStackTrace();
			return null;
		} catch(NullPointerException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
