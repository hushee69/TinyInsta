package tiny.insta;

import java.io.Serializable;

public class TinyUser implements Serializable
{
	public String email, username, password;
	public String error;
	
	public TinyUser() {}
	
	public TinyUser(String email, String username, String password)
	{
		this.email = email;
		this.username = username;
		this.password = password;
	}
}
