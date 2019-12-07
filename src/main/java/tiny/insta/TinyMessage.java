package tiny.insta;

import java.util.ArrayList;
import java.util.List;

public class TinyMessage
{
	public String message;
	public List<String> messages;
	public String error;
	
	public TinyMessage()
	{
		this.message = new String();
		this.messages = new ArrayList<String>();
	}
}
