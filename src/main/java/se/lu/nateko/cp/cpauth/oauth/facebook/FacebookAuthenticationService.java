package se.lu.nateko.cp.cpauth.oauth.facebook;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;

import se.lu.nateko.cp.cpauth.FacebookConfig;
import se.lu.nateko.cp.cpauth.oauth.UserInfo;

public class FacebookAuthenticationService  {

	private final FacebookConfig config;
	private final String serviceHost;
	
	public FacebookAuthenticationService(FacebookConfig config, String serviceHost){
		this.config = config;
		this.serviceHost = serviceHost;
	}
	
    public String generateService() throws OAuthSystemException {
	    OAuthClientRequest request = OAuthClientRequest
	        .authorizationProvider(OAuthProviderType.FACEBOOK)
	        .setClientId(this.config.clientId())
	        .setRedirectURI("https://" + this.serviceHost + this.config.redirectUri())
	        .setScope("email, public_profile")
	        .buildQueryMessage();
	
	    return request.getLocationUri();
   
    }	

    public UserInfo retrieveUserInfo(String code) throws OAuthSystemException, OAuthProblemException {	
    	
    	OAuthClientRequest request = OAuthClientRequest
            	.tokenProvider(OAuthProviderType.FACEBOOK)
                .setGrantType(GrantType.AUTHORIZATION_CODE)
                .setClientId(this.config.clientId())
                .setClientSecret(this.config.clientSecret())
                .setRedirectURI("https://" + this.serviceHost + this.config.redirectUri())
                .setCode(code)
                .buildBodyMessage();
    	
        OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
        GitHubTokenResponse oAuthResponse = oAuthClient.accessToken(request, GitHubTokenResponse.class);   
        String accessToken = oAuthResponse.getAccessToken();
        
        OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest("https://graph.facebook.com/me?fields=first_name,last_name,email")
                .setAccessToken(accessToken)
                .buildQueryMessage(); 
       
        OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
        
        java.util.Map<String, Object> json = JSONUtils.parseJSON(resourceResponse.getBody());
               
    	UserInfo user = new UserInfo(json.get("first_name").toString(), json.get("last_name").toString(), json.get("email").toString());
		return user;
	}  
}
