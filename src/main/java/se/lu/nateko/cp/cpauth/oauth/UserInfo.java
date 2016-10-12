package se.lu.nateko.cp.cpauth.oauth;

public class UserInfo {

	public final String email;
	public final String givenName;
	public final String surname;

	public UserInfo(String givenName, String surname, String email){
		this.givenName = givenName;
		this.surname = surname;
		this.email = email;
	}
	
}
