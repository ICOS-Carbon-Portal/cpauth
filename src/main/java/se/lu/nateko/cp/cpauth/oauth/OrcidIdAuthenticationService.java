package se.lu.nateko.cp.cpauth.oauth;

import se.lu.nateko.cp.cpauth.OAuthProviderConfig;


/**
 * Created by paul on 2017-04-06.
 */
public class OrcidIdAuthenticationService {

	private final OAuthProviderConfig config;
	private final String serviceHost;
	
	public OrcidIdAuthenticationService(OAuthProviderConfig config, String serviceHost){
		this.config = config;
		this.serviceHost = serviceHost;
	}

	public UserInfo retrieveUserInfo(String code) {

		System.out.println("The code is: " + code);

		// url "https://orcid.org/oauth/token"
		// client_secret
		// grant_type authorization_code
		// redirect_uri https://cpauth.icos-cp.eu/
		// code


		return null;
	}
}
