package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.LinkedList;

import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Entity.Builder;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;
import pt.unl.fct.di.adc.firstwebapp.util.UserData;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UsersResource {

	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private final Gson g = new Gson();
	private static final Logger LOG = Logger.getLogger(UsersResource.class.getName());

	public UsersResource() {
	}

	@POST
	@Path("/register")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerUser(RegisterData data) {
		LOG.fine("Register attempt by user: " + data.getUsername());

		// Checks input data
		if (!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		Transaction txn = datastore.newTransaction();

		Key userKey = datastore.newKeyFactory().setKind("user").newKey(data.getUsername());
		Entity user = txn.get(userKey);

		try {
			if (user != null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User already exists.").build();

			} else {
				user = Entity.newBuilder(userKey).set("user_name", data.getName())
						.set("user_pwd", DigestUtils.sha512Hex(data.getPassword())).set("user_email", data.getEmail())
						.set("user_profile", "INDEFINIDO").set("user_landline", "INDEFINIDO")
						.set("user_mobilePhone", "INDEFINIDO").set("user_address", "INDEFINIDO")
						.set("user_NIF", "INDEFINIDO").set("user_role", "USER").set("user_status", "INATIVO").build();

				txn.add(user);
				LOG.info("User " + data.getUsername() + " registered");
				txn.commit();
				return Response.status(Status.CREATED).entity("New User Registered Successfully").build();
			}

		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@DELETE
	@Path("/remove")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response removeUser(RegisterData data) {
		LOG.fine("Attempt to remove user: " + data.getUsername());

		Transaction txn = datastore.newTransaction();

		Key userRemovingKey = datastore.newKeyFactory().setKind("user").newKey(data.getUsername());
		Entity uRemoving = txn.get(userRemovingKey);

		Key userBeingRemovedKey = datastore.newKeyFactory().setKind("user").newKey(data.getUserToBeRemoved());
		Entity uBeingRemoved = txn.get(userBeingRemovedKey);

		Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(data.getAuthToken().getTokenID());
		Entity token = txn.get(tokenKey);

		try {

			if (!validToken(token, txn))
				return Response.status(Status.FORBIDDEN).entity("Invalid Token").build();

			if (uRemoving == null || uBeingRemoved == null) {
				txn.rollback();

				return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
			}

			if (!validRemoval(uRemoving.getString("user_role"), uBeingRemoved.getString("user_role")))
				return Response.status(Status.FORBIDDEN).entity("Removal is not allowed.").build();

			txn.delete(userBeingRemovedKey);
			LOG.info("User " + data.getUserToBeRemoved() + " removed");
			txn.commit();
			return Response.status(Status.OK).entity("User Removed Successfully").build();

		} finally {
			if (txn.isActive())
				txn.rollback();
		}

	}

	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response loginUser(LoginData data) {
		LOG.fine("Attempt to login user: " + data.getUsername());

		Transaction txn = datastore.newTransaction();

		Key userKey = datastore.newKeyFactory().setKind("user").newKey(data.getUsername());
		Entity user = txn.get(userKey);

		try {
			if (user == null || user.getString("user_status").equals("INATIVO")) {
				return Response.status(Status.BAD_REQUEST).entity("User does not exists or is not valid.").build();
			}

			AuthToken authToken = new AuthToken(data.getUsername(), user.getString("user_role"));
			Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(authToken.getTokenID());

			String hashedPWD = user.getString("user_pwd");
			if (hashedPWD.equals(DigestUtils.sha512Hex(data.getPassword()))) {

				Entity token = Entity.newBuilder(tokenKey).set("username", authToken.getUsername())
						.set("role", user.getString("user_role")).set("creationData", authToken.getCreationData())
						.set("expirationData", authToken.getExpirationData()).set("magicNumber", authToken.getTokenID())
						.build();

				txn.add(token);
				LOG.info("User " + data.getUsername() + " logged in successfully.");
				txn.commit();
				return Response.ok(g.toJson(authToken)).status(Status.OK).build();
			}
			LOG.warning("Wrong password for username: " + data.getUsername());
			return Response.status(Status.FORBIDDEN).entity("Login not allowed").build();
		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@PUT
	@Path("/modAttribs")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response modifyUserAttributes(RegisterData data) {
		LOG.fine("Attemp to modify user " + data.getUsername() + " attributes");

		Transaction txn = datastore.newTransaction();

		Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(data.getAuthToken().getTokenID());
		Entity token = txn.get(tokenKey);

		Key userKey = datastore.newKeyFactory().setKind("user").newKey(data.getUsername());
		Entity user = datastore.get(userKey);

		try {
			if (!validToken(token, txn))
				return Response.status(Status.FORBIDDEN).entity("Invalid Token").build();

			if (user == null) {
				return Response.status(Status.BAD_REQUEST).entity("User does not exist").build();
			}

			Builder userBuilder = Entity.newBuilder(user);

			boolean canModify = false;
			if (token.getString("username").equals(user.getKey().getName())
					&& user.getString("user_role").equals("USER"))
				canModify = true;

			if (token.getString("role").equals("SU") && !user.getString("user_role").equals("SU")
					&& !data.getRole().equals("SU"))
				canModify = true;

			if (token.getString("role").equals("GS") && !user.getString("user_role").equals("SU")
					&& !user.getString("user_role").equals("GS") && !data.getRole().equals("SU")
					&& !data.getRole().equals("GS"))
				canModify = true;

			if (token.getString("role").equals("GBO") && user.getString("user_role").equals("USER")
					&& data.getRole().equals("USER"))
				canModify = true;

			if (canModify && data.validProfile() && data.validStatus() && data.validStrings())
				userBuilder.set("user_profile", data.getProfile()).set("user_landline", data.getLandline())
						.set("user_mobilePhone", data.getMobilePhone()).set("user_address", data.getAddress())
						.set("user_NIF", data.getNIF()).set("user_status", data.getStatus());
			else {
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("Modification failed").build();
			}

			if (!token.getString("role").equals("USER") && data.validRole())
				userBuilder.set("user_name", data.getName()).set("user_email", data.getEmail()).set("user_role",
						data.getRole());

			Entity modifiedUser = userBuilder.build();
			txn.put(modifiedUser);
			LOG.info("User " + data.getUsername() + " updated successfully.");
			txn.commit();
			return Response.status(Status.CREATED).entity("Updated Successfully").build();

		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@PUT
	@Path("/modPwd")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response modifyUserPassword(RegisterData data) {
		LOG.fine("Attemp to modify user " + data.getUsername() + " password");

		Transaction txn = datastore.newTransaction();

		Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(data.getAuthToken().getTokenID());
		Entity token = txn.get(tokenKey);

		try {
			if (!validToken(token, txn))
				return Response.status(Status.FORBIDDEN).entity("Invalid Token").build();

			Key userKey = datastore.newKeyFactory().setKind("user").newKey(token.getString("username"));
			Entity user = datastore.get(userKey);

			if (user == null) {
				return Response.status(Status.BAD_REQUEST).entity("User does not exist").build();
			}

			Builder userBuilder = Entity.newBuilder(user);

			String hashedPassGiven = DigestUtils.sha512Hex(data.getPassword());

			// Check if given password is valid and equal to current password
			// Check if new password is equal to confirmation (validPassword)
			if (data.getPassword() != null && hashedPassGiven.equals(user.getString("user_pwd"))
					&& data.validNewPassword())
				userBuilder.set("user_pwd", DigestUtils.sha512Hex(data.getNewPassword()));
			else
				return Response.status(Status.FORBIDDEN).entity("Password can not be changed").build();

			Entity modifiedUser = userBuilder.build();
			txn.put(modifiedUser);
			LOG.info("User " + data.getUsername() + " updated successfully.");
			txn.commit();
			return Response.status(Status.CREATED).entity("Password updated Successfully").build();

		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@GET
	@Path("/listUsers")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listUsers(AuthToken at) {
		Transaction txn = datastore.newTransaction();

		Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(at.getTokenID());
		Entity token = txn.get(tokenKey);

		try {
			if (!validToken(token, txn))
				return Response.status(Status.FORBIDDEN).entity("Invalid Token").build();

			Query<Entity> query = null;
			if (token.getString("role").equals("USER")) {
				query = Query.newEntityQueryBuilder().setKind("user")
						.setFilter(CompositeFilter.and(
								CompositeFilter.and(PropertyFilter.eq("user_role", "USER"),
										PropertyFilter.eq("user_profile", "Público")),
								PropertyFilter.eq("user_status", "ATIVO")))
						.build();
			}

			if (token.getString("role").equals("GBO")) {
				query = Query.newEntityQueryBuilder().setKind("user").setFilter(PropertyFilter.eq("user_role", "USER"))
						.build();
			}

			if (token.getString("role").equals("GS")) {
				query = Query.newEntityQueryBuilder().setKind("user").setFilter(PropertyFilter.eq("user_role", "USER"))
						.setFilter(PropertyFilter.eq("user_role", "GBO")).build();
			}

			if (token.getString("role").equals("SU")) {
				query = Query.newEntityQueryBuilder().setKind("user").build();
			}

			QueryResults<Entity> queryResults = datastore.run(query);
			List<UserData> users = new LinkedList<>();
			queryResults.forEachRemaining(userEntity -> {
				UserData newUser = null;
				if (token.getString("role").equals("USER"))
					newUser = new UserData(userEntity.getKey().getName(), "", userEntity.getString("user_email"),
							userEntity.getString("user_name"), "", "", "", "", "", "", "");
				else
					newUser = new UserData(userEntity.getKey().getName(), userEntity.getString("user_pwd"),
							userEntity.getString("user_email"), userEntity.getString("user_name"),
							userEntity.getString("user_profile"), userEntity.getString("user_landline"),
							userEntity.getString("user_mobilePhone"), userEntity.getString("user_address"),
							userEntity.getString("user_NIF"), userEntity.getString("user_role"),
							userEntity.getString("user_status"));
				users.add(newUser);
			});
			return Response.ok(g.toJson(users)).build();

		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@GET
	@Path("/listToken")
	public Response listToken(AuthToken at) {
		LOG.fine("Attemp to list token " + at.getTokenID());

		Transaction txn = datastore.newTransaction();

		Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(at.getTokenID());
		Entity token = txn.get(tokenKey);

		try {
			if (!validToken(token, txn))
				return Response.status(Status.FORBIDDEN).entity("Invalid Token").build();

			AuthToken authToken = new AuthToken(at.getUsername(), at.getTokenID(), at.getCreationData(),
					at.getExpirationData(), at.getRole());

			txn.commit();
			return Response.ok(g.toJson(authToken)).build();
		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@DELETE
	@Path("/logout")
	public Response logoutUser(AuthToken at) {
		LOG.fine("Attemp to logout user " + at.getUsername());

		Transaction txn = datastore.newTransaction();

		Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(at.getTokenID());
		Entity token = txn.get(tokenKey);

		try {
			if (!validToken(token, txn))
				return Response.status(Status.FORBIDDEN).entity("Invalid Token").build();

			txn.delete(tokenKey);
			LOG.info("Token " + tokenKey + " deleted successfully.");
			txn.commit();
			return Response.status(Status.OK).entity("Logout executed Successfully").build();
		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	private boolean validToken(Entity token, Transaction txn) {
		if (token == null)
			return false;

		if (token.getLong("creationData") > token.getLong("expirationData")) {
			txn.delete(token.getKey());
			return false;
		}
		return true;
	}

	private boolean validRemoval(String userRemoving, String userBeingRemoved) {
		if (userRemoving.equals("SU"))
			return true;

		if (userRemoving.equals("GS") && (userBeingRemoved.equals("USER") || userBeingRemoved.equals("GBO")))
			return true;

		if (userRemoving.equals("GBO") && userBeingRemoved.equals("USER"))
			return true;

		if (userRemoving.equals("USER") && userBeingRemoved.equals("USER") && userRemoving.equals(userBeingRemoved))
			return true;

		return false;
	}
}