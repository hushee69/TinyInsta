/*
 * Author: Harry Jandu
 */

package tiny.insta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

@Api(
	name = "tinyInstaApi",
	version = "v1",
	namespace = @ApiNamespace(
		ownerDomain = "tinyinsta.example.com",
		ownerName = "tinyinsta.example.com",
		packagePath = ""
	)
)
public class TinyInstaEndpoint
{
	@ApiMethod(name = "registerUser")
	public TinyUser registerUser(@Named("email") String email, @Named("username") String username, @Named("password") String password)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		Query query = new Query("users");
		
		query.setFilter(new FilterPredicate("email", FilterOperator.EQUAL, email));
		
		PreparedQuery pq = ds.prepare(query);
		Entity e = pq.asSingleEntity();
		
		TinyUser tu = new TinyUser();
		
		// if we found a user
		if( e != null )
		{
			tu.error = "Email exists";
			
			return tu;
		}
		
		tu = new TinyUser(email, username, password);
		
		Key email_key = KeyFactory.createKey("users", email);
		e = new Entity(email_key);
		
		e.setProperty("email", email);
		e.setProperty("username", username);
		e.setProperty("password", password);
		
		ds.put(e);
		
		return tu;
	}
	
	@ApiMethod(name = "newUser")
	public TinyUser newUser(@Named("email") String email)
	{
		TinyUser tu = new TinyUser();
		
		tu.email = email;
		
		return tu;
	}
	
	@ApiMethod(name = "authenticateUser")
	public TinyUser authenticateUser(@Named("email") String email, @Named("password") String password)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		Query query = new Query("users");
		
		query.setFilter(
			new Query.CompositeFilter(
				CompositeFilterOperator.AND, 
				Arrays.asList(
					new FilterPredicate("email", FilterOperator.EQUAL, email.toLowerCase()), 
					new FilterPredicate("password", FilterOperator.EQUAL, password)
				)
			)
		);
		
		TinyUser ret = new TinyUser();
		PreparedQuery pq = ds.prepare(query);
		Entity user = pq.asSingleEntity();
		if( user != null )
		{
			ret.email = (String) user.getProperty("email");
			ret.username = (String) user.getProperty("username");
			ret.password = (String) user.getProperty("password");
			
			ds.put(user);
		}
		else
		{
			ret.error = "Username or password invalid";
		}
		
		return ret;
	}
	
	/*
	 * follower is the user that's logged in
	 * followee is the person that the user logged in wants to follow
	 */
	@ApiMethod(name = "followUser")
	public TinyUser followUser(@Named("follower") String follower, @Named("followee") String followee)
	{
		TinyUser tu = new TinyUser();
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		Query query = null;
		PreparedQuery pq = null;
		
		query = new Query("users");
		
		// get current user
		query.setFilter(new Query.FilterPredicate("email", FilterOperator.EQUAL, follower));
		pq = ds.prepare(query);
		Entity e = pq.asSingleEntity();
		
		if( e == null )
		{
			tu.error = "follower - is null";
			
			return tu;
		}
		
		List<String> follow_set = (ArrayList<String>) e.getProperty("following");
		if( follow_set == null )
		{
			tu.error = "following set null - creating follow set";
			
			follow_set = new ArrayList<String>();
		}
		follow_set.add(followee);
		e.setProperty("following", follow_set);
		ds.put(e);
		
		query.setFilter(new Query.FilterPredicate("email", FilterOperator.EQUAL, followee));
		pq = ds.prepare(query);
		e = pq.asSingleEntity();
		
		if( e == null )
		{
			tu.error = "followee - is null";
			
			return tu;
		}
		
		follow_set = (ArrayList<String>) e.getProperty("followers");
		if( follow_set == null )
		{
			tu.error = "follwers set null, creating set";
			
			follow_set = new ArrayList<String>();
		}
		follow_set.add(follower);
		e.setProperty("followers", follow_set);
		ds.put(e);
		
		return tu;
	}
	
	@ApiMethod(name = "postMessage")
	public TinyMessage postMessage(@Named("email") String email, @Named("message") String message)
	{
		TinyMessage tm = new TinyMessage();
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		Query query = new Query("users");
		
		query.setFilter(new FilterPredicate("email", FilterOperator.EQUAL, email));
		
		PreparedQuery pq = ds.prepare(query);
		Entity res = pq.asSingleEntity();
		Key user_key = res.getKey();
		if( user_key == null )
		{
			tm.error = "User non existent";
			
			return tm;
		}
		// get list of people that should see this messages
		List<String> followers = (ArrayList<String>) res.getProperty("followers");
		
		Entity posts = new Entity("posts", user_key);
		
		posts.setProperty("seers", followers);
		posts.setProperty("message", message);
		
		ds.put(posts);
		
		tm.message = message;
		
		return tm;
	}
	
	@ApiMethod(name = "getMessages")
	public TinyMessage getMessages(@Named("email") String email)
	{
		TinyMessage tm = new TinyMessage();
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		Query query = new Query("posts");
		query.setFilter(new FilterPredicate("seers", FilterOperator.EQUAL, email.toLowerCase()));
		
		PreparedQuery pq = ds.prepare(query);
		List<Entity> res = pq.asList(FetchOptions.Builder.withDefaults());
		
		for( Entity e : res )
		{
			String msg = (String) e.getProperty("message");
			tm.messages.add(msg);
		}
		
		return tm;
	}
	
	@ApiMethod(name = "getUsersList")
	public List<TinyUser> getUsersList()
	{
		List<TinyUser> tus = new ArrayList<TinyUser>();
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		Query query = new Query("users");
		PreparedQuery pq = ds.prepare(query);
		
		List<Entity> res = pq.asList(FetchOptions.Builder.withDefaults());
		
		for( Entity e : res )
		{
			String email = (String) e.getProperty("email");
			TinyUser cur = new TinyUser();
			
			cur.email = email;
			tus.add(cur);
		}
		
		return tus;
	}
}
