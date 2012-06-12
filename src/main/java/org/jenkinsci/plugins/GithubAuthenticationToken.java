/**
 The MIT License

Copyright (c) 2011 Michael O'Cleirigh

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.



 */
package org.jenkinsci.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import hudson.security.SecurityRealm;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.providers.AbstractAuthenticationToken;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

/**
 * @author mocleiri
 * 
 *         to hold the authentication token from the github oauth process.
 * 
 */
public class GithubAuthenticationToken extends AbstractAuthenticationToken {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String accessToken;

	private final String userName;
	private final GitHub gh;

    private final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

	public GithubAuthenticationToken(String accessToken, String githubServer) throws IOException {

		super(new GrantedAuthority[] {});

		this.accessToken = accessToken;
        this.gh = GitHub.connectUsingOAuth(githubServer, accessToken);

        GHUser me = gh.getMyself();
        assert me!=null;

        setAuthenticated(true);

        this.userName = me.getLogin();
        authorities.add(SecurityRealm.AUTHENTICATED_AUTHORITY);
        for (GHOrganization org : me.getOrganizations()) {
            authorities.add(new GrantedAuthorityImpl(org.getLogin()));
        }
	}

    /**
     * Gets the OAuth access token, so that it can be persisted and used elsewhere.
     */
    public String getAccessToken() {
        return accessToken;
    }

    public GitHub getGitHub() {
        return gh;
    }

    @Override
    public GrantedAuthority[] getAuthorities() {
        return authorities.toArray(new GrantedAuthority[authorities.size()]);
    }

	public Object getCredentials() {
		return ""; // do not expose the credential
	}

    /**
     * Returns the login name in GitHub.
     */
	public String getPrincipal() {
		return this.userName;
	}

	/**
	 * For some reason I can't get the github api to tell me for the current
	 * user the groups to which he belongs.
	 * 
	 * So this is a slightly larger consideration. If the authenticated user is
	 * part of any team within the organization then they have permission.
	 * 
	 * @param candidateName
	 * @param organization
	 * @return
	 */
	public boolean hasOrganizationPermission(String candidateName,
			String organization) {

		try {

			Map<String, GHOrganization> myOrgsMap = gh.getMyOrganizations();

			if (myOrgsMap.keySet().contains(organization))
				return true;

			return false;

		} catch (IOException e) {

			throw new RuntimeException("authorization failed for user = "
					+ candidateName, e);

		}
	}

	private static final Logger LOGGER = Logger
			.getLogger(GithubAuthenticationToken.class.getName());

	public GHUser loadUser(String username) throws IOException {
		if (gh != null && isAuthenticated())
			return gh.getUser(username);
		else
			return null;
	}

	public GHOrganization loadOrganization(String organization)
			throws IOException {

		if (gh != null && isAuthenticated())
			return gh.getOrganization(organization);
		else
			return null;

	}

	public GHTeam loadTeam(String organization, String team) throws IOException {
		if (gh != null && isAuthenticated()) {

			GHOrganization org = gh.getOrganization(organization);

			if (org != null) {
				Map<String, GHTeam> teamMap = org.getTeams();

				return teamMap.get(team);
			} else
				return null;

		} else
			return null;
	}
}
