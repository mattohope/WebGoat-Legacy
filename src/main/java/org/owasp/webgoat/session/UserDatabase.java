package org.owasp.webgoat.session;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;

class UserDatabase {
	private Connection userDB;
	private final String USER_DB_URI = "jdbc:h2:userDatabase";

	private final String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, username VARCHAR(255) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL);";
	private final String CREATE_ROLES_TABLE = "CREATE TABLE IF NOT EXISTS roles (id INTEGER PRIMARY KEY, rolename VARCHAR(255) NOT NULL UNIQUE);";
	private final String CREATE_USER_ROLES_TABLE = "CREATE TABLE IF NOT EXISTS user_roles (id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL, role_id INTEGER NOT NULL);";
	private final String CREATE_USER_ROLES_USER_KEY = "ALTER TABLE user_roles ADD CONSTRAINT user_key FOREIGN KEY user_id REFERENCES users(id);";
	private final String CREATE_USER_ROLES_ROLE_KEY = "ALTER TABLE user_roles ADD CONSTRAINT role_key FOREIGN KEY role_id REFERENCES roles(id);";
	private final String ADD_DEFAULT_USERS = "INSERT INTO users (username, password) VALUES ('webgoat','webgoat'),('basic','basic'),('guest','guest');";
	private final String ADD_DEFAULT_ROLES = "INSERT INTO roles (rolename) VALUES ('webgoat_basic'),('webgoat_admin'),('webgoat_user');";
	private final String ADD_ROLE_TO_USER = "INSERT INTO user_roles (user_id, role_id) VALUES SELECT users.id, roles.id FROM users, roles WHERE users.username = ? AND roles.rolename = ?;";

	private final String QUERY_ALL_USERS = "SELECT username FROM users;";
	private final String QUERY_ALL_ROLES_FOR_USERNAME = "SELECT rolename FROM roles WHERE roles.id = user_roles.role_id AND user_roles.user_id = users.id AND users.username = ?;";

	private final String DELETE_ALL_ROLES_FOR_USER = "DELETE FROM roles WHERE roles.user_id = users.id and users.username = ?;";
	private final String DELETE_USER = "DELETE FROM users WHERE user.username = ?;";

	public UserDatabase() {
		createDefaultTables();
		createDefaultUsers();
		createDefaultRoles();
		addDefaultRolesToDefaultUsers();
	}

	public boolean open() {
		try {
			if (userDB == null || userDB.isClosed()) {
				Class.forName("org.h2.Driver");
				userDB = DriverManager.getConnection(USER_DB_URI, "webgoat_admin", "");
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public boolean close() {
		try {
			if (userDB != null && !userDB.isClosed())
				userDB.close();
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	public Iterator<User> getUsers() {
		ArrayList<User> users = new ArrayList<User>();		
		User currentUser;
		ResultSet userResults, roleResults;

		try {
			open();		
			Statement statement = userDB.createStatement();
			PreparedStatement rolesForUsers = userDB.prepareStatement(QUERY_ALL_ROLES_FOR_USERNAME);

			userResults = statement.executeQuery(QUERY_ALL_USERS);
			while (userResults.next()) {
				currentUser = new User(userResults.getString("username"));
				rolesForUsers.setString(1, currentUser.getUsername());
				roleResults = rolesForUsers.executeQuery();
				while (roleResults.next()) {
					currentUser.addRole(roleResults.getString("rolename"));
				}
				roleResults.close();
			}
			rolesForUsers.close();
			userResults.close();
			close();
		} catch (SQLException e) {
			users = new ArrayList<User>();
		}

		return users.iterator();
	}

	public boolean addRoleToUser(String username, String rolename) {
		try {
			open();
			PreparedStatement statement = userDB.prepareStatement(ADD_ROLE_TO_USER);
			statement.setString(1, username);
			statement.setString(2, rolename);
			statement.execute();
			statement.close();
			close();
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	public boolean removeUser(User user) {
		return removeUser(user.getUsername());
	}

	public boolean removeUser(String username) {
		try {
			open();

			PreparedStatement deleteUserRoles = userDB.prepareStatement(DELETE_ALL_ROLES_FOR_USER);
			PreparedStatement deleteUser = userDB.prepareStatement(DELETE_USER);

			deleteUserRoles.setString(1, username);
			deleteUser.setString(1, username);

			deleteUserRoles.execute();
			deleteUser.execute();

			deleteUserRoles.close();
			deleteUser.close();

			close();	
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	/*
	 * Methods to initialise the default state of the database.
	 */

	private boolean createDefaultTables() {
		try {
			open();
			Statement statement = userDB.createStatement();
			statement.execute(CREATE_USERS_TABLE);
			statement.execute(CREATE_ROLES_TABLE);
			statement.execute(CREATE_USER_ROLES_TABLE);
			statement.execute(CREATE_USER_ROLES_USER_KEY);
			statement.execute(CREATE_USER_ROLES_ROLE_KEY);
			statement.close();
			close();
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	private boolean createDefaultUsers() {
		try {
			open();
			Statement statement = userDB.createStatement();
			statement.execute(ADD_DEFAULT_USERS);
			statement.close();
			close();	
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	private boolean createDefaultRoles() {
		try {
			open();
			Statement statement = userDB.createStatement();
			statement.execute(ADD_DEFAULT_ROLES);
			statement.close();
			close();
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	private void addDefaultRolesToDefaultUsers() {
		addRoleToUser("webgoat", "webgoat_admin");
		addRoleToUser("basic", "webgoat_user");
		addRoleToUser("basic", "webgoat_basic");
		addRoleToUser("guest", "webgoat_user");
	}
}